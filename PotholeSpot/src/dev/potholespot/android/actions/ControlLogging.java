package dev.potholespot.android.actions;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import dev.potholespot.android.db.Pspot.Tracks;
import dev.potholespot.android.logger.GPSLoggerServiceManager;
import dev.potholespot.android.util.Constants;
import dev.potholespot.uganda.R;

/**
 * Empty Activity that pops up the dialog to name the track
 * 
 * @version $Id$
 * @author Martin Bbaale
 */
public class ControlLogging extends Activity
{
   private static final int DIALOG_LOGCONTROL = 26;
   private static final String TAG = "OGT.ControlTracking";

   private GPSLoggerServiceManager mLoggerServiceManager;
   private Button start;
   private Button pause;
   private Button resume;
   private Button stop;
   private boolean paused;

   private final View.OnClickListener mLoggingControlListener = new View.OnClickListener()
      {
         @Override
         public void onClick(final View v)
         {
            final int id = v.getId();
            final Intent intent = new Intent();
            switch (id)
            {
               case R.id.logcontrol_start:
                  final long loggerTrackId = ControlLogging.this.mLoggerServiceManager.startGPSLogging(null);

                  // Start a naming of the track
                  final Intent namingIntent = new Intent(ControlLogging.this, NameRoute.class);
                  namingIntent.setData(ContentUris.withAppendedId(Tracks.CONTENT_URI, loggerTrackId));
                  startActivity(namingIntent);

                  // Create data for the caller that a new track has been started
                  final ComponentName caller = ControlLogging.this.getCallingActivity();
                  if (caller != null)
                  {
                     intent.setData(ContentUris.withAppendedId(Tracks.CONTENT_URI, loggerTrackId));
                     setResult(RESULT_OK, intent);
                  }
                  break;
               case R.id.logcontrol_pause:
                  ControlLogging.this.mLoggerServiceManager.pauseGPSLogging();
                  setResult(RESULT_OK, intent);
                  break;
               case R.id.logcontrol_resume:
                  ControlLogging.this.mLoggerServiceManager.resumeGPSLogging();
                  setResult(RESULT_OK, intent);
                  break;
               case R.id.logcontrol_stop:
                  ControlLogging.this.mLoggerServiceManager.stopGPSLogging();
                  setResult(RESULT_OK, intent);
                  break;
               default:
                  setResult(RESULT_CANCELED, intent);
                  break;
            }
            finish();
         }
      };
   private final OnClickListener mDialogClickListener = new OnClickListener()
      {
         @Override
         public void onClick(final DialogInterface dialog, final int which)
         {
            setResult(RESULT_CANCELED, new Intent());
            finish();
         }
      };

   @Override
   protected void onCreate(final Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);

      setVisible(false);
      this.paused = false;
      this.mLoggerServiceManager = new GPSLoggerServiceManager(this);
   }

   @Override
   protected void onResume()
   {
      super.onResume();
      this.mLoggerServiceManager.startup(this, new Runnable()
         {
            @SuppressWarnings("deprecation")
            @Override
            public void run()
            {
               showDialog(DIALOG_LOGCONTROL);
            }
         });
   }

   @Override
   protected void onPause()
   {
      super.onPause();
      this.mLoggerServiceManager.shutdown(this);
      this.paused = true;
   }

   @Override
   protected Dialog onCreateDialog(final int id)
   {
      Dialog dialog = null;
      LayoutInflater factory = null;
      View view = null;
      Builder builder = null;
      switch (id)
      {
         case DIALOG_LOGCONTROL:
            builder = new AlertDialog.Builder(this);
            factory = LayoutInflater.from(this);
            view = factory.inflate(R.layout.logcontrol, null);
            builder.setTitle(R.string.dialog_tracking_title).setIcon(android.R.drawable.ic_dialog_alert).setNegativeButton(R.string.btn_cancel, this.mDialogClickListener).setView(view);
            dialog = builder.create();
            this.start = (Button) view.findViewById(R.id.logcontrol_start);
            this.pause = (Button) view.findViewById(R.id.logcontrol_pause);
            this.resume = (Button) view.findViewById(R.id.logcontrol_resume);
            this.stop = (Button) view.findViewById(R.id.logcontrol_stop);
            this.start.setOnClickListener(this.mLoggingControlListener);
            this.pause.setOnClickListener(this.mLoggingControlListener);
            this.resume.setOnClickListener(this.mLoggingControlListener);
            this.stop.setOnClickListener(this.mLoggingControlListener);
            dialog.setOnDismissListener(new OnDismissListener()
               {
                  @Override
                  public void onDismiss(final DialogInterface dialog)
                  {
                     if (!ControlLogging.this.paused)
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

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
    */
   @Override
   protected void onPrepareDialog(final int id, final Dialog dialog)
   {
      switch (id)
      {
         case DIALOG_LOGCONTROL:
            updateDialogState(this.mLoggerServiceManager.getLoggingState());
            break;
         default:
            break;
      }
      super.onPrepareDialog(id, dialog);
   }

   private void updateDialogState(final int state)
   {
      switch (state)
      {
         case Constants.STOPPED:
            this.start.setEnabled(true);
            this.pause.setEnabled(false);
            this.resume.setEnabled(false);
            this.stop.setEnabled(false);
            break;
         case Constants.LOGGING:
            this.start.setEnabled(false);
            this.pause.setEnabled(true);
            this.resume.setEnabled(false);
            this.stop.setEnabled(true);
            break;
         case Constants.PAUSED:
            this.start.setEnabled(false);
            this.pause.setEnabled(false);
            this.resume.setEnabled(true);
            this.stop.setEnabled(true);
            break;
         default:
            Log.w(TAG, String.format("State %d of logging, enabling and hope for the best....", state));
            this.start.setEnabled(false);
            this.pause.setEnabled(false);
            this.resume.setEnabled(false);
            this.stop.setEnabled(false);
            break;
      }
   }
}
