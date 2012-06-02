package tim.pokexcel;

import android.os.Handler;

/*
 * 指定したファイルに対して行う処理を設定するクラス
 * filePathを変更してからaction()を実行する
 * actionをオーバーライドすることで処理を実装する
 */
abstract class ActionSelectedFile{
	
	// ファイルパス
	String filePath = null;
	
	// new した時に呼ばれる処理
	public ActionSelectedFile(){
		//Log.d(TAG, "ActionSelectedFile created");
	}
	
	// 実行する動作（オーバーライド用）
	protected abstract void action();
	
	// 指定した処理を実行する
	public void run(){
		new Handler().post(new Runnable() {
			
			@Override
			public void run() {
				action();
			}
		});
	}
}
