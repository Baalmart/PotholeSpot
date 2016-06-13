package com.potholespot;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
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

import com.potholespot.custom.CustomActivity;
import com.potholespot.ui.History;
import com.potholespot.ui.NewActivity;
import com.potholespot.ui.Routes;
import com.potholespot.ui.Workout;

import dev.potholespot.uganda.R;
import dev.potholespot.android.db.AndroidDatabaseManager;
import dev.potholespot.android.db.DatabaseHelper;
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
        return new NewActivity();
      if (paramInt == 1)
        return new Routes();
      if (paramInt == 2)
        return new Workout();
      return new History();
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
   mSensorManager.unregisterListener(this); mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

   mSensorManager.registerListener(this, mSensorManager.getDefaultSensor
                   (Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    lastTime = System.currentTimeMillis();
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


  
  
}
