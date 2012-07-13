package com.studiosix.tlvbikes;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;

import android.location.Location;

import com.google.android.maps.GeoPoint;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class StationManager {
	private static final String NEW_STATION_STATUS_URL = "https://tel-o-fast.appspot.com/stationdata?s=h1u1";

	private HashMap<String, Station> mStationsMap = new HashMap<String, Station>();
	private GeoPoint mUserLocation;
	private Location mUserLocationNew;
	
	private final String TAG = StationManager.class.getSimpleName();
	
	public StationManager() {
	}

	public Collection<StationStatus> retrieveStationStatuses() {
		Collection<StationStatus> stationsStatus = null;
		URL url;
		try {
			url = new URL(NEW_STATION_STATUS_URL);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			InputStream instream = new BufferedInputStream(urlConnection.getInputStream());
			String result = Utils.convertStreamToString(instream);
			Gson gson = new Gson();
			Type type =  new TypeToken<Collection<StationStatus>>(){}.getType();
			stationsStatus = gson.fromJson(result, type);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return stationsStatus;
	}
		
	public void init() {
		refresh();
		/*
		populateStationMap(stations);
		//ServerData serverData = null; //retrieveDataFromServer(false);
		getStationList(serverData);
		updateStationStatus(serverData);*/
	}

	public void refresh() {
		Collection<StationStatus> stations = retrieveStationStatuses();
		populateStationMap(stations);
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
	
	private void populateStationMap(Collection<StationStatus> stationsStatus) {
		mStationsMap.clear();
		for (StationStatus stationStatus : stationsStatus) {
			String id = String.valueOf(stationStatus.getId());
			int lat = (int) (stationStatus.getLat() * 1e6);
			int lng = (int) (stationStatus.getLng() * 1e6);
			Station station = new Station(stationStatus.getName(), id, lat, lng);
			station.setAvailableBikes(stationStatus.getNumBikes());
			station.setAvailableDocks(stationStatus.getNumDocks());
			mStationsMap.put(id, station);
		}
	}
	
	/*
	private void getStationList(ServerData serverData) {
		String namesString = serverData.names;
		String[] stationNames = namesString.substring(namesString.indexOf("id")).split(",");
		String[] stationCords = serverData.locations.split(", ");
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
	/*
	private ServerData retrieveDataFromServer(boolean retrieveOnlyStatuses) {
		ExecutorService executor = Executors.newCachedThreadPool();
		Future<String> statuses = executor.submit(new DownloadSpreadsheetsRowTask(STATION_STATUS_URL));
		Future<String> names = null;
		Future<String> locations = null;
		if (!retrieveOnlyStatuses) {
			names = executor.submit(new DownloadSpreadsheetsRowTask(STATION_NAMES_URL));
			locations = executor.submit(new DownloadSpreadsheetsRowTask(STATION_LOCATION_URL));
		}
		try {
			executor.awaitTermination(15, TimeUnit.SECONDS);
		  ServerData serverData = new ServerData();
		  serverData.statuses = statuses.get();
		  if (names != null) {
		  	serverData.names = names.get();
		  	serverData.locations = locations.get();
		  }
		  return serverData;
		} catch (InterruptedException e) {
			Log.e(TAG, "exception while downloading data", e);
		} catch (ExecutionException e) {
			Log.e(TAG, "exception while downloading data", e);
		}
		return null;
	}*/
/*
	private void updateStationStatus(ServerData serverData) {
		String[] stationCords = serverData.statuses.split(", ");
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
			} else if (availableBikes == 1 || availableBikes == 2) {
				station.setStatus(Station.Status.FEW_BIKES);
			} else if (availableDocks == 0) {
				station.setStatus(Station.Status.NO_DOCKS);
			} else if (availableDocks == 1 | availableDocks == 2) {
				station.setStatus(Station.Status.FEW_DOCKS);
			} else {
				station.setStatus(Station.Status.OK);
			}
		}
	}*/
/*
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
	
	private class DownloadSpreadsheetsRowTask implements Callable<String> {

		private final String urlString;
		
		DownloadSpreadsheetsRowTask(String urlString) {
			this.urlString = urlString;
		}

		public String call() throws Exception {
			URL url = new URL(urlString);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			try {
		     InputStream instream = new BufferedInputStream(urlConnection.getInputStream());
					String result= Utils.convertStreamToString(instream);
					JSONObject jsonResult = new JSONObject(result);
					JSONObject entry = jsonResult.getJSONObject("entry");
					JSONObject content0 = entry.getJSONObject("content");
					String data = content0.getString("$t");
		     return data;
			} finally {
		     urlConnection.disconnect();
			}
		}
	}
	
	private static class ServerData {
		public String names;
		public String locations;
		public String statuses;
		
	}*/
}
