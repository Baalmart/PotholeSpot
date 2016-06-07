package dev.potholespot.android.viewer.map;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.mapquest.android.maps.GeoPoint;
import com.mapquest.android.maps.MapActivity;
import com.mapquest.android.maps.MapView;
import com.mapquest.android.maps.MyLocationOverlay;

import dev.potholespot.android.util.SlidingIndicatorView;
import dev.potholespot.android.viewer.map.overlay.OverlayProvider;
import dev.potholespot.uganda.R;

public class MapQuestLoggerMap extends MapActivity implements LoggerMap
{
   LoggerMapHelper mHelper;
   private MapView mMapView;
   private TextView[] mSpeedtexts; //this is an array for calculating speed 
   private TextView mLastGPSSpeedView;
   private TextView mLastGPSAltitudeView;
   private TextView mDistanceView;
   private MyLocationOverlay mMylocation;

   /**
    * Called when the activity is first created.
    */
   @Override
   protected void onCreate(final Bundle load)
   {
      super.onCreate(load);
      setContentView(R.layout.map_mapquest);
      setupActionBar();

      this.mMapView = (MapView) findViewById(R.id.myMapView);
      this.mHelper = new LoggerMapHelper(this);
      this.mMapView = (MapView) findViewById(R.id.myMapView);
      this.mMylocation = new MyLocationOverlay(this, this.mMapView);
      this.mMapView.setBuiltInZoomControls(true);
      final TextView[] speeds = { (TextView) findViewById(R.id.speedview05), (TextView) findViewById(R.id.speedview04), (TextView) findViewById(R.id.speedview03),
            (TextView) findViewById(R.id.speedview02), (TextView) findViewById(R.id.speedview01), (TextView) findViewById(R.id.speedview00) };
      this.mSpeedtexts = speeds;
      this.mLastGPSSpeedView = (TextView) findViewById(R.id.currentSpeed);
      this.mLastGPSAltitudeView = (TextView) findViewById(R.id.currentAltitude);
      this.mDistanceView = (TextView) findViewById(R.id.currentDistance);

      this.mHelper.onCreate(load);
   }

   @Override
   protected void onResume()
   {
      super.onResume();
      this.mHelper.onResume();
   }

   @Override
   protected void onPause()
   {
      this.mHelper.onPause();
      super.onPause();
   }

   @Override
   protected void onDestroy()
   {
      this.mHelper.onDestroy();
      super.onDestroy();
   }

   @Override
   public void onNewIntent(final Intent newIntent)
   {
      this.mHelper.onNewIntent(newIntent);
   }

   @Override
   protected void onRestoreInstanceState(final Bundle load)
   {
      if (load != null)
      {
         super.onRestoreInstanceState(load);
      }
      this.mHelper.onRestoreInstanceState(load);
   }

   @Override
   protected void onSaveInstanceState(final Bundle save)
   {
      super.onSaveInstanceState(save);
      this.mHelper.onSaveInstanceState(save);
   }

   @Override
   public boolean onCreateOptionsMenu(final Menu menu)
   {
      final boolean result = super.onCreateOptionsMenu(menu);
      this.mHelper.onCreateOptionsMenu(menu);
      return result;
   }

   @Override
   public boolean onPrepareOptionsMenu(final Menu menu)
   {
      this.mHelper.onPrepareOptionsMenu(menu);
      return super.onPrepareOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(final MenuItem item)
   {
      boolean handled = this.mHelper.onOptionsItemSelected(item);
      if (!handled)
      {
         handled = super.onOptionsItemSelected(item);
      }
      return handled;
   }

   @Override
   protected void onActivityResult(final int requestCode, final int resultCode, final Intent intent)
   {
      super.onActivityResult(requestCode, resultCode, intent);
      this.mHelper.onActivityResult(requestCode, resultCode, intent);
   }

   @Override
   public boolean onKeyDown(final int keyCode, final KeyEvent event)
   {
      boolean propagate = true;
      switch (keyCode)
      {
         default:
            propagate = this.mHelper.onKeyDown(keyCode, event);
            if (propagate)
            {
               propagate = super.onKeyDown(keyCode, event);
            }
            break;
      }
      return propagate;
   }

   @Override
   protected Dialog onCreateDialog(final int id)
   {
      Dialog dialog = this.mHelper.onCreateDialog(id);
      if (dialog == null)
      {
         dialog = super.onCreateDialog(id);
      }
      return dialog;
   }

   @Override
   protected void onPrepareDialog(final int id, final Dialog dialog)
   {
      this.mHelper.onPrepareDialog(id, dialog);
      super.onPrepareDialog(id, dialog);
   }

   /******************************/
   /** Loggermap methods **/
   /******************************/

   @Override
   public void updateOverlays()
   {
   }

   @Override
   public void setDrawingCacheEnabled(final boolean b)
   {
      findViewById(R.id.mapScreen).setDrawingCacheEnabled(true);
   }

   @Override
   public Activity getActivity()
   {
      return this;
   }

   @Override
   public void onLayerCheckedChanged(final int checkedId, final boolean isChecked)
   {
   }

   @Override
   public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key)
   {
   }

   @Override
   public Bitmap getDrawingCache()
   {
      return findViewById(R.id.mapScreen).getDrawingCache();
   }

   @Override
   public void showMediaDialog(final BaseAdapter mediaAdapter)
   {
      this.mHelper.showMediaDialog(mediaAdapter);
   }

   public void onDateOverlayChanged()
   {
      this.mMapView.postInvalidate();
   }

