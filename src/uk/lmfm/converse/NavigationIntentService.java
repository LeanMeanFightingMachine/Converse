package uk.lmfm.converse;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import com.google.android.gms.location.LocationClient;

public class NavigationIntentService extends IntentService {

	public NavigationIntentService() {
		super(NavigationIntentService.class.getSimpleName());
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		// TODO Auto-generated method stub

		Bundle b = null;

		// Check we have a bundle
		if ((b = intent.getExtras()) != null) {
			// Check if we have location data
			Object o = b.get(LocationClient.KEY_LOCATION_CHANGED);
			if (o != null && o instanceof Location) {

				Intent i = new Intent();
				i.setAction(NavigationAnimatorActivity.INTENT_ACTION);
				i.putExtra(LocationClient.KEY_LOCATION_CHANGED, (Location) o);

				/*
				 * Log.v("IntentFilter", intent.getAction());
				 * Log.v(getClass().getSimpleName(), intent.getExtras()
				 * .get(LocationClient.KEY_LOCATION_CHANGED) .toString());
				 */

				// Broadcast the intent carrying the data

				sendBroadcast(i);
			}
		}

	}

}
