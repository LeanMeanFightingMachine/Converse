package uk.lmfm.converse.util;

import java.util.Locale;

import android.content.Context;
import android.os.Vibrator;
import android.util.Log;

public class ConverseVibrator {

	private static final long[] LEFT = new long[] { 0, 250, 125, 250 };
	private static final long[] RIGHT = new long[] { 0, 250, 125, 250, 125, 250 };
	private static final long[] NORTH = new long[] { 0, 125, 64, 125 };
	private static final long[] SOUTH = new long[] { 0, 125, 64, 125, 64, 125 };
	private static final long[] CONT = new long[] { 0, 375, 125 };
	private static Vibrator VIBRATOR;

	private static final float NON_VAL = -1000.0f;

	public static void vibrateForDirection(String s, float bearing, Context c) {
		VIBRATOR = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);

		String lower = s.toLowerCase(Locale.getDefault());

		Log.v(ConverseVibrator.class.getSimpleName(), "Direction: " + lower);

		if (lower.contains("right")) {
			Log.v(ConverseVibrator.class.getSimpleName(), "RIGHT");
			VIBRATOR.vibrate(RIGHT, -1);
		}
		if (lower.contains("left")) {
			Log.v(ConverseVibrator.class.getSimpleName(), "LEFT");
			VIBRATOR.vibrate(LEFT, -1);
		}
		if (lower.contains("continue")) {
			Log.v(ConverseVibrator.class.getSimpleName(), "CONT");
			VIBRATOR.vibrate(CONT, -1);
		}

		if (lower.contains("north")) {
			Log.v(ConverseVibrator.class.getSimpleName(), "NORTH");
			VIBRATOR.vibrate(NORTH, -1);
		}

		if (lower.contains("south")) {
			Log.v(ConverseVibrator.class.getSimpleName(), "SOUTH");
			VIBRATOR.vibrate(SOUTH, -1);
		}

		// We have bearing data, try and translate into left/right
		if (bearing != NON_VAL) {

			Log.v(ConverseVibrator.class.getSimpleName(),
					String.format("Current bearing is %.2f degrees.", bearing));

			// if ±15 degrees, lets call it north, so go straight
			if (Math.abs(bearing) <= 45) {
				Log.v(ConverseVibrator.class.getSimpleName(), "NORTH");
				VIBRATOR.vibrate(NORTH, -1);
				return;
			}

			// if 15 ≤ bearing ≤ 105, its east, so go right
			if (bearing > 45 && bearing <= 135) {
				Log.v(ConverseVibrator.class.getSimpleName(), "RIGHT");
				VIBRATOR.vibrate(RIGHT, -1);
				return;
			}

			/*
			 * if 105 ≤ bearing ≤ 180 or -165 ≥ bearing ≥ -180, we'll call south
			 * so make u-turn
			 */

			/*
			 * if -105 ≥ bearing ≥ -15, it's west so go left
			 */
			if (bearing < -45 && bearing >= -135) {
				Log.v(ConverseVibrator.class.getSimpleName(), "LEFT");
				VIBRATOR.vibrate(LEFT, -1);
				return;
			}

			Log.v(ConverseVibrator.class.getSimpleName(), "SOUTH");
			VIBRATOR.vibrate(SOUTH, -1);
			return;

		}

	}

	public static void vibrateForDirection(String s, Context c) {
		vibrateForDirection(s, NON_VAL, c);
	}

}
