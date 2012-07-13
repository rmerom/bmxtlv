package com.studiosix.tlvbikes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class Utils {
	public static String convertStreamToString(InputStream is) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	public static String formatDistance(double distance) {
		if (distance < 1000) {
			return String.format("%1$.0fm", distance);
		}
		if (distance < 1000000) {
			return String.format("%1$.1fkm", distance / 1000);
		}
		return String.format("%1$.1fkkm", distance / 1000000);
	}

	public static double bearing(double lat1, double lon1, double lat2, double lon2) {
		double lat1Rad = Math.toRadians(lat1);
		double lat2Rad = Math.toRadians(lat2);
		double deltaLonRad = Math.toRadians(lon2 - lon1);

		double y = Math.sin(deltaLonRad) * Math.cos(lat2Rad);
		double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) - Math.sin(lat1Rad) * Math.cos(lat2Rad)
				* Math.cos(deltaLonRad);
		return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
	}
	
	public static void navigateTo(Context context, int lat, int lng) {
		Intent intent = new Intent(Intent.ACTION_VIEW, 
				Uri.parse("http://maps.google.com/maps?f=d&daddr=" + lat / 1.0e6 + "," + lng / 1.0e6 +"&dirflg=w"));
		intent.setComponent(new ComponentName("com.google.android.apps.maps", 
				"com.google.android.maps.MapsActivity"));
		context.startActivity(intent);

	}
}
