package tim.pokexcel;

import java.util.List;

import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.Session.AccessType;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
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
	final static private String APP_KEY = "CHANGE_ME";
	final static private String APP_SECRET = "CHANGE_ME_SECRET";
	
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
	private Button mSubmit;
	
	//////////////////////////
	// ここからメソッド宣言 //
	//////////////////////////
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		setContentView(R.layout.main);
		
		if (savedInstanceState != null) {
			//mCameraFileName = savedInstanceState.getString("mCameraFileName");
		}
		
		dbc = new DropBoxController(activity, APP_KEY, APP_SECRET, ACCESS_TYPE);
		
		dbc.setup();
		
		session = dbc.mApi.getSession();
		
		mSubmit = (Button)findViewById(R.id.auth_button);
		
		mSubmit.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// This logs you out if you're logged in, or vice versa
				if (mLoggedIn) {
					dbc.logOut();
					//logOut();
					setLoggedIn(false);
				} else {
					// Start the remote authentication
					dbc.startAuthentication();
				}
			}
		});
		
		// Display the proper UI state if logged in or not
		setLoggedIn(session.isLinked());
		//*/
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
		//AndroidAuthSession session = mApi.getSession();
		
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
			mSubmit.setText("Unlink from Dropbox");
			setTextView(textView);
			textView.setVisibility(TextView.VISIBLE);
		} else {
			mSubmit.setText("Link with Dropbox");
			textView.setVisibility(TextView.GONE);
		}
	}
	
	private void setTextView(TextView textView) {
		Log.d(TAG, "setTextView");
		
		try {
			Entry diEntry = dbc.mApi.metadata("/", 0, null, true, null);
			List<Entry> fileList = diEntry.contents;
			
			String text = "";
			for(final Entry entry : fileList){
				text = text + ", " + entry.path;
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
