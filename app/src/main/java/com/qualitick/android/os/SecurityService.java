package com.qualitick.android.os;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.StrictMode;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;


public class SecurityService extends Service implements LocationListener,GoogleApiClient.ConnectionCallbacks{
	/**
	 * The desired interval for location updates. Inexact. Updates may be more or less frequent.
	 */
	private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 60000;

	/**
	 * The fastest rate for active location updates. Updates will never be more frequent
	 * than this value.
	 */
	private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
			UPDATE_INTERVAL_IN_MILLISECONDS / 2;
	/**
	 * Contains parameters used by {@link com.google.android.gms.location.FusedLocationProviderApi}.
	 */
	private LocationRequest mLocationRequest;

	/**
	 * Provides access to the Fused Location Provider API.
	 */
	private FusedLocationProviderClient mFusedLocationClient;
	/**
	 * Callback for changes in location.
	 */
	private LocationCallback mLocationCallback;

	private Location location;

	private int interval = 15 * 60 * 1000; // 15 minutes

	private LocationManager mLocationManager;

	public static final String PREFS_NAME = "prefs";
	private String TAG = "GPS";
	private GoogleApiClient googleApiClient;
	private LocationListener locationListener;

	@Override
	public IBinder onBind(Intent intent) {
		// no binding for this service
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
	public SecurityService() {
		super();
		Log.d(getClass().getSimpleName(), "SecurityService Init");
	}

	@Override
	public void onCreate() {
		showNotification();

		mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
		googleApiClient = new GoogleApiClient.Builder(this)
				.addApi(LocationServices.API)
				.addConnectionCallbacks(this)
				.build();
		if (googleApiClient != null) {
			googleApiClient.connect();
		}
//
		mLocationCallback = new LocationCallback() {
			@Override
			public void onLocationResult(LocationResult locationResult) {
				super.onLocationResult(locationResult);
				onNewLocation(locationResult.getLastLocation());
			}
		};

		createLocationRequest();

		getLastLocation();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(getClass().getSimpleName(), "Service Started");
		enable();
		initLocationListener();
		//As last location was used this 15minutes refresh was required, and now using fused location so this is not required
		new ReRun().execute(interval);
		return START_NOT_STICKY;
	}

	protected void enable() {		      
        if (!MyDeviceAdminReceiver.active(this)) {
        	Log.i(getClass().getSimpleName(), "Should Launch Activity");
            // Launch the activity to have the user enable our admin.
        	Intent intent = new Intent(this, TestActivity.class);
        	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
	}
    
	private void initLocationListener(){
		mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		locationListener = new LocationListener() {

			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
				// Nothing to do here
				Log.i(getClass().getSimpleName(), "onStatusChanged");
			}

			@Override
			public void onProviderEnabled(String provider) {
				// Nothing to do here
				Log.i(getClass().getSimpleName(), "onProviderEnabled");
			}

			@Override
			public void onProviderDisabled(String provider) {
				// Nothing to do here
				Log.i(getClass().getSimpleName(), "onProviderDisabled");
			}

			@Override
			public void onLocationChanged(Location location) {
				Log.i(getClass().getSimpleName(), "onLocationChanged");
				Log.i(getClass().getSimpleName(), "New Location: " + Double.toString(location.getLatitude()) + " " + 
						Double.toString(location.getLongitude()));	
				//makeUseOfNewLocation(location);
				mLocationManager.removeUpdates(this);
			}
		};
	}

	private void makeUseOfNewLocation(Location location){
//		SecurityReceiver.showToast(this,"Fired PingSurvey API");
		if(location != null && location.getLatitude() != 0 && location.getLongitude() != 0){
			// post GPS coordinates to server & results from post
			int responseInt = postGPS(location.getLatitude(), location.getLongitude());
			// check the response
			if (responseInt == 1){
				// lock device
				MyDeviceAdminReceiver.initiateDeviceLock(this);
			}
			else if (responseInt == 0){
				MyDeviceAdminReceiver.setDeviceLockPrefs(this, false);
			}
		}else {
			//if location is null ping server with dummy value for tracking user online status
			postGPS(99999, 99999);
			Log.i(getClass().getSimpleName(), "Null location ");
		}
	}
	


	private int postGPS(double latit, double longit){
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy); 
		String latitude = convertDotToUnderScore(latit);
		String longitude = convertDotToUnderScore(longit);
		String deviceID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
		String urlStr = "http://www.qualitick.net/wsstagev3/survey.svc/" + "PingSurvey/" + deviceID + "/" + latitude  + "/" + longitude + "/" ;
		//String urlStr = "http://www.qualitick.net/wsstagev3/survey.svc/" + "PingSurvey/" + "e053a3ad124509c4" + "/" + latitude  + "/" + longitude + "/" ;
		Log.i(getClass().getSimpleName(), "URL: " + urlStr);
		String responseStr = null;
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet(urlStr);
		HttpResponse response;
		try {
			response = client.execute(request);
			if(response != null) {
				HttpEntity resEntity = response.getEntity();
				responseStr = EntityUtils.toString(resEntity);
				if (resEntity != null) {
					Log.i(getClass().getSimpleName(), "RESPONSE: " + responseStr);
				}
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return 0;
	}

	private String convertDotToUnderScore(double value) {
		String valueStr = String.valueOf(value);
		String[] strArr = valueStr.split("\\.");
		String underscoredStr = strArr[0] + "_" + strArr[1];
		return underscoredStr;
	}

	@Override
	public void onLocationChanged(Location location) {
	}

	@Override
	public void onStatusChanged(String s, int i, Bundle bundle) {

	}

	@Override
	public void onProviderEnabled(String s) {

	}

	@Override
	public void onProviderDisabled(String s) {

	}

	@Override
	public void onConnected(@Nullable Bundle bundle) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
				&& ContextCompat.checkSelfPermission(this,
				Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
				Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

				mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
		}
	}

	@Override
	public void onConnectionSuspended(int i) {

	}

	class ReRun extends AsyncTask<Integer, Void, Void> {

		@Override
		protected Void doInBackground(Integer... params) {
			try {
				int delayMillis = params[0];
				synchronized (this){
//					wait(delayMillis);	
					Thread.sleep(delayMillis);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
//			} catch (IllegalMonitorStateException e){
//				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			// only restart the service if Internet connection is available
			if (SecurityReceiver.connected(SecurityService.this)){
				Intent serviceIntent = new Intent(getApplicationContext(), SecurityService.class);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					startForegroundService(serviceIntent);
				}else{
					startService(serviceIntent);
				}
			}				
			stopSelf();
		}
	}

	//Start Service changes for Oreo device
	private void showNotification(){
		NotificationManager mNotificationManager = ((NotificationManager) getSystemService(NOTIFICATION_SERVICE));
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this,null);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel mChannel = new NotificationChannel("1","GPS",NotificationManager.IMPORTANCE_DEFAULT );
			mChannel.setDescription("GPS enabled.");
			mChannel.enableLights(false);
			mChannel.enableVibration(false);
			mNotificationManager.createNotificationChannel(mChannel);
			builder.setChannelId("1");
			Notification notification = builder.build();
			startForeground(1,notification);
		}
	}

