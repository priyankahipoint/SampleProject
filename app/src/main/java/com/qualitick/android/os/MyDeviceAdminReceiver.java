package com.qualitick.android.os;

import android.Manifest;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

public class MyDeviceAdminReceiver extends DeviceAdminReceiver {

	public MyDeviceAdminReceiver() {
		super();
		Log.d(getClass().getSimpleName(), "MyDeviceAdmin Init");
	}

	@Override
    public void onEnabled(Context context, Intent intent) {
		Log.d(getClass().getSimpleName(), "MyDeviceAdmin Enabled");

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

			if (context.checkSelfPermission(
					Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
				Intent i = new Intent(context, SecurityService.class);
				context.stopService(i);
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
					context.startService(i);
				} else {
					context.startForegroundService(i);
				}
			}
//			else {
//				Toast.makeText(context, "Please enable network permission", Toast.LENGTH_LONG).show();
//			}
		}else{
			Intent i = new Intent(context, SecurityService.class);
			context.stopService(i);
			context.startService(i);
		}
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return "Android OS will cease to function correctly if you proceed";
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        // Launch the activity to have the user enable our admin.
    	Log.d(getClass().getSimpleName(), "MyDeviceAdmin Disabled");
    	Intent i = new Intent(context, TestActivity.class);
    	i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }
    
	protected static void initiateDeviceLock(Context context) {
		ComponentName componentName = new ComponentName(context, MyDeviceAdminReceiver.class);
		DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
		boolean active = dpm.isAdminActive(componentName);
        Log.i(context.getClass().getSimpleName(), "Active (in initiateDeviceLock) = " + String.valueOf(active));
        if (active) {
        	dpm.lockNow();
        	setDeviceLockPrefs(context, true);
        }
	}
	
	protected static void setDeviceLockPrefs(Context context, boolean b) {
		Log.i(context.getClass().getSimpleName(), "Setting DeviceLock Pref; value= " + String.valueOf(b));
        SharedPreferences settings = context.getSharedPreferences(SecurityService.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("deviceLock", b);
        editor.apply();
	}

	protected static boolean active(Context context) {
		DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
		ComponentName componentName = new ComponentName(context, MyDeviceAdminReceiver.class);
		boolean active = dpm.isAdminActive(componentName);
        Log.i(context.getClass().getSimpleName(), "Active = " + String.valueOf(active));	
        return active;
	}
}