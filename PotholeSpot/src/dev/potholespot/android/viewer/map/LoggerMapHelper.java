package dev.potholespot.android.viewer.map;

import java.util.concurrent.Semaphore;

import com.mapquest.android.maps.GeoPoint;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Gallery;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import dev.potholespot.android.actions.ControlLogging;
import dev.potholespot.android.actions.ManualMode;
import dev.potholespot.android.actions.ShareRoute;
import dev.potholespot.android.db.Pspot.Media;
import dev.potholespot.android.db.Pspot.Tracks;
import dev.potholespot.android.db.Pspot.TracksColumns;
import dev.potholespot.android.db.Pspot.Waypoints;
import dev.potholespot.android.db.Pspot.WaypointsColumns;
import dev.potholespot.android.logger.GPSLoggerServiceManager;
import dev.potholespot.android.util.Constants;
import dev.potholespot.android.util.SlidingIndicatorView;
import dev.potholespot.android.util.UnitsI18n;
import dev.potholespot.android.viewer.About;
import dev.potholespot.android.viewer.ApplicationPreferenceActivity;
import dev.potholespot.android.viewer.RouteList;
import dev.potholespot.android.viewer.map.overlay.BitmapSegmentsOverlay;
import dev.potholespot.android.viewer.map.overlay.SegmentRendering;
import dev.potholespot.uganda.R;

public class LoggerMapHelper
{

   public static final String OSM_PROVIDER = "OSM";
   public static final String GOOGLE_PROVIDER = "GOOGLE";
   public static final String MAPQUEST_PROVIDER = "MAPQUEST";

   private static final String INSTANCE_E6LONG = "e6long";
   private static final String INSTANCE_E6LAT = "e6lat";
   private static final String INSTANCE_ZOOM = "zoom";
   private static final String INSTANCE_AVGSPEED = "averagespeed";
   private static final String INSTANCE_HEIGHT = "averageheight";
   private static final String INSTANCE_TRACK = "track";
   private static final String INSTANCE_SPEED = "speed";
   private static final String INSTANCE_ALTITUDE = "altitude";
   private static final String INSTANCE_DISTANCE = "distance";

   private static final int ZOOM_LEVEL = 16;
   // MENU'S
   private static final int MENU_SETTINGS = 1;
   private static final int MENU_TRACKING = 2;
   private static final int MENU_TRACKLIST = 3;
   private static final int MENU_STATS = 4;
   private static final int MENU_ABOUT = 5;
   private static final int MENU_LAYERS = 6;
   private static final int MENU_NOTE = 7;
   private static final int MENU_SHARE = 13;
   private static final int MENU_CONTRIB = 14;
   private static final int DIALOG_NOTRACK = 24;
   private static final int DIALOG_LAYERS = 31;
   private static final int DIALOG_URIS = 34;
   private static final int DIALOG_CONTRIB = 35;
   private static final int MENU_HOME = 36;
   private static final String TAG = "OGT.LoggerMap";

   private double mAverageSpeed = 33.33d / 3d;
   private double mAverageHeight = 33.33d / 3d;
   private long mTrackId = -1;
   private long mLastSegment = -1;
   private UnitsI18n mUnits;
   private WakeLock mWakeLock = null;
   private SharedPreferences mSharedPreferences;
   private GPSLoggerServiceManager mLoggerServiceManager;
   private SegmentRendering mLastSegmentOverlay;
   private BaseAdapter mMediaAdapter;

   private Handler mHandler;

   private ContentObserver mTrackSegmentsObserver;
   private ContentObserver mSegmentWaypointsObserver;
   private ContentObserver mTrackMediasObserver;
   private DialogInterface.OnClickListener mNoTrackDialogListener;
   private OnItemSelectedListener mGalerySelectListener;
   private Uri mSelected;
   private OnClickListener mNoteSelectDialogListener;
   private OnCheckedChangeListener mCheckedChangeListener;
   private android.widget.RadioGroup.OnCheckedChangeListener mGroupCheckedChangeListener;
   private OnSharedPreferenceChangeListener mSharedPreferenceChangeListener;
   private UnitsI18n.UnitsChangeListener mUnitsChangeListener;

   /**
    * Run after the ServiceManager completes the binding to the remote service
    */

   private Runnable mServiceConnected;
   private Runnable speedCalculator;
   private Runnable heightCalculator;
   private final LoggerMap mLoggerMap;
   private BitmapSegmentsOverlay mBitmapSegmentsOverlay;
   private float mSpeed;
   private double mAltitude;
   private float mDistance;

   public LoggerMapHelper(final LoggerMap loggerMap)
   {
      this.mLoggerMap = loggerMap;
   }

   /**
    * Called when the activity is first created.
    */

