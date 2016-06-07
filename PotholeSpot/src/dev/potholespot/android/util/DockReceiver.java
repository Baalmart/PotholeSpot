package dev.potholespot.android.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import dev.potholespot.android.logger.GPSLoggerService;

public class DockReceiver extends BroadcastReceiver
{
   private final static String TAG = "PRIM.DockReceiver";

   @Override
   public void onReceive(final Context context, final Intent intent)
   {
      final String action = intent.getAction();
      if (action.equals(Intent.ACTION_DOCK_EVENT))
      {
         final Bundle extras = intent.getExtras();
         boolean start = false;
         boolean stop = false;
         if (extras != null && extras.containsKey(Intent.EXTRA_DOCK_STATE))
         {
            final int dockstate = extras.getInt(Intent.EXTRA_DOCK_STATE, -1);
            if (dockstate == Intent.EXTRA_DOCK_STATE_CAR)
            {
               start = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.LOGATDOCK, false);
            }
            else if (dockstate == Intent.EXTRA_DOCK_STATE_UNDOCKED)
            {
               stop = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.STOPATUNDOCK, false);
            }
         }
         if (start)
         {
            final Intent serviceIntent = new Intent(Constants.SERVICENAME);
            serviceIntent.putExtra(GPSLoggerService.COMMAND, GPSLoggerService.EXTRA_COMMAND_START);
            context.startService(serviceIntent);
         }
         else if (stop)
         {
            final Intent serviceIntent = new Intent(Constants.SERVICENAME);
            serviceIntent.putExtra(GPSLoggerService.COMMAND, GPSLoggerService.EXTRA_COMMAND_STOP);
            context.startService(serviceIntent);
         }
      }
      else
      {
         Log.w(TAG, "PotholeSpot's BootReceiver received " + action + ", but it's only able to respond to " + Intent.ACTION_BOOT_COMPLETED + ". This shouldn't happen !");
      }
   }
}
