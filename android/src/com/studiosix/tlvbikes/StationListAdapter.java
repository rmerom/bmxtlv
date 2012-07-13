package com.studiosix.tlvbikes;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;

public class StationListAdapter extends BaseAdapter {

	final int MAX_STATIONS = 15;
	private Context mContext;
	private StationManager mStationManager;
	private Station[] mStations;

	public StationListAdapter(Context context, StationManager stationManager) {
		mContext = context;
		mStationManager = stationManager;
		Collection<Station> stations = stationManager.getStations();
		// Copy the up to the first MAX_STATIONS.
		int numCopy = Math.min(stations.size(), MAX_STATIONS);
		mStations = new Station[numCopy];
		Iterator<Station> iter = stations.iterator();
		for (int i = 0 ; i < numCopy; ++i) {
			mStations[i] = iter.next();
		}
		DistanceComparator comparator = new DistanceComparator(stationManager.getUserLocation());
		Arrays.sort(mStations, comparator);
	}

	public int getCount() {
		return mStations.length;
	}

	public Object getItem(int position) {
		return mStations[position];
	}

	public long getItemId(int position) {
		return mStations[position].getLatitude() * mStations[position].getLongtitude();
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		RelativeLayout itemLayout;
		Station station = mStations[position];

		itemLayout= (RelativeLayout) LayoutInflater.from(mContext).inflate(R.layout.station_list_item, parent, false);

		TextView stationNameView = (TextView) itemLayout.findViewById(R.id.listTitle);
		stationNameView.setText(station.getName());

		int availableBikes = station.getAvailableBikes();
		TextView stationBikesView = (TextView) itemLayout.findViewById(R.id.listBikeCount);
		stationBikesView.setText(String.valueOf(availableBikes));
		TextView stationBikesLabelView = (TextView) itemLayout.findViewById(R.id.listBikeLabel);
		if (availableBikes == 0) {
			stationBikesView.setTextColor(Color.parseColor("#CC0000"));
			stationBikesLabelView.setTextColor(Color.parseColor("#CC0000"));
		} else if (availableBikes < 3) {
			stationBikesView.setTextColor(Color.parseColor("#FF8800"));
			stationBikesLabelView.setTextColor(Color.parseColor("#FF8800"));
		}

		int availableDocks = station.getAvailableDocks();
		TextView stationDocksView = (TextView) itemLayout.findViewById(R.id.listDockCount);
		stationDocksView.setText(String.valueOf(availableDocks));
		TextView stationDocksLabelView = (TextView) itemLayout.findViewById(R.id.listDockLabel);
		if (availableDocks == 0) {
			stationDocksView.setTextColor(Color.parseColor("#CC0000"));
			stationDocksLabelView.setTextColor(Color.parseColor("#CC0000"));
		} else if (availableDocks < 3) {
			stationDocksView.setTextColor(Color.parseColor("#FF8800"));
			stationDocksLabelView.setTextColor(Color.parseColor("#FF8800"));
		}

		TextView distanceView = (TextView) itemLayout.findViewById(R.id.listDistance);
		GeoPoint userLocation = mStationManager.getUserLocation();

		if (userLocation == null) {
			distanceView.setText("");
		} else {
			distanceView.setText(Utils.formatDistance(station.distanceFrom(userLocation)));
		}

		return itemLayout;
	}
}
