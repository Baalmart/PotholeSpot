package com.prim.ui;



/**
 * 
 * @author Martin Bbaale
 * 
 * 
 * ***/

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.location.GpsStatus.Listener;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.prim.MainActivity;
import com.prim.Others;
import com.prim.IssueList;
import com.prim.custom.CustomFragment;
import com.prim.model.Data;

import dev.baalmart.gps.R;
import dev.ugasoft.android.gps.actions.ControlTracking;
import dev.ugasoft.android.gps.actions.NameTrack;
import dev.ugasoft.android.gps.db.AndroidDatabaseManager;
import dev.ugasoft.android.gps.db.DatabaseHelper;
import dev.ugasoft.android.gps.db.Prim.Labels;
import dev.ugasoft.android.gps.db.Prim.Tracks;
import dev.ugasoft.android.gps.db.Prim.Waypoints;
import dev.ugasoft.android.gps.db.Prim.WaypointsColumns;
import dev.ugasoft.android.gps.db.Prim.Xyz;
import dev.ugasoft.android.gps.db.PrimProvider;
import dev.ugasoft.android.gps.logger.GPSLoggerService;
import dev.ugasoft.android.gps.logger.GPSLoggerServiceManager;
import dev.ugasoft.android.gps.logger.IGPSLoggerServiceRemote;
import dev.ugasoft.android.gps.streaming.StreamUtils;
import dev.ugasoft.android.gps.util.Constants;
import dev.ugasoft.android.gps.viewer.map.CommonLoggerMap;
import dev.ugasoft.android.gps.viewer.map.LoggerLabelHelper;
import dev.ugasoft.android.gps.viewer.map.LoggerMapHelper;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

@SuppressWarnings("deprecation")
public class MainFragment extends CustomFragment  
{	
      LoggerLabelHelper mHelper;
      //MainFragment.Result lastRecorderdResult;
       SensorEvent mLastRecordedEvent;
       Location mLastRecordedLocation;      
      Context mContext;
      DatabaseHelper mDbHelper;      
      PrimProvider pProvider;
      SensorManager mSensorManager;
      Sensor mAccelerometer;
      SQLiteDatabase db;
      String lastKnownLabelName;
      private Vector<SensorEvent> mWeakSensorEvent;
      MainActivity mA;
      String queryUpdateLongitude;
      String queryUpdateLatitude;
      String queryUpdateSpeed;
      
      String queryLatitude;
      String querySpeed;
      String queryTime;
      String queryLongitude;
      
      double mQueryLatitude;
      double mQueryLongitude;
      float mQuerySpeed;
      long mQueryTime;
      
      double mQueryUpdateLongitude;
      double mQueryUpdateLatitude;
      float mQueryUpdateSpeed;
      long mQueryUpdateTime;
      
      float LastRecorded_xVal;
      float LastRecorded_yVal;
      float LastRecorded_zVal;
   
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

	   
	   //private SensorEvent mSensorEvent;
	   private float mDistance;

	   private Vector<Location> mWeakLocations;
	   private Queue<Double> mAltitudes;

//declarations	
private Sensor sensor;
protected static final String TAG = "PRIM.MainFragment";
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
private IGPSLoggerServiceRemote mGPSLoggerRemote;
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
long lastTime;

Uri mLabelUri;
private Activity mActivity;


@Override
public void onCreate(Bundle savedInstanceState) 
{
   super.onCreate(savedInstanceState);
   mDbHelper = new DatabaseHelper(getActivity()); 
   lastTime = System.currentTimeMillis();   
   //startLogging();
  //mLastRecordedEvent = (SensorEvent)this.getSystemService(SENSOR_SERVICE);
}


@Override
public void onAttach(Activity activity) 
{
   
   mDbHelper = new DatabaseHelper(getActivity()); 
   lastTime = System.currentTimeMillis();
    super.onAttach(activity);
    mActivity = activity;
}

