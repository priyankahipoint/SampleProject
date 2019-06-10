package com.qualitick.android.os;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;

public class DialogActivity extends Activity{

	private static final int CODE = 2;
	private TextView tv;
	private String provider;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
        Log.i(getClass().getSimpleName(), "onCreate Activity - Dialog");
		setContentView(R.layout.test_layout);
		tv = (TextView) findViewById(R.id.textView1);

	}


	@Override
	protected void onResume() {
		super.onResume();
        Log.i(getClass().getSimpleName(), "onResume Activity - Dialog");
		provider = getIntent().getExtras().getString("provider");
		AlertDialog.Builder builder = new AlertDialog.Builder(DialogActivity.this);
		builder.setMessage("Please enable " + provider + " location settings")
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				startActivityForResult(settingsIntent, CODE);
			}
		});
		builder.show();
	}


	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i(getClass().getSimpleName(), "Activity Result");
		if (CODE == requestCode){
			if (resultCode == Activity.RESULT_OK) {
				// Has become the device administrator.
				tv.setText("You may now use your Android device");
				this.finish();
			} else {
				//Canceled or failed.
				tv.setText("Please activate " + provider + " location settings");
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

}
