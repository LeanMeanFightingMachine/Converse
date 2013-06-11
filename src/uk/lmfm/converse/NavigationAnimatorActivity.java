package uk.lmfm.converse;

import uk.lmfm.amarino.R;
import uk.lmfm.converse.ConverseNavigationService.LocalBinder;
import uk.lmfm.converse.util.ConverseVibrator;
import uk.lmfm.converse.util.Journey;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.AnimationDrawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.model.LatLng;

/**
 * This class is used to render and display animation of navigation while the
 * user is travelling.
 * 
 * Guides consulted:
 * http://android-er.blogspot.co.uk/2011/10/animation-on-scale.html
 * 
 * @author niallgunter
 * 
 */
public class NavigationAnimatorActivity extends Activity {

	private static final float RADIUS = 10;
	AnimationDrawable ripple;

	public static final String INTENT_ACTION = "uk.lmfm.converse.LOCATION_UPDATE";

	private BluetoothAdapter mBluetooth; // Bluetooth
	private String leftShoe; // MAC Address of left bluetooth shoe
	private String rightShoe; // MAC Address of right bluetooth shoe

	// Milliseconds per second
	private static final int MILLISECONDS_PER_SECOND = 1000;
	// How we should go for before checking that the user is heading in the
	// right direction
	private static final int TIME_OUT = MILLISECONDS_PER_SECOND * 60;

	Location mCurrentLocation;
	Location mDestination;
	Location mOldCurrentLocation = null;
	long timestamp = -1;

	// Object containing our journey details
	private Journey journey;
	private boolean mHasJourneyData = false;

	/**
	 * Callback for LocalService. We use this to start the location updates from
	 * the service.
	 */
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// We've bound to LocalService, cast the IBinder and get
			// LocalService instance
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			mCurrentLocation = mService.getMostRecentLocation();