  private void loadDummyData() //from private
  {
    iList = new ArrayList<Data>();
    
    //potholes
    iList.add(new Data(new String[] { "potholes" }, 
    		new int[] { R.drawable.potholes, 
    		R.drawable.potholes, R.drawable.potholes }));
    
    //road humps
    iList.add(new Data(new String[] { "road humps" }, 
    		new int[] { R.drawable.road_humps, 
    		R.drawable.road_humps, 
    		R.drawable. road_humps }));
    
    //uneven roads
    iList.add(new Data(new String[] { "uneven roads" }, 
    		new int[] { R.drawable.uneven_roads, 
    		R.drawable.uneven_roads, 
    		R.drawable.uneven_roads }));
    
    //pavements
    iList.add(new Data(new String[] { "pavements" }, 
    		new int[] {R.drawable.pavements, 
    		R.drawable.pavements, 
    		R.drawable.pavements }));    
    
    //road markings
    //ArrayList localArrayList1 = iList;    
    String[] arrayOfString1 = { "road markings" };   
    int[] arrayOfInt1 = new int[3];
    arrayOfInt1[0] = R.drawable.road_markings;
    arrayOfInt1[2] = R.drawable.road_markings;
    iList.add(new Data(arrayOfString1, arrayOfInt1));    
    
    //sudden breaking
    iList.add(new Data(new String[] { "sudden breaking" }, 
    		new int[] { R.drawable.sudden_breaking, 
    		R.drawable.sudden_breaking, 
    		R.drawable.sudden_breaking }));   
    
   //gravel
   // ArrayList localArrayList2 = iList;   
    String[] arrayOfString2 = { "gravel" };
    int[] arrayOfInt2 = new int[3];
    arrayOfInt2[0] = R.drawable.gravel;
    arrayOfInt2[2] = R.drawable.gravel;
    iList.add(new Data(arrayOfString2, arrayOfInt2));
    
    //others
    iList.add(new Data(new String[] { "others" }, 
    		new int[] { R.drawable.others, 
    		R.drawable.others, 
    		R.drawable.others }));
  }
  
  private void setupView(View paramView)
  {
    setTouchNClick(paramView.findViewById(R.id.nearby));
    loadDummyData();
    GridView localGridView = (GridView)paramView.findViewById(R.id.grid);
    
    localGridView.setAdapter(new GridAdapter());   
    
    
  /*  mNoticationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
    stopNotification();*/
    
    ///handling item click events
    localGridView.setOnItemClickListener(new AdapterView.OnItemClickListener()
    {
       
	@Override
   public void onItemClick(AdapterView<?> paramAnonymousAdapterView, View paramAnonymousView, 
    		  int paramAnonymousInt, long paramAnonymousLong)
      {
	   
	   lastKnownLabelName = iList.get(paramAnonymousInt).getTexts()[0].toString();
      
	  mLastRecordedEvent = ((MainActivity)getActivity()).getmLastRecordedEvent();
	  mLastRecordedLocation = ((MainActivity)getActivity()).getmLastRecordedLocation();
	  
	   
	 // ((MainActivity)getActivity()).onSensorChanged(mLastRecordedEvent);
	   
	    try
	          {  	   
	            if (mLastRecordedEvent != null)
	            {
	             storeAllValues(mLastRecordedEvent);
	             }
	            
	            // mLoggerService.pauseLogging();
	            
	           /* if (mLoggerService.getmLastRecordedEvent() != null && mLoggerService.getmLastRecordedLocation() != null )
               {
                storeAllValues(mLoggerService.getmLastRecordedEvent(), mLoggerService.getmLastRecordedLocation());
                }*/
	            
	           // mLoggerService.startLogging();
	            float x = mLastRecordedEvent.values[0];
               float y = mLastRecordedEvent.values[1];
               float z = mLastRecordedEvent.values[2];
	            
	            Log.d("x:", "" + x );
               Log.d("y:", "" + y );
               Log.d("z:", "" + z );
	          	             
	          }
	    
     catch (NullPointerException e)
             {
                Log.e(TAG, "NullPointerException", e);
               
                try 
                {
                double lat = mLoggerService.getmLastRecordedLocation().getLatitude();
                double lon = mLoggerService.getmLastRecordedLocation().getLongitude();
              
                Log.d("latitude:","" +lat);
                Log.d("longitude:","" + lon);
                }
                catch(NullPointerException f)
                {
                   Log.d(TAG, "exception noticed inside onLocationChanged", f); 
                   
                }
             
       
             }
	  
      }
      });   
    
  }
  
