package dev.potholespot.android.actions;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import dev.potholespot.android.db.Pspot.TracksColumns;
import dev.potholespot.uganda.R;
import dev.potholespot.android.actions.NameRoute;

/**
 * Empty Activity that pops up the dialog to name the track
 * 
 * @version $Id$
 * @author Martin Bbaale
 */
public class NameRoute extends Activity
{
   private static final int DIALOG_TRACKNAME = 23;

   protected static final String TAG = "pspot.NameTrack";

   private EditText mTrackNameView;
   private boolean paused;
   Uri mTrackUri;
   Notification notification;
   NotificationManager nManager;

   private final DialogInterface.OnClickListener mTrackNameDialogListener = new DialogInterface.OnClickListener()
      {
         @Override
         public void onClick(final DialogInterface dialog, final int which)
         {
            String trackName = null;
            switch (which)
            {
               case DialogInterface.BUTTON_POSITIVE:
                  trackName = NameRoute.this.mTrackNameView.getText().toString();
                  final ContentValues values = new ContentValues();
                  values.put(TracksColumns.NAME, trackName);
                  getContentResolver().update(NameRoute.this.mTrackUri, values, null, null);
                  clearNotification();
                  break;
               case DialogInterface.BUTTON_NEUTRAL:
                  startDelayNotification();
                  break;
               case DialogInterface.BUTTON_NEGATIVE:
                  clearNotification();
                  break;
               default:
                  Log.e(TAG, "Unknown option ending dialog:" + which);
                  break;
            }
            finish();
         }

      };

   private void clearNotification()
   {

      final NotificationManager noticationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      noticationManager.cancel(R.layout.namedialog);
   }

   /*
   @SuppressWarnings("deprecation")
   private void startDelayNotification()
   {
      final int resId = R.string.dialog_routename_title;
      final int icon = R.drawable.ic_maps_indicator_current_position;
      final CharSequence tickerText = getResources().getString(resId);
      final long when = System.currentTimeMillis();

      final Notification nameNotification = new Notification(icon, tickerText, when);
      nameNotification.flags |= Notification.FLAG_AUTO_CANCEL;

      final CharSequence contentTitle = getResources().getString(R.string.app_name);
      final CharSequence contentText = getResources().getString(resId);

      final Intent notificationIntent = new Intent(this, NameRoute.class);
      notificationIntent.setData(this.mTrackUri);

      final PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
      nameNotification.setLatestEventInfo(this, contentTitle, contentText, contentIntent);

      final NotificationManager noticationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      noticationManager.notify(R.layout.namedialog, nameNotification);
   }
   */
   
   private void startDelayNotification()
   {
      int resId = R.string.dialog_routename_title;
      int icon = R.drawable.ic_maps_indicator_current_position;
      CharSequence tickerText = getResources().getString( resId );
      long when = System.currentTimeMillis();
      
      Intent notificationIntent = new Intent( this, NameRoute.class );
      notificationIntent.setData( mTrackUri );
      PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, Intent.FLAG_ACTIVITY_NEW_TASK );
      
      Notification.Builder builder = new Notification.Builder(this);
      
      builder.setAutoCancel(true);
      builder.setTicker(tickerText);
      builder.setSmallIcon(icon);
      builder.setWhen(when);
      builder.setContentTitle(getResources().getString(R.string.app_name ));               
      builder.setContentText(getResources().getString(resId));
     
      builder.setContentIntent(contentIntent);

      notification = builder.build();
      nManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
      nManager.notify(R.layout.namedialog, notification);
   } 

   @Override
   protected void onCreate(final Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setVisible(false);
      this.paused = false;
      this.mTrackUri = getIntent().getData();
   }

   @Override
   protected void onPause()
   {
      super.onPause();
      this.paused = true;
   }

   /*
    * (non-Javadoc)
    * @see com.google.android.maps.MapActivity#onPause()
    */
   @Override
   protected void onResume()
   {
      super.onResume();
      if (this.mTrackUri != null)
      {
         showDialog(DIALOG_TRACKNAME);
      }
      else
      {
         Log.e(TAG, "Naming track without a track URI supplied.");
         finish();
      }
   }

   @SuppressWarnings("deprecation")
   @Override
   protected Dialog onCreateDialog(final int id)
   {
      Dialog dialog = null;
      LayoutInflater factory = null;
      View view = null;
      Builder builder = null;
      switch (id)
      {
         case DIALOG_TRACKNAME:
            builder = new AlertDialog.Builder(this);
            factory = LayoutInflater.from(this);
            view = factory.inflate(R.layout.namedialog, null);
            this.mTrackNameView = (EditText) view.findViewById(R.id.nameField);
            builder.setTitle(R.string.dialog_routename_title).setMessage(R.string.dialog_routename_message).setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(R.string.btn_okay, this.mTrackNameDialogListener).setNeutralButton(R.string.btn_skip, this.mTrackNameDialogListener)
                  .setNegativeButton(R.string.btn_cancel, this.mTrackNameDialogListener).setView(view);
            dialog = builder.create();
            dialog.setOnDismissListener(new OnDismissListener()
               {
                  @Override
                  public void onDismiss(final DialogInterface dialog)
                  {
                     if (!NameRoute.this.paused)
                     {
                        finish();
                     }
                  }
               });
            return dialog;
         default:
            return super.onCreateDialog(id);
      }
   }

   @SuppressWarnings("deprecation")
   @Override
   protected void onPrepareDialog(final int id, final Dialog dialog)
   {
      switch (id)
      {
         case DIALOG_TRACKNAME:
            String trackName;
            final Calendar c = Calendar.getInstance();
            trackName = String.format(getString(R.string.dialog_routename_default), c, c, c, c, c);
            this.mTrackNameView.setText(trackName);
            this.mTrackNameView.setSelection(0, trackName.length());
            break;
         default:
            super.onPrepareDialog(id, dialog);
            break;
      }
   }
}
