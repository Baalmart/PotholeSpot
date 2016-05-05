package com.prim;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.ActionMode.Callback;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.google.android.maps.GeoPoint;
import com.prim.custom.CustomActivity;
import com.prim.model.Data;
import com.prim.ui.LeftNavAdapter;
import com.prim.ui.MainFragment;
import com.prim.ui.Profile;
import com.prim.ui.Settings;

import dev.baalmart.gps.R;
import dev.ugasoft.android.gps.actions.ControlTracking;
import dev.ugasoft.android.gps.actions.NameTrack;
import dev.ugasoft.android.gps.actions.PotholeSpotLabel;
import dev.ugasoft.android.gps.actions.ShareTrack;
import dev.ugasoft.android.gps.db.AndroidDatabaseManager;
import dev.ugasoft.android.gps.db.DatabaseHelper;
import dev.ugasoft.android.gps.db.PrimProvider;
import dev.ugasoft.android.gps.db.Prim.Labels;
import dev.ugasoft.android.gps.db.Prim.Tracks;
import dev.ugasoft.android.gps.db.Prim.Xyz;
import dev.ugasoft.android.gps.logger.GPSLoggerService;
import dev.ugasoft.android.gps.logger.GPSLoggerServiceManager;
/*import dev.ugasoft.android.gps.logger.IGPSLoggerServiceRemote;*/
import dev.ugasoft.android.gps.util.Constants;
import dev.ugasoft.android.gps.viewer.map.CommonLoggerMap;
import dev.ugasoft.android.gps.viewer.map.GoogleLoggerMap;
import dev.ugasoft.android.gps.viewer.map.LoggerLabelHelper;
import dev.ugasoft.android.gps.viewer.map.LoggerMap;
import dev.ugasoft.android.gps.viewer.map.LoggerMapHelper;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.GpsStatus.Listener;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

public class MainActivity extends CustomActivity implements SensorEventListener, LocationListener
{	
	   LoggerMapHelper mHelper;	 
	   ShareTrack shareLabels;	   
	   MainFragment mFragment;
	   private SensorManager mSensorManager;
	   String TAG_IT = "Main Activity";
	   
	   long startTime = 0;	
	   PotholeSpotLabel potholeSpot;
	    //runs without a timer by reposting this handler at the end of the runnable
	  /*  Handler timerHandler = new Handler();	    
	    Runnable timerRunnable = new Runnable() 
	    
	    {
	        @Override
	        public void run() 
	        {
	            long millis = System.currentTimeMillis() - startTime;
	            int seconds = (int) (millis / 1000);
	            int minutes = seconds / 60;
	            seconds = seconds % 60;
	            storeAllAccelerationValues(mLastRecordedEvent);
	            timerHandler.postDelayed(this, 500);
	        }
	    };*/
	
      //MainFragment.Result lastRecorderdResult;
      private SensorEvent mLastRecordedEvent;
      private Location mLastRecordedLocation;  
      // private float accelationSquareRoot;
      Context mContext;
      DatabaseHelper mDbHelper;      
      PrimProvider pProvider;
      Sensor mAccelerometer;
      SQLiteDatabase db;
      
      private Vector<SensorEvent> mWeakSensorEvent;
   
      private static final float FINE_DISTANCE = 5F;
      private static final long  FINE_INTERVAL = 1000l;
      private static final float FINE_ACCURACY = 20f;
      
      private static final float NORMAL_DISTANCE = 10F;
      private static final long  NORMAL_INTERVAL = 15000l;
      private static final float NORMAL_ACCURACY = 30f;

      private static final float COARSE_DISTANCE = 25F;
      private static final long  COARSE_INTERVAL = 30000l;
      private static final float COARSE_ACCURACY = 75f;
      
      private static final float GLOBAL_DISTANCE = 500F;
      private static final long  GLOBAL_INTERVAL = 300000l;
      private static final float GLOBAL_ACCURACY = 1000f;

      /**
       * <code>MAX_REASONABLE_SPEED</code> is about 324 kilometer per hour or 201
       * mile per hour.
       */
      private static final int MAX_REASONABLE_SPEED = 90;

      /**
       * <code>MAX_REASONABLE_ALTITUDECHANGE</code> between the last few waypoints
       * and a new one the difference should be less then 200 meter.
       */
      private static final int MAX_REASONABLE_ALTITUDECHANGE = 200;

      private static final boolean VERBOSE = false;


