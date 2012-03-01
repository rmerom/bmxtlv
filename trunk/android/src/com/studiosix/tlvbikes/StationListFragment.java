package com.studiosix.tlvbikes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;

public class StationListFragment extends ListFragment implements SensorEventListener {
	private Context mContext;
	private StationManager mStationManager;
	private StationListAdapter mListAdapter;
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private Sensor mMagnetometer;
	private float[] mGravity = null;
	private float[] mGeomagnetic = null;
	
	public StationListFragment(Context context, StationManager stationManager) {
		mContext = context;
		mStationManager = stationManager;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mListAdapter = new StationListAdapter(getActivity(), mStationManager);
		setListAdapter(mListAdapter);
		mSensorManager = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	}

	@Override
	public void onResume() {
		super.onResume();
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);
	}

	@Override
	public void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Log.i("FragmentList", "Item clicked: " + id);
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {}

	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
			mGravity = event.values;
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
			mGeomagnetic = event.values;
		/*if (mGravity != null && mGeomagnetic != null) {
			float L[] = new float[9];
			float I[] = new float[9];
			boolean success = SensorManager.getRotationMatrix(L, I, mGravity, mGeomagnetic);
			if (success) {
				float orientation[] = new float[3];
				SensorManager.getOrientation(L, orientation);
				float azimuth = orientation[0];
				azimuth = azimuth * 360 / (2 * (float) Math.PI);
				
				// Convert magnetic north into true north
				Location currentLoc = mStationManager.getUserLocationNew();
				GeomagneticField geoField = new GeomagneticField(
				             Double.valueOf(currentLoc.getLatitude()).floatValue(),
				             Double.valueOf(currentLoc.getLongitude()).floatValue(),
				             Double.valueOf(currentLoc.getAltitude()).floatValue(),
				             System.currentTimeMillis());
				azimuth += geoField.getDeclination(); 
				
				ListView listView;
				try {
					listView = getListView();
				} catch (Exception e) {
					return;
				}
				int numStations = mListAdapter.getCount();
				for (int i = 0; i < numStations; i++) {
					View v = listView.getChildAt(i - listView.getFirstVisiblePosition());
					if (v == null) {
						continue;
					}
					
					Station station = (Station) mListAdapter.getItem(i);
					
					float bearing = (float) Utils.bearing(currentLoc.getLatitude(), currentLoc.getLongitude(), station.getLatitude() / 1E6, station.getLongtitude() / 1E6);
					float direction = azimuth - bearing;
					
					
					ImageView arrow = (ImageView) v.findViewById(R.id.listDirectionImage);
					Bitmap bMap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_direction);
			        Matrix matrix = new Matrix();
			        matrix.postRotate(-direction);
			        Bitmap bMapRotate = Bitmap.createBitmap(bMap, 0, 0, bMap.getWidth(), bMap.getHeight(), matrix, true);
			        arrow.setImageBitmap(bMapRotate);
				}
			}
		}*/
	}
}