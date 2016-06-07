package dev.potholespot.android.logger;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.Semaphore;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import dev.potholespot.android.db.Pspot.Labels;
import dev.potholespot.android.db.Pspot.MediaColumns;
import dev.potholespot.android.db.Pspot.MetaDataColumns;
import dev.potholespot.android.db.Pspot.Tracks;
import dev.potholespot.android.db.Pspot.TracksColumns;
import dev.potholespot.android.db.Pspot.WaypointsColumns;
import dev.potholespot.android.db.Pspot.XYZColumns;
import dev.potholespot.android.streaming.StreamUtils;
import dev.potholespot.android.util.Constants;
import dev.potholespot.android.viewer.map.CommonLoggerMap;
import dev.potholespot.uganda.R;

/**
 * A system service as controlling the background logging of gps locations.
 * 
 * @author Martin Bbaale
 */
public class GPSLoggerService extends Service implements LocationListener, SensorEventListener
{

   //accelerometer readings

   long lastTime;
   Uri mXyzUri;
   private SensorManager sensorManager;

   private SensorEvent mLastRecordedEvent;
   private Location mLastRecordedLocation;

   private static final float FINE_DISTANCE = 5F;
   private static final long FINE_INTERVAL = 1000l;
   private static final float FINE_ACCURACY = 20f;

   private static final float NORMAL_DISTANCE = 10F;
   private static final long NORMAL_INTERVAL = 15000l;
   private static final float NORMAL_ACCURACY = 30f;

   private static final float COARSE_DISTANCE = 25F;
   private static final long COARSE_INTERVAL = 30000l;
   private static final float COARSE_ACCURACY = 75f;

   private static final float GLOBAL_DISTANCE = 500F;
   private static final long GLOBAL_INTERVAL = 300000l;
   private static final float GLOBAL_ACCURACY = 1000f;

   /**
    * <code>MAX_REASONABLE_SPEED</code> is about 324 kilometer per hour or 201 mile per hour.
    */

   private static final int MAX_REASONABLE_SPEED = 90;

   /**
    * <code>MAX_REASONABLE_ALTITUDECHANGE</code> between the last few waypoints and a new one the difference should be less then 200 meter.
    */
   private static final int MAX_REASONABLE_ALTITUDECHANGE = 200;

   private static final Boolean DEBUG = false;
   private static final boolean VERBOSE = false;
   private static final String TAG = "PRIM.GPSLoggerService";

   private static final String SERVICESTATE_DISTANCE = "SERVICESTATE_DISTANCE";
   private static final String SERVICESTATE_STATE = "SERVICESTATE_STATE";
   private static final String SERVICESTATE_PRECISION = "SERVICESTATE_PRECISION";
   private static final String SERVICESTATE_SEGMENTID = "SERVICESTATE_SEGMENTID";
   private static final String SERVICESTATE_TRACKID = "SERVICESTATE_TRACKID";
   private static final String SERVICESTATE_LABELID = "SERVICESTATE_LABELID";
   private static final String SERVICESTATE_XYZID = "SERVICESTATE_XYZID";
   private static final String SERVICESTATE_LOCATIONID = "SERVICESTATE_LOCATIONID";

   private static final int ADDGPSSTATUSLISTENER = 0;
   private static final int REQUEST_FINEGPS_LOCATIONUPDATES = 1;
   private static final int REQUEST_NORMALGPS_LOCATIONUPDATES = 2;
   private static final int REQUEST_COARSEGPS_LOCATIONUPDATES = 3;
   private static final int REQUEST_GLOBALNETWORK_LOCATIONUPDATES = 4;
   private static final int REQUEST_CUSTOMGPS_LOCATIONUPDATES = 5;
   private static final int STOPLOOPER = 6;
   private static final int GPSPROBLEM = 7;

   private static final int LOGGING_UNAVAILABLE = R.string.service_connectiondisabled;

   /**
    * DUP from android.app.Service.START_STICKY
    */
   private static final int START_STICKY = 1;

   public static final String COMMAND = "dev.potholespot.android.extra.COMMAND";
   public static final int EXTRA_COMMAND_START = 0;
   public static final int EXTRA_COMMAND_PAUSE = 1;
   public static final int EXTRA_COMMAND_RESUME = 2;
   public static final int EXTRA_COMMAND_STOP = 3;

   private LocationManager mLocationManager;
   private NotificationManager mNoticationManager;
   private PowerManager.WakeLock mWakeLock;
   private Handler mHandler;
   private Notification.Builder nBuilder;
   /**
    * If speeds should be checked to sane values
    */
   private boolean mSpeedSanityCheck;
   /**
    * If broadcasts of location about should be sent to stream location
    */
   private boolean mStreamBroadcast;

   private long mTrackId = -1;
   private long mLabelId = -1;
   private long mXyzId = -1;
   private long mSegmentId = -1;
   private long mWaypointId = -1;
   private long mLocationId = -1;
   private int mPrecision;
   private int mLoggingState = Constants.STOPPED;
   private boolean mStartNextSegment;

   private String mSources;

   private Location mPreviousLocation;
   private float mDistance;
   private Notification mNotification;

   private Vector<Location> mWeakLocations;
   private Vector<SensorEvent> mWeakSensorEvent;
   private Queue<Double> mAltitudes;

   /**
    * <code>mAcceptableAccuracy</code> indicates the maximum acceptable accuracy of a waypoint in meters.
    */
   private float mMaxAcceptableAccuracy = 20;
   private int mSatellites = 0;

   private boolean mShowingGpsDisabled;

   /**
    * Should the GPS Status monitor update the notification bar
    */
   private boolean mStatusMonitor;

   /**
    * Time thread to runs tasks that check whether the GPS listener has received enough to consider the GPS system alive.
    */
   private Timer mHeartbeatTimer;

