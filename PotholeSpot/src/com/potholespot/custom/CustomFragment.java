package com.potholespot.custom;

import dev.potholespot.android.db.AndroidDatabaseManager;
import dev.potholespot.android.db.DatabaseHelper;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;



public class CustomFragment extends Fragment implements SensorEventListener, View.OnClickListener
{
   
   //global variables
   
   private SensorManager mSensorManager;
   Sensor mAccelerometer;

   
  public void onClick(View paramView)
  {
  }

  public View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup, Bundle paramBundle)
  {
    return super.onCreateView(paramLayoutInflater, paramViewGroup, paramBundle);
  }

  public View setTouchNClick(View paramView)
  {
    paramView.setOnClickListener(this);
    paramView.setOnTouchListener(CustomActivity.TOUCH);
    return paramView;
  }
  
  @Override
public void onCreate(Bundle savedInstanceState) 
  {
     super.onCreate(savedInstanceState);
  
     //dbm = new DatabaseHelper(AndroidDatabaseManager.this);  
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
   //mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
  }

  @Override
  public void onDestroy() 
  {
    super.onDestroy();
   // mSensorManager.unregisterListener(this); 
  }

  @Override
  public void onStop() 
  {
        super.onStop();
       // mSensorManager.unregisterListener(this);         
  }

@Override
public void onSensorChanged(SensorEvent event)
{
   // TODO Auto-generated method stub
   
}

@Override
public void onAccuracyChanged(Sensor sensor, int accuracy)
{
   // TODO Auto-generated method stub
   
}
  
  
  
}