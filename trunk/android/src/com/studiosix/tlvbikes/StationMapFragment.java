package com.studiosix.tlvbikes;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

public class StationMapFragment extends Fragment {
	private View mRootView;
	private MapView mMapView;
	private MapController mMapController;
	private MyLocationOverlay mMyLocationOverlay;
	private StationOverlay mStationsOverlay;
	private StationManager mStationManager;
	private Context mContext;

	public StationMapFragment() {
	}
	
	@Override
	public void onActivityCreated(Bundle arg0) {
		Log.w("StationMapFragment", "onActvityCreated called");
		super.onActivityCreated(arg0);
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.w("StationMapFragment", "onDestroy called");
	}

	@Override
	public void onDestroyView() {
		Log.w("StationMapFragment", "onDestroyView called");
		super.onDestroyView();
		if (mRootView.getParent() != null) {
			((ViewGroup)mRootView.getParent()).removeView(mRootView);
		}
	}


	@Override
	public void onDetach() {
		super.onDetach();
		Log.w("StationMapFragment", "onDetach called");
	}




	@Override
	public void onCreate(Bundle state) {
		Log.w("StationMapFragment", "onCreate called");
		mContext = getActivity();
		super.onCreate(state);
	}
	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.w("StationMapFragment", "onCreateView called");
		super.onCreateView(inflater, container, savedInstanceState);
		if (mRootView == null) {
			mRootView = LayoutInflater.from(getActivity()).inflate(R.layout.map_fragement, null, false);
			mMapView = (MapView) mRootView.findViewById(R.id.mapview);
			// Set up map
			mMapView.setBuiltInZoomControls(false);
			final GeoPoint defaultLocation = new GeoPoint(32066501, 34777822);  // Tel Aviv
			mMapView.getController().setCenter(defaultLocation);

			mMapController = mMapView.getController();
			mMapController.setZoom(15);
			// Set up user location
			mMyLocationOverlay = new MyLocationOverlay(mContext, mMapView);
			mMyLocationOverlay.runOnFirstFix(new Runnable() {
				public void run() {
					GeoPoint userLocation = mMyLocationOverlay.getMyLocation();
					if (userLocation == null) {
						return;
					}
					if (userLocation.getLatitudeE6() < 32131940 &&
							userLocation.getLatitudeE6() > 32030381 &&
							userLocation.getLongitudeE6()  < 34847946 &&
							userLocation.getLongitudeE6() > 34739285) {
						mMapController.animateTo(mMyLocationOverlay.getMyLocation());
					}
					mMapController.setZoom(17);
					mStationManager.setUserLocation(userLocation);
					mStationManager.setUserLocationNew(mMyLocationOverlay.getLastFix());
				}
			});
			mMapView.getOverlays().add(mMyLocationOverlay);
			ImageButton centerMyLocationButton = (ImageButton)mRootView.findViewById(R.id.centerMyLocationButton);
			centerMyLocationButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					centerMapAroundUser();
				}
			});
			Drawable greenbike = this.getResources().getDrawable(R.drawable.ic_station_ok);
			mStationsOverlay = new StationOverlay(greenbike, mContext);
			mMapView.getOverlays().add(mStationsOverlay);
		}

		return mRootView;
	}
	
	@Override
	public void onAttach(Activity activity) {
		Log.w("StationMapFragment", "onAttach called");

		super.onAttach(activity);
		mStationManager = ((HasStationManager) activity).getStationManager();
	}

	public void drawStationsOnMap() {		
		mStationsOverlay.clear();
		mStationsOverlay.addStations(mStationManager.getStations());
		mMapView.postInvalidate();
	}
	
	public void enableLocation() {
		if (mMyLocationOverlay != null) {
			mMyLocationOverlay.enableMyLocation();		
		}
	}
	
	public void disableLocation() {
		if (mMyLocationOverlay != null) {
			mMyLocationOverlay.disableMyLocation();
		}
	}
	
	private void centerMapAroundUser() {
		Toast toast = Toast.makeText(mContext, "Getting your location...", Toast.LENGTH_SHORT);
		toast.show();

		mMyLocationOverlay.runOnFirstFix(new Runnable() {
			public void run() {
				GeoPoint userLocation = mMyLocationOverlay.getMyLocation();
				mMapController.animateTo(userLocation);
				mStationManager.setUserLocation(userLocation);
			}
		});
	}

	public MapView getMapView() {
		return mMapView;
	}
}
