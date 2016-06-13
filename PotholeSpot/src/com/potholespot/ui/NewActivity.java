package com.potholespot.ui;

import java.util.Timer;
import java.util.TimerTask;

import com.potholespot.MainActivity;

import dev.potholespot.android.actions.PotholeSpotLabel;
import dev.potholespot.android.db.DatabaseHelper;
import dev.potholespot.android.db.Pspot.Xyz;
import dev.potholespot.uganda.R;
import dev.potholespot.uganda.R.drawable;
import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

@SuppressLint("InflateParams")
public class NewActivity extends Fragment 
{
   DatabaseHelper mDbHelper;
   public long lastTime;
   public long currentTime;
   PotholeSpotLabel potholeSpot;    
   String TAG = "Pspot.NewActivity";
   MainActivity mActivity;
   
   
  private void setupView(View paramView)
  {
    final View localView = paramView.findViewById(R.id.vSwitch);
    
    //Drawable right = localView.getBackground().getCurrent();
    /*Drawable left = localView.getBackground();*/
    
    localView.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View paramAnonymousView)
      {
        if (localView.getTag() == null)
        {
          localView.setBackgroundResource(R.drawable.swith_up_right);
         // mActivity.startAccelerometerSensor();
          //callAsynchronousTask();
          localView.setTag("gps");          
          //return;
        }      
        
        else 
           
        {
        localView.setBackgroundResource(R.drawable.swith_up_left);
       // mActivity.stopAccelerometerSensor();
        localView.setTag(null);
        }
       
        
        
    
      }
    });
    
    
    if (localView.getTag() == null)
    {
       
       callAsynchronousTask();
    }     
      
  }

  @Override
public View onCreateView(LayoutInflater paramLayoutInflater, 
		  ViewGroup paramViewGroup, Bundle paramBundle)
  {
    View localView = paramLayoutInflater.inflate(R.layout.new_activity, null);
    setupView(localView);
    return localView;
  }
  
  
  //the continuous collection of data
  
  public void callAsynchronousTask() 
  {
      final Handler handler = new Handler();
      Timer timer = new Timer();
      TimerTask doAsynchronousTask = new TimerTask() 
      {       
          @Override
          public void run() {
              handler.post(new Runnable() {
                  public void run() {       
                      try {
                         
                         //mLastRecordedEvent = ((MainActivity)getActivity()).getmLastRecordedEvent();
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
      timer.schedule(doAsynchronousTask, 0, 20); //execute in every 20 ms
  }
  
  

private class Storing_xyz extends AsyncTask<String, Void, String> 
{
   @Override
   protected String doInBackground(String... params) 
   {               /*storeAllAccelerationValues(mLastRecordedEvent);*/              
              SensorEvent mLastContinousEvent = ((MainActivity)getActivity()).getmLastRecordedEvent();
              storeAllAccelerationValues(mLastContinousEvent);          
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


//storage of all acceleration values inside the database
public void storeAllAccelerationValues(SensorEvent event) 
{ 
 float[] value = event.values;
   
   float xVal = value[0];
   float yVal = value[1];
   float zVal = value[2];
   
   float accelationSquareRoot = (xVal*xVal + yVal*yVal + zVal*zVal) 
         / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
  
   long actualTime = System.currentTimeMillis();
   
   /*if (accelationSquareRoot >= 1) 
   {
   
      //the time interval is 2x10 power 9
      if(actualTime - lastTime < 2000000000) 
      {*/
 
   //code block stores accelerometer values in xyz table.
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
       
    //code block stores accelerometer values in DTW table after DTW algorithm Performs labelling.
    
    potholeSpot.segmentStream(event.values[0], event.values[1], event.values[2]);  
}

  
}
