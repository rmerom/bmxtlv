package com.studiosix.tlvbikes;

import java.util.ArrayList;
import java.util.Collection;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class StationOverlay extends ItemizedOverlay<OverlayItem> {
	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
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
		greenbike = mContext.getResources().getDrawable(R.drawable.ic_station_ok);
		int width = greenbike.getIntrinsicWidth();
		int height = greenbike.getIntrinsicHeight();
		greenbike.setBounds(-width / 2, -height, width - (width / 2), 0);
		nobikes = mContext.getResources().getDrawable(R.drawable.ic_station_no_bikes);
		nobikes.setBounds(-width / 2, -height, width - (width / 2), 0);
		nodocks = mContext.getResources().getDrawable(R.drawable.ic_station_no_docks);
		nodocks.setBounds(-width / 2, -height, width - (width / 2), 0);
		onebike = mContext.getResources().getDrawable(R.drawable.ic_station_few_bikes);
		onebike.setBounds(-width / 2, -height, width - (width / 2), 0);
		onedock = mContext.getResources().getDrawable(R.drawable.ic_station_few_docks);
		onedock.setBounds(-width / 2, -height, width - (width / 2), 0);
		dunno = mContext.getResources().getDrawable(R.drawable.ic_station_unknown);
		dunno.setBounds(-width / 2, -height, width - (width / 2), 0);
	}
		
	public void addStations(Collection<Station> stations) {
		for (Station station : stations) {
			GeoPoint stationPoint = new GeoPoint(station.getLatitude() , station.getLongtitude());
			String stationSnippet = "אופניים: " + station.getAvailableBikes() + "\n" +
					"תחנות: " + station.getAvailableDocks();
			OverlayItem overlayitem = new OverlayItem(stationPoint, station.getName(), stationSnippet);

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

	public void addOverlay(OverlayItem overlay) {
		mOverlays.add(overlay);
	}

	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	@Override
	public int size() {
		return mOverlays.size();
	}

	@Override
	protected boolean onTap(final int index) {
		OverlayItem item = mOverlays.get(index);
		AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
		dialog.setIcon(item.getMarker(OverlayItem.ITEM_STATE_FOCUSED_MASK));
		dialog.setTitle(item.getTitle());
		dialog.setMessage(item.getSnippet());
		dialog.setNeutralButton("אוקיי", null);
		dialog.setPositiveButton("הנחיות", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (which == DialogInterface.BUTTON_NEUTRAL) {
					GeoPoint point = mOverlays.get(index).getPoint(); 
					Utils.navigateTo(mContext, point.getLatitudeE6(), point.getLongitudeE6());
				}
			}
		});
		dialog.show();
		return true;
		/*
		AlertDialog.Builder builder;
		AlertDialog alertDialog;
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.station_dialog,
		                               (ViewGroup) findViewById(R.id.mapview));
		
		TextView text = (TextView) layout.findViewById(R.id.dialogBikeCount);
		text.setText("11");

		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setView(layout);
		alertDialog = builder.create();
		
		Dialog dialog = new Dialog(mContext);

		dialog.setContentView(R.layout.station_dialog);
		//dialog.setTitle(item.getTitle());
		TextView text = (TextView) dialog.findViewById(R.id.dialogBikeCount);
		text.setText("15");
		
		dialog.show();
		return true;*/
	}
}
