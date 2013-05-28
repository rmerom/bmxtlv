package com.telofast.server.cron;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.googlecode.objectify.Objectify;
import com.telofast.server.DAO;
import com.telofast.server.StationStatus;
import com.telofast.server.StationStatuses;

public class TelofunDataFetchServlet  extends HttpServlet {
	private static final Logger LOG = Logger.getLogger(TelofunDataFetchServlet.class.getSimpleName());

	private final Logger log = Logger.getLogger(TelofunDataFetchServlet.class.getSimpleName());
	private final String TELOFUN_URL = "http://www.tel-o-fun.co.il/%D7%AA%D7%97%D7%A0%D7%95%D7%AA%D7%AA%D7%9C%D7%90%D7%95%D7%A4%D7%9F.aspx";
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		Date now = new Date();
		String content = readTelofunPage();
		List<StationStatus> stations = parseTelofunContent(content, now);
		Objectify ofy = new DAO().ofy();
		//ofy.put(stations);
		StationStatuses stationStatuses = new StationStatuses();
		stationStatuses.setKey(42L);
		stationStatuses.setStationStatuses(stations);
		stationStatuses.setTimestamp(now);
		ofy.put(stationStatuses);
		//Utils.createCache().put(Utils.STATIONS_KEY, stationStatuses);
	}

	//Markers for which we look are of the form:
	//setMarker(34.8179,32.1223,202,
	//'אהרון בקר 15',
	//'','20', '11', 
	//'<a href="javascript:void()" onclick="JumpToStation(34.8214,32.1202,203);"><span style="text-decoration:underline">לאה 16 פינת אלתרמן</span><br></a><a href="javascript:void()" onclick="JumpToStation(34.8132,32.1176,214);"><span style="text-decoration:underline">תל ברוך ביהס אלחריזי</span><br></a><a href="javascript:void()" onclick="JumpToStation(34.8258,32.1218,204);"><span style="text-decoration:underline">צהל פינת עיר שמש בכיכר</span><br></a>');
	private List<StationStatus> parseTelofunContent(String content, Date timestamp) {
		final String wsNumberWs = "\\s*([\\d|\\.]+)\\s*"; 
		String patternToFind = "setMarker\\(" + wsNumberWs + "," + wsNumberWs + ",\\s*(\\d+)\\s*,\\s*'(.*?)'\\s*,.*?,\\s*'(-?\\d+)'\\s*,\\s*'(-?\\d+)";
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
		URL url = new URL(TELOFUN_URL);
		BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
		StringBuilder content = new StringBuilder();
		String line;

		while ((line = reader.readLine()) != null && content.length() < LIMIT_TELOFUN_CONTENT_CHARS) {
			content.append(line).append("\n");
		}
		log.fine("size of telofun page content (in chars: " + content.length());
		reader.close();

		return content.toString();
	}
	private static final long serialVersionUID = 4026070688237157624L;



}
