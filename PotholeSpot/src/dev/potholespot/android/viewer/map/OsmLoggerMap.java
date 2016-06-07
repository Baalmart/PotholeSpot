package dev.potholespot.android.viewer.map;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.util.CloudmadeUtil;
import org.osmdroid.views.MapView;
import org.osmdroid.views.MapView.Projection;
import org.osmdroid.views.overlay.MyLocationOverlay;
import org.osmdroid.views.overlay.Overlay;
import com.mapquest.android.maps.GeoPoint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.BaseAdapter;
import android.widget.TextView;
import dev.potholespot.android.util.Constants;
import dev.potholespot.android.util.SlidingIndicatorView;
import dev.potholespot.android.viewer.map.overlay.OverlayProvider;
import dev.potholespot.uganda.R;

public class OsmLoggerMap extends Activity implements LoggerMap
{
   protected static final String TAG = "OsmLoggerMap";
   LoggerMapHelper mHelper;
   private MapView mMapView;
   private TextView[] mSpeedtexts;
   private TextView mLastGPSSpeedView;
   private TextView mLastGPSAltitudeView;
   private TextView mDistanceView;
   private MyLocationOverlay mMylocation;
   private Projection mProjecton;

   /**
    * Called when the activity is first created.
    */
   @Override
   protected void onCreate(final Bundle load)
   {
      super.onCreate(load);
      setContentView(R.layout.map_osm);

      this.mMapView = (MapView) findViewById(R.id.myMapView);
      final TextView[] speeds = { (TextView) findViewById(R.id.speedview05), (TextView) findViewById(R.id.speedview04), (TextView) findViewById(R.id.speedview03),
            (TextView) findViewById(R.id.speedview02), (TextView) findViewById(R.id.speedview01), (TextView) findViewById(R.id.speedview00) };
      this.mSpeedtexts = speeds;
      this.mLastGPSSpeedView = (TextView) findViewById(R.id.currentSpeed);
      this.mLastGPSAltitudeView = (TextView) findViewById(R.id.currentAltitude);
      this.mDistanceView = (TextView) findViewById(R.id.currentDistance);

      this.mHelper = new LoggerMapHelper(this);
      this.mMapView.setBuiltInZoomControls(true);
      this.mProjecton = this.mMapView.getProjection();
      this.mHelper.onCreate(load);

      this.mMylocation = new MyLocationOverlay(this, this.mMapView);
      this.mMapView.getOverlays().add(new Overlay(this)
         {

            @Override
            protected void draw(final Canvas arg0, final MapView map, final boolean arg2)
            {
               final Projection projecton = map.getProjection();
               OsmLoggerMap.this.mProjecton = projecton;
               final IGeoPoint gepoint = map.getMapCenter();
               final Point point = projecton.toPixels(gepoint, null);
               Log.d(TAG, "Found center (" + gepoint.getLatitudeE6() + "," + gepoint.getLongitudeE6() + ") matching screen point (" + point.x + "," + point.y + ") ");
            }
         });
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
   /** Own methods **/
   /******************************/

   private void setTrafficOverlay(final boolean b)
   {
      final SharedPreferences sharedPreferences = this.mHelper.getPreferences();
      final Editor editor = sharedPreferences.edit();
      editor.putBoolean(Constants.TRAFFIC, b);
      editor.commit();
   }

   private void setSatelliteOverlay(final boolean b)
   {
      final SharedPreferences sharedPreferences = this.mHelper.getPreferences();
      final Editor editor = sharedPreferences.edit();
      editor.putBoolean(Constants.SATELLITE, b);
      editor.commit();
   }

   /******************************/
   /** Loggermap methods **/
   /******************************/

   @Override
   public void updateOverlays()
   {
      final SharedPreferences sharedPreferences = this.mHelper.getPreferences();
      final int renderer = sharedPreferences.getInt(Constants.OSMBASEOVERLAY, 2);
      switch (renderer)
      {
         case Constants.OSM_CLOUDMADE:
            CloudmadeUtil.retrieveCloudmadeKey(getApplicationContext());
            this.mMapView.setTileSource(TileSourceFactory.CLOUDMADESTANDARDTILES);
            break;
         case Constants.OSM_MAKNIK:
            this.mMapView.setTileSource(TileSourceFactory.MAPNIK);
            break;
         case Constants.OSM_CYCLE:
            this.mMapView.setTileSource(TileSourceFactory.CYCLEMAP);
            break;
         default:
            break;
      }
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
      switch (checkedId)
      {
         case R.id.layer_google_satellite:
            setSatelliteOverlay(true);
            break;
         case R.id.layer_google_regular:
            setSatelliteOverlay(false);
            break;
         case R.id.layer_traffic:
            setTrafficOverlay(isChecked);
            break;
         default:
            break;
      }
   }

   @Override
   public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key)
   {
      if (key.equals(Constants.TRAFFIC))
      {
         updateOverlays();
      }
      else if (key.equals(Constants.SATELLITE))
      {
         updateOverlays();
      }
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
      return LoggerMapHelper.GOOGLE_PROVIDER;
   }

