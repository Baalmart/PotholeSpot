package dev.potholespot.android.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import dev.potholespot.android.actions.ControlLogging;
import dev.potholespot.android.actions.ManualMode;
import dev.potholespot.android.util.Constants;
import dev.potholespot.uganda.R;

/**
 * An App Widget for on the home screen to control logging with a start, pause, resume and stop
 * 
 * @version $Id$
 * @author Martin Bbaale
 */
public class ControlWidgetProvider extends AppWidgetProvider
{
   private static final int BUTTON_TRACKINGCONTROL = 2;
   private static final int BUTTON_INSERTNOTE = 3;
   private static final String TAG = "OGT.ControlWidgetProvider";

   static final ComponentName THIS_APPWIDGET = new ComponentName("dev.potholespot.uganda", "dev.potholespot.android.widget.ControlWidgetProvider");

   //dev.ugasoft.android.gps.widget
   private static int mState;

   public ControlWidgetProvider()
   {
      super();
   }

   @Override
   public void onEnabled(final Context context)
   {
      //      Log.d(TAG, "onEnabled() ");
      super.onEnabled(context);

      context.startService(new Intent(Constants.SERVICENAME));
   }

   @Override
   public void onDisabled(final Context context)
   {
      //      Log.d(TAG, "onDisabled() ");
   }

   @Override
   public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds)
   {
      //      Log.d(TAG, "onDisabled() ");
      // Update each requested appWidgetId
      final RemoteViews view = buildUpdate(context, -1);

      for (final int appWidgetId : appWidgetIds)
      {
         appWidgetManager.updateAppWidget(appWidgetId, view);
      }
   }

   /**
    * Load image for given widget and build {@link RemoteViews} for it.
    */
   static RemoteViews buildUpdate(final Context context, final int appWidgetId)
   {
      final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.control_appwidget);
      views.setOnClickPendingIntent(R.id.widget_insertnote_enabled, getLaunchPendingIntent(context, appWidgetId, BUTTON_INSERTNOTE));
      views.setOnClickPendingIntent(R.id.widget_trackingcontrol, getLaunchPendingIntent(context, appWidgetId, BUTTON_TRACKINGCONTROL));
      updateButtons(views, context);
      return views;
   }

   /**
    * Load image for given widget and build {@link RemoteViews} for it.
    */
   private static void updateButtons(final RemoteViews views, final Context context)
   {
      //      Log.d(TAG, "Updated the remote views to state " + mState);
      switch (mState)
      {
         case Constants.LOGGING:
            setEnableInsertNote(views, true);
            break;
         case Constants.PAUSED:
            setEnableInsertNote(views, false);
            break;
         case Constants.STOPPED:
            setEnableInsertNote(views, false);
            break;
         case Constants.UNKNOWN:
            setEnableInsertNote(views, false);
            break;
         default:
            Log.w(TAG, "Unknown logging state for widget: " + mState);
            break;
      }
   }

   private static void setEnableInsertNote(final RemoteViews views, final boolean enabled)
   {
      if (enabled)
      {
         views.setViewVisibility(R.id.widget_insertnote_enabled, View.VISIBLE);
         views.setViewVisibility(R.id.widget_insertnote_disabled, View.GONE);
      }
      else
      {
         views.setViewVisibility(R.id.widget_insertnote_enabled, View.GONE);
         views.setViewVisibility(R.id.widget_insertnote_disabled, View.VISIBLE);
      }
   }

   /**
    * Creates PendingIntent to notify the widget of a button click.
    * 
    * @param context
    * @param appWidgetId
    * @return
    */
   private static PendingIntent getLaunchPendingIntent(final Context context, final int appWidgetId, final int buttonId)
   {
      final Intent launchIntent = new Intent();
      launchIntent.setClass(context, ControlWidgetProvider.class);
      launchIntent.addCategory(Intent.CATEGORY_ALTERNATIVE);
      launchIntent.setData(Uri.parse("custom:" + buttonId));
      final PendingIntent pi = PendingIntent.getBroadcast(context, 0 /* no requestCode */, launchIntent, 0 /*
                                                                                                            * no flags
                                                                                                            */);
      return pi;
   }

   /**
    * Receives and processes a button pressed intent or state change.
    * 
    * @param context
    * @param intent Indicates the pressed button.
    */
   @Override
   public void onReceive(final Context context, final Intent intent)
   {
      //      Log.d(TAG, "Did recieve intent with action: " + intent.getAction());
      super.onReceive(context, intent);
      final String action = intent.getAction();
      if (Constants.LOGGING_STATE_CHANGED_ACTION.equals(action))

      {
         mState = intent.getIntExtra(Constants.EXTRA_LOGGING_STATE, Constants.UNKNOWN);
         updateWidget(context);
      }

      else if (intent.hasCategory(Intent.CATEGORY_ALTERNATIVE))
      {
         final Uri data = intent.getData();
         final int buttonId = Integer.parseInt(data.getSchemeSpecificPart());
         if (buttonId == BUTTON_TRACKINGCONTROL)
         {
            final Intent controlIntent = new Intent(context, ControlLogging.class);
            controlIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(controlIntent);
         }
         else if (buttonId == BUTTON_INSERTNOTE)
         {
            final Intent noteIntent = new Intent(context, ManualMode.class);
            noteIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(noteIntent);
         }
      }

      else
      {
         // Don't fall-through to updating the widget.  The Intent
         // was something unrelated or that our super class took
         // care of.
         return;
      }
      // State changes fall through
      updateWidget(context);
   }

   /**
    * Updates the widget when something changes, or when a button is pushed.
    * 
    * @param context
    */
   public static void updateWidget(final Context context)
   {
      final RemoteViews views = buildUpdate(context, -1);
      // Update specific list of appWidgetIds if given, otherwise default to all
      final AppWidgetManager gm = AppWidgetManager.getInstance(context);
      gm.updateAppWidget(THIS_APPWIDGET, views);
   }

}
