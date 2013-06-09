package uk.lmfm.converse.util;

import java.util.Locale;

import android.content.Context;
import android.os.Vibrator;

public class ConverseVibrator {

	private static final long[] LEFT = new long[] { 0, 250, 125, 250 };
	private static final long[] RIGHT = new long[] { 0, 250, 125, 250, 125, 250 };
	private static final long[] NORTH = new long[] { 0, 125, 64, 125 };
	private static final long[] SOUTH = new long[] { 0, 125, 64, 125, 64, 125 };
	private static final long[] CONT = new long[] { 0, 375, 125 };
	private static Vibrator VIBRATOR;

	public static void vibrateForDirection(String s, Context c) {
		VIBRATOR = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
		
		String lower = s.toLowerCase(Locale.getDefault());
		
		if (lower.contains("turn") && lower.contains("right")) {
			VIBRATOR.vibrate(RIGHT, -1);
		}
		if (lower.contains("turn") && lower.contains("left")) {
			VIBRATOR.vibrate(LEFT, -1);
		}
		if (lower.contains("continue")) {
			VIBRATOR.vibrate(CONT, -1);
		}

		if (lower.contains("head") && lower.contains("north")) {
			VIBRATOR.vibrate(NORTH, -1);
		}

		if (lower.contains("head") && lower.contains("south")) {
			VIBRATOR.vibrate(SOUTH, -1);
		}

	}

}