			mService.startLocationUpdates();
			mBound = true;
			Log.d(getClass().getSimpleName(), "Bound to Service");
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
			Log.d(getClass().getSimpleName(), "Disconnected from Service");
		}
	};

	private boolean reachedDest = false;
	private ReceiveLocationUpdate rlu = null;
	private boolean isReceiverRegistered = false;

	ConverseNavigationService mService;
	boolean mBound = false;

	/**
	 * Inner class used to handle broadcasts that have been sent by local
	 * services.
	 * 
	 * @author Niall
	 * 
	 */
	class ReceiveLocationUpdate extends BroadcastReceiver {

		/**
		 * Callback for handling receipt of new data
		 */
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub

			String action = intent.getAction();

			// send message to activity

			Bundle b = null;

			// Check we have a bundle
			if ((b = intent.getExtras()) != null) {
				// Check if we have location data
				if (action.equalsIgnoreCase(INTENT_ACTION)) {

					Object o = b.get(LocationClient.KEY_LOCATION_CHANGED);
					Log.v(getClass().getName(), "Received location update");
					if (o != null && o instanceof Location) {

						// Store the new location
						mCurrentLocation = (Location) o;

						// Update old current location periodically
						if ((System.currentTimeMillis() - timestamp) >= TIME_OUT) {
							mOldCurrentLocation = mCurrentLocation;
							timestamp = System.currentTimeMillis();
						}

						if (mHasJourneyData) {
							checkIfAtLocation(mCurrentLocation);
						} else {

							// Start a thread to download Journey info
							new Thread(new JourneyDownloader(mCurrentLocation,
									mDestination,
									NavigationAnimatorActivity.this) {

								@Override
								public void handleJourney(Journey j) {
									if (j != null) {
										journey = j;
										mHasJourneyData = true;
										checkIfAtLocation(mCurrentLocation);
									} else {
										Log.e(getClass().getSimpleName(),
												"Could not retrieve directions for journey");
										// TODO: popup indicating invalid journey, try again
									}

								}
							}).start();

						}

					}
				}

			}

		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Check that we have Google Play services
		checkForPlayServices();
		mDestination = new Location("DESTINATION");

		Intent caller = null;
		if ((caller = getIntent()) != null) {

			Bundle b = null;

			if ((b = caller.getExtras()) != null) {
				LatLng ll = (LatLng) b.get(ConverseMapActivity.INTENT_KEY);

				if (ll != null) {
					mDestination.setLatitude(ll.latitude);
					mDestination.setLongitude(ll.longitude);
					Log.v(getClass().getName(), mDestination.toString());
				}

			}
		}

		// Create new BroadcastReceiver
		rlu = new ReceiveLocationUpdate();

		reachedDest = false;
		setContentView(R.layout.activity_navigation_animator);

		/*
		 * ImageView rocketImage = (ImageView) findViewById(R.id.RippleImg);
		 * 
		 * rocketImage.setBackgroundResource(R.drawable.ripple_anim); ripple =
		 * (AnimationDrawable) rocketImage.getBackground();
		 */

		// getJourney(mCurrentLocation);

	}

	/*
	 * Called when the Activity becomes visible.
	 */
	@Override
	protected void onStart() {
		super.onStart();
		// Start our connection
		Intent intent = new Intent(this, ConverseNavigationService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		// ripple.start();

	}

	/**
	 * Checks if the users current location is at a waypoint in navigation (i.e
	 * we should be telling the user to turn right, turn left etc.)
	 * 
	 * @param currLocation
	 */
	private void checkIfAtLocation(Location currLocation) {

		// Sanity check to make sure we have obtained directions from google
		if (mHasJourneyData) {
			// Get the next journey step
			Journey.Step s = journey.getFirstStep();
			Log.v(getClass().getSimpleName(), String.format(
					"Checking if current location is near journey step: %s", s));
			Location l = s.asLocation();
			float dist = 0;

			// Check if we're within range of the desired destination
			if (mCurrentLocation.distanceTo(mDestination) <= RADIUS) {
				reachedDest = true;
				cleanUp();
				// TODO: navigate to finished activity
			}

			// Check if we're within range of a waypoint, notifying user if we
			// are, and giving them the next direction to take
			if ((dist = mCurrentLocation.distanceTo(l)) <= RADIUS) {

				// Vibrate for 500 milliseconds

				ConverseVibrator.vibrateForDirection(s.getInstruction(), this);
				journey.removeFirstStep();
				Log.v(getClass().getSimpleName(), String.format(
						"Removed current step, next step is: %s", journey
								.getFirstStep().toString()));
			} else {
				float oldDistToWaypoint = mOldCurrentLocation.distanceTo(l);

				// Add additional value to account for error
				if (dist > oldDistToWaypoint + RADIUS) {
					/*
					 * This means our most recent location update is FURTHER
					 * away from our next waypoint than the previous, meaning
					 * that the user has gone off course.
					 */
					Log.v(getClass().getSimpleName(),
							String.format(
									"User is moving further away from next waypoint %.2fm vs %.2fm",
									dist, oldDistToWaypoint));
					resetNavigation();
				}
			}

		}

	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.navigation_animator, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onResume() {
		super.onResume(); // Always call the superclass method first

		if (!isReceiverRegistered) {

			// If not registered, register now
			IntentFilter ifilter = new IntentFilter();
			ifilter.addAction(INTENT_ACTION);

			registerReceiver(rlu, ifilter);
			isReceiverRegistered = true;
			Log.i(getClass().getSimpleName(), "Registering BroadcastReciever");
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		cleanUp();
	}

	private void cleanUp() {
		if (isReceiverRegistered) {

			// If registered, unregister now
			unregisterReceiver(rlu);
			isReceiverRegistered = false;
			Log.i(getClass().getSimpleName(), "Unregistering BroadcastReciever");
		}

		mService.stopLocationUpdates();

		// Unbind from the service
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}
	}

	/**
	 * Resets the navigation if the user has strayed too far from a waypoint
	 */
	private void resetNavigation() {

	}

	/* Classes and methods for checking location services */

	private void checkForPlayServices() {
		int errorCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		if (errorCode != ConnectionResult.SUCCESS) {
			GooglePlayServicesUtil.getErrorDialog(errorCode, this, 0).show();
		}
	}

	/** Defines callbacks for service binding, passed to bindService() */

}
