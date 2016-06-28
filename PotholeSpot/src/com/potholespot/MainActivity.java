package com.potholespot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.potholespot.custom.CustomActivity;
import com.potholespot.ui.History;
import com.potholespot.ui.NewFragment;
import com.potholespot.ui.Routes;
import com.potholespot.ui.Workout;

import dev.potholespot.uganda.R;
import dev.potholespot.android.db.AndroidDatabaseManager;
import dev.potholespot.android.db.DatabaseHelper;
import dev.potholespot.android.db.sync.SampleBC;
import dev.potholespot.android.viewer.map.CommonLoggerMap;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
@SuppressLint("NewApi")
public class MainActivity extends CustomActivity implements SensorEventListener
{
   
   
  private View currentTab;
  private ViewPager pager;
  DatabaseHelper mDbHelper;
  public long lastTime;
  public long currentTime;
  private SensorEvent mLastRecordedEvent;
  private SensorManager mSensorManager;
  String TAG = "Pspot.MainActivity";  
  
    
  //for the sync
//DB Class to perform DB related operations..more like the database helper
  //DBController controller = new DBController(this);
  DatabaseHelper dbHelper = new DatabaseHelper(this);
  // Progress Dialog Object
  ProgressDialog prgDialog;
  HashMap<String, String> queryValues;

  @SuppressWarnings("deprecation")
private void initPager()
  {
    pager = ((ViewPager)findViewById(R.id.pager));
    pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener()
    {
      @Override
      public void onPageScrollStateChanged(int paramAnonymousInt)
      {
      }

      @Override
      public void onPageScrolled(int paramAnonymousInt1, 
    		  float paramAnonymousFloat, int paramAnonymousInt2)
      {
      }

      @Override
      public void onPageSelected(int paramAnonymousInt)
      {
        MainActivity.this.setCurrentTab(paramAnonymousInt);
      }
    });
    pager.setAdapter(new DummyPageAdapter(getSupportFragmentManager()));
  }

  private void initTabs()
  {
    findViewById(R.id.tab1).setOnClickListener(this);
    findViewById(R.id.tab2).setOnClickListener(this);
    findViewById(R.id.tab3).setOnClickListener(this);
    findViewById(R.id.tab4).setOnClickListener(this);
    //setCurrentTab(0);
  }
  //so the problem is inside this method below:
  
  private void setCurrentTab(int paramInt)
  {
    if (currentTab != null)
      currentTab.setEnabled(true);
    if (paramInt == 0)
      currentTab = findViewById(R.id.tab1);
    
    while (true)
    {
      currentTab.setEnabled(false);
      getActionBar().setTitle(((Button)currentTab).getText().toString());
      //return;
      
      try 
      {
      if (paramInt == 1)
        currentTab = findViewById(R.id.tab2);
      else if (paramInt == 2)
        currentTab = findViewById(R.id.tab3);
      else
        currentTab = findViewById(R.id.tab4);
      }
      catch(Exception e)
      {         
         e.printStackTrace();
      }
    }
  }

  @Override
public void onClick(View paramView)
  {
    super.onClick(paramView);
    if (paramView.getId() == R.id.tab1)
      pager.setCurrentItem(0, true);
    
/*    do
    {
      //return;
      if (paramView.getId() == R.id.tab2)
      {
        pager.setCurrentItem(1, true);
        return;
      }
      if (paramView.getId() == R.id.tab3)
      {
        pager.setCurrentItem(2, true);
        return;
      }
    }
    while (paramView.getId() != R.id.tab4);
    pager.setCurrentItem(3, true);*/
    

    if (paramView.getId() == R.id.tab2)
    {
      pager.setCurrentItem(1, true);
      return;
    }
    if (paramView.getId() == R.id.tab3)
    {
      pager.setCurrentItem(2, true);
      return;
    }
  
  if (paramView.getId() == R.id.tab4)
  {
      pager.setCurrentItem(3, true);
      return;
  }
  }

  @Override
