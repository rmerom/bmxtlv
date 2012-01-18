package com.studiosix.tlvbikes;

import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class StationMapFragment extends Fragment {
	private View mMapViewContainer;
	private MapView mMapView;
	private MapController mMapController;
	private MyLocationOverlay mMyLocationOverlay;
	private StationManager mStationManager;
	private Context mContext;

	public StationMapFragment(Context context, StationManager stationManager) {
		mMapViewContainer = LayoutInflater.from(context).inflate(R.layout.map_fragement, null);
		mMapView = (MapView)mMapViewContainer.findViewById( R.id.mapview );
		mStationManager = stationManager;
		mContext = context;
		mMapController = mMapView.getController();

		// Set up map
		mMapView.setBuiltInZoomControls(false);
		mMapController = mMapView.getController();
		mMapController.animateTo(new GeoPoint(32066501, 34777822)); // Tel Aviv
		mMapController.setZoom(15);
		
		// Set up user location
		mMyLocationOverlay = new MyLocationOverlay(mContext, mMapView);
		mMyLocationOverlay.runOnFirstFix(new Runnable() {
			public void run() {
				GeoPoint userLocation = mMyLocationOverlay.getMyLocation();
				if (userLocation.getLatitudeE6() < 3213194 &&
						userLocation.getLatitudeE6() > 32030381 &&
						userLocation.getLongitudeE6()  < 34847946 &&
						userLocation.getLongitudeE6() > 34739285) {
					mMapController.animateTo(mMyLocationOverlay.getMyLocation());
					mMapController.setZoom(20);
				}
				mStationManager.setUserLocation(userLocation);
				mStationManager.setUserLocationNew(mMyLocationOverlay.getLastFix());
			}
		});
		mMapView.getOverlays().add(mMyLocationOverlay);
		mMapView.postInvalidate();
		ImageButton centerMyLocationButton = (ImageButton)mMapViewContainer.findViewById(R.id.centerMyLocationButton);
		centerMyLocationButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				centerMapAroundUser();
			}
		});
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView( inflater, container, savedInstanceState );

		MainActivity mapActivity = (MainActivity) getActivity();		
		mContext = mapActivity;

		return mMapViewContainer;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		ViewGroup parentViewGroup = (ViewGroup) mMapViewContainer.getParent();
		if( null != parentViewGroup ) {
			parentViewGroup.removeView( mMapViewContainer );
		}
	}

	public void drawStationsOnMap() {		
		Drawable greenbike = this.getResources().getDrawable(R.drawable.ic_station_ok);
		StationOverlay stationOverlay = new StationOverlay(greenbike, mContext, mStationManager.getStations());	
		List<Overlay> mapOverlays = mMapView.getOverlays();
		mapOverlays.add(stationOverlay);
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
}
