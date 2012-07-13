package com.telofast.server;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheException;
import net.sf.jsr107cache.CacheFactory;
import net.sf.jsr107cache.CacheManager;

import com.google.appengine.api.memcache.jsr107cache.GCacheFactory;
import com.google.gson.Gson;

@SuppressWarnings("serial")
public class StationDataServlet extends HttpServlet {
	private static final String STATIONS_KEY = "stations";
	private static final int MEMCACHE_EXPIRATION = 30;

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		if (!("h1u1").equals(req.getParameter("s"))) {
			return;
		}
		StationStatuses stationStatuses = null;
		// Try memcache first
		stationStatuses = new DAO().ofy().get(StationStatuses.class, 42L);
		Cache cache;
    try {
    	Map<String, Object> props = new HashMap<String, Object>();
    	props.put(GCacheFactory.EXPIRATION_DELTA, MEMCACHE_EXPIRATION);
    	CacheFactory cacheFactory = CacheManager.getInstance().getCacheFactory();
    	cache = cacheFactory.createCache(Collections.emptyMap());
    	if (cache.containsKey(STATIONS_KEY )) {
    		stationStatuses = (StationStatuses) cache.get(STATIONS_KEY);
    	} else {
    		cache.put(STATIONS_KEY, stationStatuses);
    	}
    } catch (CacheException e) {
        // ...
    }
		List<StationStatus> stations = stationStatuses.getStationStatuses();
		resp.setContentType("text/plain; charset=UTF-8");
		resp.setCharacterEncoding("UTF-8");
		resp.setHeader("Cache-Control","no-cache"); //HTTP 1.1
		resp.setHeader("Pragma","no-cache"); //HTTP 1.0
		resp.setDateHeader ("Expires", 0); //prevents caching at the proxy server
		Gson gson = new Gson();
		resp.getWriter().println(gson.toJson(stations));
		resp.getWriter().close();
	}
}
