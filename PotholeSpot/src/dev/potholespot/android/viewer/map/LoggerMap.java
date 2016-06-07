package dev.potholespot.android.viewer.map;

import com.mapquest.android.maps.GeoPoint;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.widget.BaseAdapter;
import android.widget.TextView;
import dev.potholespot.android.util.SlidingIndicatorView;
import dev.potholespot.android.viewer.map.overlay.OverlayProvider;

public interface LoggerMap
{

   void setDrawingCacheEnabled(boolean b);

   Activity getActivity();

   void updateOverlays();

   void onLayerCheckedChanged(int checkedId, boolean b);

   void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key);

   Bitmap getDrawingCache();

   void showMediaDialog(BaseAdapter mediaAdapter);

   String getDataSourceId();

   boolean isOutsideScreen(GeoPoint lastPoint);

   boolean isNearScreenEdge(GeoPoint lastPoint);

   void executePostponedActions();

   void disableMyLocation();

   void disableCompass();

   void setZoom(int int1);

   void animateTo(GeoPoint storedPoint);

   int getZoomLevel();

   GeoPoint getMapCenter();

   boolean zoomOut();

   boolean zoomIn();

   void postInvalidate();

   void enableCompass();

   void enableMyLocation();

   void addOverlay(OverlayProvider overlay);

   void clearAnimation();

   void setCenter(GeoPoint lastPoint);

   int getMaxZoomLevel();

   GeoPoint fromPixels(int x, int y);

   boolean hasProjection();

   float metersToEquatorPixels(float float1);

   void toPixels(GeoPoint geopoint, Point screenPoint);

   TextView[] getSpeedTextViews();

   TextView getAltitideTextView();

   TextView getSpeedTextView();

   TextView getDistanceTextView();

   void clearOverlays();

   SlidingIndicatorView getScaleIndicatorView();
}
