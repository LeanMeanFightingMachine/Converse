package uk.lmfm.converse;

import android.app.Service;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class ConverseNavigationService extends Service implements
		LocationListener, GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener {

	/* Constants */

	// Milliseconds per second
	private static final int MILLISECONDS_PER_SECOND = 1000;
	// Update frequency in seconds
	public static final int UPDATE_INTERVAL_IN_SECONDS = 10;
	// Update frequency in milliseconds
	private static final long UPDATE_INTERVAL = MILLISECONDS_PER_SECOND
			* UPDATE_INTERVAL_IN_SECONDS;
	// The fastest update frequency, in seconds
	private static final int FASTEST_INTERVAL_IN_SECONDS = 5;
	// A fast frequency ceiling in milliseconds
	private static final long FASTEST_INTERVAL = MILLISECONDS_PER_SECOND
			* FASTEST_INTERVAL_IN_SECONDS;

	public static final String LOCATION_UPDATE = "uk.lmfm.converse.LOCATION_UPDATE";
	public static final String LOCATION_SERVICE_CONN_ERR = "uk.lmfm.converse.LOCATION_SERVICE_CONN_ERR";
	public static final String CONN_RESULT = "Play Services Location Client ConnectionResult";

	LocationClient mLocationClient;
	LocationRequest mLocationRequest;
	boolean mLocationUpdates = false;
	boolean mLocationClientConnected = false;

	/**
	 * Creation of Bound Service
	 */
	@Override
	public void onCreate() {

		Log.i(getClass().getSimpleName(), "Starting Service");

		/*
		 * Create a new location client, using the enclosing class to handle
		 * callbacks.
		 */
		mLocationClient = new LocationClient(this, this, this);

		// Create the LocationRequest object
		mLocationRequest = LocationRequest.create();
		// Use high accuracy
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		// Set the update interval to 10 seconds
		mLocationRequest.setInterval(UPDATE_INTERVAL);
		// Set the fastest update interval to 5 second
		mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
		mLocationClient.connect();

	}

	// Binder given to clients
	private final IBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return mBinder;
	}

	/**
	 * Class used for the client Binder. Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		ConverseNavigationService getService() {
			// Return this instance of LocalService so clients can call public
			// methods
			return ConverseNavigationService.this;
		}
	}

	/* Client methods */

	public Location getMostRecentLocation() {
		if (mLocationClientConnected)
			return mLocationClient.getLastLocation();
		return null;
	}

	public void stopLocationUpdates() {
		if (mLocationClientConnected) {
			if (mLocationUpdates) {
				mLocationClient.removeLocationUpdates(this);
			}
		}

		mLocationUpdates = false;
	}

	public void startLocationUpdates() {
		if (mLocationClientConnected) {
			if (!mLocationUpdates) {
				/* Request location updates. */

				mLocationClient.requestLocationUpdates(mLocationRequest, this);
				Log.d(getClass().getSimpleName(), "Location updates requested.");
				mLocationUpdates = true;
			}
		}
	}

	@Override
	public void onLocationChanged(Location location) {

		Intent i = new Intent();
		i.setAction(NavigationAnimatorActivity.INTENT_ACTION);
		i.putExtra(LocationClient.KEY_LOCATION_CHANGED, location);

		String locationString = "New location {%f,%f}";
		String bearing = "Current Bearing %f";

		Log.v(getClass().getSimpleName(),
				String.format(locationString, location.getLatitude(),
						location.getLongitude()));
		Log.v(getClass().getSimpleName(),
				String.format(bearing, location.getBearing()));

		// Broadcast the intent carrying the data

		sendBroadcast(i);

	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {

		Log.e(getClass().getSimpleName(),
				"Failed to connect to Location Services.");

	}

	@Override
	public void onConnected(Bundle connectionHint) {
		Log.i(getClass().getSimpleName(), "Connected to Location services");
		mLocationClientConnected = true;

	}

	@Override
	public void onDisconnected() {
		mLocationClientConnected = false;
		Log.i(getClass().getSimpleName(), "Disconnected from Location services");

	}

	@Override
	public void onDestroy() {
		Log.d(getClass().getSimpleName(), "Stopping Service");
		stopLocationUpdates();
	}

}
