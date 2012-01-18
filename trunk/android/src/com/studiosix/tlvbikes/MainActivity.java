package com.studiosix.tlvbikes;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

public class MainActivity extends FragmentActivity {
	public enum VisibleFragement {
		MAP,
		LIST
	}

	private StationManager mStationManager;
	private FragmentManager mFragmentManager;
	private StationMapFragment mStationMapFragment;
	private StationListFragment mStationListFragment;
	private VisibleFragement mVisibleFragement = VisibleFragement.MAP;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Set up managers
		mFragmentManager = getSupportFragmentManager();
		mStationManager = new StationManager();

		// Set up fragments
		mStationMapFragment = new StationMapFragment(this, mStationManager);
		mStationListFragment = new StationListFragment(this, mStationManager);
		mFragmentManager.beginTransaction().add( R.id.fragmentContainer, mStationMapFragment).commit();

		// Set up action bar
		final ImageButton viewButton = (ImageButton) findViewById(R.id.viewButton);
		viewButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (mVisibleFragement == MainActivity.VisibleFragement.MAP) {
					viewButton.setImageResource(R.drawable.ic_map);
					mVisibleFragement = VisibleFragement.LIST;

					FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
					fragmentTransaction.replace(R.id.fragmentContainer, mStationListFragment);
					fragmentTransaction.setTransition( FragmentTransaction.TRANSIT_FRAGMENT_FADE ).commit();

				} else {
					viewButton.setImageResource(R.drawable.ic_action_list);
					mVisibleFragement = VisibleFragement.MAP;

					FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
					fragmentTransaction.replace(R.id.fragmentContainer, mStationMapFragment);
					fragmentTransaction.setTransition( FragmentTransaction.TRANSIT_FRAGMENT_FADE ).commit();

				}
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

		// can use UI thread here
		protected void onPreExecute() {
			this.dialog.setMessage("Getting station status...");
			this.dialog.show();
		}

		@Override
		protected Void doInBackground(Void... params) {
			mStationManager.init();
			return null;
		}

		@Override
		protected void onPostExecute(Void params) {
			mStationMapFragment.drawStationsOnMap();
			if (this.dialog.isShowing()) {
				this.dialog.dismiss();
			}
		}
	}

	private class UpdateStationsTask extends AsyncTask<Void, Void, Void> {
		private final ProgressDialog dialog = new ProgressDialog(MainActivity.this);

		// can use UI thread here
		protected void onPreExecute() {
			this.dialog.setMessage("Getting station status...");
			this.dialog.show();
		}

		@Override
		protected Void doInBackground(Void... params) {
			mStationManager.refresh();
			return null;
		}

		@Override
		protected void onPostExecute(Void params) {
			mStationMapFragment.drawStationsOnMap();
			if (this.dialog.isShowing()) {
				this.dialog.dismiss();
			}
		}
	}
}
