package uk.lmfm.converse;

import uk.lmfm.amarino.R;
import uk.lmfm.converse.util.LocationUtils;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class ConverseMapActivity extends FragmentActivity implements
		LocationListener, GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener {

	/**
	 * Note that this may be null if the Google Play services APK is not
	 * available.
	 */
	private GoogleMap mMap;
	public static final String INTENT_KEY = "uk.lmfm.converse.DESTINATION";

	// Stores the current instantiation of the location client in this object
	private LocationClient mLocationClient;

	// Current location
	private Location mCurrentLocation;

	private static final LatLng LEANMEAN = new LatLng(51.543865, -0.149188);

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Check that we have Google Play services
		checkForPlayServices();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// Show the Up button in the action bar.
			// getActionBar().setDisplayHomeAsUpEnabled(true);
			getActionBar().setDisplayShowTitleEnabled(false);
		}

		/*
		 * Create a new location client, using the enclosing class to handle
		 * callbacks.
		 */
		mLocationClient = new LocationClient(this, this, this);

		setContentView(R.layout.activity_converse_map);
		setUpMapIfNeeded();

	}

	/*
	 * Called when the Activity becomes visible.
	 */
	@Override
	protected void onStart() {
		super.onStart();
		// Connect the client.
		mLocationClient.connect();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.converse_map, menu);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		setUpMapIfNeeded();
	}

	/*
	 * Called when the Activity is no longer visible.
	 */
	@Override
	protected void onPause() {
		super.onPause();
		// Disconnecting the client invalidates it.
		mLocationClient.disconnect();
		Log.d(getClass().getSimpleName(), "Disconnected from Location client.");
	}

	/**
	 * Sets up the map if it is possible to do so (i.e., the Google Play
	 * services APK is correctly installed) and the map has not already been
	 * instantiated.. This will ensure that we only ever call
	 * {@link #setUpMap()} once when {@link #mMap} is not null.
	 * <p>
	 * If it isn't installed {@link SupportMapFragment} (and
	 * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt
	 * for the user to install/update the Google Play services APK on their
	 * device.
	 * <p>
	 * A user can return to this FragmentActivity after following the prompt and
	 * correctly installing/updating/enabling the Google Play services. Since
	 * the FragmentActivity may not have been completely destroyed during this
	 * process (it is likely that it would only be stopped or paused),
	 * {@link #onCreate(Bundle)} may not be called again so we should call this
	 * method in {@link #onResume()} to guarantee that it will be called.
	 */
	private void setUpMapIfNeeded() {
		// Do a null check to confirm that we have not already instantiated the
		// map.
		if (mMap == null) {
			// Try to obtain the map from the SupportMapFragment.
			mMap = ((SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.map)).getMap();
			// Check if we were successful in obtaining the map.
			if (mMap != null) {
				setUpMap();
			}
		}
	}

	/**
	 * This is where we can add markers or lines, add listeners or move the
	 * camera. In this case, we just add a marker near Africa.
	 * <p>
	 * This should only be called once and when we are sure that {@link #mMap}
	 * is not null.
	 */
	private void setUpMap() {

		mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
		mMap.addMarker(new MarkerOptions()
				.position(LEANMEAN)
				.title("lmfm")
				.icon(BitmapDescriptorFactory
						.fromResource(R.drawable.mapmarker)));
		mMap.setMyLocationEnabled(true);
		Toast t = Toast.makeText(this,
				"Long press a location on the map to set your destination",
				Toast.LENGTH_LONG);
		t.setGravity(Gravity.TOP, 5, 40);
		t.show();
		addMapListeners();

	}

	/**
	 * Adds the listeners required.
	 */
	private void addMapListeners() {
		if (mMap != null) {
			mMap.setOnMapLongClickListener(new OnMapLongClickListener() {

				@Override
				public void onMapLongClick(LatLng point) {
					// Check that we have a map before we attempt to add a
					// marker
					if (ConverseMapActivity.this.mMap != null) {

						// Remove existing markers first
						mMap.clear();

						// Add our new marker
						mMap.addMarker(new MarkerOptions()
								.position(point)
								.title("Secret Destination!")
								.icon(BitmapDescriptorFactory
										.fromResource(R.drawable.mapmarker)));

					}

				}

			});

			mMap.setOnMarkerClickListener(new OnMarkerClickListener() {

				@Override
				public boolean onMarkerClick(Marker marker) {

					if (ConverseMapActivity.this.mMap != null) {

						// TODO navigate to next pulsing screen, comparing
						// converse location with marker position

						LatLng ll = marker.getPosition();

						if (ll != null)
							Log.v("Testing LatLng", "Heeeeello");

						Intent intent = new Intent(ConverseMapActivity.this,
								NavigationAnimatorActivity.class);

						intent.putExtra(INTENT_KEY, ll);

						startActivity(intent);

						return false;

					}

					return false;

				}
			});
		}

	}

	/**
	 * Sets the Google map camera to center on the given location
	 * 
	 * @param l
	 *            Location to center map camera on
	 * @return true if animation completed and map object is not null, false
	 *         otherwise
	 */
	private boolean setCameraToLocation(Location l) {

		if (mMap != null) {
			mMap.animateCamera(CameraUpdateFactory
					.newCameraPosition(getPositionForLocation(l)));

			return true;
		}

		return false;
	}

	/**
	 * Create a new CameraPosition object given a Location object
	 * 
	 * @param l
	 *            location to set camera position to
	 * @return CameraPosition, representing the location, and zoom we wish to
	 *         display
	 */
	private CameraPosition getPositionForLocation(Location l) {
		if (mMap == null) {
			Log.e(getClass().getSimpleName(), "Can't get Camera Position.");
			return null;
		}

		CameraPosition cameraPosition = new CameraPosition.Builder()
				.target(getLatLngForLocation(l)) // Sets the
													// center of
													// the map
													// to
													// Mountain
													// View
				.zoom(17) // Sets the zoom

				.build(); // Creates a CameraPosition from the builder

		return cameraPosition;
	}

	private LatLng getLatLngForLocation(Location l) {
		// Log.v("getLatLngForLocation", l.toString());
		return new LatLng(l.getLatitude(), l.getLongitude());
	}

	/*
	 * Called by Location Services if the attempt to Location Services fails.
	 */
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		/*
		 * Google Play services can resolve some errors it detects. If the error
		 * has a resolution, try sending an Intent to start a Google Play
		 * services activity that can resolve error.
		 */
		if (connectionResult.hasResolution()) {
			try {
				// Start an Activity that tries to resolve the error
				connectionResult.startResolutionForResult(this,
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
			GooglePlayServicesUtil.getErrorDialog(
					connectionResult.getErrorCode(), this, 0).show();
		}
	}

	/*
	 * Called by Location Services when the request to connect the client
	 * finishes successfully. At this point, you can request the current
	 * location or start periodic updates
	 */
	@Override
	public void onConnected(Bundle connectionHint) {
		// Display the connection status
		Toast.makeText(this, "Connected to Location Services",
				Toast.LENGTH_SHORT).show();
		mCurrentLocation = mLocationClient.getLastLocation();

		// We very rarely can get a null location.
		if (mCurrentLocation != null)
			setCameraToLocation(mCurrentLocation);

	}

	/*
	 * Called by Location Services if the connection to the location client
	 * drops because of an error.
	 */
	@Override
	public void onDisconnected() {
		// Display the connection status
		Toast.makeText(this, "Disconnected. Please re-connect.",
				Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		Log.d(getClass().getSimpleName(), String.format(
				"Location changed from %f to %f", mCurrentLocation, location));
		if (location != null)
			setCameraToLocation(location);
		mCurrentLocation = location;

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