	@Override
	public void onDestroy() {
//		mServiceHandler.removeCallbacksAndMessages(null);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			stopForeground(true); //true will remove notification
		}

	}

	private void getLastLocation() {
		try {
			mFusedLocationClient.getLastLocation()
					.addOnCompleteListener(new OnCompleteListener<Location>() {
						@Override
						public void onComplete(@NonNull Task<Location> task) {
							if (task.isSuccessful() && task.getResult() != null) {
								location = task.getResult();
								makeUseOfNewLocation(location);
							} else {
								makeUseOfNewLocation(null);
								Log.w(TAG, "Failed to get location.");
							}
						}
					});
		} catch (SecurityException unlikely) {
			Log.e(TAG, "Lost location permission." + unlikely);
		}
	}

	private void onNewLocation(Location location) {
		Log.i(TAG, "New location: " + location);

		makeUseOfNewLocation(location);
	}
	/**
	 * Sets the location request parameters.
	 */
	private void createLocationRequest() {
		mLocationRequest = LocationRequest.create();
		mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
		mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

	}

	private String getDeviceOsVersion() {
		StringBuilder builder = new StringBuilder();

		String deviceID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
		builder.append(deviceID).append("__").append(Build.VERSION.SDK_INT);
		return builder.toString();
	}

}
