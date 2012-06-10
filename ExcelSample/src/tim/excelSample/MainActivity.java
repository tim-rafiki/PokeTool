package tim.excelSample;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		String filePath = "/sdcard/hoge.xlsm";
		
		ExcelController controller = new ExcelController(filePath);
		
		Log.d(TAG, controller.getFilePath());
		
		controller.createFile();
	}
}