      private static final String SERVICESTATE_DISTANCE = "SERVICESTATE_DISTANCE";
      private static final String SERVICESTATE_STATE = "SERVICESTATE_STATE";
      private static final String SERVICESTATE_PRECISION = "SERVICESTATE_PRECISION";
      private static final String SERVICESTATE_SEGMENTID = "SERVICESTATE_SEGMENTID";
      private static final String SERVICESTATE_TRACKID = "SERVICESTATE_TRACKID";

      private static final int ADDGPSSTATUSLISTENER = 0;
      private static final int REQUEST_FINEGPS_LOCATIONUPDATES = 1;
      private static final int REQUEST_NORMALGPS_LOCATIONUPDATES = 2;
      private static final int REQUEST_COARSEGPS_LOCATIONUPDATES = 3;
      private static final int REQUEST_GLOBALNETWORK_LOCATIONUPDATES = 4;
      private static final int REQUEST_CUSTOMGPS_LOCATIONUPDATES = 5;
      private static final int STOPLOOPER = 6;
      private static final int GPSPROBLEM = 7;

      /**
       * DUP from android.app.Service.START_STICKY
       */
      private static final int START_STICKY = 1;

      public static final String COMMAND = "com.prim.extra.COMMAND";
      public static final int EXTRA_COMMAND_START = 0;
      public static final int EXTRA_COMMAND_PAUSE = 1;
      public static final int EXTRA_COMMAND_RESUME = 2;
      public static final int EXTRA_COMMAND_STOP = 3;
      
      /**
       * If broadcasts of location about should be sent to stream location
       */
      
      private boolean mStreamBroadcast;
     
      private long mLabel = -1;
      private long mSegmentId = -1;
      private long mWaypointId = -1;
      private long mLocationId = -1;
      private long mCheckPeriod;

      private float mBroadcastDistance;

      private long mLastTimeBroadcast;

      private String mSources;

      
      private SensorEvent mSensorEvent;
      private float mDistance;

      private Vector<Location> mWeakLocations;
      private Queue<Double> mAltitudes;

//declarations 
private Sensor sensor;
protected static final String TAG = null;
private ArrayList<Data> iList;
private static final Boolean DEBUG = false;
private int mPrecision;
private boolean mShowingGpsDisabled;
private boolean mStartNextSegment;
private float mMaxAcceptableAccuracy = 20;


private LocationManager mLocationManager;
private PowerManager.WakeLock mWakeLock;
private Handler mHandler;

/**
 * If speeds should be checked to sane values
 */
private boolean mSpeedSanityCheck;

/**
 * Time thread to runs tasks that check whether the GPS listener has received
 * enough to consider the GPS system alive.
 */

private Timer mHeartbeatTimer;
private NotificationManager mNoticationManager;
private static final int LOGGING_UNAVAILABLE = R.string.service_connectiondisabled;
private GPSLoggerServiceManager mLoggerServiceManager;
/*private IGPSLoggerServiceRemote mGPSLoggerRemote;*/
private GPSLoggerService mLoggerService;
private NameTrack nameRoute;
//GetAccelerometerValues getAccelerometerValues;
//private static final Boolean GPSenabled = false; 
private int mLoggingState = Constants.STOPPED;

//String labelName = "label";
Context context = null;
//Long labelTime = null;
private long mLabelId = -1;
private Notification mNotification;
private int mSatellites = 0;
/**
 * Should the GPS Status monitor update the notification bar
 */
private boolean mStatusMonitor;

//private SensorManager sensorManager;
public long lastTime;
public long currentTime;

Uri mLabelUri;
private Activity mActivity;

  private DrawerLayout drawerLayout;
  private ListView drawerLeft;
  @SuppressWarnings("deprecation")
  private ActionBarDrawerToggle drawerToggle;
 
  //private FixedMyLocationOverlay mMylocation;

  private void setupContainer(int pos)
  {
	    String str = getString(R.string.app_name);	   
	    Fragment fragment = null;
	    Activity activity = null;
	    boolean handled = false;
	  
	  //******************Share********************************
   /* if (pos == 4)
    {
    }    
   */
    if (pos == 0)
    {
    	fragment = new Profile();
      str = "Martin Bbaale";
    }
    
    while (fragment == null)
    {
      if (pos == 1)
      {
    	  fragment = new MainFragment();
      }
      //*********************favorite***********************
    /*  else if (pos == 2)
      {
        startActivity(new Intent(this, IssueList.class).putExtra("title", "Favorites"));
        fragment = null;
      } */     
      
      else
      {
        //**********************settings*****************
        if (pos == 3)
        {
        	fragment = new Settings();
          str = "Settings";
        }
      }
    }    
    //setting the details of each item on the nav bar....
    getActionBar().setTitle(str);
    getSupportFragmentManager().beginTransaction().
    replace(R.id.content_frame, (Fragment)fragment).commit();
  }

