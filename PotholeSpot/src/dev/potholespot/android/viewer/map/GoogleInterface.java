package dev.potholespot.android.viewer.map;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;

import dev.potholespot.android.util.SlidingIndicatorView;
import dev.potholespot.android.viewer.map.overlay.OverlayProvider;

public interface GoogleInterface
{

   void setDrawingCacheEnabled(boolean b);

   Activity getActivity();

   void updateOverlays();

   void onLayerCheckedChanged(int checkedId, boolean b);

   void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key);

   Bitmap getDrawingCache();

   void showMediaDialog(BaseAdapter mediaAdapter);

   String getDataSourceId();

   boolean isOutsideScreen(LatLng lastPoint);

   boolean isNearScreenEdge(LatLng lastPoint);

   void executePostponedActions();

   void disableMyLocation();

   void disableCompass();

   void setZoom(int int1);

   void animateTo(LatLng storedPoint);

   int getZoomLevel();

   GoogleMap getMapCenter();

   boolean zoomOut();

   boolean zoomIn();

   void postInvalidate();

   void enableCompass();

   void enableMyLocation();

   //this one is for the GoogleMapsV1
   void addOverlay(OverlayProvider overlay);
   
   //replacement for overlays
   void addPolygon (Polygon polygon);   
   void addPolyline (Polyline polyline);

   void clearAnimation();

   void setCenter(LatLng lastPoint);

   int getMaxZoomLevel();

   LatLng fromPixels(int x, int y);

   boolean hasProjection();

   float metersToEquatorPixels(float float1);

   void toPixels(LatLng geopoint, Point screenPoint);

   TextView[] getSpeedTextViews();

   TextView getAltitideTextView();

   TextView getSpeedTextView();

   TextView getDistanceTextView();

   void clearOverlays();

   SlidingIndicatorView getScaleIndicatorView();
}
