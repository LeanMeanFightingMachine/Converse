package uk.lmfm.converse;

import uk.lmfm.converse.util.DirectionsWrapper;
import uk.lmfm.converse.util.IOUtils;
import uk.lmfm.converse.util.Journey;
import uk.lmfm.converse.util.LocationUtils;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

/**
 * This class is used to render annd display animation of navigation while the
 * user is travelling.
 * 
 * Guides consulted:
 * http://android-er.blogspot.co.uk/2011/10/animation-on-scale.html
 * 
 * @author niallgunter
 * 
 */
public class NavigationAnimatorActivity extends Activity implements
		LocationListener, GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener {

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

	private static final int CACHE_SIZE = 20; // Number of locations to Cache

	private BluetoothAdapter mBluetooth; // Bluetooth
	private String leftShoe; // MAC Address of left bluetooth shoe
	private String rightShoe; // MAC Address of right bluetooth shoe

	private Location[] cachedLocations; // Store locations so we can work
										// out bearings
	private int cacheIndex = 0; // Current index to store location data in

	protected ImageView converse;
	protected ImageButton mButton;
	LocationClient mLocationClient;
	LocationRequest mLocationRequest;
	Location mCurrentLocation;
	protected Animation enlarge, shrink;
	private boolean reachedDest;

	static class LeanMeanLocation {
		final double lat = 51.5438122;
		final double lon = -0.1499303;
	}

	static class CamdenMarket {
		final double lat = 51.540088999999995;
		final double lon = -0.14293520000000562;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		cachedLocations = new Location[CACHE_SIZE];

		// Check that we have Google Play services
		checkForPlayServices();

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

		reachedDest = false;
		setContentView(R.layout.activity_navigation_animator);
		mButton = (ImageButton) findViewById(R.id.imageButton1);
		mButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Navigate back to ConverseMapActivity for now
				Intent intent = new Intent(NavigationAnimatorActivity.this,
						ConverseMapActivity.class);
				startActivity(intent);
			}
		});
		loadImage();

	}

	/*
	 * Called when the Activity becomes visible.
	 */
	@Override
	protected void onStart() {
		super.onStart();
		// Start our connection
		mLocationClient.connect();
	}

	private void checkIfAtLocation(Location currLocation) {
		// TODO: check if we're withing range of our destination
	}

	private void loadImage() {
		converse = (ImageView) findViewById(R.id.logodisplay);
		converse.setImageBitmap(decodeSampledBitmapFromResource(getResources(),
				R.drawable.logo, 325, 200));
		// converse.startAnimation(enlarge);
	}

	public static int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			// Calculate ratios of height and width to requested height and
			// width
			final int heightRatio = Math.round((float) height
					/ (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);

			// Choose the smallest ratio as inSampleSize value, this will
			// guarantee
			// a final image with both dimensions larger than or equal to the
			// requested height and width.
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}

		return inSampleSize;
	}

	private Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
			int reqWidth, int reqHeight) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, resId, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth,
				reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeResource(res, resId, options);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		converse.clearAnimation();
	}

	private void setUpAnimations() {

		enlarge = AnimationUtils.loadAnimation(this, R.anim.enlarge);
		shrink = AnimationUtils.loadAnimation(this, R.anim.shrink);
		converse.startAnimation(enlarge);

		enlarge.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				converse.startAnimation(shrink);

			}
		});

		shrink.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				// TODO Auto-generated method stub
				converse.startAnimation(enlarge);

			}
		});
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

		setUpAnimations();
	}

	@Override
	protected void onDestroy() {

		// Here we'll kill everything pertaining to location

		// If the client is connected
		if (mLocationClient.isConnected()) {
			/*
			 * Remove location updates for a listener. The current Activity is
			 * the listener, so the argument is "this".
			 */
			mLocationClient.removeLocationUpdates(this);
		}
		/*
		 * After disconnect() is called, the client is considered "dead".
		 */
		mLocationClient.disconnect();

		super.onDestroy();
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {

		Log.d(getClass().getSimpleName(),
				"Failed to connect to Location Services.");

		/*
		 * Google Play services can resolve some errors it detects. If the error
		 * has a resolution, try sending an Intent to start a Google Play
		 * services activity that can resolve error.
		 */
		if (result.hasResolution()) {
			try {
				// Start an Activity that tries to resolve the error
				result.startResolutionForResult(this,
						LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);
				/*
				 * Thrown if Google Play services canceled the original
				 * PendingIntent
				 */
			} catch (IntentSender.SendIntentException e) {
				// Log the error
				e.printStackTrace();
			}
		} else {
			/*
			 * If no resolution is available, display a dialog to the user with
			 * the error.
			 */
			GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this,
					0).show();
		}
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		// TODO Auto-generated method stub
		Log.d(getClass().getSimpleName(), "Connected to Location services");

		mCurrentLocation = mLocationClient.getLastLocation();

		// Check if we have a data connection
		if (IOUtils.isOnline(this)) {

			new Thread(new Runnable() {
				@Override
				public void run() {

					CamdenMarket cm = new CamdenMarket();
					DirectionsWrapper dw = new DirectionsWrapper(cm.lat,
							cm.lon, mCurrentLocation.getLatitude(),
							mCurrentLocation.getLongitude());
					Journey j = dw.getJourney();

				}
			}).start();

		}

		mLocationClient.requestLocationUpdates(mLocationRequest, this);
		Log.d(getClass().getSimpleName(), "Location updates requested.");

	}

	@Override
	public void onDisconnected() {
		Log.d(getClass().getSimpleName(), "Disconnected from Location services");

	}

	@Override
	public void onLocationChanged(Location location) {
		Log.d(getClass().getSimpleName(), String.format(
				"Location changed from %f,%f to %f,%f",
				mCurrentLocation.getLatitude(),
				mCurrentLocation.getLongitude(), location.getLatitude(),
				location.getLongitude()));

		if (mCurrentLocation.distanceTo(location) < 10) {
			reachedDest = true;
			/*
			 * Intent intent = new Intent(NavigationAnimatorActivity.this,
			 * ConverseMapActivity.class); startActivity(intent);
			 */
			Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			// Vibrate for 500 milliseconds

			v.vibrate(new long[] { 0, 250, 125 }, 1);
		}

		mCurrentLocation = location;
		cachedLocations[cacheIndex] = location;
		cacheIndex = (cacheIndex + 1) % CACHE_SIZE;
	}

	/* Classes and methods for checking location services */

	private void checkForPlayServices() {
		int errorCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		if (errorCode != ConnectionResult.SUCCESS) {
			GooglePlayServicesUtil.getErrorDialog(errorCode, this, 0).show();
		}
	}

}
