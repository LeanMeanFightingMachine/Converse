package uk.lmfm.converse.async;

import uk.lmfm.converse.util.DirectionsWrapper;
import uk.lmfm.converse.util.IOUtils;
import uk.lmfm.converse.util.Journey;
import android.content.Context;
import android.location.Location;
import android.util.Log;

public abstract class JourneyDownloader implements Runnable {

	Location start;
	Location dest;
	Context c;

	public JourneyDownloader(Location start, Location dest, Context c) {
		super();
		this.start = start;
		this.dest = dest;
		this.c = c;
	}

	/**
	 * Abstract method to handle the retrieval of a journey object
	 * 
	 * @param j
	 */
	public abstract void handleJourney(Journey j);

	@Override
	public void run() {
		// Check if we have a data connection
		if (IOUtils.isOnline(c)) {
			Log.d(getClass().getSimpleName(),
					"Downloading Journey information from Google.");
			DirectionsWrapper dw = new DirectionsWrapper(start.getLatitude(),
					start.getLongitude(), dest.getLatitude(),
					dest.getLongitude());
			handleJourney(dw.getJourney());
		}

	}

}
