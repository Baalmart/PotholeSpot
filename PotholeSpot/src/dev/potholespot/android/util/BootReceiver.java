package dev.potholespot.android.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver
{
   private final static String TAG = "PRIM.BootReceiver";

   @Override
   public void onReceive(final Context context, final Intent intent)
   {
      //      Log.d( TAG, "BootReceiver.onReceive(), probably ACTION_BOOT_COMPLETED" );
      final String action = intent.getAction();

      // start on BOOT_COMPLETED
      if (action.equals(Intent.ACTION_BOOT_COMPLETED))
      {
         //         Log.d( TAG, "BootReceiver received ACTION_BOOT_COMPLETED" );

         // check in the settings if we need to auto start
         final boolean startImmidiatly = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.STARTUPATBOOT, false);

         if (startImmidiatly)
         {
            //            Log.d( TAG, "Starting LoggerMap activity..." );
            context.startService(new Intent(Constants.SERVICENAME));
         }
         else
         {
            Log.i(TAG, "Not starting Logger Service. Adjust the settings if you wanted this !");
         }
      }
      else
      {
         // this shouldn't happen !
         Log.w(TAG, "PotholeSpot's BootReceiver received " + action + ", but it's only able to respond to " + Intent.ACTION_BOOT_COMPLETED + ". This shouldn't happen !");
      }
   }
}
