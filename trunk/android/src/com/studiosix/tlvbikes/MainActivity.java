package com.studiosix.tlvbikes;

import java.text.DateFormat;
import java.util.TimeZone;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

public class MainActivity extends MapActivity {
	private StationManager mStationManager;
	private MapView mMapView = null;
	MyLocationOverlay mMyLocationOverlay;
	private boolean mIsFetching = false;
	private StationOverlay mStationsOverlay;
	private ViewSwitcher mSwitcher;
	StationListAdapter mListAdapter;
	
	public MainActivity() {
		super();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mStationManager = new StationManager(this);

		mSwitcher = (ViewSwitcher)findViewById(R.id.mainViewSwitcher);
		configureStationsList();
		configureMapView();
		
		// Set up action bar
		final ImageButton viewButton = (ImageButton) findViewById(R.id.viewButton);
		viewButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mSwitcher.showNext();
			}
		});
		ImageButton refreshButton = (ImageButton) findViewById(R.id.refreshButton);
		refreshButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				new UpdateStationsTask().execute();
			}
		});

		// Update station manager
		new UpdateStationsTask().execute();
	}

	public void configureStationsList() {
		final ListView stationListView = (ListView) findViewById(R.id.stationList);
		mListAdapter = new StationListAdapter(this, mStationManager);
		stationListView.setAdapter(mListAdapter);
		stationListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View itemView, int position,
					long arg3) {
				int currentId = mSwitcher.getCurrentView().getId();
				if (currentId != R.id.stationListContainer)
					return;
				Station station = (Station) mListAdapter.getItem(position);
				GeoPoint geoPoint = new GeoPoint(station.getLatitude(), station
						.getLongtitude());
				mMapView.getController().animateTo(geoPoint);
				mSwitcher.showNext();
			}
		});
	}

	private void configureMapView() {
		mMapView = (MapView) findViewById(R.id.mapview);
		mMapView.setBuiltInZoomControls(false);
		final GeoPoint defaultLocation = new GeoPoint(32066501, 34777822);  // Tel Aviv
		mMapView.getController().setCenter(defaultLocation);

		final MapController mMapController = mMapView.getController();
		mMapController.setZoom(15);
		// Set up user location
		mMyLocationOverlay = new MyLocationOverlay(this, mMapView);
		mMyLocationOverlay.enableMyLocation();
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
		ImageButton centerMyLocationButton = (ImageButton)findViewById(R.id.centerMyLocationButton);
		centerMyLocationButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				centerMapAroundUser();
			}
		});
		Drawable greenbike = this.getResources().getDrawable(R.drawable.ic_station_ok);
		mStationsOverlay = new StationOverlay(greenbike, this);
		mMapView.getOverlays().add(mStationsOverlay);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mMyLocationOverlay != null) {
			mMyLocationOverlay.enableMyLocation();	
			// mMyLocationOverlay.enableCompass();	
		}
		if (!mIsFetching && mStationManager.isStationDataStale()) {
			new UpdateStationsTask().execute();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mMyLocationOverlay != null) {
			mMyLocationOverlay.disableMyLocation();
			// mMyLocationOverlay.disableCompass();
		}
	}

	private void centerMapAroundUser() {
		Toast toast = Toast.makeText(this, "Getting your location...", Toast.LENGTH_SHORT);
		toast.show();

		mMyLocationOverlay.runOnFirstFix(new Runnable() {
			public void run() {
				GeoPoint userLocation = mMyLocationOverlay.getMyLocation();
				mMapView.getController().animateTo(userLocation);
				mStationManager.setUserLocation(userLocation);
			}
		});
		
	}
	
	protected boolean isRouteDisplayed() {
		return false;
	}

	private class UpdateStationsTask extends AsyncTask<Void, Void, Void> {
		private final ProgressDialog dialog = new ProgressDialog(MainActivity.this);
		private Long mTimestamp;

		// can use UI thread here
		protected void onPreExecute() {
			dialog.setMessage("Getting station status...");
			dialog.show();
			mIsFetching = true;
		}

		@Override
		protected Void doInBackground(Void... params) {
			mTimestamp = mStationManager.refresh();
			return null;
		}

		@Override
		protected void onPostExecute(Void params) {
			mStationsOverlay.clear();
			mStationsOverlay.addStations(mStationManager.getStations()); 
	
			if (dialog.isShowing()) {
				dialog.dismiss();
			}
			if (mTimestamp != null) {
				showStaleDataDialog(mTimestamp);
			}
			mIsFetching = false;
			if (mMapView != null) 
				mMapView.postInvalidate();
			mListAdapter.updateStations();
		}

	}

	private void showStaleDataDialog(long timestamp) {
		Builder builder = new AlertDialog.Builder(MainActivity.this);
		DateFormat dateFormat = DateFormat.getDateTimeInstance();
		dateFormat.setTimeZone(TimeZone.getDefault());
		builder.setTitle("המידע אינו מעודכן");
		long now = System.currentTimeMillis();
		long minBefore = Math.round((now - timestamp) / (1000.0 /* msecs/secs */ * 60.0 /* secs/minute */));
		long hoursBefore = Math.round((now - timestamp) / (1000.0 /* msecs/secs */ * 3600.0 /* secs/hour */));
		String timeString = minBefore >= 90 
				?  "- " + Math.round(hoursBefore) + " שעות"
			  : (minBefore >= 60
			      ? "שעה"
			      : "- " + minBefore + " דקות");
		builder.setMessage("המידע המוצג הינו מלפני כ"  + timeString + " עקב תקלה בשרת");
		builder.setPositiveButton("אוקיי", null);
		builder.show();
	}

	public StationManager getStationManager() {
		return mStationManager;
	}
}
