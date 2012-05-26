package tim.pokexcel;

import android.app.Activity;
import android.os.Bundle;

/**
 * Dropbox内のポケモン用エクセルシート操作アプリ
 * @author mibe
 *
 */
public class PokExcelActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
}