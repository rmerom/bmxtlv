package com.studiosix.tlvbikes;

import android.graphics.drawable.Drawable;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class StationOverlayItem extends OverlayItem {

	public StationOverlayItem(GeoPoint point, String title, String snippet) {
		super(point, title, snippet);
	}

	public Drawable getMarkerCopy() {
		return mMarker.getConstantState().newDrawable();
	}

}
