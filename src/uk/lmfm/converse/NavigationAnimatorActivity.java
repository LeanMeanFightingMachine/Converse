package uk.lmfm.converse;

import uk.lmfm.converse.async.JourneyDownloader;
import uk.lmfm.converse.services.ConverseNavigationService;
import uk.lmfm.converse.services.ConverseNavigationService.LocalBinder;
import uk.lmfm.converse.util.ConverseVibrator;
import uk.lmfm.converse.util.Journey;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.ImageView;

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

	/* Static fields */

	private static final float RADIUS = 3;
	AnimationDrawable ripple;

	public static final String INTENT_ACTION = "uk.lmfm.converse.LOCATION_UPDATE";

	// Milliseconds per second
	private static final int MILLISECONDS_PER_SECOND = 1000;
	// How we should go for before checking that the user is heading in the
	// right direction
	private static final int TIME_OUT = MILLISECONDS_PER_SECOND * 60;

	/* Fields for handling Location data */

	Location mCurrentLocation;
	Location mDestination;
	Location mOldCurrentLocation = null;
	long timestamp = -1;

	// Object containing our journey details
	private Journey journey;
	private boolean mHasJourneyData = false;
	private boolean reachedDest = false;
	private ReceiveLocationUpdate rlu = null;
	private boolean isReceiverRegistered = false;

	/* Graphical objects */
	AnimationDrawable frameAnimation;
	ImageView img;

	/*
	 * Local Service definitions. Here we define fields for connecting to and
	 * communicating with our location update and bluetooth services.
	 */
	ConverseNavigationService mNavigationService;
	Object mBluetoothService; // TODO: Create class
	boolean mNavigationBound = false;
	boolean mBluetoothBound = false;

	/**
	 * Callback for LocalService. We use this to start the location updates from
	 * the service.
	 */
	private ServiceConnection mLocationServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// We've bound to LocalService, cast the IBinder and get
			// LocalService instance
			LocalBinder binder = (LocalBinder) service;
			mNavigationService = binder.getService();
			mCurrentLocation = mNavigationService.getMostRecentLocation();

			mNavigationService.startLocationUpdates();
			mNavigationBound = true;
			Log.d(getClass().getSimpleName(), "Bound to Service");
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mNavigationBound = false;
			Log.d(getClass().getSimpleName(), "Disconnected from Service");
		}
	};

	/**
	 * Callback for LocalService. We use this to start to initiate a connection
	 * to our bluetooth service.
	 */
	private ServiceConnection mBluetoothServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// We've bound to LocalService, cast the IBinder and get
			// LocalService instance
			LocalBinder binder = (LocalBinder) service;
			mBluetoothService = binder.getService();

			mBluetoothBound = true;
			Log.d(getClass().getSimpleName(), "Bound to bluetooth service");
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mNavigationBound = false;
			Log.d(getClass().getSimpleName(),
					"Disconnected from bluetooth service");
		}
	};

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

			// Get the Intent action
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
										Log.v(getClass().getSimpleName(),
												j.toString());
										checkIfAtLocation(mCurrentLocation);
									} else {
										Log.e(getClass().getSimpleName(),
												"Could not retrieve directions for journey");
										// TODO: popup indicating invalid
										// journey, try again
									}

								}
							}).start();

						}

					}
				}

			}

		}

	}

	private void showErrorDialog(String title, String message) {
		// 1. Instantiate an AlertDialog.Builder with its constructor
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		// 2. Chain together various setter methods to set the dialog
		// characteristics
		builder.setMessage(message).setTitle(title);
		
		builder.setCancelable(false);

		// 3. Get the AlertDialog from create()
		AlertDialog dialog = builder.create();
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

		img = (ImageView) NavigationAnimatorActivity.this
				.findViewById(R.id.RippleImg);
		img.setBackgroundResource(R.drawable.ripple_anim);
		frameAnimation = (AnimationDrawable) img.getBackground();

	}

	/*
	 * Called when the Activity becomes visible.
	 */
	@Override
	protected void onStart() {
		super.onStart();
		// Start our connection
		startServices();
		startAnimation();

	}

	public void startAnimation() {
		if (img != null && frameAnimation != null) {
			if (!frameAnimation.isRunning())
				frameAnimation.start();
		}
	}

	/**
	 * This displays a dialog informing the user that the bluetooth connection
	 * to the shoes has failed.The user has the option of navigating to a
	 * settings page or back to the initial activity.
	 */
	private void showBluetoothDialog() {
		// 1. Instantiate an AlertDialog.Builder with its constructor
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		// 2. Chain together various setter methods to set the dialog
		// characteristics
		builder.setMessage(R.string.set_destination_text).setTitle(
				R.string.set_destination_title);

		builder.setPositiveButton(R.string.btn_ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						/*
						 * user clicked OK button. Return to bluetooth
						 * connecting activity.
						 */
					}
				});
		builder.setNeutralButton(R.string.btn_settings,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// TODO: navigate to a settings activity to reconfigure
						// shoes
					}
				});

		builder.setCancelable(false);

		// 3. Get the AlertDialog from create()
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	/**
	 * Starts or binds the services required by this activity.
	 */
	private void startServices() {
		Intent locationUpdateIntent = new Intent(this,
				ConverseNavigationService.class);
		bindService(locationUpdateIntent, mLocationServiceConnection,
				Context.BIND_AUTO_CREATE);
	}

	/**
	 * Checks if the users current location is at a waypoint in navigation (i.e
	 * we should be telling the user to turn right, turn left etc.)
	 * 
	 * @param currLocation
	 */
	private void checkIfAtLocation(Location currLocation) {

		float dist = 0;

		// Sanity check to make sure we have obtained directions from google
		if (mHasJourneyData) {
			// Get the next journey step
			Journey.Step s = journey.getFirstStep();

			if (s != null) {
				Log.v(getClass().getSimpleName(),
						String.format(
								"Checking if current location is near journey step: %s",
								s));
				dist = mCurrentLocation.distanceTo(s.asLocation());
			} else {
				/*
				 * We have no step data, but we had valid journey information so
				 * we must be close. Use the final destination as a waypoint. A
				 * kludge.
				 */
				dist = mCurrentLocation.distanceTo(mDestination);
			}

			// Check if we're within range of the desired destination
			if (mCurrentLocation.distanceTo(mDestination) <= RADIUS
					+ mCurrentLocation.getAccuracy()) {
				reachedDest = true;
				cleanUp();
				Log.i(getClass().getSimpleName(),
						"User has reached destination");

				// Navigate to the DestinationReachedActivity, since we're done.
				Intent intent = new Intent(this,
						DestinationReachedActivity.class);
				startActivity(intent);
			}

			Log.v(getClass().getSimpleName(),
					String.format(
							"Distance from current location to waypoint is %.2fm (±%.2fm)",
							dist, mCurrentLocation.getAccuracy()));

			// Check if we're within range of a waypoint, notifying user if we
			// are, and giving them the next direction to take
			if (dist <= RADIUS + mCurrentLocation.getAccuracy()) {

				/*
				 * Returns the approximate initial bearing in degrees East of
				 * true North when traveling along the shortest path between
				 * this location and the given location. The shortest path is
				 * defined using the WGS84 ellipsoid. Locations that are
				 * (nearly) antipodal may produce meaningless results.
				 */
				float bearing = mCurrentLocation.bearingTo(s.asLocation());

				// Vibrate for 500 milliseconds

				ConverseVibrator.vibrateForDirection(s.getInstruction(),
						bearing, this);
				if (!journey.removeFirstStep()) {
					return;
				}

				// If there's only one step, we'll have null afterwards

				if (journey.getFirstStep() != null) {
					Log.v(getClass().getSimpleName(), String.format(
							"Removed current step, next step is: %s", journey
									.getFirstStep().toString()));
				} else {
					Log.v(getClass().getSimpleName(),
							"Removed last step, next step is final destination");
				}

			} else {
				float oldDistToWaypoint;

				if (s != null) {
					oldDistToWaypoint = mOldCurrentLocation.distanceTo(s
							.asLocation());
				} else {
					oldDistToWaypoint = mOldCurrentLocation
							.distanceTo(mDestination);
				}

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

		startAnimation();

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
	protected void onPause() {
		super.onPause();
		if (img != null && frameAnimation != null) {
			frameAnimation.stop();
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

		// Unbind from the service and stop location updates
		if (mNavigationBound) {
			mNavigationService.stopLocationUpdates();
			unbindService(mLocationServiceConnection);
			mNavigationBound = false;
		}
	}

	/**
	 * Resets the navigation if the user has strayed too far from a waypoint
	 */
	private void resetNavigation() {
		mHasJourneyData = false;
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
