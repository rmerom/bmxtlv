package com.studiosix.tlvbikes;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;
import android.util.Log;

import com.google.android.maps.GeoPoint;

public class StationManager {
	private static final String STATION_NAMES_URL = "https://spreadsheets.google.com/feeds/list/0AoOjWPdv2TXodHlMTTFrakJKR2F6cldJTGktQnNXV0E/od6/public/basic/cn6ca?alt=json";
	private static final String STATION_LOCATION_URL = "https://spreadsheets.google.com/feeds/list/0AoOjWPdv2TXodHlMTTFrakJKR2F6cldJTGktQnNXV0E/od6/public/basic/205aqv?alt=json";
	private static final String STATION_STATUS_URL = "https://spreadsheets.google.com/feeds/list/0AoOjWPdv2TXodHlMTTFrakJKR2F6cldJTGktQnNXV0E/od6/public/basic/25ncrc?alt=json";

	private HashMap<String, Station> mStationsMap = new HashMap<String, Station>();
	private GeoPoint mUserLocation;
	private Location mUserLocationNew;
	
	public StationManager() {
	}

	public void init() {
		getStationList();
		updateStationStatus();
	}

	public void refresh() {
		updateStationStatus();
	}

	public Collection<Station> getStations() {
		return mStationsMap.values();
	}

	public void setUserLocation(GeoPoint userLocation) {
		mUserLocation = userLocation;
	}

	public GeoPoint getUserLocation() {
		return mUserLocation;
	}

	public void setUserLocationNew(Location userLocation) {
		mUserLocationNew = userLocation;
	}

	public Location getUserLocationNew() {
		return mUserLocationNew;
	}
	
	private void getStationList() {
		String namesString = getDataFromServer(STATION_NAMES_URL);
		String[] stationNames = namesString.substring(namesString.indexOf("id")).split(",");
		String[] stationCords = getDataFromServer(STATION_LOCATION_URL).split(", ");
		if (stationCords.length != stationNames.length) {
			Log.i("ERROR", "Names and cords not same length");
		}

		// Insert into map
		for (int i = 0; i < stationCords.length; i++) {
			String[] latLongLine = stationCords[i].split("(: )|(; )");
			String id = latLongLine[0].trim();
			int latitude = (int) (Double.parseDouble(latLongLine[1]) * 1e6);
			int longtitude = (int) (Double.parseDouble(latLongLine[2]) * 1e6);
			String[] nameLine = stationNames[i].split(": ");
			String name = nameLine[1].trim();
			Station station = new Station(name, id, latitude, longtitude);
			mStationsMap.put(id, station);
		}
		
	}

	private void updateStationStatus() {
		String[] stationCords = getDataFromServer(STATION_STATUS_URL).split(", ");
		for (int i = 0; i < stationCords.length; i++) {
			String[] latLongLine = stationCords[i].split("(: )");
			String id = latLongLine[0];
			if (!id.startsWith("id")) {
				continue;
			}
			int statusNumber = 0;
			try {
				statusNumber = Integer.parseInt(latLongLine[1]);
			} catch (NumberFormatException e) {
			}
			Station station = mStationsMap.get(id);
			if (station == null) {
				continue;
			}
			int availableBikes = statusNumber % 100;
			int availableDocks = statusNumber / 100;
			station.setAvailableBikes(availableBikes);
			station.setAvailableDocks(availableDocks);
			if (statusNumber == 0) {
				station.setStatus(Station.Status.NO_INFO);
			} else if (availableBikes == 0) {
				station.setStatus(Station.Status.NO_BIKES);
			} else if (availableBikes == 1) {
				station.setStatus(Station.Status.ONE_BIKE);
			} else if (availableDocks == 0) {
				station.setStatus(Station.Status.NO_DOCKS);
			} else if (availableDocks == 1) {
				station.setStatus(Station.Status.ONE_DOCK);
			} else {
				station.setStatus(Station.Status.OK);
			}
		}
	}

	private String getDataFromServer(String URL) {
		HttpClient client = new DefaultHttpClient();
		HttpConnectionParams.setConnectionTimeout(client.getParams(), 20000);
		HttpGet request = new HttpGet(URL); 

		try {
			HttpEntity entity = client.execute(request).getEntity();
			if (entity != null) {
				InputStream instream = entity.getContent();

				// Parse response
				String result= Utils.convertStreamToString(instream);
				JSONObject jsonResult = new JSONObject(result);
				JSONObject entry = jsonResult.getJSONObject("entry");
				JSONObject content0 = entry.getJSONObject("content");
				String data = content0.getString("$t");
				instream.close();
				return data;
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return null;
	}
}