   @Override
   public String getDataSourceId()
   {
      return LoggerMapHelper.MAPQUEST_PROVIDER;
   }

   @Override
   public boolean isOutsideScreen(final GeoPoint lastPoint)
   {
      final Point out = new Point();
      this.mMapView.getProjection().toPixels(MapQuestLoggerMap.convertGeoPoint(lastPoint), out);
      final int height = this.mMapView.getHeight();
      final int width = this.mMapView.getWidth();
      return (out.x < 0 || out.y < 0 || out.y > height || out.x > width);
   }

   @Override
   public boolean isNearScreenEdge(final GeoPoint lastPoint)
   {
      final Point out = new Point();
      this.mMapView.getProjection().toPixels(MapQuestLoggerMap.convertGeoPoint(lastPoint), out);
      final int height = this.mMapView.getHeight();
      final int width = this.mMapView.getWidth();
      return (out.x < width / 4 || out.y < height / 4 || out.x > (width / 4) * 3 || out.y > (height / 4) * 3);
   }

   @Override
   public void executePostponedActions()
   {
   }

   @Override
   public void enableCompass()
   {
      this.mMylocation.enableCompass();
   }

   @Override
   public void enableMyLocation()
   {
      this.mMylocation.enableMyLocation();
   }

   @Override
   public void disableMyLocation()
   {
      this.mMylocation.disableMyLocation();
   }

   @Override
   public void disableCompass()
   {
      this.mMylocation.disableCompass();
   }

   @Override
   public void setZoom(final int zoom)
   {
      this.mMapView.getController().setZoom(zoom);
   }

   @Override
   public void animateTo(final GeoPoint storedPoint)
   {
      this.mMapView.getController().animateTo(MapQuestLoggerMap.convertGeoPoint(storedPoint));
   }

   @Override
   public int getZoomLevel()
   {
      return this.mMapView.getZoomLevel();
   }

   @Override
   public GeoPoint getMapCenter()
   {
      return MapQuestLoggerMap.convertMapQuestGeoPoint(this.mMapView.getMapCenter());
   }

   @Override
   public boolean zoomOut()
   {
      return this.mMapView.getController().zoomOut();
   }

   @Override
   public boolean zoomIn()
   {
      return this.mMapView.getController().zoomIn();
   }

   @Override
   public void postInvalidate()
   {
      this.mMapView.postInvalidate();
   }

   @Override
   public void addOverlay(final OverlayProvider overlay)
   {
      this.mMapView.getOverlays().add(overlay.getMapQuestOverlay());
   }

   @Override
   public void clearAnimation()
   {
      this.mMapView.clearAnimation();
   }

   @Override
   public void setCenter(final GeoPoint lastPoint)
   {
      this.mMapView.getController().setCenter(MapQuestLoggerMap.convertGeoPoint(lastPoint));
   }

   @Override
   public int getMaxZoomLevel()
   {
      return this.mMapView.getMaxZoomLevel();
   }

   @Override
   public GeoPoint fromPixels(final int x, final int y)
   {
      final com.mapquest.android.maps.GeoPoint mqGeopoint = this.mMapView.getProjection().fromPixels(x, y);
      return convertMapQuestGeoPoint(mqGeopoint);
   }

   @Override
   public boolean hasProjection()
   {
      return this.mMapView.getProjection() != null;
   }

   @Override
   public float metersToEquatorPixels(final float float1)
   {
      return this.mMapView.getProjection().metersToEquatorPixels(float1);
   }

   @Override
   public void toPixels(final GeoPoint geoPoint, final Point screenPoint)
   {
      final com.mapquest.android.maps.GeoPoint mqGeopoint = MapQuestLoggerMap.convertGeoPoint(geoPoint);
      this.mMapView.getProjection().toPixels(mqGeopoint, screenPoint);
   }

   @Override
   public TextView[] getSpeedTextViews()
   {
      return this.mSpeedtexts;
   }

   @Override
   public TextView getAltitideTextView()
   {
      return this.mLastGPSAltitudeView;
   }

   @Override
   public TextView getSpeedTextView()
   {
      return this.mLastGPSSpeedView;
   }

   @Override
   public TextView getDistanceTextView()
   {
      return this.mDistanceView;
   }

   static com.mapquest.android.maps.GeoPoint convertGeoPoint(final GeoPoint point)
   {
      return new com.mapquest.android.maps.GeoPoint(point.getLatitudeE6(), point.getLongitudeE6());
   }

   static GeoPoint convertMapQuestGeoPoint(final com.mapquest.android.maps.GeoPoint mqPoint)
   {
      return new GeoPoint(mqPoint.getLatitudeE6(), mqPoint.getLongitudeE6());
   }

   @Override
   public void clearOverlays()
   {
      this.mMapView.getOverlays().clear();
   }

   @Override
   public SlidingIndicatorView getScaleIndicatorView()
   {
      return (SlidingIndicatorView) findViewById(R.id.scaleindicator);
   }

   /******************************/
   /** Own methods **/
   /******************************/

   @Override
   public boolean isRouteDisplayed()
   {
      return true;
   }

   @SuppressWarnings("deprecation")
   protected void setupActionBar()
   {
      final ActionBar localActionBar = getActionBar();
      localActionBar.setDisplayShowTitleEnabled(true);
      localActionBar.setTitle("Map");
      localActionBar.setNavigationMode(0);
      localActionBar.setDisplayUseLogoEnabled(true);
      localActionBar.setLogo(R.drawable.ic_action_map);
      localActionBar.setDisplayHomeAsUpEnabled(true);
      localActionBar.setHomeButtonEnabled(true);
   }

}
