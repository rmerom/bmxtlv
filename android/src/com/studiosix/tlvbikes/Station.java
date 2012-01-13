package com.studiosix.tlvbikes;

import java.util.Comparator;

import android.location.Location;

import com.google.android.maps.GeoPoint;

public class Station {
	public enum Status {
	    OK,
	    NO_BIKES,
	    ONE_BIKE,
	    NO_DOCKS,
	    ONE_DOCK,
	    NO_INFO
	}
	
	private String mName;
	private String mId;
	private int mLatitude;
	private int mLongtitude;
	private Status mStatus;
	private int mAvailableBikes;
	private int mAvailableDocks;
	
	public Station(String name, String id, int latitude, int longtitude) {
		super();
		this.mName = name;
		this.mId = id;
		this.mLatitude = latitude;
		this.mLongtitude = longtitude;
		this.mStatus = Status.NO_INFO;
		this.mAvailableBikes = 0;
		this.mAvailableDocks = 0;
	}
	
	public String getName() {
		return mName;
	}

	public String getId() {
		return mId;
	}
	
	public int getLatitude() {
		return mLatitude;
	}
	
	public int getLongtitude() {
		return mLongtitude;
	}
	
	public Status getStatus() {
		return mStatus;
	}
	
	public void setStatus(Status status) {
		mStatus = status;
	}

	public int getAvailableBikes() {
		return mAvailableBikes;
	}

	public void setAvailableBikes(int availableBikes) {
		this.mAvailableBikes = availableBikes;
	}

	public int getAvailableDocks() {
		return mAvailableDocks;
	}
	
	public void setAvailableDocks(int availableDocks) {
		this.mAvailableDocks = availableDocks;
	}
	
	public double distanceFrom(GeoPoint location) {
		double distance = 0.0;
        try {
                final float[] results = new float[3];
                Location.distanceBetween(mLatitude / 1E6, mLongtitude / 1E6, location.getLatitudeE6() / 1E6, location.getLongitudeE6() / 1E6, results);
                distance = results[0];
        }
        catch (final Exception ex) {
                distance = 0.0;
        }
        
        return distance;
	}
}

class DistanceComparator implements Comparator<Station> {
	private GeoPoint mLocation;
	
	public DistanceComparator(GeoPoint location) {
		mLocation = location;
	}

	public int compare(Station a, Station b) {
       double distanceA = a.distanceFrom(mLocation);
       double distanceB = b.distanceFrom(mLocation);
       
       return (distanceA < distanceB) ? -1 : 1;
    }
}