  public void getX(float xval)
  {
	//  xVal = xval;
	  
  }
  /**
   * the getSystemService() method that provides access to system 
   * services comes from Context. An Activity extends Context, 
   * a Fragment does not. Hence, you first need to get a reference to the 
   * Activity in which the Fragment is contained and then magically retrieve
   * the system service you want.   
   *  
   * 
   * **/
 
//the click listener for the upper section of the main fragment....the sign for location image....
  @Override
  public void onClick(View paramView)
  {
    super.onClick(paramView);
    if (paramView.getId() == R.id.nearby) 
    {
    	//using this one to start and stop the logging
       try
       {
        Intent intent = new Intent(getActivity(), CommonLoggerMap.class);
        this.startActivity(intent);
        } 
        catch(Exception e)
        {           
           Log.e(TAG, "just handling any damn exception", e);
        }
 
    }
      //startActivity(new Intent(getActivity(), IssueList.class).putExtra("title", getString(R.string.nearby)));
  }

  @SuppressLint({"InflateParams"})
  public View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup, Bundle paramBundle)
  {
    View localView = paramLayoutInflater.inflate(R.layout.main_container, null);
    setupView(localView);
    return localView;
  }

  private class GridAdapter extends BaseAdapter
  {
    private GridAdapter()
    {
    }

    public int getCount()
    {
       
     
      return iList.size();
      

        
    }

    public Data getItem(int paramInt)
    {
      return (Data)iList.get(paramInt);
    }

    public long getItemId(int paramInt)
    {
      return paramInt;
    }
    
    @SuppressLint({"InflateParams"})
    public View getView(int paramInt, View paramView, ViewGroup paramViewGroup)
    {
      if (paramView == null)
        paramView = getActivity().getLayoutInflater().inflate(R.layout.grid_item, null);
      Data localData = getItem(paramInt);
      TextView localTextView = (TextView)paramView;
      localTextView.setText(localData.getTexts()[0]);
      localTextView.setCompoundDrawablesWithIntrinsicBounds(0, localData.getResources()[0], 0, 0);
      return paramView;
    }
 
  }

@Override
public void onPause() 
{
// TODO Auto-generated method stub
super.onPause();
//mSensorManager.unregisterListener(this);
}

@Override
public void onResume() 
{
// TODO Auto-generated method stub
  super.onResume();
 // resumeLogging();  
}

@Override
public void onDestroy() 
{
  super.onDestroy();
  //mSensorManager.unregisterListener(this);	
}

@Override
public void onStop() 
{
	   super.onStop();
	   //mSensorManager.unregisterListener(this);		   
}

