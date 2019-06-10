package com.qualitick.android.os;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

public class SecurityReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(getClass().getSimpleName(), "Broadcast Received");
		Intent service = new Intent(context, SecurityService.class);
		
		SharedPreferences settings = context.getSharedPreferences(SecurityService.PREFS_NAME, 0);
		boolean locked = settings.getBoolean("deviceLock", false);
	       
		if (locked){
			MyDeviceAdminReceiver.initiateDeviceLock(context);
		}

		// if the connectivity has changed...
//		if (intent.getAction() != null && intent.getAction().equals(android.net.ConnectivityManager.CONNECTIVITY_ACTION)) {
//			if (connected(context)){
//				Log.d(getClass().getSimpleName(), "Now Connected");
//				// start the service
//				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
//					context.startService(service);
//				}
//				else {
//					context.startForegroundService(service);
//				}
//				//context.startService(service);
//			}
//			else {
//				Log.d(getClass().getSimpleName(), "Now Disconnected");
//				// stop the service
//				context.stopService(service);
//			}
//		}
//		else {
			Log.d(getClass().getSimpleName(), "Service Started");
			context.stopService(service);
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
				context.startService(service);
			}
			else {
				context.startForegroundService(service);
			}
			//context.startService(service);
//		}
	}

	public static boolean connected(Context context){
		ConnectivityManager connectivityManager = (ConnectivityManager) context.
				getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		return networkInfo != null && networkInfo.isConnected();
	}

	public static void showToast(final Context context, final String message){
		Handler handler = new Handler(Looper.getMainLooper());
		handler.post(new Runnable() {

			@Override
			public void run() {
//				Toast.makeText(context,message,Toast.LENGTH_SHORT).show();
			}
		});
	}
}
