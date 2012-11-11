package com.studiosix.tlvbikes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class StationStatuses implements Serializable {
	private static final long serialVersionUID = 13213321811L;

	private Long key;

	private List<StationStatus> stationStatuses = new ArrayList<StationStatus>();

	private Date timestamp = new Date();

	public Long getKey() {
		return key;
	}

	public void setKey(Long key) {
		this.key = key;
	}

	public List<StationStatus> getStationStatuses() {
		return stationStatuses;
	}

	public void setStationStatuses(List<StationStatus> stationStatuses) {
		this.stationStatuses = stationStatuses;
	}

	public Date getTimestamp() {
		return timestamp;
	}
}
