package tim.pokexcel;

import java.util.List;

import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.Session.AccessType;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
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
	
	// Dropboxコントローラ
	private DropBoxController dbc = null;
	
	// アクティビティ
	private Activity activity = this;
	
	// Dropbox用セッション
	AndroidAuthSession session;
	
	// ログイン状態，Trueのときログイン中
	private boolean mLoggedIn;
	
	// インターフェイスのボタン
	//private Button mSubmit;
	
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
		setTitle(R.string.window_name);
		
		// アプリの識別キーと認証パスを取得する
		app_key = getString(R.string.app_key);
		app_sec = getString(R.string.app_sec);
		
		dbc = new DropBoxController(activity, app_key, app_sec, ACCESS_TYPE);
		
		dbc.setup();
		
		session = dbc.mApi.getSession();
		
		// Display the proper UI state if logged in or not
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
	}
	
	/**
	 * Convenience function to change UI state based on being logged in
	 */
	private void setLoggedIn(boolean loggedIn) {
		Log.d(TAG, "setLoggedIn");
		TextView textView = (TextView)findViewById(R.id.textVuew);
		
		mLoggedIn = loggedIn;
		if (loggedIn) {
			//mSubmit.setText("Unlink from Dropbox");
			setTextView(textView);
			textView.setVisibility(TextView.VISIBLE);
		} else {
			//mSubmit.setText("Link with Dropbox");
			textView.setVisibility(TextView.GONE);
		}
	}
	
	// メニューボタンを最初に押したときの処理
	public boolean onCreateOptionsMenu(Menu menu){
		super.onCreateOptionsMenu(menu);
		Log.d(TAG, "onCreateOptionsMenu");
		
		getMenuInflater().inflate(R.menu.menu, menu);
		
		return true;
	}
	
	// メニューボタンの内容を選択した時の処理
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
				case dialog_config_setDropbox:
					// アカウント設定が選択されたとき
					
					// Dropboxログイン状態を設定する
					setDropbox();
					break;
				case dialog_config_setPokExcel:
					// ファイル指定が選択されたとき
					
					// データベース用のファイルを指定する
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
	
	// ファイルリストを作成する
	private void setTextView(TextView textView) {
		Log.d(TAG, "setTextView");
		
		try {
			Entry diEntry = dbc.mApi.metadata("/", 0, null, true, null);
			List<Entry> fileList = diEntry.contents;
			
			String text = "";
			for(final Entry entry : fileList){
				text = text + ",\n" + entry.path;
			}
			
			textView.setText(text);
			
		} catch (DropboxException e) {
			//e.printStackTrace();
			Log.e(TAG, e.toString());
		}
		
	}
	
	private void showToast(String msg) {
		Log.d(TAG, "showToast");
		
		Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
		error.show();
	}
}
