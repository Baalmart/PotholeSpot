package dev.potholespot.android.viewer.map;

import dev.potholespot.uganda.R;
import dev.potholespot.android.util.Constants;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;


public class CommonLoggerMap extends Activity
{
   private static final String TAG = "PRIM.CommonLoggerMap";

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      Intent myIntent = getIntent();
      Intent realIntent;
     

      Class<?> mapClass = OsmLoggerMap.class;
      int provider = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString
            (Constants.MAPPROVIDER, "" + Constants.OSM)).intValue();
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
            Log.e(TAG, "Fault in value " + provider + " as MapProvider, defaulting to MapQuest.");
            break;
      }
      
      if( myIntent != null )
      {
         realIntent = new Intent(myIntent.getAction(), myIntent.getData(), this, mapClass);
         realIntent.putExtras(myIntent);
      }
      
      else
      {
         realIntent = new Intent(this, mapClass);
         realIntent.putExtras(myIntent);
      }
      startActivity(realIntent);
      finish();
   }
   
  
   
   
}
