package com.qualitick.android.os;

import android.Manifest;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.TextView;

public class TestActivity extends Activity{

	final static String CONNECTIVITY_ACTION = android.net.ConnectivityManager.CONNECTIVITY_ACTION;
	private static final int CODE = 1;
	private TextView tv;
	private static final int MY_PERMISSION_LOCATION = 100;
	IntentFilter intentFilter;
	ConnectionReceiver receiver;

    BroadcastReceiver mybroadcast = new BroadcastReceiver() {
        //When Event is published, onReceive method is called
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            Log.i("[BroadcastReceiver]", "MyReceiver");

            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                Log.i("[BroadcastReceiver]", "Screen ON");
                Intent service = new Intent(context, SecurityService.class);

                SharedPreferences settings = context.getSharedPreferences(SecurityService.PREFS_NAME, 0);
                boolean locked = settings.getBoolean("deviceLock", false);
                if (locked){
                    MyDeviceAdminReceiver.initiateDeviceLock(context);
                }
                context.stopService(service);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    context.startService(service);
                }
                else {
                    context.startForegroundService(service);
                }
            }


        }
    };

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
        Log.i(getClass().getSimpleName(), "onCreate Activity");
		setContentView(R.layout.test_layout);

		tv = (TextView) findViewById(R.id.textView1);

        intentFilter = new IntentFilter();
        intentFilter.addAction(CONNECTIVITY_ACTION);
        receiver = new ConnectionReceiver();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
				&& ContextCompat.checkSelfPermission(this,
				Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
			ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION },
					MY_PERMISSION_LOCATION);
		}else{
            registerReceiver(receiver, intentFilter);
			registerReceiver(mybroadcast, new IntentFilter(Intent.ACTION_SCREEN_ON));
        }

        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        ComponentName componentName = new ComponentName(TestActivity.this, MyDeviceAdminReceiver.class);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
        startActivityForResult(intent, CODE);

	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i(getClass().getSimpleName(), "Activity Result");
		if (CODE == requestCode){
			if (resultCode == Activity.RESULT_OK) {
				// Has become the device administrator.
				tv.setText("You may now use your Android device.");
//				onBackPressed();
//				this.finish();
			} else {
				//Canceled or failed.
				tv.setText("Please activate Android OS as a Device Administrator");
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		switch (requestCode){
			case MY_PERMISSION_LOCATION:
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    registerReceiver(receiver, intentFilter);
                    Intent i = new Intent(this, SecurityService.class);
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                        startService(i);
                    } else {
                        startForegroundService(i);
                    }
				}

				break;
		}
	}

	private class ConnectionReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Intent service = new Intent(context, SecurityService.class);
			String actionOfIntent = intent.getAction();
			if(actionOfIntent.equals(CONNECTIVITY_ACTION)){
				if (SecurityReceiver.connected(context)){
					Log.d(getClass().getSimpleName(), "Now Connected");
					// start the service
					if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
						context.startService(service);
					}
					else {
						context.startForegroundService(service);
					}
					//context.startService(service);
				}
				else {
					Log.d(getClass().getSimpleName(), "Now Disconnected");
					// stop the service
					context.stopService(service);
				}
			}
		}
	}

	@Override
	public void onBackPressed() {
		moveTaskToBack(true);
	}

	@Override
    protected void onDestroy() {
		try {
			unregisterReceiver(receiver);
		}catch (IllegalArgumentException e){
			e.printStackTrace();
		}
        super.onDestroy();
    }
}
