package tim.pokexcel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.Session.AccessType;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Dropbox内のポケモン用エクセルシート操作アプリ
 * @author mibe
 */
public class PokExcelActivity extends Activity {
	
	// ログ出力用タグ
	private static final String TAG = "PokExcelActivity";
	
	/////////////////////////////////////////////////////////
	// ここからアプリ固有の設定                            //
	// 1. それぞれの値はDropboxのWebサイトで設定・取得する //
	// 2. 他のアプリと共有できない（チェックしている模様） //
	/////////////////////////////////////////////////////////
	
	// アプリの識別キーと認証パス
	private String app_key = null;
	private String app_sec = null;
	
	// アクセスタイプ（一部or全体）
	final static private AccessType ACCESS_TYPE = AccessType.DROPBOX;
	
	////////////////////////////
	// ここからグローバル変数 //
	////////////////////////////

	// 設定取得用
	private SharedPreferences sp = null;
	
	// Dropboxコントローラ
	private DropBoxController dbc = null;
	
	// アクティビティ
	private Activity activity = this;
	
	// Dropbox用セッション
	AndroidAuthSession session;
	
	// ログイン状態，Trueのときログイン中
	private boolean mLoggedIn = false;
	
	//////////////////////////
	// ここからメソッド宣言 //
	//////////////////////////
	
	// アクティビティ作成時に呼ばれるメソッド
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		setContentView(R.layout.main);
		
		if (savedInstanceState != null) {
			//mCameraFileName = savedInstanceState.getString("mCameraFileName");
		}
		// ウィンドウ名を変更する
		setWindowTitle();
		
		// 設定取得用インターフェイスを取得する（失敗したらアプリ終了）
		sp = getSharedPreferences(getString(R.string.app_config_configFileName), MODE_PRIVATE);
		if(sp == null){
			myFinal(getString(R.string.common_error), "error getSharedPreferences()");
			return;
		}
		
		// アプリの識別キーと認証パスを取得する
		app_key = getString(R.string.app_key);
		app_sec = getString(R.string.app_sec);
		
		// Dropboxコントローラを作成する
		dbc = new DropBoxController(activity, app_key, app_sec, ACCESS_TYPE);
		dbc.setup();
		
		// セッションを作成する
		session = dbc.mApi.getSession();
		
