package com.telofast.server;

import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.DAOBase;

public class DAO extends DAOBase {
	static {
		//ObjectifyService.register(StationStatus.class);
		ObjectifyService.register(StationStatuses.class);
	}
}
