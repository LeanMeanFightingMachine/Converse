package uk.lmfm.converse;

import uk.lmfm.converse.ConverseNavigationService.LocalBinder;
import uk.lmfm.converse.util.DirectionsWrapper;
import uk.lmfm.converse.util.IOUtils;
import uk.lmfm.converse.util.Journey;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
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
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;

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
public class NavigationAnimatorActivity extends Activity {

	private static final int CACHE_SIZE = 20; // Number of locations to Cache

	public static final String INTENT_ACTION = "uk.lmfm.converse.LOCATION_UPDATE";

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
	Location mDestination;

	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// We've bound to LocalService, cast the IBinder and get
			// LocalService instance
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			mBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
		}
	};

	protected Animation enlarge, shrink;
	private boolean reachedDest;
	private ReceiveLocationUpdate rlu = null;
	private boolean isReceiverRegistered = false;

	ConverseNavigationService mService;
	boolean mBound = false;

	/*
	 * Store the PendingIntent used to send activity recognition events back to
	 * the app
	 */
	private PendingIntent mNavigationPendingIntent;

	static class LeanMeanLocation {
		final double lat = 51.5438122;
		final double lon = -0.1499303;
	}

	static class CamdenMarket {
		final double lat = 51.540088999999995;
		final double lon = -0.14293520000000562;
	}

	class ReceiveLocationUpdate extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub

			Log.v(getClass().getName(), "DERP");

			String action = intent.getAction();

			// send message to activity

			Bundle b = null;

			// Check we have a bundle
			if ((b = intent.getExtras()) != null) {
				// Check if we have location data
				if (action.equalsIgnoreCase(INTENT_ACTION)) {

					Object o = b.get(LocationClient.KEY_LOCATION_CHANGED);
					Log.v(getClass().getName(), o.toString());
					if (o != null && o instanceof Location) {
						mCurrentLocation = (Location) o;
						checkIfAtLocation(mCurrentLocation);
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

		cachedLocations = new Location[CACHE_SIZE];
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

	}

	private void checkIfAtLocation(Location currLocation) {
		// TODO: check if we're withing range of our destination
		String s = String
				.format("Distance from current location {%f,%f} to {%f,%f} is %f meters.",
						mCurrentLocation.getLatitude(),
						mCurrentLocation.getLongitude(),
						mDestination.getLatitude(),
						mDestination.getLongitude(),
						mCurrentLocation.distanceTo(mDestination));
		Log.i(getClass().getName(), s);
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

		if (!isReceiverRegistered) {

			// If not registered, register now
			IntentFilter ifilter = new IntentFilter();
			ifilter.addAction(INTENT_ACTION);

			registerReceiver(rlu, ifilter);
			isReceiverRegistered = true;
			Log.i(getClass().getSimpleName(), "Registering BroadcastReciever");
		}

		setUpAnimations();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (isReceiverRegistered) {

			// If registered, unregister now
			unregisterReceiver(rlu);
			isReceiverRegistered = false;
			Log.i(getClass().getSimpleName(), "Unregistering BroadcastReciever");
		}

		// Unbind from the service
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}

	}

	@Override
	protected void onStop() {
		super.onStop();
		// Unbind from the service
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}
	}

	private void getJourney(Location l) {
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