		// ログイン状態を取得し，アプリに反映させる
		setLoggedIn(session.isLinked());
		//*/
	}
	
	// アクティビティのレジューム時に呼ばれるメソッド
	// Dropbox認証アクティビティ終了後にも実行されるっぽい
	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
		
		// check session
		if(session == null){
			Log.d(TAG, "onResume: session = null");
			return;
		}
		
		// The next part must be inserted in the onResume() method of the
		// activity from which session.startAuthentication() was called, so
		// that Dropbox authentication completes properly.
		if (session.authenticationSuccessful()) {
			try {
				// Mandatory call to complete the auth
				session.finishAuthentication();
				
				// Store it locally in our app for later use
				dbc.storeKeys();
				setLoggedIn(true);
			} catch (IllegalStateException e) {
				showToast("Couldn't authenticate with Dropbox:" + e.getLocalizedMessage());
				Log.i(TAG, "Error authenticating", e);
			}
		}
		
		// ログインしていない時，処理を中断する
		if(!mLoggedIn)return;
		
		// PokExcelファイルを展開する
		// デバッグ中はここはコメントアウトしておく
		//setPokExcel();
	}
	
	/*
	 * ログイン状態を指定し，アプリに反映させる
	 * 新しいログイン状態に合わせて画面や設定を変更するのもここ
	 * loggedIn : ログイン状態，ログインしているときTrue
	 */
	private void setLoggedIn(boolean loggedIn) {
		Log.d(TAG, "setLoggedIn");
		TextView textView = (TextView)findViewById(R.id.textVuew);
		

		textView.setVisibility(TextView.VISIBLE);
		
		mLoggedIn = loggedIn;
		if (loggedIn) {
			// ログインしているとき
			
			//setTextView(textView);
			//textView.setVisibility(TextView.VISIBLE);
			
			textView.setText("login");
		} else {
			//textView.setVisibility(TextView.GONE);
			
			textView.setText("logout");
		}
	}
	
	// メニューボタンを最初に押したときの処理
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		super.onCreateOptionsMenu(menu);
		Log.d(TAG, "onCreateOptionsMenu");
		
		getMenuInflater().inflate(R.menu.menu, menu);
		
		return true;
	}
	
	// メニューボタンの内容を選択した時の処理
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		int id = item.getItemId();
		Log.d(TAG, "onOptionsItemSelected: id = " + id);
		
		// 項目のIDで分岐
		switch(item.getItemId()) {
		case R.id.menuItem_option:
			// 設定ボタンが押された時
			return onOptionSelected();
		}
		
		// 未設定な場合はここ
		return false;
	}
	
	// オプションボタンが選択された時の処理
	private boolean onOptionSelected(){
		Log.d(TAG, "onOptionSelected");
		
		// 設定の一覧
		CharSequence[] optionList = new CharSequence[]{
				getString(R.string.dialog_config_loginDropbox),
				getString(R.string.dialog_config_setPokExcel)
		};
		
		// 既にPokExcelファイルが指定されているとき
		if(mLoggedIn){
			
			// 設定テキストの変更
			optionList[0] = getString(R.string.dialog_config_logoutDropbox);
		}
		
		// 設定の番号
		final int dialog_config_setDropbox =  0x0;
		final int dialog_config_setPokExcel =  0x1;
		
		// オプションメニューを表示する
		new AlertDialog.Builder(activity)
		.setTitle(R.string.dialog_config_title)
		.setItems(optionList, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				// 選択したボタンによって分岐する
				switch(which){
				case dialog_config_setDropbox:		// アカウント設定が選択されたとき
					// Dropboxログイン状態を設定する
					setDropbox();
					break;
				case dialog_config_setPokExcel:		// ファイル指定が選択されたとき
					// データベース用のファイルを指定する
					selectPokExcelFile();
					break;
				}
			}
		})
		.show();
		
		return false;
	}
	
	// Dropboxのログイン状態を設定する
	private boolean setDropbox(){
		Log.d(TAG, "setDropbox");
		
		// 既にログインしている時
		if(mLoggedIn){
			
			// ログアウトを確認するダイアログを表示する
			new AlertDialog.Builder(activity)
			.setTitle(R.string.dialog_logout_title)
			.setPositiveButton(R.string.common_run, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// ログアウトし，状態を未接続に変更する
					dbc.logOut();
					setLoggedIn(false);
				}
			})
			.setNegativeButton(R.string.common_cancel, null)
			.show();
			
			return false;
		}
		
		// 認証を実行する（ログインアクティビティ起動）
		dbc.startAuthentication();
		
		return true;
	}
	
	// PokExcelファイルを指定する
	private boolean selectPokExcelFile(){
		Log.d(TAG, "selectPokExcelFile");
		
		// ログインしていないとき
		if(!mLoggedIn){
			
			// トースト出力して処理を中断する
			showToast("err: notlogin Dropbox");
			return false;
		}
		
		// パスを指定した後の処理を作成する
		ActionSelectedFile action = new ActionSelectedFile(){
			@Override
			protected void action() {
				actionSelectedPokExcel(filePath);
			}
		};
		
		// 既に設定されているパスを取得する
		String filePath = sp.getString(getString(R.string.app_config_PokExcelFilePath), null);
		if(filePath != null){
			filePath = new File(filePath).getParent();
		} else {
			filePath = "/";
		}
		
		
		// ファイルを指定して，処理を実行させる（ブロックしない）
		dbc.selectFilePath(filePath, action);
		
		return false;
	}
	
	/*
	 *  PokExcelファイルを指定した後の処理
	 *  filePath : 
	 */
	private void actionSelectedPokExcel(String filePath){
		
		// 設定にファイル名とそのパスを追加する
		sp.edit()
		.putString(getString(R.string.app_config_PokExcelFilePath), filePath)
		.commit();
		
		// PokExcelファイルを取得，準備する
		setPokExcel();
	}
	
	// PokExcelファイルを取得，展開する
	private void setPokExcel(){
		Log.d(TAG, "setPokExcel");
		
		// PokExcelファイルの名前とパスを取得する
		final String dbFilePath = sp.getString(getString(R.string.app_config_PokExcelFilePath), null);
		
		// ファイルパスが存在しないとき，処理を中断する
		if(dbFilePath == null)return;
		
		// ファイル名を取得する（アプリのローカルフォルダ）
		String fileName = "temp.xlsm";
		final String localFilePath = getFilesDir() + "/" + fileName;
		
		// タイトルを変更する
		setWindowTitle("pull PokExcel");
		
		// ダウンロードと展開の処理を設定する
		new Handler().post(new Runnable() {
			@Override
			public void run() {
				
				// PokExcelをダウンロードする（失敗したら処理終了）
				if(!pullPokExcel(dbFilePath, localFilePath))return;
				
				// PokExcelを展開する
				openPokExcel(localFilePath);
			}
		});
	}
	
	/*
	 * PokExcelをダウンロードする
	 * dbFilePath    : Dropbox上のファイルパス
	 * localFilePath : ローカルのファイルパス
	 * 戻り値        : 成功したらTrue
	 * 注意. 成功，失敗にかかわらず，ウィンドウタイトルを修正すること
	 */
	protected boolean pullPokExcel(String dbFilePath, String localFilePath) {
		Log.d(TAG, "pullPokExcel : " + dbFilePath + " -> " + localFilePath);
		
		// ダウンロード用のストリーム
		FileOutputStream fos = null;
		
		// ローカルファイル用のストリームを生成する
		try {
			fos = new FileOutputStream(localFilePath);
		} catch (FileNotFoundException e) {
			Log.d(TAG, e.toString());
		}
		
		// ストリームが生成出来なかったとき，
		if(fos == null){			
			// タイトルをデフォルトに変更する
			setWindowTitle();
			
			// タイトルに3秒間エラーを表示する
			setWindowTitle("Err", 3000);
			
			// 処理を中断する
			return false;
		}
		
		// ファイルをダウンロードする
		boolean result = dbc.getFile(dbFilePath, null, fos);

		// ストリームを閉じる
		try {
			fos.close();
		} catch (IOException e) {
			Log.d(TAG, e.toString());
		}
		
		// ダウンロードに失敗した時
		if(!result){
			// タイトルをデフォルトに変更する
			setWindowTitle();
			
			// タイトルに3秒間エラーを表示する
			setWindowTitle("Err", 3000);
			
			// 処理を中断する
			return false;
		}
		
		// タイトルを元に戻す
		setWindowTitle();
		
		// ダウンロード成功を3秒間表示する
		setWindowTitle("Pull OK", 3000);
		
		// 成功
		return true;
	}
	
	// ローカルのPokExcelファイルを展開する
	protected void openPokExcel(String localFilePath) {
		Log.d(TAG, "openPokExcel: localFilePath = " + localFilePath);
		
		
		
	}

	// 簡易トースト
	private void showToast(String msg) {
		Log.d(TAG, "showToast: msg = " + msg);
		
		Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
		error.show();
	}
	
	/*
	 * ウィンドウのタイトルを変更する
	 * msg   : 任意 アプリ名の後ろに続く文字列．nullの時無視する
	 * delay : 任意 変更をしている時間をミリ秒で指定
	 */
	private void setWindowTitle(){
		setWindowTitle(null);
	}
	private void setWindowTitle(String msg){
		
		// ウィンドウに使う文字列
		String text = getString(R.string.window_name);
		
		// msgが存在するとき，末尾に追加する
		if(msg != null)text = text + " - " + msg;
		
		// ウィンドウ名を変更する
		setTitle(text);
	}
	private void setWindowTitle(String msg, long delay){
		
		// もともとのタイトルを取得する
		final String oldText = getTitle().toString();
		
		// 新しいタイトルに変更する
		setWindowTitle(msg);
		
		// 一転時間後に，元のタイトルに戻す
		new Handler().postDelayed(new Runnable() {
			
			@Override
			public void run() {
				// 元のタイトルに戻す
				setTitle(oldText);
			}
		}, delay);
		
	}
	
	/*
	 *  エラーを出力してアプリを終了させる
	 *  title   : ダイアログのタイトル
	 *  message : ダイアログのメッセージ
	 */
	private void myFinal(String title, String message){
		
		Log.d(TAG, "serverBootFailed");
		
		// DB起動失敗のダイアログを表示する
		new AlertDialog.Builder(activity)
		.setTitle(title)
		.setMessage(message)
		.setCancelable(false)
		.setPositiveButton(R.string.common_end, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// アプリを正常終了させる
				finish();
			}
		})
		.show();		
	}
}
