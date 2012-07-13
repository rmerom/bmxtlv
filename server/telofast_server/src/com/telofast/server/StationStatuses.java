package com.telofast.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.googlecode.objectify.annotation.Serialized;

@Entity
public class StationStatuses implements Serializable {
	private static final long serialVersionUID = 13213321811L;

	@Id
  private Long key;
	
	@Serialized private List<StationStatus> stationStatuses = new ArrayList<StationStatus>();

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
}
