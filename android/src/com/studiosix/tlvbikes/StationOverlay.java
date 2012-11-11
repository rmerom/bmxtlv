package com.studiosix.tlvbikes;

import java.util.ArrayList;
import java.util.Collection;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;

public class StationOverlay extends ItemizedOverlay<StationOverlayItem> {
	private ArrayList<StationOverlayItem> mOverlays = new ArrayList<StationOverlayItem>();
	private Context mContext;
	private Drawable greenbike, nobikes, nodocks, onebike, onedock, dunno;

	public StationOverlay(Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
	}
	
	public void clear() {
		mOverlays.clear();
	}

	public StationOverlay(Drawable defaultMarker, Context context) {
		this(defaultMarker);
		mContext = context;
		
		// Set up markers
		greenbike = getDrawable(R.drawable.ic_station_ok);
		nobikes = getDrawable(R.drawable.ic_station_no_bikes);
		nodocks = getDrawable(R.drawable.ic_station_no_docks);
		onebike = getDrawable(R.drawable.ic_station_few_bikes);
		onedock = getDrawable(R.drawable.ic_station_few_docks);
		dunno = getDrawable(R.drawable.ic_station_unknown);
	}
	
	private Drawable getDrawable(int drawableId) {
		return boundCenterBottom(mContext.getResources().getDrawable(drawableId));
	}
		
	public void addStations(Collection<Station> stations) {
		for (Station station : stations) {
			GeoPoint stationPoint = new GeoPoint(station.getLatitude() , station.getLongtitude());
			String stationSnippet = "אופניים: " + station.getAvailableBikes() + "\n" +
					"עמדות פנויות: " + station.getAvailableDocks();
			StationOverlayItem overlayitem = new StationOverlayItem(stationPoint, station.getName(), stationSnippet);

			switch (station.getStatus()) {
			case OK:
				overlayitem.setMarker(greenbike);
				break;
			case NO_BIKES:
				overlayitem.setMarker(nobikes);
				break;
			case NO_DOCKS:
				overlayitem.setMarker(nodocks);
				break;
			case FEW_BIKES:
				overlayitem.setMarker(onebike);
				break;
			case FEW_DOCKS:
				overlayitem.setMarker(onedock);
				break;
			case NO_INFO:
				overlayitem.setMarker(dunno);
				break;
			default:
				overlayitem.setMarker(dunno);
				break;
			}
			addOverlay(overlayitem);
		}
		populate();

	}

	public void addOverlay(StationOverlayItem overlay) {
		mOverlays.add(overlay);
	}

	@Override
	protected StationOverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	@Override
	public int size() {
		return mOverlays.size();
	}

	@Override
	protected boolean onTap(final int index) {
		StationOverlayItem item = mOverlays.get(index);
		AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
		dialog.setIcon(item.getMarkerCopy());
		dialog.setTitle(item.getTitle());
		dialog.setMessage(item.getSnippet());
		dialog.setNeutralButton("אוקיי", null);
		dialog.setPositiveButton("הנחיות", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					GeoPoint point = mOverlays.get(index).getPoint(); 
					Utils.navigateTo(mContext, point.getLatitudeE6(), point.getLongitudeE6());
				}
			}
		});
		dialog.show();
		return true;
	}
}
