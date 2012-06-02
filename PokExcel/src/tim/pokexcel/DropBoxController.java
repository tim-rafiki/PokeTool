package tim.pokexcel;

import java.io.File;
import java.io.OutputStream;
import java.util.List;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.android.AuthActivity;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.TokenPair;
import com.dropbox.client2.session.Session.AccessType;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

/**
 * Dropboxを操作するクラス
 * @author mibe
 *
 */
public class DropBoxController {
	
	// ログ出力用タグ
	private static final String TAG = "DropBoxController";
	
	/////////////////////////////////////////////////////////
	// ここからアプリ固有の設定                            //
	// 1. それぞれの値はDropboxのWebサイトで設定・取得する //
	// 2. 他のアプリと共有できない（チェックしている模様） //
	/////////////////////////////////////////////////////////
	
	// アプリの識別キーと認証パス
	private String app_key = "CHANGE_ME";
	private String app_secret = "CHANGE_ME_SECRET";
	
	// アクセスタイプ（一部or全体）
	private AccessType access_type = AccessType.APP_FOLDER;
	
	////////////////////
	// ここから定数値 //
	////////////////////
	
	final static private String ACCOUNT_PREFS_NAME = "prefs";
	final static private String ACCESS_KEY_NAME = "ACCESS_KEY";
	final static private String ACCESS_SECRET_NAME = "ACCESS_SECRET";
	
	final static int DIALOG_ID = 0;
	
	//////////////////
	// ここから変数 //
	//////////////////
	
	// 呼び出し元のアクティビティ
	private Activity activity = null;
	
	// Dropbox用API
	public DropboxAPI<AndroidAuthSession> mApi;
	
	// ファイル選択用のダイアログ
	private Dialog dialog = null;
	
	/*
	 * クラス作成時に呼ばれる処理
	 * activity   : 必須，呼び出し元のアクティビティ
	 * app_key    : 必須，アプリ識別用キー
	 * app_secret : 必須，アプリ認証用キー
	 * accessType : 任意，アプリのアクセス範囲（デフォルトで一部のみ）
	 */
	public DropBoxController(Activity activity, String app_key, String app_secret ){
		init(activity, app_key, app_secret, null);
	}
	
	public DropBoxController(Activity activity, String app_key, String app_secret, AccessType accessType){
		init(activity, app_key, app_secret, accessType);
	}
	
	/*
	 * クラス作成時に呼ばれる処理
	 */
	private void init(Activity activity, String app_key, String app_secret, AccessType accessType){
		Log.d(TAG, "init");
		
		// 必須の引数がnullのとき，エラーを出力する
		checkArgument(activity, "activity");
		checkArgument(app_key, "app_key");
		checkArgument(app_secret, "app_secret");
		
		// 引数をコピーする
		this.activity = activity;
		this.app_key = app_key;
		this.app_secret = app_secret;
		
		// 
		if(accessType != null)this.access_type = accessType;
	}
	
	/*
	 * 接続前の準備を行う
	 */
	public boolean setup(){
		Log.d(TAG, "setup");
		
		// セッションの作成
		AndroidAuthSession session = buildSession();
		mApi = new DropboxAPI<AndroidAuthSession>(session);
		
		checkAppKeySetup();
		
		// ここまで来たらTrue
		return true;
	}
	
	// AndroidAuthSessionを生成する
	private AndroidAuthSession buildSession() {
		Log.d(TAG, "buildSession");
		
		AppKeyPair appKeyPair = new AppKeyPair(app_key, app_secret);
		AndroidAuthSession session;
		
		String[] stored = getKeys();
		if (stored != null) {
			AccessTokenPair accessToken = new AccessTokenPair(stored[0], stored[1]);
			session = new AndroidAuthSession(appKeyPair, access_type, accessToken);
		} else {
			session = new AndroidAuthSession(appKeyPair, access_type);
		}
		
		return session;
	}
	