   protected void onCreate(final Bundle load)
   {
      this.mLoggerMap.setDrawingCacheEnabled(true);
      this.mUnits = new UnitsI18n(this.mLoggerMap.getActivity());
      this.mLoggerServiceManager = new GPSLoggerServiceManager(this.mLoggerMap.getActivity());

      final Semaphore calulatorSemaphore = new Semaphore(0);
      final Thread calulator = new Thread("OverlayCalculator")
         {
            @Override
            public void run()
            {
               Looper.prepare();
               LoggerMapHelper.this.mHandler = new Handler();
               calulatorSemaphore.release();
               Looper.loop();
            }
         };

      calulator.start();
      try
      {
         calulatorSemaphore.acquire();
      }
      catch (final InterruptedException e)
      {
         Log.e(TAG, "Failed waiting for a semaphore", e);
      }
      this.mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.mLoggerMap.getActivity());
      this.mBitmapSegmentsOverlay = new BitmapSegmentsOverlay(this.mLoggerMap, this.mHandler);
      createListeners();
      onRestoreInstanceState(load);
      this.mLoggerMap.updateOverlays();
   }

   protected void onResume()
   {
      updateMapProvider();

      this.mLoggerServiceManager.startup(this.mLoggerMap.getActivity(), this.mServiceConnected);
      this.mSharedPreferences.registerOnSharedPreferenceChangeListener(this.mSharedPreferenceChangeListener);
      this.mUnits.setUnitsChangeListener(this.mUnitsChangeListener);
      setupActionBar();
      //updateTitleBar();
      updateBlankingBehavior();

      if (this.mTrackId >= 0)
      {
         final ContentResolver resolver = this.mLoggerMap.getActivity().getContentResolver();
         final Uri trackUri = Uri.withAppendedPath(Tracks.CONTENT_URI, this.mTrackId + "/segments");
         final Uri lastSegmentUri = Uri.withAppendedPath(Tracks.CONTENT_URI, this.mTrackId + "/segments/" + this.mLastSegment + "/waypoints");
         final Uri mediaUri = ContentUris.withAppendedId(Media.CONTENT_URI, this.mTrackId);

         resolver.unregisterContentObserver(this.mTrackSegmentsObserver);
         resolver.unregisterContentObserver(this.mSegmentWaypointsObserver);
         resolver.unregisterContentObserver(this.mTrackMediasObserver);

         resolver.registerContentObserver(trackUri, false, this.mTrackSegmentsObserver);
         resolver.registerContentObserver(lastSegmentUri, true, this.mSegmentWaypointsObserver);
         resolver.registerContentObserver(mediaUri, true, this.mTrackMediasObserver);
      }
      updateDataOverlays();

      updateSpeedColoring();
      updateSpeedDisplayVisibility();
      updateAltitudeDisplayVisibility();
      updateDistanceDisplayVisibility();
      updateCompassDisplayVisibility();
      updateLocationDisplayVisibility();

      updateTrackNumbers();

      this.mLoggerMap.executePostponedActions();
   }

   protected void setupActionBar()
   {
      final ActionBar localActionBar = this.mLoggerMap.getActivity().getActionBar();

      /*
       * if (localActionBar == null) return; localActionBar.setDisplayShowTitleEnabled(true); localActionBar.setNavigationMode(0); localActionBar.setDisplayUseLogoEnabled(true);
       * localActionBar.setLogo(R.drawable.ic_car_white); localActionBar.setBackgroundDrawable(mLoggerMap.getActivity(). getResources().getDrawable(R.drawable.action_bar_bg));
       * localActionBar.setDisplayHomeAsUpEnabled(true); localActionBar.setHomeButtonEnabled(true); //localActionBar.setTitle(updateTitleBar()); //localActionBar.setIcon(R.drawable.ic_action_share);
       * localActionBar.setIcon(R.drawable.ic_action_share);
       */

      //ActionBar localActionBar = getActionBar();
      localActionBar.setDisplayShowTitleEnabled(true);
      localActionBar.setTitle("PotholeSpot");
      localActionBar.setNavigationMode(0);
      localActionBar.setDisplayUseLogoEnabled(true);
      localActionBar.setLogo(R.drawable.ic_action_map);
      localActionBar.setDisplayHomeAsUpEnabled(true);
      localActionBar.setHomeButtonEnabled(true);
   }

   protected void onPause()
   {
      if (this.mWakeLock != null && this.mWakeLock.isHeld())
      {
         this.mWakeLock.release();
         Log.w(TAG, "onPause(): Released lock to keep screen on!");
      }
      this.mLoggerMap.clearOverlays();
      this.mBitmapSegmentsOverlay.clearSegments();
      this.mLastSegmentOverlay = null;
      final ContentResolver resolver = this.mLoggerMap.getActivity().getContentResolver();
      resolver.unregisterContentObserver(this.mTrackSegmentsObserver);
      resolver.unregisterContentObserver(this.mSegmentWaypointsObserver);
      resolver.unregisterContentObserver(this.mTrackMediasObserver);
      this.mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this.mSharedPreferenceChangeListener);
      this.mUnits.setUnitsChangeListener(null);
      this.mLoggerMap.disableMyLocation();
      this.mLoggerMap.disableCompass();
      this.mLoggerServiceManager.shutdown(this.mLoggerMap.getActivity());
   }

   protected void onDestroy()
   {
      this.mLoggerMap.clearOverlays();
      this.mHandler.post(new Runnable()
         {
            @Override
            public void run()
            {
               Looper.myLooper().quit();
            }
         });

      if (this.mWakeLock != null && this.mWakeLock.isHeld())
      {
         this.mWakeLock.release();
         Log.w(TAG, "onDestroy(): Released lock to keep screen on!");
      }
      if (this.mLoggerServiceManager.getLoggingState() == Constants.STOPPED)
      {
         this.mLoggerMap.getActivity().stopService(new Intent(Constants.SERVICENAME));
      }
      this.mUnits = null;
   }

   public void onNewIntent(final Intent newIntent)
   {
      final Uri data = newIntent.getData();
      if (data != null)
      {
         moveToTrack(Long.parseLong(data.getLastPathSegment()), true);
      }
   }

   protected void onRestoreInstanceState(final Bundle load)
   {
      final Uri data = this.mLoggerMap.getActivity().getIntent().getData();
      if (load != null && load.containsKey(INSTANCE_TRACK))
      // 1st method: track from a previous instance of this activity
      {
         final long loadTrackId = load.getLong(INSTANCE_TRACK);
         moveToTrack(loadTrackId, false);
         if (load.containsKey(INSTANCE_AVGSPEED))
         {
            this.mAverageSpeed = load.getDouble(INSTANCE_AVGSPEED);
         }
         if (load.containsKey(INSTANCE_HEIGHT))
         {
            this.mAverageHeight = load.getDouble(INSTANCE_HEIGHT);
         }
         if (load.containsKey(INSTANCE_SPEED))
         {
            this.mSpeed = load.getFloat(INSTANCE_SPEED);
         }
         if (load.containsKey(INSTANCE_ALTITUDE))
         {
            this.mAltitude = load.getDouble(INSTANCE_HEIGHT);
         }
         if (load.containsKey(INSTANCE_DISTANCE))
         {
            this.mDistance = load.getFloat(INSTANCE_DISTANCE);
         }
      }
      else if (data != null) // 2nd method: track ordered to make
      {
         final long loadTrackId = Long.parseLong(data.getLastPathSegment());
         moveToTrack(loadTrackId, true);
      }
      else
      // 3rd method: just try the last track
      {
         moveToLastTrack();
      }

      if (load != null && load.containsKey(INSTANCE_ZOOM))
      {
         this.mLoggerMap.setZoom(load.getInt(INSTANCE_ZOOM));
      }
      else
      {
         this.mLoggerMap.setZoom(ZOOM_LEVEL);
      }

      if (load != null && load.containsKey(INSTANCE_E6LAT) && load.containsKey(INSTANCE_E6LONG))
      {
         final GeoPoint storedPoint = new GeoPoint(load.getInt(INSTANCE_E6LAT), load.getInt(INSTANCE_E6LONG));
         this.mLoggerMap.animateTo(storedPoint);
      }
      else
      {
         final GeoPoint lastPoint = getLastTrackPoint();
         this.mLoggerMap.animateTo(lastPoint);
      }
   }

   protected void onSaveInstanceState(final Bundle save)
   {
      save.putLong(INSTANCE_TRACK, this.mTrackId);
      save.putDouble(INSTANCE_AVGSPEED, this.mAverageSpeed);
      save.putDouble(INSTANCE_HEIGHT, this.mAverageHeight);
      save.putInt(INSTANCE_ZOOM, this.mLoggerMap.getZoomLevel());
      save.putFloat(INSTANCE_SPEED, this.mSpeed);
      save.putDouble(INSTANCE_ALTITUDE, this.mAltitude);
      save.putFloat(INSTANCE_DISTANCE, this.mDistance);
      final GeoPoint point = this.mLoggerMap.getMapCenter();
      save.putInt(INSTANCE_E6LAT, point.getLatitudeE6());
      save.putInt(INSTANCE_E6LONG, point.getLongitudeE6());
   }

   public boolean onKeyDown(final int keyCode, final KeyEvent event)
   {
      boolean propagate = true;
      switch (keyCode)
      {
         case KeyEvent.KEYCODE_T:
            propagate = this.mLoggerMap.zoomIn();
            propagate = false;
            break;
         case KeyEvent.KEYCODE_G:
            propagate = this.mLoggerMap.zoomOut();
            propagate = false;
            break;
         case KeyEvent.KEYCODE_F:
            moveToTrack(this.mTrackId - 1, true);
            propagate = false;
            break;
         case KeyEvent.KEYCODE_H:
            moveToTrack(this.mTrackId + 1, true);
            propagate = false;
            break;
      }
      return propagate;
   }

   private void setSpeedOverlay(final boolean b)
   {
      final Editor editor = this.mSharedPreferences.edit();
      editor.putBoolean(Constants.SPEED, b);
      editor.commit();
   }

   private void setAltitudeOverlay(final boolean b)
   {
      final Editor editor = this.mSharedPreferences.edit();
      editor.putBoolean(Constants.ALTITUDE, b);
      editor.commit();
   }

   private void setDistanceOverlay(final boolean b)
   {
      final Editor editor = this.mSharedPreferences.edit();
      editor.putBoolean(Constants.DISTANCE, b);
      editor.commit();
   }

   private void setCompassOverlay(final boolean b)
   {
      final Editor editor = this.mSharedPreferences.edit();
      editor.putBoolean(Constants.COMPASS, b);
      editor.commit();
   }

   private void setLocationOverlay(final boolean b)
   {
      final Editor editor = this.mSharedPreferences.edit();
      editor.putBoolean(Constants.LOCATION, b);
      editor.commit();
   }

   private void setOsmBaseOverlay(final int b)
   {
      final Editor editor = this.mSharedPreferences.edit();
      editor.putInt(Constants.OSMBASEOVERLAY, b);
      editor.commit();
   }

   private void createListeners()
   {
      /*******************************************************
       * 8 Runnable listener actions
       */
      this.speedCalculator = new Runnable()
         {
            @Override
            public void run()
            {
               double avgspeed = 0.0;
               final ContentResolver resolver = LoggerMapHelper.this.mLoggerMap.getActivity().getContentResolver();
               Cursor waypointsCursor = null;
               try
               {
                  waypointsCursor = resolver.query(Uri.withAppendedPath(Tracks.CONTENT_URI, LoggerMapHelper.this.mTrackId + "/waypoints"), new String[] { "avg(" + WaypointsColumns.SPEED + ")",
                        "max(" + WaypointsColumns.SPEED + ")" }, null, null, null);

                  if (waypointsCursor != null && waypointsCursor.moveToLast())
                  {
                     final double average = waypointsCursor.getDouble(0);
                     final double maxBasedAverage = waypointsCursor.getDouble(1) / 2;
                     avgspeed = Math.min(average, maxBasedAverage);
                  }
                  if (avgspeed < 2)
                  {
                     avgspeed = 5.55d / 2;
                  }
               }
               finally
               {
                  if (waypointsCursor != null)
                  {
                     waypointsCursor.close();
                  }
               }
               LoggerMapHelper.this.mAverageSpeed = avgspeed;
               LoggerMapHelper.this.mLoggerMap.getActivity().runOnUiThread(new Runnable()
                  {
                     @Override
                     public void run()
                     {
                        updateSpeedColoring();
                     }
                  });
            }
         };

      this.heightCalculator = new Runnable()
         {
            @Override
            public void run()
            {
               double avgHeight = 0.0;
               final ContentResolver resolver = LoggerMapHelper.this.mLoggerMap.getActivity().getContentResolver();
               Cursor waypointsCursor = null;
               try
               {
                  waypointsCursor = resolver.query(Uri.withAppendedPath(Tracks.CONTENT_URI, LoggerMapHelper.this.mTrackId + "/waypoints"), new String[] { "avg(" + WaypointsColumns.ALTITUDE + ")",
                        "max(" + WaypointsColumns.ALTITUDE + ")" }, null, null, null);

                  if (waypointsCursor != null && waypointsCursor.moveToLast())
                  {
                     final double average = waypointsCursor.getDouble(0);
                     final double maxBasedAverage = waypointsCursor.getDouble(1) / 2;
                     avgHeight = Math.min(average, maxBasedAverage);
                  }
               }
               finally
               {
                  if (waypointsCursor != null)
                  {
                     waypointsCursor.close();
                  }
               }
               LoggerMapHelper.this.mAverageHeight = avgHeight;
               LoggerMapHelper.this.mLoggerMap.getActivity().runOnUiThread(new Runnable()
                  {
                     @Override
                     public void run()
                     {
                        updateSpeedColoring();
                     }
                  });
            }
         };

      this.mServiceConnected = new Runnable()
         {
            @Override
            public void run()
            {
               updateBlankingBehavior();
            }
         };

      /*******************************************************
       * 8 Various dialog listeners
       */

      this.mGalerySelectListener = new AdapterView.OnItemSelectedListener()
         {
            @Override
            public void onItemSelected(final AdapterView< ? > parent, final View view, final int pos, final long id)
            {
               LoggerMapHelper.this.mSelected = (Uri) parent.getSelectedItem();
            }

            @Override
            public void onNothingSelected(final AdapterView< ? > arg0)
            {
               LoggerMapHelper.this.mSelected = null;
            }
         };

      this.mNoteSelectDialogListener = new DialogInterface.OnClickListener()
         {

            @Override
            public void onClick(final DialogInterface dialog, final int which)
            {
               SegmentRendering.handleMedia(LoggerMapHelper.this.mLoggerMap.getActivity(), LoggerMapHelper.this.mSelected);
               LoggerMapHelper.this.mSelected = null;
            }
         };

      this.mGroupCheckedChangeListener = new android.widget.RadioGroup.OnCheckedChangeListener()
         {
            @Override
            public void onCheckedChanged(final RadioGroup group, final int checkedId)
            {
               switch (checkedId)
               {
                  case R.id.layer_osm_cloudmade:
                     setOsmBaseOverlay(Constants.OSM_CLOUDMADE);
                     break;
                  case R.id.layer_osm_maknik:
                     setOsmBaseOverlay(Constants.OSM_MAKNIK);
                     break;
                  case R.id.layer_osm_bicycle:
                     setOsmBaseOverlay(Constants.OSM_CYCLE);
                     break;
                  default:
                     LoggerMapHelper.this.mLoggerMap.onLayerCheckedChanged(checkedId, true);
                     break;
               }
            }
         };

      this.mCheckedChangeListener = new OnCheckedChangeListener()
         {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked)
            {
               int checkedId;
               checkedId = buttonView.getId();
               switch (checkedId)
               {
                  case R.id.layer_speed:
                     setSpeedOverlay(isChecked);
                     break;
                  case R.id.layer_altitude:
                     setAltitudeOverlay(isChecked);
                     break;
                  case R.id.layer_distance:
                     setDistanceOverlay(isChecked);
                     break;
                  case R.id.layer_compass:
                     setCompassOverlay(isChecked);
                     break;
                  case R.id.layer_location:
                     setLocationOverlay(isChecked);
                     break;
                  default:
                     LoggerMapHelper.this.mLoggerMap.onLayerCheckedChanged(checkedId, isChecked);
                     break;
               }
            }
         };

      this.mNoTrackDialogListener = new DialogInterface.OnClickListener()
         {
            @Override
            public void onClick(final DialogInterface dialog, final int which)
            {
               Log.d(TAG, "mNoTrackDialogListener" + which);
               final Intent tracklistIntent = new Intent(LoggerMapHelper.this.mLoggerMap.getActivity(), RouteList.class);
               tracklistIntent.putExtra(BaseColumns._ID, LoggerMapHelper.this.mTrackId);
               LoggerMapHelper.this.mLoggerMap.getActivity().startActivityForResult(tracklistIntent, MENU_TRACKLIST);
            }
         };

      /**
       * Listeners to events outside this mapview
       */
      this.mSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener()
         {
            @Override
            public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key)
            {
               if (key.equals(Constants.TRACKCOLORING))
               {
                  LoggerMapHelper.this.mAverageSpeed = 0.0;
                  LoggerMapHelper.this.mAverageHeight = 0.0;
                  updateSpeedColoring();
               }
               else if (key.equals(Constants.DISABLEBLANKING) || key.equals(Constants.DISABLEDIMMING))
               {
                  updateBlankingBehavior();
               }
               else if (key.equals(Constants.SPEED))
               {
                  updateSpeedDisplayVisibility();
               }
               else if (key.equals(Constants.ALTITUDE))
               {
                  updateAltitudeDisplayVisibility();
               }
               else if (key.equals(Constants.DISTANCE))
               {
                  updateDistanceDisplayVisibility();
               }
               else if (key.equals(Constants.COMPASS))
               {
                  updateCompassDisplayVisibility();
               }
               else if (key.equals(Constants.LOCATION))
               {
                  updateLocationDisplayVisibility();
               }
               else if (key.equals(Constants.MAPPROVIDER))
               {
                  updateMapProvider();
               }
               else if (key.equals(Constants.OSMBASEOVERLAY))
               {
                  LoggerMapHelper.this.mLoggerMap.updateOverlays();
               }
               else
               {
                  LoggerMapHelper.this.mLoggerMap.onSharedPreferenceChanged(sharedPreferences, key);
               }
            }
         };

      this.mTrackMediasObserver = new ContentObserver(new Handler())
         {
            @Override
            public void onChange(final boolean selfUpdate)
            {
               if (!selfUpdate)
               {
                  if (LoggerMapHelper.this.mLastSegmentOverlay != null)
                  {
                     LoggerMapHelper.this.mLastSegmentOverlay.calculateMedia();
                  }
               }
               else
               {
                  Log.w(TAG, "mTrackMediasObserver skipping change on " + LoggerMapHelper.this.mLastSegment);
               }
            }
         };

      this.mTrackSegmentsObserver = new ContentObserver(new Handler())
         {
            @Override
            public void onChange(final boolean selfUpdate)
            {
               if (!selfUpdate)
               {
                  updateDataOverlays();
               }
               else
               {
                  Log.w(TAG, "mTrackSegmentsObserver skipping change on " + LoggerMapHelper.this.mLastSegment);
               }
            }
         };

      this.mSegmentWaypointsObserver = new ContentObserver(new Handler())
         {
            @Override
            public void onChange(final boolean selfUpdate)
            {
               if (!selfUpdate)
               {
                  updateTrackNumbers();
                  if (LoggerMapHelper.this.mLastSegmentOverlay != null)
                  {
                     moveActiveViewWindow();
                     updateMapProviderAdministration(LoggerMapHelper.this.mLoggerMap.getDataSourceId());
                  }
                  else
                  {
                     Log.e(TAG, "Error the last segment changed but it is not on screen! " + LoggerMapHelper.this.mLastSegment);
                  }
               }
               else
               {
                  Log.w(TAG, "mSegmentWaypointsObserver skipping change on " + LoggerMapHelper.this.mLastSegment);
               }
            }
         };

      this.mUnitsChangeListener = new UnitsI18n.UnitsChangeListener()
         {
            @Override
            public void onUnitsChange()
            {
               LoggerMapHelper.this.mAverageSpeed = 0.0;
               LoggerMapHelper.this.mAverageHeight = 0.0;
               updateTrackNumbers();
               updateSpeedColoring();
            }
         };
   }

   public void onCreateOptionsMenu(final Menu menu)
   {
      menu.add(Menu.NONE, MENU_TRACKING, Menu.NONE, R.string.menu_tracking).setIcon(R.drawable.ic_action_set).setAlphabeticShortcut('T');

      /*
       * menu.add(ContextMenu.NONE, MENU_HOME, ContextMenu.NONE, R.string.menu_home). setIcon(R.drawable.ic_action_umbrella).setAlphabeticShortcut('H');
       */

      menu.add(Menu.NONE, MENU_LAYERS, Menu.NONE, R.string.menu_showLayers).setIcon(R.drawable.ic_menu_mapmode).setAlphabeticShortcut('L');

      menu.add(Menu.NONE, MENU_NOTE, Menu.NONE, R.string.menu_insertnote).setIcon(R.drawable.ic_menu_myplaces);

      menu.add(Menu.NONE, MENU_STATS, Menu.NONE, R.string.menu_statistics).setIcon(R.drawable.ic_menu_picture).setAlphabeticShortcut('S');

      menu.add(Menu.NONE, MENU_SHARE, Menu.NONE, R.string.menu_shareTrack).setIcon(R.drawable.ic_action_share).setAlphabeticShortcut('I');

      menu.add(Menu.NONE, MENU_SETTINGS, Menu.NONE, R.string.menu_settings).setIcon(R.drawable.ic_menu_preferences).setAlphabeticShortcut('C');

      menu.add(Menu.NONE, MENU_ABOUT, Menu.NONE, R.string.menu_about).setIcon(R.drawable.ic_menu_info_details).setAlphabeticShortcut('A');

      // More
      /*
       * menu.add(ContextMenu.NONE, MENU_TRACKLIST, ContextMenu.NONE, R.string.menu_tracklist). setIcon(R.drawable.ic_menu_show_list).setAlphabeticShortcut('P'); menu.add(ContextMenu.NONE, MENU_ABOUT,
       * ContextMenu.NONE, R.string.menu_about). setIcon(R.drawable.ic_menu_info_details).setAlphabeticShortcut('A'); menu.add(ContextMenu.NONE, MENU_CONTRIB, ContextMenu.NONE, R.string.menu_contrib).
       * setIcon(R.drawable.ic_menu_allfriends);
       */
   }

   //the class below is used to prepare some options to become active only after
   // some conditions are met as seen below
   public void onPrepareOptionsMenu(final Menu menu)
   {
      final MenuItem noteMenu = menu.findItem(MENU_NOTE);
      noteMenu.setEnabled(this.mTrackId >= 0 || this.mLoggerServiceManager.isMediaPrepared());
      /* noteMenu.setEnabled(mLoggerServiceManager.isMediaPrepared()); */

      final MenuItem shareMenu = menu.findItem(MENU_SHARE);
      shareMenu.setEnabled(this.mTrackId >= 0);
   }

   @SuppressWarnings("deprecation")
   public boolean onOptionsItemSelected(final MenuItem item)
   {
      boolean handled = false;

      Uri trackUri;
      Intent intent;
      switch (item.getItemId())
      {
         case MENU_TRACKING:
            intent = new Intent(this.mLoggerMap.getActivity(), ControlLogging.class);
            this.mLoggerMap.getActivity().startActivityForResult(intent, MENU_TRACKING);
            handled = true;
            break;

         /*
          * case MENU_HOME: intent = new Intent(mLoggerMap.getActivity(), MainActivity.class); mLoggerMap.getActivity().startActivityForResult(intent, MENU_HOME); handled = true; break;
          */

         case MENU_LAYERS:
            this.mLoggerMap.getActivity().showDialog(DIALOG_LAYERS);
            handled = true;
            break;

         case MENU_NOTE:
            intent = new Intent(this.mLoggerMap.getActivity(), ManualMode.class);
            this.mLoggerMap.getActivity().startActivityForResult(intent, MENU_NOTE);
            handled = true;
            break;

         case MENU_SETTINGS:
            intent = new Intent(this.mLoggerMap.getActivity(), ApplicationPreferenceActivity.class);
            this.mLoggerMap.getActivity().startActivity(intent);
            handled = true;
            break;
         case MENU_TRACKLIST:
            intent = new Intent(this.mLoggerMap.getActivity(), RouteList.class);
            intent.putExtra(BaseColumns._ID, this.mTrackId);
            this.mLoggerMap.getActivity().startActivityForResult(intent, MENU_TRACKLIST);
            handled = true;
            break;
         /*
          * case MENU_STATS: if (this.mTrackId >= 0) { intent = new Intent(mLoggerMap.getActivity(), Statistics.class); trackUri = ContentUris.withAppendedId(Tracks.CONTENT_URI, mTrackId);
          * intent.setData(trackUri); mLoggerMap.getActivity().startActivity(intent); break; } else { mLoggerMap.getActivity().showDialog(DIALOG_NOTRACK); } handled = true; break;
          */
         case MENU_ABOUT:
            intent = new Intent(this.mLoggerMap.getActivity(), About.class);
            this.mLoggerMap.getActivity().startActivity(intent);
            break;
         case MENU_SHARE:
            intent = new Intent(Intent.ACTION_RUN);
            trackUri = ContentUris.withAppendedId(Tracks.CONTENT_URI, this.mTrackId);
            intent.setDataAndType(trackUri, Tracks.CONTENT_ITEM_TYPE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            final Bitmap bm = this.mLoggerMap.getDrawingCache();
            final Uri screenStreamUri = ShareRoute.storeScreenBitmap(bm);
            intent.putExtra(Intent.EXTRA_STREAM, screenStreamUri);
            this.mLoggerMap.getActivity().startActivityForResult(Intent.createChooser(intent, this.mLoggerMap.getActivity().getString(R.string.share_track)), MENU_SHARE);
            handled = true;
            break;
         /*
          * case MENU_CONTRIB: mLoggerMap.getActivity().showDialog(DIALOG_CONTRIB);
          */
         default:
            handled = false;
            break;
      }
      return handled;
   }

   protected Dialog onCreateDialog(final int id)
   {
      Dialog dialog = null;
      LayoutInflater factory = null;
      View view = null;
      Builder builder = null;
      switch (id)
      {
         case DIALOG_LAYERS:

            builder = new AlertDialog.Builder(this.mLoggerMap.getActivity());
            factory = LayoutInflater.from(this.mLoggerMap.getActivity());
            view = factory.inflate(R.layout.layerdialog, null);

            final CheckBox traffic = (CheckBox) view.findViewById(R.id.layer_traffic);
            final CheckBox speed = (CheckBox) view.findViewById(R.id.layer_speed);
            final CheckBox altitude = (CheckBox) view.findViewById(R.id.layer_altitude);
            final CheckBox distance = (CheckBox) view.findViewById(R.id.layer_distance);
            final CheckBox compass = (CheckBox) view.findViewById(R.id.layer_compass);
            final CheckBox location = (CheckBox) view.findViewById(R.id.layer_location);

            ((RadioGroup) view.findViewById(R.id.google_backgrounds)).setOnCheckedChangeListener(this.mGroupCheckedChangeListener);
            ((RadioGroup) view.findViewById(R.id.osm_backgrounds)).setOnCheckedChangeListener(this.mGroupCheckedChangeListener);

            traffic.setOnCheckedChangeListener(this.mCheckedChangeListener);
            speed.setOnCheckedChangeListener(this.mCheckedChangeListener);
            altitude.setOnCheckedChangeListener(this.mCheckedChangeListener);
            distance.setOnCheckedChangeListener(this.mCheckedChangeListener);
            compass.setOnCheckedChangeListener(this.mCheckedChangeListener);
            location.setOnCheckedChangeListener(this.mCheckedChangeListener);

            builder.setTitle(R.string.dialog_layer_title).setIcon(android.R.drawable.ic_dialog_map).setPositiveButton(R.string.btn_okay, null).setView(view);
            dialog = builder.create();
            return dialog;
         case DIALOG_NOTRACK:
            builder = new AlertDialog.Builder(this.mLoggerMap.getActivity());
            builder.setTitle(R.string.dialog_notrack_title).setMessage(R.string.dialog_notrack_message).setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(R.string.btn_selecttrack, this.mNoTrackDialogListener).setNegativeButton(R.string.btn_cancel, null);
            dialog = builder.create();
            return dialog;
         case DIALOG_URIS:
            builder = new AlertDialog.Builder(this.mLoggerMap.getActivity());
            factory = LayoutInflater.from(this.mLoggerMap.getActivity());
            view = factory.inflate(R.layout.mediachooser, null);
            builder.setTitle(R.string.dialog_select_media_title).setMessage(R.string.dialog_select_media_message).setIcon(android.R.drawable.ic_dialog_alert)
                  .setNegativeButton(R.string.btn_cancel, null).setPositiveButton(R.string.btn_okay, this.mNoteSelectDialogListener).setView(view);
            dialog = builder.create();
            return dialog;
            /*
             * case DIALOG_CONTRIB: builder = new AlertDialog.Builder(mLoggerMap.getActivity()); factory = LayoutInflater.from(mLoggerMap.getActivity()); view = factory.inflate(R.layout.contrib,
             * null); TextView contribView = (TextView) view.findViewById(R.id.contrib_view); contribView.setText(R.string.dialog_contrib_message);
             * builder.setTitle(R.string.dialog_contrib_title).setView(view).setIcon(android.R.drawable.ic_dialog_email) .setPositiveButton(R.string.btn_okay, null); dialog = builder.create(); return
             * dialog;
             */
         default:
            return null;
      }
   }

   protected void onPrepareDialog(final int id, final Dialog dialog)
   {
      RadioButton satellite;
      RadioButton regular;
      RadioButton cloudmade;
      RadioButton mapnik;
      RadioButton cycle;
      switch (id)
      {
         case DIALOG_LAYERS:
            satellite = (RadioButton) dialog.findViewById(R.id.layer_google_satellite);
            regular = (RadioButton) dialog.findViewById(R.id.layer_google_regular);
            satellite.setChecked(this.mSharedPreferences.getBoolean(Constants.SATELLITE, false));
            regular.setChecked(!this.mSharedPreferences.getBoolean(Constants.SATELLITE, false));

            final int osmbase = this.mSharedPreferences.getInt(Constants.OSMBASEOVERLAY, 2); //the default overlay for OSM
            cloudmade = (RadioButton) dialog.findViewById(R.id.layer_osm_cloudmade);
            mapnik = (RadioButton) dialog.findViewById(R.id.layer_osm_maknik);
            cycle = (RadioButton) dialog.findViewById(R.id.layer_osm_bicycle);
            cloudmade.setChecked(osmbase == Constants.OSM_CLOUDMADE);
            mapnik.setChecked(osmbase == Constants.OSM_MAKNIK);
            cycle.setChecked(osmbase == Constants.OSM_CYCLE);

            ((CheckBox) dialog.findViewById(R.id.layer_traffic)).setChecked(this.mSharedPreferences.getBoolean(Constants.TRAFFIC, false));
            ((CheckBox) dialog.findViewById(R.id.layer_speed)).setChecked(this.mSharedPreferences.getBoolean(Constants.SPEED, false));
            ((CheckBox) dialog.findViewById(R.id.layer_altitude)).setChecked(this.mSharedPreferences.getBoolean(Constants.ALTITUDE, false));
            ((CheckBox) dialog.findViewById(R.id.layer_distance)).setChecked(this.mSharedPreferences.getBoolean(Constants.DISTANCE, false));
            ((CheckBox) dialog.findViewById(R.id.layer_compass)).setChecked(this.mSharedPreferences.getBoolean(Constants.COMPASS, false));
            ((CheckBox) dialog.findViewById(R.id.layer_location)).setChecked(this.mSharedPreferences.getBoolean(Constants.LOCATION, false));
            // the default provider for maps is done here
            final int provider = Integer.valueOf(this.mSharedPreferences.getString(Constants.MAPPROVIDER, "" + Constants.OSM)).intValue();

            switch (provider)
            {

            //so this is where I get to state the kind of layers we shall be using in each instance of maps.
               case Constants.GOOGLE:
                  dialog.findViewById(R.id.google_backgrounds).setVisibility(View.VISIBLE);
                  dialog.findViewById(R.id.osm_backgrounds).setVisibility(View.GONE);
                  dialog.findViewById(R.id.shared_layers).setVisibility(View.VISIBLE);
                  dialog.findViewById(R.id.google_overlays).setVisibility(View.VISIBLE);
                  break;

               case Constants.OSM:
                  dialog.findViewById(R.id.osm_backgrounds).setVisibility(View.VISIBLE);
                  dialog.findViewById(R.id.google_backgrounds).setVisibility(View.GONE);
                  dialog.findViewById(R.id.shared_layers).setVisibility(View.VISIBLE);
                  dialog.findViewById(R.id.google_overlays).setVisibility(View.GONE);
                  break;

               default:
                  dialog.findViewById(R.id.osm_backgrounds).setVisibility(View.VISIBLE);
                  dialog.findViewById(R.id.google_backgrounds).setVisibility(View.GONE);
                  dialog.findViewById(R.id.shared_layers).setVisibility(View.INVISIBLE);
                  dialog.findViewById(R.id.google_overlays).setVisibility(View.GONE);
                  break;
            }
            break;
         case DIALOG_URIS:
            final Gallery gallery = (Gallery) dialog.findViewById(R.id.gallery);
            gallery.setAdapter(this.mMediaAdapter);
            gallery.setOnItemSelectedListener(this.mGalerySelectListener);
         default:
            break;
      }
   }

   protected void onActivityResult(final int requestCode, final int resultCode, final Intent intent)
   {
      Uri trackUri;
      long trackId;
      switch (requestCode)
      {
         case MENU_TRACKLIST:
            if (resultCode == Activity.RESULT_OK)
            {
               trackUri = intent.getData();
               trackId = Long.parseLong(trackUri.getLastPathSegment());
               moveToTrack(trackId, true);
            }
            break;
         case MENU_TRACKING:
            if (resultCode == Activity.RESULT_OK)
            {
               trackUri = intent.getData();
               if (trackUri != null)
               {
                  trackId = Long.parseLong(trackUri.getLastPathSegment());
                  moveToTrack(trackId, true);
               }
            }
            break;
         case MENU_SHARE:
            ShareRoute.clearScreenBitmap();
            break;
         default:
            Log.e(TAG, "Returned form unknow activity: " + requestCode);
            break;
      }
   }

   private void updateTitleBar()
   {
      final ContentResolver resolver = this.mLoggerMap.getActivity().getContentResolver();
      Cursor trackCursor = null;
      try
      {
         trackCursor = resolver.query(ContentUris.withAppendedId(Tracks.CONTENT_URI, this.mTrackId), new String[] { TracksColumns.NAME }, null, null, null);
         if (trackCursor != null && trackCursor.moveToLast())
         {
            final String trackName = trackCursor.getString(0);
            this.mLoggerMap.getActivity().setTitle(this.mLoggerMap.getActivity().getString(R.string.app_name) + ": " + trackName);
         }
      }
      finally
      {
         if (trackCursor != null)
         {
            trackCursor.close();
         }
      }
   }

   private void updateMapProvider()
   {
      Class< ? > mapClass = OsmLoggerMap.class; //changed from null
      final int provider = Integer.valueOf(this.mSharedPreferences.getString(Constants.MAPPROVIDER, "" + Constants.OSM)).intValue();
      switch (provider)
      {
         case Constants.GOOGLE:
            mapClass = GoogleLoggerMap.class;
            break;
         case Constants.OSM:
            mapClass = OsmLoggerMap.class;
            break;
         case Constants.MAPQUEST:
            mapClass = MapQuestLoggerMap.class;
            break;
         default:
            mapClass = OsmLoggerMap.class;
            Log.e(TAG, "Fault in value " + provider + " as MapProvider, defaulting to Google Maps.");
            break;
      }
      if (mapClass != this.mLoggerMap.getActivity().getClass())
      {
         final Intent myIntent = this.mLoggerMap.getActivity().getIntent();
         Intent realIntent;
         if (myIntent != null)
         {
            realIntent = new Intent(myIntent.getAction(), myIntent.getData(), this.mLoggerMap.getActivity(), mapClass);
            realIntent.putExtras(myIntent);
         }
         else
         {
            realIntent = new Intent(this.mLoggerMap.getActivity(), mapClass);
            realIntent.putExtras(myIntent);
         }
         this.mLoggerMap.getActivity().startActivity(realIntent);
         this.mLoggerMap.getActivity().finish();
      }
   }

   protected void updateMapProviderAdministration(final String provider)
   {
      this.mLoggerServiceManager.storeDerivedDataSource(provider);
   }

   private void updateBlankingBehavior()
   {
      final boolean disableblanking = this.mSharedPreferences.getBoolean(Constants.DISABLEBLANKING, false);
      final boolean disabledimming = this.mSharedPreferences.getBoolean(Constants.DISABLEDIMMING, false);
      if (disableblanking)
      {
         if (this.mWakeLock == null)
         {
            final PowerManager pm = (PowerManager) this.mLoggerMap.getActivity().getSystemService(Context.POWER_SERVICE);
            if (disabledimming)
            {
               this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
            }
            else
            {
               this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
            }
         }
         if (this.mLoggerServiceManager.getLoggingState() == Constants.LOGGING && !this.mWakeLock.isHeld())
         {
            this.mWakeLock.acquire();
            Log.w(TAG, "Acquired lock to keep screen on!");
         }
      }
   }

   private void updateSpeedColoring()
   {
      final int trackColoringMethod = Integer.valueOf(this.mSharedPreferences.getString(Constants.TRACKCOLORING, "3")).intValue();
      final View speedbar = this.mLoggerMap.getActivity().findViewById(R.id.speedbar);
      final SlidingIndicatorView scaleIndicator = this.mLoggerMap.getScaleIndicatorView();

      TextView[] speedtexts = this.mLoggerMap.getSpeedTextViews();
      switch (trackColoringMethod)
      {
         case SegmentRendering.DRAW_MEASURED:
         case SegmentRendering.DRAW_CALCULATED:
            // mAverageSpeed is set to 0 if unknown or to trigger an recalculation here
            if (this.mAverageSpeed == 0.0)
            {
               this.mHandler.removeCallbacks(this.speedCalculator);
               this.mHandler.post(this.speedCalculator);
            }
            else
            {
               drawSpeedTexts();
               speedtexts = this.mLoggerMap.getSpeedTextViews();
               speedbar.setVisibility(View.INVISIBLE);
               scaleIndicator.setVisibility(View.INVISIBLE);
               for (final TextView speedtext : speedtexts)
               {
                  speedtext.setVisibility(View.INVISIBLE);
               }
            }
            break;
         case SegmentRendering.DRAW_DOTS:
         case SegmentRendering.DRAW_GREEN:
         case SegmentRendering.DRAW_RED:
            speedbar.setVisibility(View.INVISIBLE);
            scaleIndicator.setVisibility(View.INVISIBLE);
            for (final TextView speedtext : speedtexts)
            {
               speedtext.setVisibility(View.INVISIBLE);
            }
            break;
         case SegmentRendering.DRAW_HEIGHT:
            if (this.mAverageHeight == 0.0)
            {
               this.mHandler.removeCallbacks(this.heightCalculator);
               this.mHandler.post(this.heightCalculator);
            }
            else
            {
               drawHeightTexts();
               speedtexts = this.mLoggerMap.getSpeedTextViews();
               speedbar.setVisibility(View.INVISIBLE);
               scaleIndicator.setVisibility(View.INVISIBLE);
               for (final TextView speedtext : speedtexts)
               {
                  speedtext.setVisibility(View.INVISIBLE);
               }
            }
            break;
         default:
            break;
      }
      this.mBitmapSegmentsOverlay.setTrackColoringMethod(trackColoringMethod, this.mAverageSpeed, this.mAverageHeight);
   }

   private void updateSpeedDisplayVisibility()
   {
      final boolean showspeed = this.mSharedPreferences.getBoolean(Constants.SPEED, false);
      final TextView lastGPSSpeedView = this.mLoggerMap.getSpeedTextView();
      if (showspeed)
      {
         lastGPSSpeedView.setVisibility(View.INVISIBLE);
      }
      else
      {
         lastGPSSpeedView.setVisibility(View.GONE);
      }
      updateScaleDisplayVisibility();
   }

   private void updateAltitudeDisplayVisibility()
   {
      final boolean showaltitude = this.mSharedPreferences.getBoolean(Constants.ALTITUDE, false);
      final TextView lastGPSAltitudeView = this.mLoggerMap.getAltitideTextView();
      if (showaltitude)
      {
         lastGPSAltitudeView.setVisibility(View.VISIBLE);
      }
      else
      {
         lastGPSAltitudeView.setVisibility(View.GONE);
      }
      updateScaleDisplayVisibility();
   }

   private void updateScaleDisplayVisibility()
   {
      final SlidingIndicatorView scaleIndicator = this.mLoggerMap.getScaleIndicatorView();
      final boolean showspeed = this.mSharedPreferences.getBoolean(Constants.SPEED, false);
      final boolean showaltitude = this.mSharedPreferences.getBoolean(Constants.ALTITUDE, false);
      final int trackColoringMethod = Integer.valueOf(this.mSharedPreferences.getString(Constants.TRACKCOLORING, "3")).intValue();
      switch (trackColoringMethod)
      {
         case SegmentRendering.DRAW_MEASURED:
         case SegmentRendering.DRAW_CALCULATED:
            if (showspeed)
            {
               scaleIndicator.setVisibility(View.VISIBLE);
            }
            else
            {
               scaleIndicator.setVisibility(View.GONE);
            }
            break;
         case SegmentRendering.DRAW_HEIGHT:
         default:
            if (showaltitude)
            {
               scaleIndicator.setVisibility(View.VISIBLE);
            }
            else
            {
               scaleIndicator.setVisibility(View.GONE);
            }
            break;
      }
   }

   private void updateDistanceDisplayVisibility()
   {
      final boolean showdistance = this.mSharedPreferences.getBoolean(Constants.DISTANCE, false);
      final TextView distanceView = this.mLoggerMap.getDistanceTextView();
      if (showdistance)
      {
         distanceView.setVisibility(View.VISIBLE);
      }
      else
      {
         distanceView.setVisibility(View.GONE);
      }
   }

   private void updateCompassDisplayVisibility()
   {
      final boolean compass = this.mSharedPreferences.getBoolean(Constants.COMPASS, true);
      if (compass)
      {
         this.mLoggerMap.enableCompass();
      }
      else
      {
         this.mLoggerMap.disableCompass();
      }
   }

   private void updateLocationDisplayVisibility()
   {
      final boolean location = this.mSharedPreferences.getBoolean(Constants.LOCATION, true);
      if (location)
      {
         this.mLoggerMap.enableMyLocation();
      }
      else
      {
         this.mLoggerMap.disableMyLocation();
      }
   }

   /**
    * Retrieves the numbers of the measured speed and altitude from the most recent waypoint and updates UI components with this latest bit of information.
    */
   private void updateTrackNumbers()
   {
      final Location lastWaypoint = this.mLoggerServiceManager.getLastWaypoint();
      final UnitsI18n units = this.mUnits;
      if (lastWaypoint != null && units != null)
      {
         // Speed number
         this.mSpeed = lastWaypoint.getSpeed();
         this.mAltitude = lastWaypoint.getAltitude();
         this.mDistance = this.mLoggerServiceManager.getTrackedDistance();
      }

      //Distance number
      final double distance = units.conversionFromMeter(this.mDistance);
      final String distanceText = String.format("%.2f %s", distance, units.getDistanceUnit());
      final TextView mDistanceView = this.mLoggerMap.getDistanceTextView();
      mDistanceView.setText(distanceText);

      //Speed number
      final double speed = units.conversionFromMetersPerSecond(this.mSpeed);
      final String speedText = units.formatSpeed(speed, false);
      final TextView lastGPSSpeedView = this.mLoggerMap.getSpeedTextView();
      lastGPSSpeedView.setText(speedText);

      //Altitude number
      final double altitude = units.conversionFromMeterToHeight(this.mAltitude);
      final String altitudeText = String.format("%.0f %s", altitude, units.getHeightUnit());
      final TextView mLastGPSAltitudeView = this.mLoggerMap.getAltitideTextView();
      mLastGPSAltitudeView.setText(altitudeText);

      // Slider indicator
      final SlidingIndicatorView currentScaleIndicator = this.mLoggerMap.getScaleIndicatorView();
      final int trackColoringMethod = Integer.valueOf(this.mSharedPreferences.getString(Constants.TRACKCOLORING, "3")).intValue();
      if (trackColoringMethod == SegmentRendering.DRAW_MEASURED || trackColoringMethod == SegmentRendering.DRAW_CALCULATED)
      {
         currentScaleIndicator.setValue((float) speed);
         // Speed color bar and reference numbers
         if (speed > 2 * this.mAverageSpeed)
         {
            this.mAverageSpeed = 0.0;
            updateSpeedColoring();
            this.mBitmapSegmentsOverlay.scheduleRecalculation();
         }
      }
      else if (trackColoringMethod == SegmentRendering.DRAW_HEIGHT)
      {
         currentScaleIndicator.setValue((float) altitude);
         // Speed color bar and reference numbers
         if (altitude > 2 * this.mAverageHeight)
         {
            this.mAverageHeight = 0.0;
            updateSpeedColoring();
            this.mLoggerMap.postInvalidate();
         }
      }

   }

   /**
    * For the current track identifier the route of that track is drawn by adding a OverLay for each segments in the track
    * 
    * @param trackId
    * @see SegmentRendering
    */
   private void createDataOverlays()
   {
      this.mLastSegmentOverlay = null;
      this.mBitmapSegmentsOverlay.clearSegments();
      this.mLoggerMap.clearOverlays();
      this.mLoggerMap.addOverlay(this.mBitmapSegmentsOverlay);

      final ContentResolver resolver = this.mLoggerMap.getActivity().getContentResolver();
      Cursor segments = null;
      final int trackColoringMethod = Integer.valueOf(this.mSharedPreferences.getString(Constants.TRACKCOLORING, "2")).intValue();

      try
      {
         final Uri segmentsUri = Uri.withAppendedPath(Tracks.CONTENT_URI, this.mTrackId + "/segments");
         segments = resolver.query(segmentsUri, new String[] { BaseColumns._ID }, null, null, null);
         if (segments != null && segments.moveToFirst())
         {
            do
            {
               final long segmentsId = segments.getLong(0);
               final Uri segmentUri = ContentUris.withAppendedId(segmentsUri, segmentsId);
               final SegmentRendering segmentOverlay = new SegmentRendering(this.mLoggerMap, segmentUri, trackColoringMethod, this.mAverageSpeed, this.mAverageHeight, this.mHandler);
               this.mBitmapSegmentsOverlay.addSegment(segmentOverlay);
               this.mLastSegmentOverlay = segmentOverlay;
               if (segments.isFirst())
               {
                  segmentOverlay.addPlacement(SegmentRendering.FIRST_SEGMENT);
               }
               if (segments.isLast())
               {
                  segmentOverlay.addPlacement(SegmentRendering.LAST_SEGMENT);
               }
               this.mLastSegment = segmentsId;
            }
            while (segments.moveToNext());
         }
      }
      finally
      {
         if (segments != null)
         {
            segments.close();
         }
      }

      final Uri lastSegmentUri = Uri.withAppendedPath(Tracks.CONTENT_URI, this.mTrackId + "/segments/" + this.mLastSegment + "/waypoints");
      resolver.unregisterContentObserver(this.mSegmentWaypointsObserver);
      resolver.registerContentObserver(lastSegmentUri, false, this.mSegmentWaypointsObserver);
   }

   private void updateDataOverlays()
   {
      final ContentResolver resolver = this.mLoggerMap.getActivity().getContentResolver();
      final Uri segmentsUri = Uri.withAppendedPath(Tracks.CONTENT_URI, this.mTrackId + "/segments");
      Cursor segmentsCursor = null;
      final int segmentOverlaysCount = this.mBitmapSegmentsOverlay.size();
      try
      {
         segmentsCursor = resolver.query(segmentsUri, new String[] { BaseColumns._ID }, null, null, null);
         if (segmentsCursor != null && segmentsCursor.getCount() == segmentOverlaysCount)
         {
            //            Log.d( TAG, "Alignment of segments" );
         }
         else
         {
            createDataOverlays();
         }
      }
      finally
      {
         if (segmentsCursor != null)
         {
            segmentsCursor.close();
         }
      }
   }

   /**
    * Call when an overlay has recalulated and has new information to be redrawn
    */

   private void moveActiveViewWindow()
   {
      final GeoPoint lastPoint = getLastTrackPoint();
      if (lastPoint != null && this.mLoggerServiceManager.getLoggingState() == Constants.LOGGING)
      {
         if (this.mLoggerMap.isOutsideScreen(lastPoint))
         {
            this.mLoggerMap.clearAnimation();
            this.mLoggerMap.setCenter(lastPoint);
         }
         else if (this.mLoggerMap.isNearScreenEdge(lastPoint))
         {
            this.mLoggerMap.clearAnimation();
            this.mLoggerMap.animateTo(lastPoint);
         }
      }
   }

   /**
    * Updates the labels next to the color bar with speeds
    */
   private void drawSpeedTexts()
   {
      final UnitsI18n units = this.mUnits;
      if (units != null)
      {
         final double avgSpeed = units.conversionFromMetersPerSecond(this.mAverageSpeed);
         final TextView[] mSpeedtexts = this.mLoggerMap.getSpeedTextViews();
         final SlidingIndicatorView currentScaleIndicator = this.mLoggerMap.getScaleIndicatorView();
         for (int i = 0; i < mSpeedtexts.length; i++)
         {
            mSpeedtexts[i].setVisibility(View.INVISIBLE); //just changed the visibility of the speedTexts from visible
            double speed;

            if (this.mUnits.isUnitFlipped())
            {
               speed = ((avgSpeed * 2d) / 5d) * (mSpeedtexts.length - i - 1);
            }

            else
            {
               speed = ((avgSpeed * 2d) / 5d) * i;
            }

            if (i == 0)
            {
               currentScaleIndicator.setMin((float) speed);
            }

            else
            {
               currentScaleIndicator.setMax((float) speed);
            }

            final String speedText = units.formatSpeed(speed, false);
            mSpeedtexts[i].setText(speedText);
         }
      }
   }

   /**
    * Updates the labels next to the color bar with heights
    */
   private void drawHeightTexts()
   {
      final UnitsI18n units = this.mUnits;
      if (units != null)
      {
         final double avgHeight = units.conversionFromMeterToHeight(this.mAverageHeight);
         final TextView[] mSpeedtexts = this.mLoggerMap.getSpeedTextViews();
         final SlidingIndicatorView currentScaleIndicator = this.mLoggerMap.getScaleIndicatorView();
         for (int i = 0; i < mSpeedtexts.length; i++)
         {
            mSpeedtexts[i].setVisibility(View.INVISIBLE);
            final double height = ((avgHeight * 2d) / 5d) * i;
            final String heightText = String.format("%d %s", (int) height, units.getHeightUnit());
            mSpeedtexts[i].setText(heightText);
            if (i == 0)
            {
               currentScaleIndicator.setMin((float) height);
            }
            else
            {
               currentScaleIndicator.setMax((float) height);
            }
         }
      }
   }

   /**
    * Alter this to set a new track as current.
    * 
    * @param trackId
    * @param center center on the end of the track
    */
   private void moveToTrack(final long trackId, final boolean center)
   {
      if (trackId == this.mTrackId)
      {
         return;
      }
      Cursor track = null;
      try
      {
         final ContentResolver resolver = this.mLoggerMap.getActivity().getContentResolver();
         final Uri trackUri = ContentUris.withAppendedId(Tracks.CONTENT_URI, trackId);
         track = resolver.query(trackUri, new String[] { TracksColumns.NAME }, null, null, null);
         if (track != null && track.moveToFirst())
         {
            this.mTrackId = trackId;
            this.mLastSegment = -1;
            resolver.unregisterContentObserver(this.mTrackSegmentsObserver);
            resolver.unregisterContentObserver(this.mTrackMediasObserver);
            final Uri tracksegmentsUri = Uri.withAppendedPath(Tracks.CONTENT_URI, trackId + "/segments");

            resolver.registerContentObserver(tracksegmentsUri, false, this.mTrackSegmentsObserver);
            resolver.registerContentObserver(Media.CONTENT_URI, true, this.mTrackMediasObserver);

            this.mLoggerMap.clearOverlays();
            this.mBitmapSegmentsOverlay.clearSegments();
            this.mAverageSpeed = 0.0;
            this.mAverageHeight = 0.0;

            // updateTitleBar();
            updateDataOverlays();
            updateSpeedColoring();
            if (center)
            {
               final GeoPoint lastPoint = getLastTrackPoint();
               this.mLoggerMap.animateTo(lastPoint);
            }
         }
      }
      finally
      {
         if (track != null)
         {
            track.close();
         }
      }
   }

   /**
    * Get the last know position from the GPS provider and return that information wrapped in a GeoPoint to which the Map can navigate.
    * 
    * @see GeoPoint
    * @return
    */
   private GeoPoint getLastKnowGeopointLocation()
   {
      int microLatitude = 0;
      int microLongitude = 0;
      final LocationManager locationManager = (LocationManager) this.mLoggerMap.getActivity().getApplication().getSystemService(Context.LOCATION_SERVICE);
      final Location locationFine = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
      if (locationFine != null)
      {
         microLatitude = (int) (locationFine.getLatitude() * 1E6d);
         microLongitude = (int) (locationFine.getLongitude() * 1E6d);
      }
      if (locationFine == null || microLatitude == 0 || microLongitude == 0)
      {
         final Location locationCoarse = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
         if (locationCoarse != null)
         {
            microLatitude = (int) (locationCoarse.getLatitude() * 1E6d);
            microLongitude = (int) (locationCoarse.getLongitude() * 1E6d);
         }
         if (locationCoarse == null || microLatitude == 0 || microLongitude == 0)
         {
            microLatitude = 51985105;
            microLongitude = 5106132;
         }
      }
      final GeoPoint geoPoint = new GeoPoint(microLatitude, microLongitude);
      return geoPoint;
   }

   /**
    * Retrieve the last point of the current track
    * 
    * @param context
    */
   private GeoPoint getLastTrackPoint()
   {
      Cursor waypoint = null;
      GeoPoint lastPoint = null;
      // First try the service which might have a cached version
      final Location lastLoc = this.mLoggerServiceManager.getLastWaypoint();
      if (lastLoc != null)
      {
         final int microLatitude = (int) (lastLoc.getLatitude() * 1E6d);
         final int microLongitude = (int) (lastLoc.getLongitude() * 1E6d);
         lastPoint = new GeoPoint(microLatitude, microLongitude);
      }

      // If nothing yet, try the content resolver and query the track
      if (lastPoint == null || lastPoint.getLatitudeE6() == 0 || lastPoint.getLongitudeE6() == 0)
      {
         try
         {
            final ContentResolver resolver = this.mLoggerMap.getActivity().getContentResolver();
            waypoint = resolver.query(Uri.withAppendedPath(Tracks.CONTENT_URI, this.mTrackId + "/waypoints"), new String[] { WaypointsColumns.LATITUDE, WaypointsColumns.LONGITUDE,
                  "max(" + Waypoints.TABLE + "." + BaseColumns._ID + ")" }, null, null, null);
            if (waypoint != null && waypoint.moveToLast())
            {
               final int microLatitude = (int) (waypoint.getDouble(0) * 1E6d);
               final int microLongitude = (int) (waypoint.getDouble(1) * 1E6d);
               lastPoint = new GeoPoint(microLatitude, microLongitude);
            }
         }
         finally
         {
            if (waypoint != null)
            {
               waypoint.close();
            }
         }
      }

      // If nothing yet, try the last generally known location
      if (lastPoint == null || lastPoint.getLatitudeE6() == 0 || lastPoint.getLongitudeE6() == 0)
      {
         lastPoint = getLastKnowGeopointLocation();
      }
      return lastPoint;
   }

   private void moveToLastTrack()
   {
      int trackId = -1;
      Cursor track = null;
      try
      {
         final ContentResolver resolver = this.mLoggerMap.getActivity().getContentResolver();
         track = resolver.query(Tracks.CONTENT_URI, new String[] { "max(" + BaseColumns._ID + ")", TracksColumns.NAME, }, null, null, null);
         if (track != null && track.moveToLast())
         {
            trackId = track.getInt(0);
            moveToTrack(trackId, false);
         }
      }
      finally
      {
         if (track != null)
         {
            track.close();
         }
      }
   }

   /**
    * Enables a SegmentOverlay to call back to the MapActivity to show a dialog with choices of media
    * 
    * @param mediaAdapter
    */
   public void showMediaDialog(final BaseAdapter mediaAdapter)
   {
      this.mMediaAdapter = mediaAdapter;
      this.mLoggerMap.getActivity().showDialog(DIALOG_URIS);
   }

   public SharedPreferences getPreferences()
   {
      return this.mSharedPreferences;
   }

   public boolean isLogging()
   {
      return this.mLoggerServiceManager.getLoggingState() == Constants.LOGGING;
   }

}
