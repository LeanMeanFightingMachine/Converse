package uk.lmfm.converse.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class IOUtils {

	public static void closeQuietly(InputStream is) {

		try {
			is.close();
		} catch (IOException ignored) {

		} catch (NullPointerException npe) {

		}
	}

	public static void closeQuietly(Reader r) {

		try {
			r.close();
		} catch (IOException ignored) {
		}

		catch (NullPointerException npe) {
		}

	}

	public static boolean isOnline(Context c) {
		ConnectivityManager connMgr = (ConnectivityManager) c
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		return (networkInfo != null && networkInfo.isConnected());
	}

}
