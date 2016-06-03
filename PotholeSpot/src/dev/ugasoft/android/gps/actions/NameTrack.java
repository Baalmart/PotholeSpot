package dev.ugasoft.android.gps.actions;

import java.util.Calendar;

import dev.baalmart.potholespot.R;
import dev.ugasoft.android.gps.db.Prim.Tracks;
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

/**
 * Empty Activity that pops up the dialog to name the track
 *
 * @version $Id$
 * @author Martin Bbaale
 */
public class NameTrack extends Activity
{
   private static final int DIALOG_TRACKNAME = 23;

   protected static final String TAG = "PRIM.NameTrack";

   private EditText mTrackNameView;
   private boolean paused;
   Uri mTrackUri;

   private final DialogInterface.OnClickListener mTrackNameDialogListener = new DialogInterface.OnClickListener()
   {
      @Override
      public void onClick( DialogInterface dialog, int which )
      {
         String trackName = null;
         switch( which )
         {
            case DialogInterface.BUTTON_POSITIVE:
               trackName = mTrackNameView.getText().toString();        
               ContentValues values = new ContentValues();
               values.put( Tracks.NAME, trackName );
               getContentResolver().update( mTrackUri, values, null, null );
               clearNotification();
               break;
            case DialogInterface.BUTTON_NEUTRAL:
               startDelayNotification();
               break;
            case DialogInterface.BUTTON_NEGATIVE:
               clearNotification();
               break;
            default:
               Log.e( TAG, "Unknown option ending dialog:"+which );
               break;
         }
         finish();
      }

   };
   
   
   private void clearNotification()
   {

      NotificationManager noticationManager = (NotificationManager) this.getSystemService( Context.NOTIFICATION_SERVICE );
      noticationManager.cancel( R.layout.namedialog );
   }
   
   @SuppressWarnings("deprecation")
   private void startDelayNotification()
   {
      int resId = R.string.dialog_routename_title;
      int icon = R.drawable.ic_maps_indicator_current_position;
      CharSequence tickerText = getResources().getString( resId );
      long when = System.currentTimeMillis();
      
      Notification nameNotification = new Notification( icon, tickerText, when );
      nameNotification.flags |= Notification.FLAG_AUTO_CANCEL;
      
      CharSequence contentTitle = getResources().getString( R.string.app_name );
      CharSequence contentText = getResources().getString( resId );
      
      Intent notificationIntent = new Intent( this, NameTrack.class );
      notificationIntent.setData( mTrackUri );
      
      PendingIntent contentIntent = PendingIntent.getActivity( this, 0, notificationIntent, Intent.FLAG_ACTIVITY_NEW_TASK );
      nameNotification.setLatestEventInfo( this, contentTitle, contentText, contentIntent );
      
      NotificationManager noticationManager = (NotificationManager) this.getSystemService( Context.NOTIFICATION_SERVICE );
      noticationManager.notify( R.layout.namedialog, nameNotification );
   }
   
   @Override
   protected void onCreate( Bundle savedInstanceState )
   {
      super.onCreate( savedInstanceState );
      this.setVisible( false );
      paused = false;
      mTrackUri = this.getIntent().getData();
   }
   
   @Override
   protected void onPause()
   {
      super.onPause();
      paused = true;
   }
   
   /*
    * (non-Javadoc)
    * @see com.google.android.maps.MapActivity#onPause()
    */
   @Override
   protected void onResume()
   {
      super.onResume();
      if(  mTrackUri != null )
      {
         showDialog( DIALOG_TRACKNAME );
      }
      else
      {
         Log.e(TAG, "Naming track without a track URI supplied." );
         finish();
      }
   }
   
   @SuppressWarnings("deprecation")
   @Override
   protected Dialog onCreateDialog( int id )
   {
      Dialog dialog = null;
      LayoutInflater factory = null;
      View view = null;
      Builder builder = null;
      switch (id)
      {
         case DIALOG_TRACKNAME:
            builder = new AlertDialog.Builder( this );
            factory = LayoutInflater.from( this );
            view = factory.inflate( R.layout.namedialog, null );
            mTrackNameView = (EditText) view.findViewById( R.id.nameField );
            builder
               .setTitle( R.string.dialog_routename_title )
               .setMessage( R.string.dialog_routename_message )
               .setIcon( android.R.drawable.ic_dialog_alert )
               .setPositiveButton( R.string.btn_okay, mTrackNameDialogListener )
               .setNeutralButton( R.string.btn_skip, mTrackNameDialogListener )
               .setNegativeButton( R.string.btn_cancel, mTrackNameDialogListener )
               .setView( view );
            dialog = builder.create();
            dialog.setOnDismissListener( new OnDismissListener()
            {
               @Override
               public void onDismiss( DialogInterface dialog )
               {
                  if( !paused )
                  {
                     finish();
                  }
               }
            });
            return dialog;
         default:
            return super.onCreateDialog( id );
      }
   }
   
   @SuppressWarnings("deprecation")
   @Override
   protected void onPrepareDialog( int id, Dialog dialog )
   {
      switch (id)
      {
         case DIALOG_TRACKNAME:
            String trackName;
            Calendar c = Calendar.getInstance();
            trackName = String.format( getString( R.string.dialog_routename_default ), c, c, c, c, c );
            mTrackNameView.setText( trackName );
            mTrackNameView.setSelection( 0, trackName.length() );
            break;
         default:
            super.onPrepareDialog( id, dialog );
            break;
      }
   }   
}
   