	/**
	 * Shows keeping the access keys returned from Trusted Authenticator in a local
	 * store, rather than storing user name & password, and re-authenticating each
	 * time (which is not to be done, ever).
	 *
	 * @return Array of [access_key, access_secret], or null if none stored
	 */
	private String[] getKeys() {
		Log.d(TAG, "getKeys");
		
		SharedPreferences prefs = activity.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
		String key = prefs.getString(ACCESS_KEY_NAME, null);
		String secret = prefs.getString(ACCESS_SECRET_NAME, null);
		if (key != null && secret != null) {
			String[] ret = new String[2];
			ret[0] = key;
			ret[1] = secret;
			return ret;
		} else {
			return null;
		}
	}
	
	private void checkAppKeySetup() {
		Log.d(TAG, "checkAppKeySetup");
		
		// Check to make sure that we have a valid app key
		if (app_key.startsWith("CHANGE") || app_secret.startsWith("CHANGE")) {
			showToast("You must apply for an app key and secret from developers.dropbox.com, and add them to the DBRoulette ap before trying it.");
			activity.finish();
			return;
		}
		
		// Check if the app has set up its manifest properly.
		Intent testIntent = new Intent(Intent.ACTION_VIEW);
		String scheme = "db-" + app_key;
		String uri = scheme + "://" + AuthActivity.AUTH_VERSION + "/test";
		testIntent.setData(Uri.parse(uri));
		PackageManager pm = activity.getPackageManager();
		if (0 == pm.queryIntentActivities(testIntent, 0).size()) {
			showToast("URL scheme in your app's " +
					"manifest is not set up correctly. You should have a " +
					"com.dropbox.client2.android.AuthActivity with the " +
					"scheme: " + scheme);
			activity.finish();
		}
	}
	
	public void logOut() {
		Log.d(TAG, "logOut");
		
		AndroidAuthSession session = mApi.getSession();
		
		// Remove credentials from the session
		session.unlink();
		
		// Clear our stored keys
		clearKeys();
	}
	
	private void clearKeys() {
		Log.d(TAG, "clearKeys");
		SharedPreferences prefs = activity.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
		Editor edit = prefs.edit();
		edit.clear();
		edit.commit();
	}
	
	public void startAuthentication(){
		Log.d(TAG, "startAuthentication");
		mApi.getSession().startAuthentication(activity);
	}
	
	/**
	 * Shows keeping the access keys returned from Trusted Authenticator in a local
	 * store, rather than storing user name & password, and re-authenticating each
	 * time (which is not to be done, ever).
	 */
	public void storeKeys() {
		Log.d(TAG, "storeKeys");
		
		TokenPair tokens = mApi.getSession().getAccessTokenPair();
		String key = tokens.key;
		String secret = tokens.secret;
		
		SharedPreferences prefs = activity.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
		Editor edit = prefs.edit();
		edit.putString(ACCESS_KEY_NAME, key);
		edit.putString(ACCESS_SECRET_NAME, secret);
		edit.commit();
	}
	
