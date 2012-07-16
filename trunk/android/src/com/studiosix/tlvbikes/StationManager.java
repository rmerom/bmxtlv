package com.studiosix.tlvbikes;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import android.content.Context;
import android.location.Location;

import com.google.android.maps.GeoPoint;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class StationManager {
	private static final String STATION_STATUS_URL = "https://tel-o-fast.appspot.com/stationdata?s=h1u2";
	private long MAX_STATION_DATA_AGE_MSECS = 6 /* mins */ * 60 /* secs/mins */ * 1000 /* msecs/secs */;

	private HashMap<String, Station> mStationsMap = new HashMap<String, Station>();
	private GeoPoint mUserLocation;
	private Location mUserLocationNew;
	private Context mContext;
	
	private final String TAG = StationManager.class.getSimpleName();
	
	public StationManager(Context context) {
		mContext = context;
	}

	public StationStatuses retrieveStationStatuses() {
		StationStatuses stationsStatuses = null;
		URL url;
		try {
			url = new URL(STATION_STATUS_URL);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			InputStream instream = new BufferedInputStream(urlConnection.getInputStream());
			String result = Utils.convertStreamToString(instream);
			Gson gson = new GsonBuilder().setDateFormat("MMM dd, yyyy HH:mm:ss zzzz").create();
			stationsStatuses = gson.fromJson(result, StationStatuses.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return stationsStatuses;
	}
		
	public Long init() {
		return refresh();
	}

	public Long refresh() {
		StationStatuses stations = retrieveStationStatuses();
		long timestamp = stations.getTimestamp().getTime();
		Utils.setLastUpdateTimestamp(mContext, new Date(timestamp));
		
		Long timestampToReturn = null;
		if ((System.currentTimeMillis() - stations.getTimestamp().getTime()) > MAX_STATION_DATA_AGE_MSECS) {
			timestampToReturn = timestamp;
		}
		populateStationMap(stations.getStationStatuses());
		return timestampToReturn;
	}
	
	public boolean isStationDataStale() {
		Date lastUpdate = Utils.getLastUpdateTimestamp(mContext);
		return ((System.currentTimeMillis() - lastUpdate.getTime()) > MAX_STATION_DATA_AGE_MSECS);
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
}