   /**
    * Listens to changes in preference to precision and sanity checks
    */
   private final OnSharedPreferenceChangeListener mSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener()
      {

         @Override
         public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key)
         {
            if (key.equals(Constants.PRECISION) || key.equals(Constants.LOGGING_DISTANCE) || key.equals(Constants.LOGGING_INTERVAL))
            {
               sendRequestLocationUpdatesMessage();
               crashProtectState();
               updateNotification();
               broadCastLoggingState();
            }
            else if (key.equals(Constants.SPEEDSANITYCHECK))
            {
               GPSLoggerService.this.mSpeedSanityCheck = sharedPreferences.getBoolean(Constants.SPEEDSANITYCHECK, true);
            }
            else if (key.equals(Constants.STATUS_MONITOR))
            {
               GPSLoggerService.this.mLocationManager.removeGpsStatusListener(GPSLoggerService.this.mStatusListener);
               sendRequestStatusUpdateMessage();
               updateNotification();
            }
            else if (key.equals(Constants.BROADCAST_STREAM) || key.equals("VOICEOVER_ENABLED") || key.equals("CUSTOMUPLOAD_ENABLED"))
            {
               if (key.equals(Constants.BROADCAST_STREAM))
               {
                  GPSLoggerService.this.mStreamBroadcast = sharedPreferences.getBoolean(Constants.BROADCAST_STREAM, false);
               }
               StreamUtils.shutdownStreams(GPSLoggerService.this);
               if (!GPSLoggerService.this.mStreamBroadcast)
               {
                  StreamUtils.initStreams(GPSLoggerService.this);
               }
            }
         }
      };

   @Override
   public void onLocationChanged(final Location location)
   {

      try
      {
         final double lat = getmLastRecordedLocation().getLatitude();
         final double lon = getmLastRecordedLocation().getLongitude();

         Log.d("latitude:", "" + lat);
         Log.d("longitude:", "" + lon);
      }

      catch (final Exception i)
      {
         Log.d(TAG, "exception noticed inside onLocationChanged" + i.getMessage());

      }

      if (VERBOSE)
      {
         Log.v(TAG, "onLocationChanged( Location " + location + " )");
      }

      // Might be claiming GPS disabled but when we were paused this changed and this location proves so
      if (this.mShowingGpsDisabled)
      {
         notifyOnEnabledProviderNotification(R.string.service_gpsenabled);
      }

      final Location filteredLocation = locationFilter(location);

      if (filteredLocation != null)
      {
         if (this.mStartNextSegment)
         {
            this.mStartNextSegment = false;
            // Obey the start segment if the previous location is unknown or far away
            if (this.mPreviousLocation == null || filteredLocation.distanceTo(this.mPreviousLocation) > 4 * this.mMaxAcceptableAccuracy)
            {
               startNewSegment();
            }
         }
         else if (this.mPreviousLocation != null)
         {
            this.mDistance += this.mPreviousLocation.distanceTo(filteredLocation);
         }

         storeLocation(filteredLocation);
         broadcastLocation(filteredLocation);
         this.mPreviousLocation = location;
         setmLastRecordedLocation(location);

      }
   }

   @Override
   public void onProviderDisabled(final String provider)
   {
      if (DEBUG)
      {
         Log.d(TAG, "onProviderDisabled( String " + provider + " )");
      }
      ;
      if (this.mPrecision != Constants.LOGGING_GLOBAL && provider.equals(LocationManager.GPS_PROVIDER))
      {
         notifyOnDisabledProvider(R.string.service_gpsdisabled);
      }
      else if (this.mPrecision == Constants.LOGGING_GLOBAL && provider.equals(LocationManager.NETWORK_PROVIDER))
      {
         notifyOnDisabledProvider(R.string.service_datadisabled);
      }

   }

   @Override
   public void onProviderEnabled(final String provider)
   {
      if (DEBUG)
      {
         Log.d(TAG, "onProviderEnabled( String " + provider + " )");
      }
      ;
      if (this.mPrecision != Constants.LOGGING_GLOBAL && provider.equals(LocationManager.GPS_PROVIDER))
      {
         notifyOnEnabledProviderNotification(R.string.service_gpsenabled);
         this.mStartNextSegment = true;
      }
      else if (this.mPrecision == Constants.LOGGING_GLOBAL && provider.equals(LocationManager.NETWORK_PROVIDER))
      {
         notifyOnEnabledProviderNotification(R.string.service_dataenabled);
      }
   }

   @Override
   public void onStatusChanged(final String provider, final int status, final Bundle extras)
   {
      if (DEBUG)
      {
         Log.d(TAG, "onStatusChanged( String " + provider + ", int " + status + ", Bundle " + extras + " )");
      }

      if (status == LocationProvider.OUT_OF_SERVICE)
      {
         Log.e(TAG, String.format("Provider %s changed to status %d", provider, status));
      }
   }

   /**
    * Listens to GPS status changes
    */
   private final Listener mStatusListener = new GpsStatus.Listener()
      {
         @Override
         public synchronized void onGpsStatusChanged(final int event)
         {
            switch (event)
            {
               case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                  if (GPSLoggerService.this.mStatusMonitor)
                  {
                     final GpsStatus status = GPSLoggerService.this.mLocationManager.getGpsStatus(null);
                     GPSLoggerService.this.mSatellites = 0;
                     final Iterable<GpsSatellite> list = status.getSatellites();
                     for (final GpsSatellite satellite : list)
                     {
                        if (satellite.usedInFix())
                        {
                           GPSLoggerService.this.mSatellites++;
                        }
                     }
                     updateNotification();
                  }
                  break;
               case GpsStatus.GPS_EVENT_STOPPED:
                  break;
               case GpsStatus.GPS_EVENT_STARTED:
                  break;
               default:
                  break;
            }
         }
      };

   /****
    * create an instance of Binder that either: contains public methods that the client can call returns the current Service instance, which has public methods the client can call or, returns an
    * instance of another class hosted by the service with public methods the client can call Return this instance of Binder from the onBind() callback method. In the client, receive the Binder from
    * the onServiceConnected() callback method and make calls to the bound service using the methods provided. My onServiceConnected() is in the GPSLoggerServiceManager Class.
    * */

   private final IBinder mBinder = new IGPSLoggerServiceRemote.Stub()
      {
         @Override
         public int loggingState() throws RemoteException
         {
            return GPSLoggerService.this.mLoggingState;
         }

         @Override
         public long startLogging() throws RemoteException
         {
            GPSLoggerService.this.startLogging();
            return GPSLoggerService.this.mTrackId;
            //return mLabelId;
         }

         @Override
         public void pauseLogging() throws RemoteException
         {
            GPSLoggerService.this.pauseLogging();
         }

         @Override
         public long resumeLogging() throws RemoteException
         {
            GPSLoggerService.this.resumeLogging();
            return GPSLoggerService.this.mSegmentId;
         }

         @Override
         public void stopLogging() throws RemoteException
         {
            GPSLoggerService.this.stopLogging();
         }

         @Override
         public Uri storeMediaUri(final Uri mediaUri) throws RemoteException
         {
            GPSLoggerService.this.storeMediaUri(mediaUri);
            return null;
         }

         @Override
         public boolean isMediaPrepared() throws RemoteException
         {
            return GPSLoggerService.this.isMediaPrepared();
         }

         @Override
         public void storeDerivedDataSource(final String sourceName) throws RemoteException
         {
            GPSLoggerService.this.storeDerivedDataSource(sourceName);
         }

         @Override
         public Location getLastWaypoint() throws RemoteException
         {
            return GPSLoggerService.this.getLastWaypoint();
         }

         @Override
         public float getTrackedDistance() throws RemoteException
         {
            return GPSLoggerService.this.getTrackedDistance();
         }
      };

   /**
    * Task that will be run periodically during active logging to verify that the logging really happens and that the GPS hasn't silently stopped.
    */
   private TimerTask mHeartbeat = null;

   /**
    * Task to determine if the GPS is alive
    */

   class Heartbeat extends TimerTask
   {

      private final String mProvider;

      public Heartbeat(final String provider)
      {
         this.mProvider = provider;
      }

      @Override
      public void run()
      {
         if (isLogging())
         {
            // Collect the last location from the last logged location or a more recent from the last weak location

            Location checkLocation = GPSLoggerService.this.mPreviousLocation;
            synchronized (GPSLoggerService.this.mWeakLocations)
            {
               if (!GPSLoggerService.this.mWeakLocations.isEmpty())
               {
                  if (checkLocation == null)
                  {
                     checkLocation = GPSLoggerService.this.mWeakLocations.lastElement();
                  }
                  else
                  {
                     final Location weakLocation = GPSLoggerService.this.mWeakLocations.lastElement();
                     checkLocation = weakLocation.getTime() > checkLocation.getTime() ? weakLocation : checkLocation;
                  }
               }
            }

            // Is the last known GPS location something nearby we are not told?
            final Location managerLocation = GPSLoggerService.this.mLocationManager.getLastKnownLocation(this.mProvider);
            if (managerLocation != null && checkLocation != null)
            {
               if (checkLocation.distanceTo(managerLocation) < 2 * GPSLoggerService.this.mMaxAcceptableAccuracy)
               {
                  checkLocation = managerLocation.getTime() > checkLocation.getTime() ? managerLocation : checkLocation;
               }
            }

            if (checkLocation == null || checkLocation.getTime() + GPSLoggerService.this.mCheckPeriod < new Date().getTime())
            {
               Log.w(TAG, "GPS system failed to produce a location during logging: " + checkLocation);
               GPSLoggerService.this.mLoggingState = Constants.PAUSED;
               resumeLogging();

               if (GPSLoggerService.this.mStatusMonitor)
               {
                  soundGpsSignalAlarm();
               }

            }
         }
      }
   };

   /**
    * Number of milliseconds that a functioning GPS system needs to provide a location. Calculated to be either 120 seconds or 4 times the requested period, whichever is larger.
    */

   private long mCheckPeriod;

   private float mBroadcastDistance;

   private long mLastTimeBroadcast;

   private class GPSLoggerServiceThread extends Thread
   {
      public Semaphore ready = new Semaphore(0);

      GPSLoggerServiceThread()
      {
         setName("GPSLoggerServiceThread");
      }

      @Override
      public void run()
      {
         Looper.prepare();
         GPSLoggerService.this.mHandler = new Handler()
            {
               @Override
               public void handleMessage(final Message msg)
               {
                  _handleMessage(msg);
               }
            };
         this.ready.release(); // Signal the looper and handler are created 
         Looper.loop();
      }
   }

   /**
    * Called by the system when the service is first created. Do not call this method directly. Be sure to call super.onCreate().
    */
   @Override
   public void onCreate()
   {
      super.onCreate();
      if (DEBUG)
      {
         Log.d(TAG, "onCreate()");
      }
      ;

      final GPSLoggerServiceThread looper = new GPSLoggerServiceThread();
      looper.start();
      try
      {
         looper.ready.acquire();
      }
      catch (final InterruptedException e)
      {
         Log.e(TAG, "Interrupted during wait for the GPSLoggerServiceThread to start, prepare for trouble!", e);
      }

      /**
       * for the case of accelerometer values*
       */

      this.sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
      this.sensorManager.registerListener(this, this.sensorManager.getDefaultSensor

      (Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
      this.lastTime = System.currentTimeMillis();

      this.mHeartbeatTimer = new Timer("heartbeat", true);

      this.mWeakLocations = new Vector<Location>(3);
      this.mWeakSensorEvent = new Vector<SensorEvent>(3);
      this.mAltitudes = new LinkedList<Double>();
      this.mLoggingState = Constants.STOPPED;
      this.mStartNextSegment = false;
      this.mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
      this.mNoticationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      stopNotification();

      final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
      this.mSpeedSanityCheck = sharedPreferences.getBoolean(Constants.SPEEDSANITYCHECK, true);
      this.mStreamBroadcast = sharedPreferences.getBoolean(Constants.BROADCAST_STREAM, false);
      final boolean startImmidiatly = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.LOGATSTARTUP, false);

      crashRestoreState();
      if (startImmidiatly && this.mLoggingState == Constants.STOPPED)
      {
         startLogging();
         final ContentValues values = new ContentValues();
         values.put(TracksColumns.NAME, "Recorded at startup");
         getContentResolver().update(ContentUris.withAppendedId(Tracks.CONTENT_URI, this.mTrackId), values, null, null);
      }
      else
      {
         broadCastLoggingState();
      }
   }

   /**
    * This is the old onStart method that will be called on the pre-2.0
    * 
    * @see android.app.Service#onStart(android.content.Intent, int) platform. On 2.0 or later we override onStartCommand() so this method will not be called.
    */
   @Override
   public void onStart(final Intent intent, final int startId)
   {
      handleCommand(intent);
   }

   @Override
   public int onStartCommand(final Intent intent, final int flags, final int startId)
   {
      handleCommand(intent);
      // We want this service to continue running until it is explicitly
      // stopped, so return sticky.
      return START_STICKY;
   }

   private void handleCommand(final Intent intent)
   {
      if (DEBUG)
      {
         Log.d(TAG, "handleCommand(Intent " + intent + ")");
      }
      ;
      if (intent != null && intent.hasExtra(COMMAND))
      {
         switch (intent.getIntExtra(COMMAND, -1))
         {
            case EXTRA_COMMAND_START:
               startLogging();
               break;
            case EXTRA_COMMAND_PAUSE:
               pauseLogging();
               break;
            case EXTRA_COMMAND_RESUME:
               resumeLogging();
               break;
            case EXTRA_COMMAND_STOP:
               stopLogging();
               break;
            default:
               break;
         }
      }
   }

   /**
    * (non-Javadoc)
    * 
    * @see android.app.Service#onDestroy()
    */
   @Override
   public void onDestroy()
   {
      if (DEBUG)
      {
         Log.d(TAG, "onDestroy()");
      }
      ;
      super.onDestroy();

      this.sensorManager.unregisterListener(this);

      if (isLogging())
      {
         Log.w(TAG, "Destroyin an activly logging service");
      }
      this.mHeartbeatTimer.cancel();
      this.mHeartbeatTimer.purge();
      if (this.mWakeLock != null)
      {
         this.mWakeLock.release();
         this.mWakeLock = null;
      }
      PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this.mSharedPreferenceChangeListener);
      this.mLocationManager.removeGpsStatusListener(this.mStatusListener);
      stopListening();
      this.mNoticationManager.cancel(R.layout.map_widgets);

      final Message msg = Message.obtain();
      msg.what = STOPLOOPER;
      this.mHandler.sendMessage(msg);
   }

   private void crashProtectState()
   {
      final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
      final Editor editor = preferences.edit();
      editor.putLong(SERVICESTATE_TRACKID, this.mTrackId);
      editor.putLong(SERVICESTATE_LABELID, this.mLabelId);
      editor.putLong(SERVICESTATE_XYZID, this.mXyzId);
      //editor.putLong(SERVICESTATE_LOCATIONID, mLocationId);
      editor.putInt(SERVICESTATE_PRECISION, this.mPrecision);
      editor.putInt(SERVICESTATE_STATE, this.mLoggingState);
      editor.putFloat(SERVICESTATE_DISTANCE, this.mDistance);
      editor.commit();
      if (DEBUG)
      {
         Log.d(TAG, "crashProtectState()");
      }
      ;
   }

   private synchronized void crashRestoreState()
   {
      final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
      final long previousState = preferences.getInt(SERVICESTATE_STATE, Constants.STOPPED);
      if (previousState == Constants.LOGGING || previousState == Constants.PAUSED)
      {
         Log.w(TAG, "Recovering from a crash or kill and restoring state.");
         startNotification();

         //mTrackId = preferences.getLong(SERVICESTATE_TRACKID, -1);
         this.mLabelId = preferences.getLong(SERVICESTATE_LABELID, -1);
         this.mXyzId = preferences.getLong(SERVICESTATE_XYZID, -1);
         this.mLocationId = preferences.getLong(SERVICESTATE_LOCATIONID, -1);

         //mSegmentId = preferences.getLong(SERVICESTATE_SEGMENTID, -1);
         this.mPrecision = preferences.getInt(SERVICESTATE_PRECISION, -1);
         this.mDistance = preferences.getFloat(SERVICESTATE_DISTANCE, 0F);

         if (previousState == Constants.LOGGING)
         {
            this.mLoggingState = Constants.PAUSED;
            resumeLogging();
         }
         else if (previousState == Constants.PAUSED)
         {
            this.mLoggingState = Constants.LOGGING;
            pauseLogging();
         }
      }
   }

   /**
    * (non-Javadoc)
    * 
    * @see android.app.Service#onBind(android.content.Intent)
    */
   @Override
   public IBinder onBind(final Intent intent)
   {
      return this.mBinder;
   }

   /**
    * (non-Javadoc)
    * 
    * @see nl.sogeti.android.gpstracker.IGPSLoggerService#getLoggingState()
    */
   protected boolean isLogging()
   {
      return this.mLoggingState == Constants.LOGGING;
   }

   /**
    * Provides the cached last stored waypoint it current logging is active alse null.
    * 
    * @return last waypoint location or null
    */
   protected Location getLastWaypoint()
   {
      Location myLastWaypoint = null;
      if (isLogging())
      {
         myLastWaypoint = this.mPreviousLocation;
      }
      return myLastWaypoint;
   }

   public float getTrackedDistance()
   {
      float distance = 0F;
      if (isLogging())
      {
         distance = this.mDistance;
      }
      return distance;
   }

   protected boolean isMediaPrepared()
   {

      return !(this.mTrackId < 0 || this.mSegmentId < 0 || this.mWaypointId < 0);

   }

   /**
    * (non-Javadoc)
    * 
    * @see dev.ugasoft.android.gps.logger.IGPSLoggerService#startLogging() * some personal notes about synchronization: First, it is not possible for two invocations of synchronized methods on the
    *      same object to interleave. When one thread is executing a synchronized method for an object, all other threads that invoke synchronized methods for the same object block (suspend execution)
    *      until the first thread is done with the object. Second, when a synchronized method exits, it automatically establishes a happens-before relationship with any subsequent invocation of a
    *      synchronized method for the same object. This guarantees that changes to the state of the object are visible to all threads.
    */
   public synchronized void startLogging()
   {
      if (DEBUG)
      {
         Log.d(TAG, "startLogging()");
      }
      ;
      if (this.mLoggingState == Constants.STOPPED)
      {
         startNewTrack();
         //createNewLabel();
         sendRequestLocationUpdatesMessage();
         sendRequestStatusUpdateMessage();
         this.mLoggingState = Constants.LOGGING; //When logging takes place, update the logging state
         updateWakeLock();
         startNotification();
         crashProtectState();
         broadCastLoggingState();

         //resume the accelerometer sensor
         this.sensorManager.registerListener(this, this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
         this.lastTime = System.currentTimeMillis();

      }
   }

   public synchronized void pauseLogging()
   {
      if (DEBUG)
      {
         Log.d(TAG, "pauseLogging()");
      }
      ;
      if (this.mLoggingState == Constants.LOGGING)
      {
         this.mLocationManager.removeGpsStatusListener(this.mStatusListener);
         stopListening();
         this.mLoggingState = Constants.PAUSED;
         this.mPreviousLocation = null;
         updateWakeLock();
         updateNotification();
         this.mSatellites = 0;
         updateNotification();
         crashProtectState();
         broadCastLoggingState();
         //unregister the acccelerometer sensor
         this.sensorManager.unregisterListener(this);
      }
   }

   public synchronized void resumeLogging()
   {
      if (DEBUG)
      {
         Log.d(TAG, "resumeLogging()");
      }
      ;
      if (this.mLoggingState == Constants.PAUSED)
      {
         if (this.mPrecision != Constants.LOGGING_GLOBAL)
         {
            this.mStartNextSegment = true;
         }
         sendRequestLocationUpdatesMessage();
         sendRequestStatusUpdateMessage();

         //resume the accelerometer sensor
         this.sensorManager.registerListener(this, this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
         this.lastTime = System.currentTimeMillis();

         this.mLoggingState = Constants.LOGGING;
         updateWakeLock();
         updateNotification();
         crashProtectState();
         broadCastLoggingState();
      }
   }

   /**
    * (non-Javadoc)
    * 
    * @see nl.sogeti.android.gpstracker.IGPSLoggerService#stopLogging()
    */
   public synchronized void stopLogging()
   {
      if (DEBUG)
      {
         Log.d(TAG, "stopLogging()");
      }
      ;
      this.mLoggingState = Constants.STOPPED;
      crashProtectState();

      this.sensorManager.unregisterListener(this);

      updateWakeLock();

      PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this.mSharedPreferenceChangeListener);

      this.mLocationManager.removeGpsStatusListener(this.mStatusListener);
      stopListening();
      stopNotification();

      broadCastLoggingState();
   }

   //check period stuff...
   private void startListening(final String provider, final long intervaltime, final float distance)
   {
      this.mLocationManager.removeUpdates(this);
      this.mLocationManager.requestLocationUpdates(provider, intervaltime, distance, this);
      this.mCheckPeriod = Math.max(12 * intervaltime, 120 * 1000);

      this.sensorManager.registerListener(this, this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
      this.lastTime = System.currentTimeMillis();

      if (this.mHeartbeat != null)
      {
         this.mHeartbeat.cancel();
         this.mHeartbeat = null;
      }

      this.mHeartbeat = new Heartbeat(provider);
      this.mHeartbeatTimer.schedule(this.mHeartbeat, this.mCheckPeriod, this.mCheckPeriod);
   }

   private void stopListening()
   {
      if (this.mHeartbeat != null)
      {
         this.mHeartbeat.cancel();
         this.mHeartbeat = null;
      }

      //this is for the case of the accelerometer sensor
      this.sensorManager.unregisterListener(this);

      this.mLocationManager.removeUpdates(this);
   }

   /**
    * (non-Javadoc)
    * 
    * @see nl.sogeti.android.gpstracker.IGPSLoggerService#storeDerivedDataSource(java.lang.String)
    */
   public void storeDerivedDataSource(final String sourceName)
   {
      final Uri trackMetaDataUri = Uri.withAppendedPath(Tracks.CONTENT_URI, this.mTrackId + "/metadata");

      if (this.mTrackId >= 0)
      {
         if (this.mSources == null)
         {
            Cursor metaData = null;
            String source = null;
            try
            {
               metaData = getContentResolver().query(trackMetaDataUri, new String[] { MetaDataColumns.VALUE }, MetaDataColumns.KEY + " = ? ", new String[] { Constants.DATASOURCES_KEY }, null);
               if (metaData.moveToFirst())
               {
                  source = metaData.getString(0);
               }
            }
            finally
            {
               if (metaData != null)
               {
                  metaData.close();
               }
            }
            if (source != null)
            {
               this.mSources = source;
            }
            else
            {
               this.mSources = sourceName;
               final ContentValues args = new ContentValues();
               args.put(MetaDataColumns.KEY, Constants.DATASOURCES_KEY);
               args.put(MetaDataColumns.VALUE, this.mSources);
               getContentResolver().insert(trackMetaDataUri, args);
            }
         }

         if (!this.mSources.contains(sourceName))
         {
            this.mSources += "," + sourceName;
            final ContentValues args = new ContentValues();
            args.put(MetaDataColumns.VALUE, this.mSources);
            getContentResolver().update(trackMetaDataUri, args, MetaDataColumns.KEY + " = ? ", new String[] { Constants.DATASOURCES_KEY });
         }
      }
   }

   private void startNotification()
   {
      this.mNoticationManager.cancel(R.layout.map_widgets);

      final int icon = R.drawable.ic_maps_indicator_current_position;
      final CharSequence tickerText = getResources().getString(R.string.service_start);
      final long when = System.currentTimeMillis();

      this.mNotification = new Notification(icon, tickerText, when);
      this.mNotification.flags |= Notification.FLAG_ONGOING_EVENT;

      updateNotification();

      if (Build.VERSION.SDK_INT >= 5)
      {
         startForegroundReflected(R.layout.map_widgets, this.mNotification);
      }
      else
      {
         this.mNoticationManager.notify(R.layout.map_widgets, this.mNotification);
      }
   }

   /*
   @SuppressWarnings("deprecation")
   private void updateNotification()
   {
      final CharSequence contentTitle = getResources().getString(R.string.app_name);

      final String precision = getResources().getStringArray(R.array.precision_choices)[this.mPrecision];
      final String state = getResources().getStringArray(R.array.state_choices)[this.mLoggingState - 1];
      CharSequence contentText;

      switch (this.mPrecision)
      {
         case (Constants.LOGGING_GLOBAL):
            contentText = getResources().getString(R.string.service_networkstatus, state, precision);
            break;
         default:
            if (this.mStatusMonitor)
            {
               contentText = getResources().getString(R.string.service_gpsstatus, state, precision, this.mSatellites);
            }
            else
            {
               contentText = getResources().getString(R.string.service_gpsnostatus, state, precision);
            }
            break;
      }

      final Intent notificationIntent = new Intent(this, CommonLoggerMap.class);
      notificationIntent.setData(ContentUris.withAppendedId(Tracks.CONTENT_URI, this.mTrackId));
      this.mNotification.contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
      this.mNotification.setLatestEventInfo(this, contentTitle, contentText, this.mNotification.contentIntent);
      this.mNoticationManager.notify(R.layout.map_widgets, this.mNotification);
   }*/
   
   @SuppressWarnings("deprecation")
   private void updateNotification()
   {
      CharSequence contentTitle = getResources().getString(R.string.app_name);

      String precision = getResources().getStringArray(R.array.precision_choices)[mPrecision];
      String state = getResources().getStringArray(R.array.state_choices)[mLoggingState - 1];
      CharSequence contentText;
      
      switch (mPrecision)
      {
         case (Constants.LOGGING_GLOBAL):
            contentText = getResources().getString(R.string.service_networkstatus, state, precision);
            break;
         default:
            if (mStatusMonitor)
            {
               contentText = getResources().getString(R.string.service_gpsstatus, state, precision, mSatellites);
            }
            else
            {
               contentText = getResources().getString(R.string.service_gpsnostatus, state, precision);
            }
            break;
      }
      
      Intent notificationIntent = new Intent(this, CommonLoggerMap.class);
      notificationIntent.setData(ContentUris.withAppendedId(Tracks.CONTENT_URI, mTrackId));
      nBuilder = new Builder(this);
      PendingIntent pIntent = PendingIntent.getActivity(this, 0, notificationIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
      nBuilder.setContentTitle(contentTitle);
      nBuilder.setContentText(contentText);
      nBuilder.setContentIntent(pIntent);
      mNotification = nBuilder.build();
      mNoticationManager.notify(R.layout.map_widgets, mNotification);
   }

   private void stopNotification()
   {
      if (Build.VERSION.SDK_INT >= 5)
      {
         stopForegroundReflected(true);
      }
      else
      {
         this.mNoticationManager.cancel(R.layout.map_widgets);
      }
   }

   private void notifyOnEnabledProviderNotification(final int resId)
   {
      this.mNoticationManager.cancel(LOGGING_UNAVAILABLE);
      this.mShowingGpsDisabled = false;
      final CharSequence text = this.getString(resId);
      final Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
      toast.show();
   }
   /*
   private void notifyOnPoorSignal(final int resId)
   {
      final int icon = R.drawable.ic_maps_indicator_current_position;
      final CharSequence tickerText = getResources().getString(resId);
      final long when = System.currentTimeMillis();
      final Notification signalNotification = new Notification(icon, tickerText, when);
      final CharSequence contentTitle = getResources().getString(R.string.app_name);
      final Intent notificationIntent = new Intent(this, CommonLoggerMap.class);
      final PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
      signalNotification.setLatestEventInfo(this, contentTitle, tickerText, contentIntent);
      signalNotification.flags |= Notification.FLAG_AUTO_CANCEL;

      this.mNoticationManager.notify(resId, signalNotification);
   }*/
   
   private void notifyOnPoorSignal(int resId)
   {
      int icon = R.drawable.ic_maps_indicator_current_position;
      CharSequence tickerText = getResources().getString(resId);
      long when = System.currentTimeMillis();
      
      Intent notificationIntent = new Intent( this, CommonLoggerMap.class );
      PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, Intent.FLAG_ACTIVITY_NEW_TASK );
      
      Notification.Builder builder = new Builder(this);
      
      builder.setAutoCancel(true);
      builder.setTicker(tickerText);
      builder.setSmallIcon(icon);
      builder.setWhen(when);
      builder.setContentTitle(getResources().getString(R.string.app_name ));               
      builder.setContentText(getResources().getString(resId));
     
      builder.setContentIntent(contentIntent);

      Notification signalNotification = builder.build();
      mNoticationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
      mNoticationManager.notify(resId, signalNotification);
   }
   
   /*
   private void notifyOnDisabledProvider(final int resId)
   {
      final int icon = R.drawable.ic_maps_indicator_current_position;
      final CharSequence tickerText = getResources().getString(resId);
      final long when = System.currentTimeMillis();
      final Notification gpsNotification = new Notification(icon, tickerText, when);
      gpsNotification.flags |= Notification.FLAG_AUTO_CANCEL;

      final CharSequence contentTitle = getResources().getString(R.string.app_name);
      final CharSequence contentText = getResources().getString(resId);
      final Intent notificationIntent = new Intent(this, CommonLoggerMap.class);
      notificationIntent.setData(ContentUris.withAppendedId(Tracks.CONTENT_URI, this.mTrackId));
      final PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
      gpsNotification.setLatestEventInfo(this, contentTitle, contentText, contentIntent);

      this.mNoticationManager.notify(LOGGING_UNAVAILABLE, gpsNotification);
      this.mShowingGpsDisabled = true;
   }*/
   
   private void notifyOnDisabledProvider(int resId)
   {
      int icon = R.drawable.ic_maps_indicator_current_position;
      CharSequence tickerText = getResources().getString(resId);
      long when = System.currentTimeMillis();
      CharSequence contentTitle = getResources().getString(R.string.app_name);
      CharSequence contentText = getResources().getString(resId);
      Intent notificationIntent = new Intent(this, CommonLoggerMap.class);
      notificationIntent.setData(ContentUris.withAppendedId(Tracks.CONTENT_URI, mTrackId));
      PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
      
      Notification.Builder builder = new Builder(this);
      builder.setAutoCancel(true);
      builder.setTicker(tickerText);
      builder.setSmallIcon(icon);
      builder.setWhen(when);
      builder.setContentTitle(contentTitle);               
      builder.setContentText(contentText);
      builder.setContentIntent(contentIntent);
      
      Notification gpsNotification = builder.build();
      mNoticationManager.notify(LOGGING_UNAVAILABLE, gpsNotification);
      mShowingGpsDisabled = true;
   }
   
   /**
    * Send a system broadcast to notify a change in the logging or precision
    */
   private void broadCastLoggingState()
   {
      final Intent broadcast = new Intent(Constants.LOGGING_STATE_CHANGED_ACTION);
      broadcast.putExtra(Constants.EXTRA_LOGGING_PRECISION, this.mPrecision);
      broadcast.putExtra(Constants.EXTRA_LOGGING_STATE, this.mLoggingState);
      getApplicationContext().sendBroadcast(broadcast);
      if (isLogging())
      {
         StreamUtils.initStreams(this);
      }
      else
      {
         StreamUtils.shutdownStreams(this);
      }
   }

   private void sendRequestStatusUpdateMessage()
   {
      this.mStatusMonitor = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.STATUS_MONITOR, false);
      final Message msg = Message.obtain();
      msg.what = ADDGPSSTATUSLISTENER;
      this.mHandler.sendMessage(msg);
   }

   private void sendRequestLocationUpdatesMessage()
   {
      stopListening();
      this.mPrecision = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.PRECISION, "2")).intValue();
      final Message msg = Message.obtain();
      switch (this.mPrecision)
      {
         case (Constants.LOGGING_FINE): // Fine
            msg.what = REQUEST_FINEGPS_LOCATIONUPDATES;
            this.mHandler.sendMessage(msg);
            break;
         case (Constants.LOGGING_NORMAL): // Normal
            msg.what = REQUEST_NORMALGPS_LOCATIONUPDATES;
            this.mHandler.sendMessage(msg);
            break;
         case (Constants.LOGGING_COARSE): // Coarse
            msg.what = REQUEST_COARSEGPS_LOCATIONUPDATES;
            this.mHandler.sendMessage(msg);
            break;
         case (Constants.LOGGING_GLOBAL): // Global
            msg.what = REQUEST_GLOBALNETWORK_LOCATIONUPDATES;
            this.mHandler.sendMessage(msg);
            break;
         case (Constants.LOGGING_CUSTOM): // Global
            msg.what = REQUEST_CUSTOMGPS_LOCATIONUPDATES;
            this.mHandler.sendMessage(msg);
            break;
         default:
            Log.e(TAG, "Unknown precision " + this.mPrecision);
            break;
      }
   }

   /**
    * Message handler method to do the work off-loaded by mHandler to GPSLoggerServiceThread
    * 
    * @param msg
    */
   private void _handleMessage(final Message msg)
   {
      if (DEBUG)
      {
         Log.d(TAG, "_handleMessage( Message " + msg + " )");
      }
      ;
      long intervaltime = 0;
      float distance = 0;
      switch (msg.what)
      {
         case ADDGPSSTATUSLISTENER:
            this.mLocationManager.addGpsStatusListener(this.mStatusListener);
            break;
         case REQUEST_FINEGPS_LOCATIONUPDATES:
            this.mMaxAcceptableAccuracy = FINE_ACCURACY;
            intervaltime = FINE_INTERVAL;
            distance = FINE_DISTANCE;
            startListening(LocationManager.GPS_PROVIDER, intervaltime, distance);
            break;
         case REQUEST_NORMALGPS_LOCATIONUPDATES:
            this.mMaxAcceptableAccuracy = NORMAL_ACCURACY;
            intervaltime = NORMAL_INTERVAL;
            distance = NORMAL_DISTANCE;
            startListening(LocationManager.GPS_PROVIDER, intervaltime, distance);
            break;
         case REQUEST_COARSEGPS_LOCATIONUPDATES:
            this.mMaxAcceptableAccuracy = COARSE_ACCURACY;
            intervaltime = COARSE_INTERVAL;
            distance = COARSE_DISTANCE;
            startListening(LocationManager.GPS_PROVIDER, intervaltime, distance);
            break;
         case REQUEST_GLOBALNETWORK_LOCATIONUPDATES:
            this.mMaxAcceptableAccuracy = GLOBAL_ACCURACY;
            intervaltime = GLOBAL_INTERVAL;
            distance = GLOBAL_DISTANCE;
            startListening(LocationManager.NETWORK_PROVIDER, intervaltime, distance);
            if (!isNetworkConnected())
            {
               notifyOnDisabledProvider(R.string.service_connectiondisabled);
            }

            break;

         case REQUEST_CUSTOMGPS_LOCATIONUPDATES:
            intervaltime = 60 * 1000 * Long.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.LOGGING_INTERVAL, "15000"));
            distance = Float.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.LOGGING_DISTANCE, "10"));
            this.mMaxAcceptableAccuracy = Math.max(10f, Math.min(distance, 50f));
            startListening(LocationManager.GPS_PROVIDER, intervaltime, distance);
            break;
         case STOPLOOPER:
            this.mLocationManager.removeGpsStatusListener(this.mStatusListener);
            stopListening();
            Looper.myLooper().quit();
            break;
         case GPSPROBLEM:
            notifyOnPoorSignal(R.string.service_gpsproblem);
            break;
      }
   }

   private void updateWakeLock()
   {
      if (this.mLoggingState == Constants.LOGGING)
      {
         PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this.mSharedPreferenceChangeListener);

         final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
         if (this.mWakeLock != null)
         {
            this.mWakeLock.release();
            this.mWakeLock = null;
         }
         this.mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
         this.mWakeLock.acquire();
      }
      else
      {
         if (this.mWakeLock != null)
         {
            this.mWakeLock.release();
            this.mWakeLock = null;
         }
      }
   }

   /**
    * Some GPS waypoints received are of to low a quality for tracking use. Here we filter those out.
    * 
    * @param proposedLocation
    * @return either the (cleaned) original or null when unacceptable
    */
   public Location locationFilter(Location proposedLocation)
   {
      // Do no include log wrong 0.0 lat 0.0 long, skip to next value in while-loop
      if (proposedLocation != null && (proposedLocation.getLatitude() == 0.0d || proposedLocation.getLongitude() == 0.0d))
      {
         Log.w(TAG, "A wrong location was received, 0.0 latitude and 0.0 longitude... ");
         proposedLocation = null;
      }

      // Do not log a waypoint which is more inaccurate then is configured to be acceptable
      if (proposedLocation != null && proposedLocation.getAccuracy() > this.mMaxAcceptableAccuracy)
      {
         Log.w(TAG, String.format("A weak location was received, lots of inaccuracy... (%f is more then max %f)", proposedLocation.getAccuracy(), this.mMaxAcceptableAccuracy));
         proposedLocation = addBadLocation(proposedLocation);
      }

      // Do not log a waypoint which might be on any side of the previous waypoint
      if (proposedLocation != null && this.mPreviousLocation != null && proposedLocation.getAccuracy() > this.mPreviousLocation.distanceTo(proposedLocation))
      {
         Log.w(TAG,
               String.format("A weak location was received, not quite clear from the previous waypoint... (%f more then max %f)", proposedLocation.getAccuracy(),
                     this.mPreviousLocation.distanceTo(proposedLocation)));
         proposedLocation = addBadLocation(proposedLocation);
      }

      // Speed checks, check if the proposed location could be reached from the previous one in sane speed
      // Common to jump on network logging and sometimes jumps on Samsung Galaxy S type of devices
      if (this.mSpeedSanityCheck && proposedLocation != null && this.mPreviousLocation != null)
      {
         // To avoid near instant teleportation on network location or glitches cause continent hopping
         final float meters = proposedLocation.distanceTo(this.mPreviousLocation);
         final long seconds = (proposedLocation.getTime() - this.mPreviousLocation.getTime()) / 1000L;
         final float speed = meters / seconds;
         if (speed > MAX_REASONABLE_SPEED)
         {
            Log.w(TAG, "A strange location was received, a really high speed of " + speed + " m/s, prob wrong...");
            proposedLocation = addBadLocation(proposedLocation);
            // Might be a messed up Samsung Galaxy S GPS, reset the logging
            if (speed > 2 * MAX_REASONABLE_SPEED && this.mPrecision != Constants.LOGGING_GLOBAL)
            {
               Log.w(TAG, "A strange location was received on GPS, reset the GPS listeners");
               stopListening();
               this.mLocationManager.removeGpsStatusListener(this.mStatusListener);
               this.mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
               sendRequestStatusUpdateMessage();
               sendRequestLocationUpdatesMessage();
            }
         }
      }

      // Remove speed if not sane
      if (this.mSpeedSanityCheck && proposedLocation != null && proposedLocation.getSpeed() > MAX_REASONABLE_SPEED)
      {
         Log.w(TAG, "A strange speed, a really high speed, prob wrong...");
         proposedLocation.removeSpeed();
      }

      // Remove altitude if not sane
      if (this.mSpeedSanityCheck && proposedLocation != null && proposedLocation.hasAltitude())
      {
         if (!addSaneAltitude(proposedLocation.getAltitude()))
         {
            Log.w(TAG, "A strange altitude, a really big difference, prob wrong...");
            proposedLocation.removeAltitude();
         }
      }
      // Older bad locations will not be needed
      if (proposedLocation != null)
      {
         this.mWeakLocations.clear();
      }
      return proposedLocation;
   }

   // a filter for the proposed accelerometer value events...

   public SensorEvent accelerometerValueFilter(final SensorEvent proposedEvent)
   {
      final float[] value = proposedEvent.values;

      final float xVal = value[0];
      final float yVal = value[1];
      final float zVal = value[2];

      final float accelationSquareRoot = (xVal * xVal + yVal * yVal + zVal * zVal) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);

      final long actualTime = System.currentTimeMillis();

      /*
       * if (proposedEvent != null && ( accelationSquareRoot < 1.2) ) { Log.w(TAG, "wrong sensor values were received, the acceleration square root" + "was less than 1.2... "); proposedEvent = null; }
       */

      /*
       * if (proposedEvent != null && (actualTime - lastTime) < 2*Math.pow(10,9)) { Log.w(TAG, "wrong sensor values were received, the difference between the actual and last time is" +
       * "less than 2*10 pow 9 ms... "); proposedEvent = null; }
       */
      // Older bad locations will not be needed
      if (proposedEvent != null)
      {
         this.mWeakSensorEvent.clear();
      }
      return proposedEvent;
   }

   /**
    * Store a bad location, when to many bad locations are stored the the storage is cleared and the least bad one is returned
    * 
    * @param location bad location
    * @return null when the bad location is stored or the least bad one if the storage was full
    */

   private Location addBadLocation(Location location)
   {
      this.mWeakLocations.add(location);
      if (this.mWeakLocations.size() < 3)
      {
         location = null;
      }
      else
      {
         Location best = this.mWeakLocations.lastElement();
         for (final Location whimp : this.mWeakLocations)
         {
            if (whimp.hasAccuracy() && best.hasAccuracy() && whimp.getAccuracy() < best.getAccuracy())
            {
               best = whimp;
            }
            else
            {
               if (whimp.hasAccuracy() && !best.hasAccuracy())
               {
                  best = whimp;
               }
            }
         }
         synchronized (this.mWeakLocations)
         {
            this.mWeakLocations.clear();
         }
         location = best;
      }
      return location;
   }

   /**
    * Builds a bit of knowledge about altitudes to expect and return if the added value is deemed sane.
    * 
    * @param altitude
    * @return whether the altitude is considered sane
    */
   private boolean addSaneAltitude(final double altitude)
   {
      boolean sane = true;
      double avg = 0;
      int elements = 0;
      // Even insane altitude shifts increases alter perception
      this.mAltitudes.add(altitude);
      if (this.mAltitudes.size() > 3)

      {
         this.mAltitudes.poll();
      }

      for (final Double alt : this.mAltitudes)
      {
         avg += alt;
         elements++;
      }

      avg = avg / elements;
      sane = Math.abs(altitude - avg) < MAX_REASONABLE_ALTITUDECHANGE;

      return sane;
   }

   /**
    * Trigged by events that start a new track
    */

   private void startNewTrack()
   {
      this.mDistance = 0;
      final Uri newTrack = getContentResolver().insert(Tracks.CONTENT_URI, new ContentValues(0));
      this.mTrackId = Long.valueOf(newTrack.getLastPathSegment()).longValue();
      startNewSegment();
   }

   /*
    * private void createNewLabel() { // mDistance = 0; Uri newLabel = this.getContentResolver().insert(Labels.CONTENT_URI, new ContentValues(0)); mLabelId =
    * Long.valueOf(newLabel.getLastPathSegment()).longValue(); // startNewSegment(); }
    */

   /**
    * Trigged by events that start a new segment
    */
   private void startNewSegment()
   {
      this.mPreviousLocation = null;
      final Uri newSegment = getContentResolver().insert(Uri.withAppendedPath(Tracks.CONTENT_URI, this.mTrackId + "/segments"), new ContentValues(0));
      this.mSegmentId = Long.valueOf(newSegment.getLastPathSegment()).longValue();
      crashProtectState();
   }

   protected void storeMediaUri(final Uri mediaUri)
   {
      if (isMediaPrepared())
      {
         final Uri mediaInsertUri = Uri.withAppendedPath(Tracks.CONTENT_URI, this.mTrackId + "/segments/" + this.mSegmentId + "/waypoints/" + this.mWaypointId + "/media");
         final ContentValues args = new ContentValues();
         args.put(MediaColumns.URI, mediaUri.toString());
         getContentResolver().insert(mediaInsertUri, args);
      }
      else
      {
         Log.e(TAG, "No logging done under which to store the track");
      }
   }

   /**
    * Use the ContentResolver mechanism to store a received location
    * 
    * @param location
    */

   public void storeLocation(final Location location)
   {
      if (!isLogging())
      {
         Log.e(TAG, String.format("Not logging but storing location %s, prepare to fail", location.toString()));
      }
      final ContentValues args = new ContentValues();

      args.put(WaypointsColumns.LATITUDE, Double.valueOf(location.getLatitude()));
      args.put(WaypointsColumns.LONGITUDE, Double.valueOf(location.getLongitude()));
      args.put(WaypointsColumns.SPEED, Float.valueOf(location.getSpeed()));
      args.put(WaypointsColumns.TIME, Long.valueOf(System.currentTimeMillis()));
      if (location.hasAccuracy())
      {
         args.put(WaypointsColumns.ACCURACY, Float.valueOf(location.getAccuracy()));
      }
      if (location.hasAltitude())
      {
         args.put(WaypointsColumns.ALTITUDE, Double.valueOf(location.getAltitude()));

      }
      if (location.hasBearing())
      {
         args.put(WaypointsColumns.BEARING, Float.valueOf(location.getBearing()));
      }

      final Uri waypointInsertUri = Uri.withAppendedPath(Tracks.CONTENT_URI, this.mTrackId + "/segments/" + this.mSegmentId + "/waypoints");
      final Uri inserted = getContentResolver().insert(waypointInsertUri, args);
      this.mWaypointId = Long.parseLong(inserted.getLastPathSegment());
   }

   /*
    * public void storeLocationB (Location location) { if (!isLogging()) { Log.e(TAG, String.format("Not logging but storing location %s, prepare to fail", location.toString())); } ContentValues args
    * = new ContentValues(); args.put(Locations.LATITUDE, Double.valueOf(location.getLatitude())); args.put(Locations.LONGITUDE, Double.valueOf(location.getLongitude())); args.put(Locations.SPEED,
    * Float.valueOf(location.getSpeed())); args.put(Locations.TIME, Long.valueOf(System.currentTimeMillis())); if (location.hasAccuracy()) { args.put(Locations.ACCURACY,
    * Float.valueOf(location.getAccuracy())); } if (location.hasAltitude()) { args.put(Locations.ALTITUDE, Double.valueOf(location.getAltitude())); } if (location.hasBearing()) {
    * args.put(Locations.BEARING, Float.valueOf(location.getBearing())); } Uri locationsInsertUri = Uri.withAppendedPath(Labels.CONTENT_URI, mLabelId + "/xyz/" + mXyzId + "/locations"); Uri inserted =
    * this.getContentResolver().insert(locationsInsertUri, args); mLocationId = Long.parseLong(inserted.getLastPathSegment()); }
    */

   /**
    * Consult broadcast options and execute broadcast if necessary
    * 
    * @param location
    */
   public void broadcastLocation(final Location location)
   {
      final Intent intent = new Intent(Constants.STREAMBROADCAST);

      if (this.mStreamBroadcast)
      {
         final long minDistance = (long) PreferenceManager.getDefaultSharedPreferences(this).getFloat("streambroadcast_distance_meter", 5000F);

         final long minTime = 60000 * Long.parseLong(PreferenceManager.getDefaultSharedPreferences(this).getString("streambroadcast_time", "1"));

         final long nowTime = location.getTime();

         if (this.mPreviousLocation != null)
         {
            this.mBroadcastDistance += location.distanceTo(this.mPreviousLocation);
         }

         if (this.mLastTimeBroadcast == 0)
         {
            this.mLastTimeBroadcast = nowTime;
         }

         final long passedTime = (nowTime - this.mLastTimeBroadcast);
         intent.putExtra(Constants.EXTRA_DISTANCE, (int) this.mBroadcastDistance);
         intent.putExtra(Constants.EXTRA_TIME, (int) passedTime / 60000);
         intent.putExtra(Constants.EXTRA_LOCATION, location);
         intent.putExtra(Constants.EXTRA_TRACK, ContentUris.withAppendedId(Tracks.CONTENT_URI, this.mTrackId));

         final boolean distanceBroadcast = minDistance > 0 && this.mBroadcastDistance >= minDistance;
         final boolean timeBroadcast = minTime > 0 && passedTime >= minTime;
         if (distanceBroadcast || timeBroadcast)
         {
            if (distanceBroadcast)
            {
               this.mBroadcastDistance = 0;
            }
            if (timeBroadcast)
            {
               this.mLastTimeBroadcast = nowTime;
            }
            this.sendBroadcast(intent, "android.permission.ACCESS_FINE_LOCATION");
         }
      }
   }

   private boolean isNetworkConnected()
   {
      final ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
      final NetworkInfo info = connMgr.getActiveNetworkInfo();

      return (info != null && info.isConnected());
   }

   private void soundGpsSignalAlarm()
   {
      Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
      if (alert == null)
      {
         alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
         if (alert == null)
         {
            alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
         }
      }
      final MediaPlayer mMediaPlayer = new MediaPlayer();
      try
      {
         mMediaPlayer.setDataSource(GPSLoggerService.this, alert);
         final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
         if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0)
         {
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mMediaPlayer.setLooping(false);
            mMediaPlayer.prepare();
            mMediaPlayer.start();
         }
      }
      catch (final IllegalArgumentException e)
      {
         Log.e(TAG, "Problem setting data source for mediaplayer", e);
      }
      catch (final SecurityException e)
      {
         Log.e(TAG, "Problem setting data source for mediaplayer", e);
      }
      catch (final IllegalStateException e)
      {
         Log.e(TAG, "Problem with mediaplayer", e);
      }
      catch (final IOException e)
      {
         Log.e(TAG, "Problem with mediaplayer", e);
      }
      final Message msg = Message.obtain();
      msg.what = GPSPROBLEM;
      this.mHandler.sendMessage(msg);
   }

   @SuppressWarnings("rawtypes")
   private void startForegroundReflected(final int id, final Notification notification)
   {

      Method mStartForeground;
      final Class[] mStartForegroundSignature = new Class[] { int.class, Notification.class };

      final Object[] mStartForegroundArgs = new Object[2];
      mStartForegroundArgs[0] = Integer.valueOf(id);
      mStartForegroundArgs[1] = notification;
      try
      {
         mStartForeground = getClass().getMethod("startForeground", mStartForegroundSignature);
         mStartForeground.invoke(this, mStartForegroundArgs);
      }
      catch (final NoSuchMethodException e)
      {
         Log.e(TAG, "Failed starting foreground notification using reflection", e);
      }
      catch (final IllegalArgumentException e)
      {
         Log.e(TAG, "Failed starting foreground notification using reflection", e);
      }
      catch (final IllegalAccessException e)
      {
         Log.e(TAG, "Failed starting foreground notification using reflection", e);
      }
      catch (final InvocationTargetException e)
      {
         Log.e(TAG, "Failed starting foreground notification using reflection", e);
      }

   }

   @SuppressWarnings("rawtypes")
   private void stopForegroundReflected(final boolean b)
   {
      final Class[] mStopForegroundSignature = new Class[] { boolean.class };

      Method mStopForeground;
      final Object[] mStopForegroundArgs = new Object[1];
      mStopForegroundArgs[0] = Boolean.TRUE;
      try
      {
         mStopForeground = getClass().getMethod("stopForeground", mStopForegroundSignature);
         mStopForeground.invoke(this, mStopForegroundArgs);
      }
      catch (final NoSuchMethodException e)
      {
         Log.e(TAG, "Failed stopping foreground notification using reflection", e);
      }
      catch (final IllegalArgumentException e)
      {
         Log.e(TAG, "Failed stopping foreground notification using reflection", e);
      }
      catch (final IllegalAccessException e)
      {
         Log.e(TAG, "Failed stopping foreground notification using reflection", e);
      }
      catch (final InvocationTargetException e)
      {
         Log.e(TAG, "Failed stopping foreground notification using reflection", e);
      }
   }

   /******
    * accelerometer related methods or SensorEvent Methods
    ********************/

   @Override
   public void onSensorChanged(final SensorEvent event)
   {
      // TODO Auto-generated method stub

      try
      {

         if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
         {
            /*
             * SensorEvent filteredEvent = accelerometerValueFilter(event); storeAccelerometerValues(filteredEvent);
             */

            setmLastRecordedEvent(event);
            /*
             * float x = event.values[0]; float y = event.values[1]; float z = event.values[2]; Log.d("x:", "" + x ); Log.d("y:", "" + y ); Log.d("z:", "" + z );
             */

         }
      }

      catch (final Exception e)
      {
         Log.d(TAG, "exception noticed inside onSensorChanged");
         Log.d(TAG, "" + e);
         /*
          * float x = event.values[0]; float y = event.values[1]; float z = event.values[2]; Log.d("x:", "" + x ); Log.d("y:", "" + y ); Log.d("z:", "" + z );
          */
      }
   }

   //getting the accelerometer readings...
   /*
    * private void storeAccelerometerValues(SensorEvent event) { float[] value = event.values; float xVal = value[0]; float yVal = value[1]; float zVal = value[2]; float accelationSquareRoot =
    * (xVal*xVal + yVal*yVal + zVal*zVal) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH); long actualTime = System.currentTimeMillis(); float xValue; float yValue; float zValue; //long
    * timeNow = System.nanoTime(); //I have changed the value from 1.2...initiall was at 2. //if(accelationSquareRoot > SensorManager.GRAVITY_EARTH) try { if(accelationSquareRoot >= 1.2) {
    * if(actualTime-lastTime < 2000000000) { zValue = zVal; xValue = xVal; yValue = yVal; ContentValues accValues = new ContentValues(); accValues.put( Xyz.X, xValue); accValues.put( Xyz.Y, yValue);
    * accValues.put( Xyz.Z, zValue); accValues.put(Xyz.TIME, Long.valueOf(System.currentTimeMillis())); getContentResolver().insert( mXyzUri, accValues ); Uri accValueInsertUri =
    * Uri.withAppendedPath(Labels.CONTENT_URI, mLabelId + "/locations/" + mLocationId + "/xyz"); Uri inserted = this.getContentResolver().insert(accValueInsertUri, accValues); mXyzId =
    * Long.parseLong(inserted.getLastPathSegment()); } lastTime = actualTime; } } catch (Exception e) { Log.e(TAG, "general exception", e); } }
    */

   private void storeAccelerometerValues(final SensorEvent event)
   {

      final float[] value = event.values;

      final float xVal = value[0];
      final float yVal = value[1];
      final float zVal = value[2];

      float xValue;
      float yValue;
      float zValue;

      //long timeNow = System.nanoTime();      
      //I have changed the value from 1.2...initiall was at 2.
      //if(accelationSquareRoot > SensorManager.GRAVITY_EARTH)

      zValue = zVal;
      xValue = xVal;
      yValue = yVal;

      final ContentValues accValues = new ContentValues();
      accValues.put(XYZColumns.X, xValue);
      accValues.put(XYZColumns.Y, yValue);
      accValues.put(XYZColumns.Z, zValue);
      accValues.put(XYZColumns.TIME, Long.valueOf(System.currentTimeMillis()));
      getContentResolver().insert(this.mXyzUri, accValues);

      final Uri accValueInsertUri = Uri.withAppendedPath(Labels.CONTENT_URI, this.mLabelId + "/locations/" + this.mLocationId + "/xyz");
      final Uri inserted = getContentResolver().insert(accValueInsertUri, accValues);
      this.mXyzId = Long.parseLong(inserted.getLastPathSegment());

   }

   @Override
   public void onAccuracyChanged(final Sensor sensor, final int accuracy)
   {
      // TODO Auto-generated method stub

   }

   public SensorEvent getmLastRecordedEvent()
   {
      return this.mLastRecordedEvent;
   }

   public void setmLastRecordedEvent(final SensorEvent mLastRecordedEvent)
   {
      this.mLastRecordedEvent = mLastRecordedEvent;
   }

   public Location getmLastRecordedLocation()
   {
      return this.mLastRecordedLocation;
   }

   public void setmLastRecordedLocation(final Location mLastRecordedLocation)
   {
      this.mLastRecordedLocation = mLastRecordedLocation;
   }
}