	/*
	 * Dropbox内のファイルorディレクトリを指定する
	 * action : ファイル決定してからの処理内容
	 */
	public void selectFilePath(final String path, final ActionSelectedFile action){
		
		Log.d(TAG, "selectFilePath");
		
		// ダイアログをいったん閉じる
		if(dialog != null){
			dialog.dismiss();
		}
		
		Entry diEntry = null;
		
		// ターゲット保存用変数
		final int[] mode = new int[1];
		mode[0] = -1;
		
		// 指定したパスの情報を取得する
		try {
			diEntry = mApi.metadata(path, 0, null, true, null);
			//List<Entry> fileList = diEntry.contents;
			
		} catch (DropboxException e) {
			Log.e(TAG, e.toString());
			return;
		}
		
		// エントリを取得できなかった時
		if(diEntry == null)return;
		
		// 指定したパスがファイルの時
		if(!diEntry.isDir){
			
			Log.d(TAG , "filePath = " + path);
			
			// ファイルパスを指定する
			action.filePath = path;
			
			// 処理を実行する
			action.run();
			
			// 処理を中断する
			return;
		}
		
		List<Entry> fileList = diEntry.contents;
		int size = fileList.size();
		
		// 指定したパスがディレクトリの時
		final CharSequence[] fileNameList = new CharSequence[size];
		
		int i = 0;
		for(final Entry entry : fileList){
			fileNameList[i] = entry.fileName();
			i++;
		}
		
		// 子ファイルorディレクトリ選択用のダイアログを作成・表示する
		dialog = new AlertDialog.Builder(activity)
		.setTitle("path = " + path)
		.setSingleChoiceItems(
				fileNameList,
				mode[0],
				new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mode[0] = which;
					}
				})
		.setPositiveButton("go", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				Log.d(TAG, "mode[0] = " + mode[0]);
				
				// 未選択の時，再度同じディレクトリを開く
				if(mode[0] == -1){
					selectFilePath(path, action);
					return;
				}
				
				// 選択したアイテム名を取得
				String newText = fileNameList[mode[0]].toString();
				
				// 次に開くパス
				String nextPath = null;
				
				// 元のパスがトップディレクトリの時
				if(path.equals("/")){
					// スラッシュなしでパスを追加する
					nextPath = path + newText;
				} else {
					// スラッシュありでパスを追加する
					nextPath = path + "/" + newText;
				}
				
				// 指定した新しいパスを展開する
				selectFilePath(nextPath, action);
			}
		})
		.setNeutralButton("parent", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {

				// 1階層上のフォルダを展開する（エラー出力なし）
				moveUpLevelDirectory(path, null, action);
			}
		})
		.setNegativeButton(R.string.common_cancel, null)
		.create();
		
		dialog.show();
	}
	
	/*
	 *  フォルダ，ディレクトリ展開失敗を表示し，1つ上のディレクトリを探索する
	 *  path   : 失敗したパス（この1階層上を再探索する）
	 *  text   : トースト出力するテキスト
	 *  action : ファイルが見つかった時の処理
	 */
	private void moveUpLevelDirectory(String path, String text, ActionSelectedFile action){
		
		// テキストが有効な時，展開失敗を表示する
		if(text !=null)Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
		
		// トップディレクトリの時，探索を終了する
		if(path.equals("/"))return;
		
		// 一つ上のディレクトリを探索する
		selectFilePath(new File(path).getParent(), action);
		
		return;
	}
	
	/*
	 * 指定したパスのファイルを，ストリームにダウンロードする
	 * path : 取得するファイルのDropbox上でのパス
	 * rev  : 取得するファイルのリビジョン，nullのときは最新版を取得
	 * os   : 書き込むファイルへのOutputStream
	 * プログレスリスナーは，Log.d以外がうまくできないので外部入力しない
	 * 戻り値 : 成功したらTrue
	 */
	public boolean getFile(String path, String rev, OutputStream os){
		Log.d(TAG, "getFile");
		
		try {
			mApi.getFile(path, rev, os, new ProgressListener(){
				@Override
				public void onProgress(long bytes, long total) {
					Log.d(TAG, "getFile : bytes=" + bytes + ", total=" + total);
				}
			});
			Log.d(TAG, "getFile : end");
		} catch (DropboxException e) {
			//e.printStackTrace();
			Log.e(TAG, e.toString());
			return false;
		}
		return true;
	}
	
	/*
	 * 引数チェック
	 * 引数がnullのとき，エラー出力をしてからエラーthrowする
	 * object : チェックする引数
	 * name   : エラー出力ようの名前
	 */
	private void checkArgument(Object object, String name){
		
		// 引数がNullのとき，エラー出力をしてからエラーthrowする
		if(object == null){
			Log.e(TAG, name + " = null");
			throw new NullPointerException();
		}
	}
	
	/*
	 * トースト出力
	 * text : 出力する文字列
	 */
	private void showToast(String text) {
		Log.d(TAG, "showToast");
		Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
	}
}
