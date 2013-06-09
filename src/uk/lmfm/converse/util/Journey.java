package uk.lmfm.converse.util;

import java.util.ArrayList;
import java.util.List;

import android.location.Location;

import com.google.android.gms.location.Geofence;

public class Journey {

	/*
	 * Use to set an expiration time for a geofence. After this amount of time
	 * Location Services will stop tracking the geofence.
	 */
	private static final long SECONDS_PER_HOUR = 60;
	private static final long MILLISECONDS_PER_SECOND = 1000;
	private static final long GEOFENCE_EXPIRATION_IN_HOURS = 1;
	private static final long GEOFENCE_EXPIRATION_TIME = GEOFENCE_EXPIRATION_IN_HOURS
			* SECONDS_PER_HOUR * MILLISECONDS_PER_SECOND;

	// Radius in meters
	private static final float RADIUS = 100;

	private List<Step> steps;
	private List<Geofence> geofences;

	public Journey(int legs) {
		this.steps = new ArrayList<Journey.Step>(legs);
		this.geofences = new ArrayList<Geofence>(legs);
	}

	public Journey() {
		this.steps = new ArrayList<Journey.Step>();
		this.geofences = new ArrayList<Geofence>();
	}

	public boolean addStep(double startLat, double startLon, double endLat,
			double endLon, String instructions) {
		Step s = new Step(startLat, endLat, startLon, endLon, android.text.Html
				.fromHtml(instructions).toString());
		boolean result = steps.add(s);
		result &= geofences.add(s.asGeofence(RADIUS));

		return result;

	}

	public boolean removeFirstStep() {
		Step sDiscarded = steps.isEmpty() ? null : steps.remove(0);
		Geofence gDiscarded = geofences.isEmpty() ? null : geofences.remove(0);

		// Return false if either object is null since we couldn't remove both
		// items from their lists
		return (sDiscarded != null) && (gDiscarded != null);
	}

	public Step getFirstStep() {
		return steps.isEmpty() ? null : steps.get(0);

	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		for (Step s : steps) {
			sb.append(String.format("At {%f, %f} %s\n", s.startLat, s.startLon,
					s.instructions));
		}

		return sb.toString();
	}

	/**
	 * This class represents a step along a journey
	 * 
	 * @author niallgunter
	 * 
	 */
	public static class Step {
		static long START_ID = 0;

		long id = 0x0;
		double startLat;
		double endLat;
		double startLon;
		double endLon;

		String instructions;

		public Step(double startLat, double endLat, double startLon,
				double endLon, String instructions) {
			super();
			this.startLat = startLat;
			this.endLat = endLat;
			this.startLon = startLon;
			this.endLon = endLon;
			this.instructions = instructions;
			this.id = ++START_ID;
		}

		public Location asLocation() {
			Location l = new Location("Step " + id);
			l.setLatitude(startLat);
			l.setLongitude(startLon);
			return l;
		}

		

		public String getInstruction() {
			return instructions;
		}

		public Geofence asGeofence(float radius) {
			// Build a new Geofence object
			return new Geofence.Builder()
					.setRequestId(String.format("%X", id))
					// This geofence records both entry and exit transitions
					.setTransitionTypes(
							Geofence.GEOFENCE_TRANSITION_ENTER
									| Geofence.GEOFENCE_TRANSITION_EXIT)

					.setCircularRegion(startLat, startLon, radius)
					.setExpirationDuration(GEOFENCE_EXPIRATION_TIME).build();
		}

		public String toString() {
			return String.format("At {%f, %f} %s\n", startLat, startLon,
					instructions);
		}
	}

}
