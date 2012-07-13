package com.telofast.server;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Represents a station and its status.
 */
@Embeddable
public class StationStatus implements Serializable {

	private static final long serialVersionUID = 19408321409L;

	private Long id;
	
	private int numBikes;
	
	private int numDocks;
	
	private String name;
	
	private float lat;
	private float lng;
	
	private Date lastUpdate;
	
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public int getNumBikes() {
		return numBikes;
	}
	public void setNumBikes(int numBikes) {
		this.numBikes = numBikes;
	}
	public int getNumDocks() {
		return numDocks;
	}
	public void setNumDocks(int numDocks) {
		this.numDocks = numDocks;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public float getLat() {
		return lat;
	}
	public void setLat(float lat) {
		this.lat = lat;
	}
	public float getLng() {
		return lng;
	}
	public void setLng(float lng) {
		this.lng = lng;
	}
	public Date getLastUpdate() {
		return lastUpdate;
	}
	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
}
