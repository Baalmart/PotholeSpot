package dev.ugasoft.android.gps.viewer.map;

import java.util.concurrent.Semaphore;

import com.prim.MainActivity;
import com.prim.ui.MainFragment;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;
import dev.ugasoft.android.gps.db.Prim.Media;
import dev.ugasoft.android.gps.db.Prim.Tracks;
import dev.ugasoft.android.gps.logger.GPSLoggerServiceManager;
import dev.ugasoft.android.gps.util.UnitsI18n;
import dev.ugasoft.android.gps.viewer.map.overlay.BitmapSegmentsOverlay;

public class LoggerLabelHelper
{ 
   private double mAverageSpeed = 33.33d / 3d;
   private long mLabelId = -1;
   private long mLastSegment = -1;
   private UnitsI18n mUnits;
   private WakeLock mWakeLock = null;
   private SharedPreferences mSharedPreferences;
   private GPSLoggerServiceManager mLoggerServiceManager;

   private Handler mHandler;

   //menus
   private static final int MENU_TRACKING = 2;
   private static final int MENU_SHARE = 13;

   
   private OnSharedPreferenceChangeListener mSharedPreferenceChangeListener;
   private ContentObserver mLabelObserver;
   private ContentObserver mXyzObserver;
   private ContentObserver mLocationsObserver;
   private Uri mSelected;
   

   /**
    * Run after the ServiceManager completes the binding to the remote service
    */
   private Runnable mServiceConnected;
   private Runnable speedCalculator;
   private MainActivity mLoggerLabel;
   private float mSpeed;
   
   private static final String TAG = "OGT.LoggerLabel";
   
   
   public LoggerLabelHelper(MainActivity mainActivity)
   {
      mLoggerLabel = mainActivity;
   }

   protected void onCreate(Bundle load)
   {
      Context context = null;
      mLoggerServiceManager = new GPSLoggerServiceManager(context); 
      
      mUnits = new UnitsI18n(mLoggerLabel);
      mLoggerServiceManager = new GPSLoggerServiceManager(mLoggerLabel);

      final Semaphore calulatorSemaphore = new Semaphore(0);
      Thread calulator = new Thread("OverlayCalculator")
      {
         @Override
         public void run()
         {
            Looper.prepare();
            mHandler = new Handler();
            calulatorSemaphore.release();
            Looper.loop();
         }
      };   
      
      calulator.start();
      try
      {
         calulatorSemaphore.acquire();
      }
      catch (InterruptedException e)
      {
         Log.e(TAG, "Failed waiting for a semaphore", e);
      }
      mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mLoggerLabel);
     // mBitmapSegmentsOverlay = new BitmapSegmentsOverlay(mLoggerMap, mHandler);
      //createListeners();
      //onRestoreInstanceState(load);
   
   }
   
   
   protected void onResume()
   {
      
      mLoggerServiceManager.startup(mLoggerLabel, mServiceConnected);
      mSharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
    //  mUnits.setUnitsChangeListener(mUnitsChangeListener);
      //updateTitleBar();
     // updateBlankingBehavior();

      if (mLabelId >= 0)
      {
         ContentResolver resolver = mLoggerLabel.getContentResolver();
         Uri trackUri = Uri.withAppendedPath(Tracks.CONTENT_URI, mLabelId + "/segments");
         Uri lastSegmentUri = Uri.withAppendedPath(Tracks.CONTENT_URI, mLabelId + "/segments/" + mLastSegment + "/waypoints");
         Uri mediaUri = ContentUris.withAppendedId(Media.CONTENT_URI, mLabelId);

         resolver.unregisterContentObserver(this.mLabelObserver);
         resolver.unregisterContentObserver(this.mLocationsObserver);
         resolver.unregisterContentObserver(this.mXyzObserver);
         
         resolver.registerContentObserver(trackUri, false, this.mLabelObserver);
         resolver.registerContentObserver(lastSegmentUri, true, this.mLocationsObserver);
         resolver.registerContentObserver(mediaUri, true, this.mXyzObserver);
      }
    

      
     // updateTrackNumbers();
      
     // mLoggerMap.executePostponedActions();
   }
   

}
