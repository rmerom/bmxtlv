package com.studiosix.tlvbikes;

import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

public class MainActivity extends FragmentActivity implements HasStationManager {
	public enum VisibleFragment {
		MAP,
		LIST
	}

	private StationManager mStationManager;
	private FragmentManager mFragmentManager;
	private StationMapFragment mStationMapFragment;
	private StationListFragment mStationListFragment;
	private VisibleFragment mVisibleFragment = VisibleFragment.MAP;
	
	public MainActivity() {
		super();
	}

	public void showFragment(VisibleFragment fragment) {
		final ImageButton viewButton = (ImageButton) findViewById(R.id.viewButton);
		if (fragment == MainActivity.VisibleFragment.LIST) {
			viewButton.setImageResource(R.drawable.ic_map);
			mVisibleFragment = VisibleFragment.LIST;

			FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
			fragmentTransaction.replace(R.id.fragmentContainer, mStationListFragment);
			fragmentTransaction.setTransition( FragmentTransaction.TRANSIT_FRAGMENT_FADE ).commit();

		} else {
			viewButton.setImageResource(R.drawable.ic_action_list);
			mVisibleFragment = VisibleFragment.MAP;

			FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
			fragmentTransaction.replace(R.id.fragmentContainer, mStationMapFragment);
			fragmentTransaction.setTransition( FragmentTransaction.TRANSIT_FRAGMENT_FADE ).commit();

		}
		
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Set up managers
		mFragmentManager = getSupportFragmentManager();
		mStationManager = new StationManager(this);

		// Set up fragments
		mStationMapFragment = new StationMapFragment();
		mStationListFragment = new StationListFragment();
		mFragmentManager.beginTransaction().add(R.id.fragmentContainer, mStationMapFragment).commit();

		// Set up action bar
		final ImageButton viewButton = (ImageButton) findViewById(R.id.viewButton);
		viewButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				VisibleFragment switchToFragment;
				if (mVisibleFragment == VisibleFragment.MAP) {
					switchToFragment = VisibleFragment.LIST;
				} else {
					switchToFragment = VisibleFragment.MAP;
				}
				showFragment(switchToFragment);
			}
		});
		ImageButton refreshButton = (ImageButton) findViewById(R.id.refreshButton);
		refreshButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				new UpdateStationsTask().execute();
			}
		});
		
		// Set up location
		/*LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		LocationListener locationListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				mUserLocation = new GeoPoint((int)(location.getLatitude() * 1E6), 
						(int)(location.getLongitude() * 1E6));
				mStationManager.setUserLocation(location);
			}
			public void onStatusChanged(String provider, int status, Bundle extras) {}
			public void onProviderEnabled(String provider) {}
			public void onProviderDisabled(String provider) {}
		};

		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);*/

		// Update station manager
		new InitStationsTask().execute();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mStationMapFragment.enableLocation();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mStationMapFragment.disableLocation();
	}

	protected boolean isRouteDisplayed() {
		return false;
	}

	private class InitStationsTask extends AsyncTask<Void, Void, Void> {
		private final ProgressDialog dialog = new ProgressDialog(MainActivity.this);
		private Long mTimestamp;

		// can use UI thread here
		protected void onPreExecute() {
			this.dialog.setMessage("Getting station status...");
			this.dialog.show();
		}

		@Override
		protected Void doInBackground(Void... params) {
			mTimestamp = mStationManager.init();
			return null;
		}

		@Override
		protected void onPostExecute(Void params) {
			mStationMapFragment.drawStationsOnMap();
			if (this.dialog.isShowing()) {
				this.dialog.dismiss();
			}
			if (mTimestamp != null) {
				showStaleDataDialog(mTimestamp);
			}
		}

	}

	private class UpdateStationsTask extends AsyncTask<Void, Void, Void> {
		private final ProgressDialog dialog = new ProgressDialog(MainActivity.this);
		private Long mTimestamp;

		// can use UI thread here
		protected void onPreExecute() {
			this.dialog.setMessage("Getting station status...");
			this.dialog.show();
		}

		@Override
		protected Void doInBackground(Void... params) {
			mTimestamp = mStationManager.refresh();
			return null;
		}

		@Override
		protected void onPostExecute(Void params) {
			mStationMapFragment.drawStationsOnMap();
			if (this.dialog.isShowing()) {
				this.dialog.dismiss();
			}
			if (mTimestamp != null) {
				showStaleDataDialog(mTimestamp);
			}
		}
	}

	private void showStaleDataDialog(long timestamp) {
		Builder builder = new AlertDialog.Builder(MainActivity.this);
		DateFormat dateFormat = DateFormat.getDateTimeInstance();
		dateFormat.setTimeZone(TimeZone.getDefault());
		builder.setTitle("מידע שאינו מעודכן");
		builder.setMessage("המידע המוצג הינו משעה: " + dateFormat.format(new Date(timestamp)));
		builder.setPositiveButton("אוקיי", null);
		builder.show();
	}

	public StationManager getStationManager() {
		return mStationManager;
	}

	public StationMapFragment getStationMapFragment() {
		return mStationMapFragment;
	}
}