public void storeAllValues(SensorEvent event) 
{ 
   //mLastRecordedEvent = event;
   
   /*if (event == null) 
   {
      
      Log.d("error:", "event is null");
      event = mLastRecordedEvent;
      
   }
   
   else 
   {*/
 String queryStoreValues ="Insert into "+Labels.TABLE+" (";
 
 queryStoreValues=queryStoreValues+Labels.X+",";
 queryStoreValues=queryStoreValues+Labels.Y+",";
 queryStoreValues=queryStoreValues+Labels.Z+",";
 queryStoreValues=queryStoreValues+Labels.NAME+",";
 queryStoreValues=queryStoreValues+Labels.LATITUDE+",";
 queryStoreValues=queryStoreValues+Labels.LONGITUDE+",";
 queryStoreValues=queryStoreValues+Labels.SPEED+",";

 queryStoreValues=queryStoreValues+Labels.CREATION_TIME;
 
 
 queryStoreValues=queryStoreValues+" ) VALUES ( ";
 
 queryStoreValues=queryStoreValues+"'"+Float.valueOf(event.values[0])+"' , ";
 queryStoreValues=queryStoreValues+"'"+Float.valueOf(event.values[1])+"' , ";
 queryStoreValues=queryStoreValues+"'"+Float.valueOf(event.values[2])+"', ";
 queryStoreValues=queryStoreValues+"'"+lastKnownLabelName+"' , ";
 queryStoreValues=queryStoreValues+"'"+getQueryUpdateLatitude()+"' , ";
 queryStoreValues=queryStoreValues+"'"+getQueryUpdateLongitude()+"' , "; 
 queryStoreValues=queryStoreValues+"'"+getQueryUpdateSpeed()+"' , ";
 //iList.get(getId()).getTexts()[0].toString();
 
 queryStoreValues=queryStoreValues+"'"+Long.valueOf(System.currentTimeMillis()) +"' ) "; 
//.get(getId()).getTexts()[0].toString()
 Log.d("Insert Query", queryStoreValues);
 
 
   queryLatitude = "Select "+Waypoints.LATITUDE+" FROM "+Waypoints.TABLE+"  ";
   queryLongitude = "Select "+Waypoints.LONGITUDE+" FROM "+Waypoints.TABLE+"  ";
   querySpeed = "Select "+Waypoints.SPEED+" FROM "+Waypoints.TABLE+"  ";
   queryTime = "Select "+Waypoints.TIME+" FROM "+Waypoints.TABLE+"  ";
  
 queryUpdateLongitude ="Update "+Labels.TABLE+" ";
 queryUpdateLongitude = queryUpdateLongitude+" SET ";
 queryUpdateLongitude = queryUpdateLongitude+ " "+Labels.LONGITUDE+" = "+getQueryLongitude()+"  ";
 queryUpdateLongitude = queryUpdateLongitude+" WHERE ";
 queryUpdateLongitude = queryUpdateLongitude+ " "+Labels.CREATION_TIME+" = "+getQueryTime()+" ";
 
 
 queryUpdateLatitude ="Update "+Labels.TABLE+" ";
 queryUpdateLatitude = queryUpdateLatitude+" SET ";
 queryUpdateLatitude = queryUpdateLatitude+ " "+Labels.LATITUDE+" = "+getQueryLatitude()+" ";
 queryUpdateLatitude= queryUpdateLatitude+" WHERE ";
 queryUpdateLatitude = queryUpdateLatitude+ " "+Labels.CREATION_TIME+" = "+getQueryTime()+" ";
 
 
 queryUpdateSpeed ="Update "+Labels.TABLE+" ";
 queryUpdateSpeed = queryUpdateSpeed+" SET ";
 queryUpdateSpeed = queryUpdateSpeed+ " "+Labels.LONGITUDE+" = "+getQuerySpeed()+" ";
 queryUpdateSpeed= queryUpdateSpeed+" WHERE ";
 queryUpdateSpeed = queryUpdateSpeed+ " "+Labels.CREATION_TIME+" = "+getQueryTime()+" ";
 
 mDbHelper.getData(queryStoreValues);
   //}
        
}

//end of method

public double getQueryLatitude()
{
   return mQueryLatitude;
}
public void setQueryLatitude(ArrayList<Cursor> mQueryLatitude)
{
  mQueryLatitude = mDbHelper.getData(queryLatitude);
}


public double getQueryLongitude()
{
   return mQueryLongitude;
}
public void setQueryLongitude(ArrayList<Cursor> mQueryLongitude)
{
  mQueryLongitude = mDbHelper.getData(queryLongitude);
}

//for the time query
public long getQueryTime()
{  
 return mQueryTime;
}

public void setQueryTime(ArrayList<Cursor> mQueryTime)
{
 mQueryTime = mDbHelper.getData(queryTime);
}

public float getQuerySpeed()
{
   return mQuerySpeed;
}
public void setQuerySpeed(ArrayList<Cursor> mQuerySpeed)
{
  mQuerySpeed = mDbHelper.getData(queryLongitude);
}


/******************************************************************
updates

*******************************************************************/

//for the latitude update
public double getQueryUpdateLatitude()
{  
   return mQueryUpdateLatitude;
}

public void setQueryUpdateLatitude(ArrayList<Cursor> mQueryUpdateLatitude)
{
   mQueryUpdateLatitude = mDbHelper.getData(queryUpdateLatitude);
}


//for the speed update
public float getQueryUpdateSpeed()
{  
   return mQueryUpdateSpeed;
}


public void setQueryUpdateSpeed(ArrayList<Cursor> mQueryUpdateSpeed)
{
   mQueryUpdateSpeed = mDbHelper.getData(queryUpdateSpeed);
}


//for the longitude update
public double getQueryUpdateLongitude()
{   
return  mQueryUpdateLongitude;
}

public void setQueryUpdateLongitude(ArrayList<Cursor> mQueryUpdateLongitude)
{
   mQueryUpdateLongitude = mDbHelper.getData(queryUpdateLongitude);
}





}