protected void onCreate(Bundle paramBundle)
  {
    super.onCreate(paramBundle);
    setContentView(R.layout.activity_main);
    setupActionBar();
    initTabs();
    initPager();
/*
 //from the Sync
    ArrayList<HashMap<String, String>> xyzList =  dbHelper.getAll_xyz();
    // If users exists in SQLite DB
    if (xyzList.size() != 0) 
    {
       // Set the labels Array list in ListView
       ListAdapter adapter = new SimpleAdapter
             (MainActivity.this, xyzList, R.layout.view_user_entry, new String[] 
                   {"id", "time", "speed", "x", "y", "z" }, 
                   new int[] { R.id.id, R.id.time, R.id.speed, R.id.x, R.id.y, R.id.z});
       ListView myList = (ListView) findViewById(android.R.id.list);
       myList.setAdapter(adapter);
    }
    */
    // Initialize Progress Dialog properties
    prgDialog = new ProgressDialog(this);
    prgDialog.setMessage("Transferring Data from Remote MySQL DB and Syncing SQLite. Please wait...");
    prgDialog.setCancelable(false);
    // BroadCase Receiver Intent Object
    Intent alarmIntent = new Intent(getApplicationContext(), SampleBC.class);
    // Pending Intent Object
    PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    // Alarm Manager Object
    AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
    // Alarm Manager calls BroadCast for every Ten seconds (10 * 1000), BroadCase further calls service to check if new records are inserted in 
    // Remote MySQL DB
    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis() + 5000, 10 * 1000, pendingIntent);
      
    mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    mSensorManager.registerListener(this, mSensorManager.getDefaultSensor
                    (Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
     lastTime = System.currentTimeMillis();
  }

  @Override
public boolean onCreateOptionsMenu(Menu paramMenu)
  {
    super.onCreateOptionsMenu(paramMenu);
    getMenuInflater().inflate(R.menu.main, paramMenu);
    return true;
  }
  
  

  @Override
public boolean onOptionsItemSelected(MenuItem paramMenuItem)
  {
    if (paramMenuItem.getItemId() == R.id.menu_setting)
    {
      startActivity(new Intent(this, Setting.class));
      return true;
    }
    
    if (paramMenuItem.getItemId() == R.id.menu_map)
    {
      startActivity(new Intent(this, CommonLoggerMap.class));
      return true;
    }
    
    
    if (paramMenuItem.getItemId() == R.id.menu_db)
    {
      startActivity(new Intent(this, AndroidDatabaseManager.class));
      return true;
    }
    
    if (paramMenuItem.getItemId() == R.id.menu_sync) 
    {
       // Transfer data from remote MySQL DB to SQLite on Android and perform Sync
      syncSQLiteMySQLDB();
       return true;
    }
    
    return super.onOptionsItemSelected(paramMenuItem);
  }


@SuppressWarnings("deprecation")
protected void setupActionBar()
  {
    ActionBar localActionBar = getActionBar();
    localActionBar.setDisplayShowTitleEnabled(true);
    localActionBar.setNavigationMode(0);
    localActionBar.setDisplayUseLogoEnabled(true);
    localActionBar.setLogo(R.drawable.signage_map);
    localActionBar.setDisplayHomeAsUpEnabled(false);
    localActionBar.setHomeButtonEnabled(false);
  }

  private class DummyPageAdapter extends FragmentPagerAdapter
  {
    public DummyPageAdapter(FragmentManager arg2)
    {
      super(arg2);
    }

    @Override
   public int getCount()
    {
      return 4;
    }

    @Override
   public Fragment getItem(int paramInt)
    {
      if (paramInt == 0)
        return new NewFragment();
      if (paramInt == 1)
        return new Routes();
      if (paramInt == 2)
        return new Workout();
      
      //return for the history....
      return new History();
    }
    
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
               // potholeSpot.segmentStream(event.values[0], event.values[1], event.values[2]);
                //new Storing_xyz().execute();
                
           // callAsynchronousTask();
           
            /*    float x = event.values[0];
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

//methods for the sync
// Method to Sync MySQL to SQLite DB
public void syncSQLiteMySQLDB() {
   // Create AsycHttpClient object
   AsyncHttpClient client = new AsyncHttpClient();
   // Http Request Params Object
   RequestParams params = new RequestParams();
   // Show ProgressBar
   prgDialog.show();
   // Make Http call to getusers.php
   client.post("http://localhost/potholespot/web/mysqlsqlitesync/get_xyz.php", 
         params, new AsyncHttpResponseHandler() 
     {
         @Override
         public void onSuccess(String response) 
         {
            // Hide ProgressBar
            prgDialog.hide();
            // Update SQLite DB with response sent by getusers.php
            //updateSQLite(response);
         }
         // When error occured
         @Override
         public void onFailure(int statusCode, Throwable error, String content) 
         {
            // TODO Auto-generated method stub
            // Hide ProgressBar
            prgDialog.hide();
            if (statusCode == 404) 
            {
               Toast.makeText(getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
            } 
            else if (statusCode == 500) 
            {
               Toast.makeText(getApplicationContext(), "Something went wrong at server end", Toast.LENGTH_LONG).show();
            } 
            else 
            {
               Toast.makeText(getApplicationContext(), "Unexpected Error occcured! [Most common Error: Device might not be connected to Internet]",
                     Toast.LENGTH_LONG).show();
            }
         }
   });
}


//this method also takes advantage of the response and extracts a JSON array.
public void updateSQLite(String response)
{
   ArrayList<HashMap<String, String>> usersynclist;
   usersynclist = new ArrayList<HashMap<String, String>>();
   // Create GSON object
   Gson gson = new GsonBuilder().create();
   try {
      // Extract JSON array from the response
      JSONArray arr = new JSONArray(response);
      System.out.println(arr.length());
      // If no of array elements is not zero
      if(arr.length() != 0){
         // Loop through each array element, get JSON object which has the columns for x, y and z.
         for (int i = 0; i < arr.length(); i++) 
         {
            // Get JSON object
            JSONObject obj = (JSONObject) arr.get(i);
            System.out.println(obj.get("id"));
            System.out.println(obj.get("time"));
            System.out.println(obj.get("speed"));
            System.out.println(obj.get("x"));
            System.out.println(obj.get("y"));
            System.out.println(obj.get("z"));               
            
            // DB QueryValues Object to insert into SQLite
            queryValues = new HashMap<String, String>();
            
            
            // Add id extracted from Object
            queryValues.put("id", obj.get("id").toString());
            // Add time extracted from Object
            queryValues.put("time", obj.get("time").toString());
             // Add speed extracted from Object
            queryValues.put("speed", obj.get("speed").toString());
            // Add x extracted from Object
            queryValues.put("x", obj.get("x").toString());
            // Add y extracted from Object
            queryValues.put("y", obj.get("y").toString());
            // Add z extracted from Object
            queryValues.put("z", obj.get("z").toString());
                           
            // Insert values into SQLite DB
           dbHelper.insert_xyz(queryValues);
            HashMap<String, String> map = new HashMap<String, String>();
            // Add status for each xyz in Hashmap
            map.put("Id", obj.get("userId").toString());
            map.put("time", obj.get("time").toString());
            map.put("speed", obj.get("speed").toString());
            map.put("x", obj.get("x").toString());
            map.put("y", obj.get("y").toString());
            map.put("z", obj.get("z").toString());
            map.put("status", "1");
            usersynclist.add(map);
         }
         // Inform Remote MySQL DB about the completion of Sync activity by passing Sync status of xyz values
         updateMySQLSyncSts(gson.toJson(usersynclist));
         // Reload the Main Activity
         reloadActivity();
      }
   } 
   
   catch (JSONException e) 
   {
      // TODO Auto-generated catch block
      e.printStackTrace();
   }
}



// Method to inform remote MySQL DB about completion of Sync activity
public void updateMySQLSyncSts(String json) 
{
   System.out.println(json);
   AsyncHttpClient client = new AsyncHttpClient();
   RequestParams params = new RequestParams();
   params.put("syncsts", json);
   // Make Http call to updatesyncsts.php with JSON parameter which has Sync statuses of Users
   client.post("http://localhost/potholespot/web/mysqlsqlitesync/updatesyncsts.php", params, new AsyncHttpResponseHandler() {
      @Override
      public void onSuccess(String response) {
         Toast.makeText(getApplicationContext(),   "MySQL DB has been informed about Sync activity", Toast.LENGTH_LONG).show();
      }

      @Override
      public void onFailure(int statusCode, Throwable error, String content) {
            Toast.makeText(getApplicationContext(), "Error Occured", Toast.LENGTH_LONG).show();
      }
   });
}

//Reload MainActivity
public void reloadActivity() 
{
   Intent objIntent = new Intent(getApplicationContext(), MainActivity.class);
   startActivity(objIntent);
}








//class life cycle

@Override
protected void onPostCreate(Bundle paramBundle)
{
  super.onPostCreate(paramBundle);
  
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

public void startAccelerometerSensor()
{
   mSensorManager.registerListener(this, mSensorManager.getDefaultSensor
         (Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
 lastTime = System.currentTimeMillis();
   
   }

public void stopAccelerometerSensor()
{
   mSensorManager.unregisterListener(this);
   
   }

public SensorEvent getmLastRecordedEvent()
{
   return mLastRecordedEvent;
}

public void setmLastRecordedEvent(SensorEvent mLastRecordedEvent)
{
   this.mLastRecordedEvent = mLastRecordedEvent;
}

  
  
}