  private void setupDrawer()
  {
    drawerLayout = ((DrawerLayout)findViewById(R.id.drawer_layout));
    //setting the drawer shadow...
    drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START); //  //8388611
    drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, 
    		R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close)
    {
      public void onDrawerClosed(View paramAnonymousView)
      {
      }

      public void onDrawerOpened(View paramAnonymousView)
      {
      }
      
    };
    
    drawerLayout.setDrawerListener(drawerToggle);
    drawerLayout.closeDrawers();
    setupLeftNavDrawer();
  }

  @SuppressLint({"InflateParams"})
  private void setupLeftNavDrawer()
  {
    drawerLeft = ((ListView)findViewById(R.id.left_drawer));
    View localView = getLayoutInflater().inflate(R.layout.left_nav_header, null);
    drawerLeft.addHeaderView(localView);
    ArrayList<Data> localArrayList = new ArrayList<Data>();
    
    //Find
    localArrayList.add(new Data(new String[] {"Find" }, new int[] 
    		{ R.drawable.ic_nav1, R.drawable.ic_nav1_sel }));
    
    //Favorite
    localArrayList.add(new Data(new String[] { "Favorite" }, new int[] 
    		{ R.drawable.ic_nav2, R.drawable.ic_nav2_sel }));
    
    //Settings
    localArrayList.add(new Data(new String[] { "Settings" }, new int[] 
    		{ R.drawable.ic_nav3, R.drawable.ic_nav3_sel }));
    
    //Share
   /* localArrayList.add(new Data(new String[] { "Share" }, new int[] 
    		{ R.drawable.ic_nav4, R.drawable.ic_action_share }));*/
    
    final LeftNavAdapter localLeftNavAdapter = new LeftNavAdapter(this, localArrayList);
    drawerLeft.setAdapter(localLeftNavAdapter);    
    drawerLeft.setOnItemClickListener(new AdapterView.OnItemClickListener()
    
    {
      @Override
      public void onItemClick(AdapterView<?> paramAnonymousAdapterView, View paramAnonymousView, 
    		  int paramAnonymousInt, long paramAnonymousLong)
      {
    	  	 
    	try
    	   { 
    		
         if (paramAnonymousInt != 2)
         {
          localLeftNavAdapter.setSelection(paramAnonymousInt - 1);
          drawerLayout.closeDrawers();
          MainActivity.this.setupContainer(paramAnonymousInt);
         }
      	   }
    	   
    	  catch (IllegalArgumentException e)
          {
    		  //Context 
            Log.e(TAG, " IllegalArgumentException", e);
            Intent intent = new Intent();
     		Context packageContext = null;
			intent.setClass(packageContext, MainActivity.class);
          }
    	
          catch (SecurityException e)
          {
             Log.e(TAG, "SecurityException", e);
             Intent intent = new Intent();
      		Context packageContext = null;
 			intent.setClass(packageContext, MainActivity.class);
          }
          catch (IllegalStateException e)
          {
             Log.e(TAG, "IllegalStateException", e);
             Intent intent = new Intent();
      		 Context packageContext = null;
 			intent.setClass(packageContext, MainActivity.class);
          }
          catch (NullPointerException e)
          {
             Log.e(TAG, "NullPointerException", e);
             Intent intent = new Intent();
      		Context packageContext = null;
 			intent.setClass(packageContext, MainActivity.class);
          }    	 
      }
    });
  }
 
  @Override
  public void onConfigurationChanged(Configuration paramConfiguration)
  {
    super.onConfigurationChanged(paramConfiguration);
    drawerToggle.onConfigurationChanged(paramConfiguration);
  }

  @Override
  protected void onCreate(Bundle paramBundle)
  {
    super.onCreate(paramBundle);
    setContentView(R.layout.activity_main);
    setupDrawer();
    setupContainer(1);
    //startLogging();
    potholeSpot = new PotholeSpotLabel(mDbHelper);
    mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

    mSensorManager.registerListener(this, mSensorManager.getDefaultSensor
                    (Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
     lastTime = System.currentTimeMillis();
    // I want to create some notifications in case the accuracy of the entire app changes
     //I will get back to this later on.....
               NotificationManager notificationManager = (NotificationManager)
                       getSystemService(NOTIFICATION_SERVICE);
    
               mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
               mNoticationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
               //stopNotification();
  }
  
  @Override
public boolean onCreateOptionsMenu(Menu paramMenu)
  {
    getMenuInflater().inflate(R.menu.main_screen_menu, paramMenu);
    return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem paramMenuItem)
  
  {
    if (drawerToggle.onOptionsItemSelected(paramMenuItem))
    
    return super.onOptionsItemSelected(paramMenuItem);
    
    switch(paramMenuItem.getItemId()) 
    {
     case R.id.subItem1:
        
        try
         {
         Intent intent = new Intent(this, CommonLoggerMap.class);
         this.startActivity(intent);
         } 
        
         catch(Exception e)
         {           
            Log.e(TAG, "just handling any damn exception", e);
         }
       
         break;
         
     case R.id.subItem2:
        
        Intent dbmanager = new Intent(this,AndroidDatabaseManager.class);
        startActivity(dbmanager);
        
     default:
        return super.onOptionsItemSelected(paramMenuItem);
     } 
    
    return true;
  }

  @Override
  protected void onPostCreate(Bundle paramBundle)
  {
    super.onPostCreate(paramBundle);
    drawerToggle.syncState();
    mSensorManager.registerListener(this, mSensorManager.getDefaultSensor
          (Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
  lastTime = System.currentTimeMillis();
  }
  
  @Override
	protected void onPause() 
  {
	  super.onPause();
	  //drawerToggle.syncState();
	  mSensorManager.unregisterListener(this);	  
  }
  
  @Override
	protected void onResume() 
  
  {
	  super.onResume();	
	  mSensorManager.registerListener(this, mSensorManager.getDefaultSensor
           (Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
   lastTime = System.currentTimeMillis();
  }
  
  @Override
	protected void onStop() 
  {
	  super.onStop();	
	  mSensorManager.unregisterListener(this);
  }

@Override
public void onSensorChanged(SensorEvent event)
{
   // TODO Auto-generated method stub
   
   try
   {
      
   if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
   {        
       //SensorEvent filteredEvent = accelerometerValueFilter(event);       
                setmLastRecordedEvent(event);    
                //mLastRecordedEvent = getmLastRecordedEvent();
               /* float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];                
                Log.d("x:", "" + x );
                Log.d("y:", "" + y );
                Log.d("z:", "" + z );*/
                
                currentTime = System.currentTimeMillis();
                potholeSpot.segmentStream(event.values[0], event.values[1], event.values[2]);
                //new Storing_xyz().execute();
                
           // callAsynchronousTask();
           
              /*  float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                
                Log.d("x:", "" + x );
                Log.d("y:", "" + y );
                Log.d("z:", "" + z );*/
    }   
   }
   
   catch(Exception e)
   {
      //e.printStackTrace();
      Log.e(TAG, "NullPointerException", e);  
      
   }
  
}


@Override
public void onAccuracyChanged(Sensor sensor, int accuracy)
{
   // TODO Auto-generated method stub
   
}


public synchronized void resumeLogging()
{
   if (DEBUG)
   {
      Log.d(TAG, "resumeLogging()");
   }
 
   try
   {
      //resume the accelerometer sensor
      mSensorManager.registerListener(this, mSensorManager.getDefaultSensor
            (Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
              lastTime = System.currentTimeMillis();      
      this.mLoggingState = Constants.LOGGING;     
      updateNotification();
   }
   catch(NullPointerException e)
   {
      Log.e(TAG, "NullPointerException", e);    
   }
 
}



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
  // Intent notificationIntent = new Intent();
/*   notificationIntent.setData(ContentUris.withAppendedId(Tracks.CONTENT_URI, mTrackId));
   mNotification.contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
   mNotification.setLatestEventInfo(this, contentTitle, contentText, mNotification.contentIntent);
   mNoticationManager.notify(R.layout.map_widgets, mNotification);*/
}

public synchronized void startLogging()
{
   if (DEBUG)
   {
      Log.d(TAG, "startLogging()");
   }
   ;
   if (this.mLoggingState == Constants.STOPPED)
   {
     
      //startNotification();
     
      //resume the accelerometer sensor
      try{
         //startNotification();
      mSensorManager.registerListener(this, mSensorManager.getDefaultSensor
            (Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
              lastTime = System.currentTimeMillis(); }
      catch(NullPointerException e)
      {
         Log.e(TAG, "NullPointerException", e);       
      }
      
   }
}

private void startNotification()
{
   //mNoticationManager.cancel(R.layout.map_widgets);long lastTime;

   int icon = R.drawable.ic_maps_indicator_current_position;
   CharSequence tickerText = getResources().getString(R.string.service_start);
   long when = System.currentTimeMillis();

   mNotification = new Notification(icon, tickerText, when);
   mNotification.flags |= Notification.FLAG_ONGOING_EVENT;

   updateNotification();

   if (Build.VERSION.SDK_INT >= 5)
   {
      startForegroundReflected(R.layout.map_widgets, mNotification);
   }
   else
   {
      mNoticationManager.notify(R.layout.map_widgets, mNotification);
   }
}

@SuppressWarnings("rawtypes")
private void startForegroundReflected(int id, Notification notification)
{

   Method mStartForeground;
   Class[] mStartForegroundSignature = new Class[] { int.class, Notification.class };

   Object[] mStartForegroundArgs = new Object[2];
   mStartForegroundArgs[0] = Integer.valueOf(id);
   mStartForegroundArgs[1] = notification;
   try
   {
      mStartForeground = getClass().getMethod("startForeground", mStartForegroundSignature);
      mStartForeground.invoke(this, mStartForegroundArgs);
   }
   catch (NoSuchMethodException e)
   {
      Log.e(TAG, "Failed starting foreground notification using reflection", e);
   }
   catch (IllegalArgumentException e)
   {
      Log.e(TAG, "Failed starting foreground notification using reflection", e);
   }
   catch (IllegalAccessException e)
   {
      Log.e(TAG, "Failed starting foreground notification using reflection", e);
   }
   catch (InvocationTargetException e)
   {
      Log.e(TAG, "Failed starting foreground notification using reflection", e);
   }

}

@Override
public void onLocationChanged(Location location)
{
   // TODO Auto-generated method stub
   
   
   if (VERBOSE)
   {
      Log.v(TAG, "onLocationChanged( Location " + location + " )");
   }

   // Might be claiming GPS disabled but when we were paused this changed and this location proves so
   if (mShowingGpsDisabled)
   {
      notifyOnEnabledProviderNotification(R.string.service_gpsenabled);
   }
   
   Location filteredLoc = locationFilter(location);
   
   
 /*  if (filteredLoc != null)
   { 
      setmLastRecordedLocation(location);
     
   } 
   */
   
   if (location != null)
   { 
      setmLastRecordedLocation(location);     
   } 
   
}

public Location locationFilter(Location proposedLocation)
{
   // Do no include log wrong 0.0 lat 0.0 long, skip to next value in while-loop
   if (proposedLocation != null && (proposedLocation.getLatitude() == 0.0d || proposedLocation.getLongitude() == 0.0d))
   {
      Log.w(TAG, "A wrong location was received, 0.0 latitude and 0.0 longitude... ");
     proposedLocation = null;
     //proposedLocation = addBadLocation(proposedLocation);
   }

   // Do not log a waypoint which is more inaccurate then is configured to be acceptable
   if (proposedLocation != null && proposedLocation.getAccuracy() > mMaxAcceptableAccuracy)
   {
      Log.w(TAG, String.format("A weak location was received, lots of inaccuracy... (%f is more then max %f)", proposedLocation.getAccuracy(),
            mMaxAcceptableAccuracy));
      proposedLocation = addBadLocation(proposedLocation);
   }

   // Do not log a location which might be on any side of the previous waypoint
   if (proposedLocation != null && getmLastRecordedLocation() != null && proposedLocation.getAccuracy() > getmLastRecordedLocation().distanceTo(proposedLocation))
   {
      Log.w(TAG,
            String.format("A weak location was received, not quite clear from the previous waypoint... (%f more then max %f)",
                  proposedLocation.getAccuracy(), getmLastRecordedLocation().distanceTo(proposedLocation)));
      proposedLocation = addBadLocation(proposedLocation);
   }

   // Speed checks, check if the proposed location could be reached from the previous one in sane speed
   // Common to jump on network logging and sometimes jumps on Samsung Galaxy S type of devices
   if (mSpeedSanityCheck && proposedLocation != null && getmLastRecordedLocation() != null)
   {
      // To avoid near instant teleportation on network location or glitches cause continent hopping
      float meters = proposedLocation.distanceTo(getmLastRecordedLocation());
      long seconds = (proposedLocation.getTime() - getmLastRecordedLocation().getTime()) / 1000L;
      float speed = meters / seconds;
      if (speed > MAX_REASONABLE_SPEED)
      {
         Log.w(TAG, "A strange location was received, a really high speed of " + speed + " m/s, prob wrong...");
         proposedLocation = addBadLocation(proposedLocation);
         // Might be a messed up Samsung Galaxy S GPS, reset the logging
         if (speed > 2 * MAX_REASONABLE_SPEED && mPrecision != Constants.LOGGING_GLOBAL)
         {
            Log.w(TAG, "A strange location was received on GPS, reset the GPS listeners");
            stopListening();
            mLocationManager.removeGpsStatusListener(mStatusListener);
            
            mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            //sendRequestStatusUpdateMessage();
            //sendRequestLocationUpdatesMessage();
         }
      }
   }
   // Older bad locations will not be needed
   if (proposedLocation != null)
   {
      mWeakLocations.clear();
   }
   
   //mLastRecordedLocation = proposedLocation;
   
      return proposedLocation;
} 

private Listener mStatusListener = new GpsStatus.Listener()
{
   @Override
   public synchronized void onGpsStatusChanged(int event)
   {
      switch (event)
      {
         case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
            if (mStatusMonitor)
            {
               GpsStatus status = mLocationManager.getGpsStatus(null);
               mSatellites = 0;
               Iterable<GpsSatellite> list = status.getSatellites();
               for (GpsSatellite satellite : list)
               {
                  if (satellite.usedInFix())
                  {
                     mSatellites++;
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

/*
public Location getLocation() {
   try {
       mLocationManager = (LocationManager) mContext
               .getSystemService(LOCATION_SERVICE);
       // getting GPS status
       isGPSEnabled = mLocationManager
               .isProviderEnabled(LocationManager.GPS_PROVIDER);
       // getting network status
       isNetworkEnabled = mLocationManager
               .isProviderEnabled(LocationManager.NETWORK_PROVIDER);
       if (!isGPSEnabled && !isNetworkEnabled) {
           // no network provider is enabled
       } else {
           this.canGetLocation = true;
           if (isNetworkEnabled) {
              mLocationManager.requestLocationUpdates(
                       LocationManager.NETWORK_PROVIDER,
                       MIN_TIME_BW_UPDATES,
                       MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
               Log.d("Network", "Network Enabled");
               if (mLocationManager != null) {
                   location = mLocationManager
                           .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                   if (location != null) {
                       latitude = location.getLatitude();
                       longitude = location.getLongitude();
                   }
               }
           }
           // if GPS Enabled get lat/long using GPS Services
           if (isGPSEnabled) {
               if (location == null) {
                  mLocationManager.requestLocationUpdates(
                           LocationManager.GPS_PROVIDER,
                           MIN_TIME_BW_UPDATES,
                           MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                   Log.d("GPS", "GPS Enabled");
                   if (mLocationManager != null) {
                       location = mLocationManager
                               .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                       if (location != null) {
                           latitude = location.getLatitude();
                           longitude = location.getLongitude();
                       }
                   }
               }
           }
       }
   } 
   catch (Exception e) 
   {
       e.printStackTrace();
   }
   return location;
}
*/

private TimerTask mHeartbeat = null;

private void stopListening()
{
   if (mHeartbeat != null)
   {
      mHeartbeat.cancel();
      mHeartbeat = null;
   }
   
   //this is for the case of the accelerometer sensor
   mSensorManager.unregisterListener(this);
   
   mLocationManager.removeUpdates(this);
}


private Location addBadLocation(Location location)
{
   mWeakLocations.add(location);
   if (mWeakLocations.size() < 3)
   {
      location = null;
   }
   else
   {
      Location best = mWeakLocations.lastElement();
      for (Location whimp : mWeakLocations)
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
      synchronized (mWeakLocations)
      {
         mWeakLocations.clear();
      }
      location = best;
   }
   return location;
}

public void storeAllAccelerationValues(SensorEvent event) 
{ 
   float[] value = event.values;
   
   
   float xVal = value[0];
   float yVal = value[1];
   float zVal = value[2];
   
   float accelationSquareRoot = (xVal*xVal + yVal*yVal + zVal*zVal) 
         / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
   
   long actualTime = System.currentTimeMillis();   
   /*if (accelationSquareRoot >= 1.2) 
   {*/
   
      //the time interval is 2x10 power 9
  String queryStoreAccelerationValues ="Insert into "+Xyz.TABLE+" (";
 
 queryStoreAccelerationValues=queryStoreAccelerationValues+Xyz.X+",";
 queryStoreAccelerationValues=queryStoreAccelerationValues+Xyz.Y+",";
 queryStoreAccelerationValues=queryStoreAccelerationValues+Xyz.Z+",";
 queryStoreAccelerationValues=queryStoreAccelerationValues+Xyz.SPEED+",";
 queryStoreAccelerationValues=queryStoreAccelerationValues+Xyz.TIME;
  
 queryStoreAccelerationValues=queryStoreAccelerationValues+" ) VALUES ( ";
 
 queryStoreAccelerationValues=queryStoreAccelerationValues+"'"+Float.valueOf(event.values[0])+"' , ";
 queryStoreAccelerationValues=queryStoreAccelerationValues+"'"+Float.valueOf(event.values[1])+"' , ";
 queryStoreAccelerationValues=queryStoreAccelerationValues+"'"+Float.valueOf(event.values[2])+"', "; 
 queryStoreAccelerationValues=queryStoreAccelerationValues+"'"+accelationSquareRoot+"', ";
 queryStoreAccelerationValues=queryStoreAccelerationValues+"'"+Long.valueOf(System.currentTimeMillis()) +"' ) "; 
 
 Log.d("Insert Query", queryStoreAccelerationValues);
 
    mDbHelper.getData(queryStoreAccelerationValues);      
 
}

private void notifyOnEnabledProviderNotification(int resId)
{
   mNoticationManager.cancel(LOGGING_UNAVAILABLE);
   mShowingGpsDisabled = false;
   CharSequence text = this.getString(resId);
   Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
   toast.show();
}


@Override
public void onStatusChanged(String provider, int status, Bundle extras)
{
   // TODO Auto-generated method stub
   
}

@Override
public void onProviderEnabled(String provider)
{
   // TODO Auto-generated method stub
   
}

@Override
public void onProviderDisabled(String provider)
{
   // TODO Auto-generated method stub
   
}


public void callAsynchronousTask() {
   final Handler handler = new Handler();
   Timer timer = new Timer();
   TimerTask doAsynchronousTask = new TimerTask() {       
       @Override
       public void run() {
           handler.post(new Runnable() {
               @Override
               public void run() {       
                   try {
                      Storing_xyz performBackgroundTask = new Storing_xyz();
                       // PerformBackgroundTask this class is the class that extends AsynchTask 
                       performBackgroundTask.execute();
                   } catch (Exception e) {
                       // TODO Auto-generated catch block
                   }
               }
           });
       }
   };
   timer.schedule(doAsynchronousTask, 0, 50000); //execute in every 50000 ms
}



private class Storing_xyz extends AsyncTask<String, Void, String> 
{
   @Override
   protected String doInBackground(String... params) 
   {
      if (mLastRecordedEvent != null)
      {
     //storeAllAccelerationValues(mLastRecordedEvent);
      }
       return "Executed";
   }

   @Override
   protected void onPostExecute(String result) 
   {
      // TextView txt = (TextView) findViewById(R.id.output);
       //txt.setText("Executed"); // txt.setText(result);
       // might want to change "executed" for the returned string passed
       // into onPostExecute() but that is upto you
   }

   @Override
   protected void onPreExecute() 
   {
      
   }

   @Override
   protected void onProgressUpdate(Void... values) 
   {
      
   }
}

public SensorEvent getmLastRecordedEvent()
{
   return mLastRecordedEvent;
}

public void setmLastRecordedEvent(SensorEvent mLastRecordedEvent)
{
   this.mLastRecordedEvent = mLastRecordedEvent;
}

public Location getmLastRecordedLocation()
{
   return mLastRecordedLocation;
}

public void setmLastRecordedLocation(Location mLastRecordedLocation)
{
   this.mLastRecordedLocation = mLastRecordedLocation;
}

}
