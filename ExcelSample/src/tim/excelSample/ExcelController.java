package tim.excelSample;

import android.util.Log;

public class ExcelController {
	private static final String TAG = "ExcelController";
	
	private String filePath = null;
	
	public ExcelController(){
		onCreate(null);
	}
	public ExcelController(String filePath){
		onCreate(filePath);
	}
	
	private void onCreate(String filePath) {
		Log.d(TAG, "onCreate");
		
		if(filePath != null)this.filePath = filePath;
	}
	
	public void setFilePath(String filePath){
		this.filePath = filePath;
	}
	
	public String getFilePath(){
		return filePath;
	}
	
	public void createFile(){
		Log.d(TAG, "createFile");
		
		// TODO ここで実装
		
	}
}
