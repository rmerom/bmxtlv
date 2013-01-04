package com.studiosix.tlvbikes;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.location.Location;

import com.google.android.maps.GeoPoint;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class StationManager {
	private static final String TELOFAST_URL = "https://tel-o-fast.appspot.com/stationdata?s=h1u2";
	private static final String TELOFUN_WEBSITE_URL = 
			"http://www.tel-o-fun.co.il/%D7%AA%D7%97%D7%A0%D7%95%D7%AA%D7%AA%D7%9C%D7%90%D7%95%D7%A4%D7%9F.aspx";
	private long MAX_STATION_DATA_AGE_MSECS = 6 /* mins */ * 60 /* secs/mins */ * 1000 /* msecs/secs */;

	private HashMap<String, Station> mStationsMap = new HashMap<String, Station>();
	private GeoPoint mUserLocation;
	private Location mUserLocationNew;
	private Context mContext;
	
	// private final String TAG = StationManager.class.getSimpleName();
	
	public StationManager(Context context) {
		mContext = context;
	}

	public StationStatuses retrieveStationStatuses() {
		StationStatuses stationsStatuses = null;
		URL url;
		try {
			url = new URL(TELOFAST_URL);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			InputStream instream = new BufferedInputStream(urlConnection.getInputStream());
			String result = Utils.convertStreamToString(instream);
			Gson gson = new GsonBuilder().setDateFormat("MMM dd, yyyy HH:mm:ss zzzz").create();
			stationsStatuses = gson.fromJson(result, StationStatuses.class);
		} catch (IOException e) {
			// Fallback to telofun original website.
			try {
				Date now = new Date();
				String content = readTelofunPage();
				List<StationStatus> stationsStatusList = parseTelofunContent(content, now);
				stationsStatuses = new StationStatuses();
				// stationsStatuses.setTimestamp(now);
				stationsStatuses.setStationStatuses(stationsStatusList);
			} catch (IOException e1) {
				throw new RuntimeException("Error falling back to telofun", e1);
			}
			
		}
		return stationsStatuses;
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
	
	//Markers for which we look are of the form:
	//setMarker(34.8179,32.1223,202,
	//'×�×”×¨×•×Ÿ ×‘×§×¨ 15',
	//'','20', '11', 
	//'<a href="javascript:void()" onclick="JumpToStation(34.8214,32.1202,203);"><span style="text-decoration:underline">×œ×�×” 16 ×¤×™× ×ª ×�×œ×ª×¨×ž×Ÿ</span><br></a><a href="javascript:void()" onclick="JumpToStation(34.8132,32.1176,214);"><span style="text-decoration:underline">×ª×œ ×‘×¨×•×š ×‘×™×”×¡ ×�×œ×—×¨×™×–×™</span><br></a><a href="javascript:void()" onclick="JumpToStation(34.8258,32.1218,204);"><span style="text-decoration:underline">×¦×”×œ ×¤×™× ×ª ×¢×™×¨ ×©×ž×© ×‘×›×™×›×¨</span><br></a>');
	private List<StationStatus> parseTelofunContent(String content, Date timestamp) {
		final String wsNumberWs = "\\s*([\\d|\\.]+)\\s*"; 
		String patternToFind = "setMarker\\(" + wsNumberWs + "," + wsNumberWs + ",\\s*(\\d+)\\s*,\\s*'(.*?)'\\s*,.*?,\\s*'(\\d+)'\\s*,\\s*'(\\d+)";
		Pattern pattern = Pattern.compile(patternToFind);
		Matcher matcher = pattern.matcher(content);
		List<StationStatus> stations = new ArrayList<StationStatus>();
		while (matcher.find()) {
			StationStatus stationStatus = new StationStatus();
			stationStatus.setLng(Float.valueOf(matcher.group(1)));
			stationStatus.setLat(Float.valueOf(matcher.group(2)));
			stationStatus.setId(Long.valueOf(matcher.group(3)));
			stationStatus.setName(matcher.group(4));
			int poles = Integer.valueOf(matcher.group(5));
			int available = Integer.valueOf(matcher.group(6));
			stationStatus.setNumDocks(available);
			stationStatus.setNumBikes(poles - available);
			stationStatus.setLastUpdate(timestamp);
			stations.add(stationStatus);
		}
		return stations;
	}

	private String readTelofunPage() throws IOException {
		final int LIMIT_TELOFUN_CONTENT_CHARS = 1024 * 1024; // 1 Mega characters.
		URL url = new URL(TELOFUN_WEBSITE_URL);
		BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
		StringBuilder content = new StringBuilder();
		String line;

		while ((line = reader.readLine()) != null && content.length() < LIMIT_TELOFUN_CONTENT_CHARS) {
			content.append(line).append("\n");
		}
		reader.close();

		return content.toString();
	}

}
