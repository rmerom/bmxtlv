package com.studiosix.tlvbikes;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ListView;

import com.google.android.maps.GeoPoint;

public class StationListFragment extends ListFragment {
	private StationManager mStationManager;
	private StationListAdapter mListAdapter;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mStationManager = ((MainActivity) getActivity()).getStationManager();
		mListAdapter = new StationListAdapter(getActivity(), mStationManager);
		setListAdapter(mListAdapter);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Station station = (Station) mListAdapter.getItem(position);
		GeoPoint geoPoint = new GeoPoint(station.getLatitude(), station.getLongtitude());
		MainActivity mainActivity = ((MainActivity) getActivity()); 
		mainActivity.getStationMapFragment().getMapView().getController().animateTo(geoPoint);
		mainActivity.showFragment(MainActivity.VisibleFragment.MAP);
		
	}
}