   @Override
   public boolean isOutsideScreen(final GeoPoint lastPoint)
   {
      final Point out = new Point();
      this.mProjecton.toMapPixels(convertGeoPoint(lastPoint), out);
      final int height = this.mMapView.getHeight();
      final int width = this.mMapView.getWidth();
      return (out.x < 0 || out.y < 0 || out.y > height || out.x > width);
   }

   @Override
   public boolean isNearScreenEdge(final GeoPoint lastPoint)
   {
      final Point out = new Point();
      this.mProjecton.toMapPixels(convertGeoPoint(lastPoint), out);
      final int height = this.mMapView.getHeight();
      final int width = this.mMapView.getWidth();
      return (out.x < width / 4 || out.y < height / 4 || out.x > (width / 4) * 3 || out.y > (height / 4) * 3);
   }

   @Override
   public void executePostponedActions()
   {
      // NOOP for Google Maps
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
      this.mMapView.getController().animateTo(convertGeoPoint(storedPoint));
   }

   @Override
   public int getZoomLevel()
   {
      return this.mMapView.getZoomLevel();
   }

   @Override
   public GeoPoint getMapCenter()
   {
      return convertOSMGeoPoint(this.mMapView.getMapCenter());
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
      this.mMapView.getOverlays().add(overlay.getOSMOverlay());
   }

   @Override
   public void clearAnimation()
   {
      this.mMapView.clearAnimation();
   }

   @Override
   public void setCenter(final GeoPoint lastPoint)
   {
      this.mMapView.getController().setCenter(convertGeoPoint(lastPoint));
   }

   @Override
   public int getMaxZoomLevel()
   {
      return this.mMapView.getMaxZoomLevel();
   }

   @Override
   public GeoPoint fromPixels(final int x, final int y)
   {
      final IGeoPoint osmGeopoint = this.mProjecton.fromPixels(x, y);
      final GeoPoint geopoint = convertOSMGeoPoint(osmGeopoint);
      return geopoint;
   }

   @Override
   public void toPixels(final GeoPoint geoPoint, final Point screenPoint)
   {
      final org.osmdroid.util.GeoPoint localGeopoint = convertGeoPoint(geoPoint);
      this.mProjecton.toMapPixels(localGeopoint, screenPoint);
   }

   @Override
   public boolean hasProjection()
   {
      return this.mProjecton != null;
   }

   @Override
   public float metersToEquatorPixels(final float float1)
   {
      return this.mProjecton.metersToEquatorPixels(float1);
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

   static org.osmdroid.util.GeoPoint convertGeoPoint(final GeoPoint point)
   {
      final org.osmdroid.util.GeoPoint geopoint = new org.osmdroid.util.GeoPoint(point.getLatitudeE6(), point.getLongitudeE6());
      return geopoint;
   }

   static GeoPoint convertOSMGeoPoint(final IGeoPoint point)
   {
      return new GeoPoint(point.getLatitudeE6(), point.getLongitudeE6());
   }

   @Override
   public void clearOverlays()
   {
      this.mMapView.getOverlayManager().clear();
   }

   @Override
   public SlidingIndicatorView getScaleIndicatorView()
   {
      return (SlidingIndicatorView) findViewById(R.id.scaleindicator);
   }
}
