package uk.lmfm.converse.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class DirectionsWrapper {

	/**
	 * This is a convenience class for storing possible status codes that can be
	 * returned from the directions API. See <a
	 * href="here for a current list of possible status codes returned"
	 * >https://developers
	 * .google.com/maps/documentation/directions/#StatusCodes</a>
	 * 
	 * @author niallgunter
	 * 
	 */
	private final static class APIStatusCodes {

		private static final Map<String, String> ERR_MESSAGES;

		// indicates the response contains a valid result
		public static final String OK = "OK";

		// indicates at least one of the locations specified in the requests's
		// origin, destination, or waypoints could not be geocoded.
		public static final String NOT_FOUND = "NOT_FOUND";

		// indicates no route could be found between the origin and destination.
		public static final String ZERO_RESULTS = "ZERO_RESULTS";

		/*
		 * indicates that too many waypointss were provided in the request. The
		 * maximum allowed waypoints is 8, plus the origin, and destination. (
		 * Google Maps API for Business customers may contain requests with up
		 * to 23 waypoints.)/*
		 */
		private static final String MAX_WAYPOINTS_EXCEEDED = "MAX_WAYPOINTS_EXCEEDED";

		// indicates that the provided request was invalid. Common causes of
		// this status include an invalid parameter or parameter value.
		private static final String INVALID_REQUEST = "INVALID_REQUEST";

		// indicates the service has received too many requests from your
		// application within the allowed time period.
		private static final String OVER_QUERY_LIMIT = "OVER_QUERY_LIMIT";

		// indicates that the service denied use of the directions service by
		// your application.

		private static final String REQUEST_DENIED = "REQUEST_DENIED";

		// indicates a directions request could not be processed due to a server
		// error. The request may succeed if you try again.
		private static final String UNKNOWN_ERROR = "UNKNOWN_ERROR";

		static {
			ERR_MESSAGES = new HashMap<String, String>();
			ERR_MESSAGES
					.put(NOT_FOUND,
							"Could not find one or both locations specified in navigation");
			ERR_MESSAGES.put(ZERO_RESULTS,
					"No results could be found for the specified route");
			ERR_MESSAGES.put(MAX_WAYPOINTS_EXCEEDED,
					"Maximum number of allowed waypoints exceeded");
			ERR_MESSAGES
					.put(INVALID_REQUEST,
							"Invalid request made. Parameter or Parameter value may not be valid.");
			ERR_MESSAGES
					.put(OVER_QUERY_LIMIT,
							"Too many requests to API have been made within allowed time period.");
			ERR_MESSAGES.put(REQUEST_DENIED, "API Request has been denied");
			ERR_MESSAGES
					.put(UNKNOWN_ERROR,
							"Request could not be processed due to server error. Please try again.");
		}

		public static boolean isOK(String s) {
			return s.equals(OK);
		}

		public static String getErrorMessageForStatus(String s) {
			return ERR_MESSAGES.get(s);
		}

	}

	private static final String URL_BASE = "https://maps.googleapis.com/maps/api/directions/json?"
			+ "origin=%s,%s&destination=%s,%s&sensor=true&mode=walking";

	private double latTo;
	private double lonTo;
	private double latFrom;
	private double lonFrom;

	public DirectionsWrapper(double latTo, double lonTo, double latFrom,
			double lonFrom) {
		super();
		this.latTo = latTo;
		this.lonTo = lonTo;
		this.latFrom = latFrom;
		this.lonFrom = lonFrom;
	}

	public Journey getJourney() {
		String url = buildRequestURL();

		String json = getJsonResponse(url);

		try {
			return parseJSON(json);
		} catch (DirectionsWrapperException dwe) {
			Log.e(getClass().getSimpleName(), "Error parsing JSON", dwe);
		}
		return null;
	}

	private String buildRequestURL() {
		return String.format(URL_BASE, latFrom, lonFrom, latTo, lonTo);
	}

	private String getJsonResponse(String url) {
		Log.i(getClass().getSimpleName(), "Making request at address: " + url);

		DefaultHttpClient httpclient = new DefaultHttpClient(
				new BasicHttpParams());
		HttpPost httppost = new HttpPost(url);
		// Depends on your web service
		httppost.setHeader("Content-type", "application/json");

		InputStream inputStream = null;
		String result = null;
		BufferedReader br = null;
		try {
			HttpResponse response = httpclient.execute(httppost);
			HttpEntity entity = response.getEntity();

			inputStream = entity.getContent();
			// json is UTF-8 by default i beleive
			br = new BufferedReader(
					new InputStreamReader(inputStream, "UTF-8"), 8);
			StringBuilder sb = new StringBuilder();

			String line = null;
			while ((line = br.readLine()) != null) {
				sb.append(line + "\n");
			}

			result = sb.toString();
			Log.v(getClass().getSimpleName(), "JSON Response:\n" + result);
		} catch (IOException ioe) {
			Log.e(getClass().getSimpleName(), "IOException", ioe);

		} finally {
			// Close our resources quietly
			IOUtils.closeQuietly(br);
			IOUtils.closeQuietly(inputStream);
		}
		return result;

	}

	/*
	 * catch (DirectionsWrapperException dwe) {
	 * Log.e(getClass().getSimpleName(), dwe.getMessage(), dwe); }
	 */

	private Journey parseJSON(String s) throws DirectionsWrapperException {
		try {
			JSONObject jObject = new JSONObject(s);
			String status = jObject.getString("status");

			if (!APIStatusCodes.isOK(status))
				throw new DirectionsWrapperException(String.format("%s",
						APIStatusCodes.getErrorMessageForStatus(status)));

			JSONArray routes = jObject.optJSONArray("routes");

			if ((routes != null) && (routes.length() > 0)) {
				// Get the first possible route
				JSONObject route = routes.optJSONObject(0);

				if (route != null) {
					JSONArray legs = route.optJSONArray("legs");

					if ((legs != null) && (legs.length() > 0)) {

						Journey journey = new Journey();

						for (int i = 0; i < legs.length(); i++) {
							JSONArray steps = legs.getJSONObject(i)
									.getJSONArray("steps");

							for (int j = 0; j < steps.length(); j++) {
								JSONObject step = steps.getJSONObject(j);
								double startLat, startLon, endLat, endLon;
								String instructions;

								JSONObject start, end;
								end = step.getJSONObject("end_location");
								endLat = end.getDouble("lat");
								endLon = end.getDouble("lng");

								instructions = step
										.getString("html_instructions");

								start = step.getJSONObject("start_location");
								startLat = start.getDouble("lat");
								startLon = start.getDouble("lng");

								journey.addStep(startLat, startLon, endLat,
										endLon, instructions);
							}

						}

						Log.v(getClass().getSimpleName(),
								String.format("Testing journey: %s",
										journey.toString()));
						return journey;

					}

				}
			}

		} catch (JSONException js) {
			Log.e(getClass().getSimpleName(),
					"Error parsing input string as JSON", js);
		}
		return null;

	}

	class DirectionsWrapperException extends Exception {
		public DirectionsWrapperException(String message) {
			super(message);
		}

		public DirectionsWrapperException(String message, Throwable throwable) {
			super(message, throwable);
		}
	}

	/* Getters & Setters for fields */

	public final double getLatTo() {
		return latTo;
	}

	public final void setLatTo(double latTo) {
		this.latTo = latTo;
	}

	public final double getLonTo() {
		return lonTo;
	}

	public final void setLonTo(double longTo) {
		this.lonTo = longTo;
	}

	public final double getLatFrom() {
		return latFrom;
	}

	public final void setLatFrom(double latFrom) {
		this.latFrom = latFrom;
	}

	public final double getLonFrom() {
		return lonFrom;
	}

	public final void setLonFrom(double lonFrom) {
		this.lonFrom = lonFrom;
	}

}
