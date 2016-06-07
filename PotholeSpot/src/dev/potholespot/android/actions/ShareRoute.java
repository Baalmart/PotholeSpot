package dev.potholespot.android.actions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.Toast;
import dev.potholespot.android.actions.tasks.GpxCreator;
import dev.potholespot.android.actions.tasks.GpxSharing;
import dev.potholespot.android.actions.tasks.JogmapSharing;
import dev.potholespot.android.actions.tasks.KmzCreator;
import dev.potholespot.android.actions.tasks.KmzSharing;
import dev.potholespot.android.actions.tasks.OsmSharing;
import dev.potholespot.android.actions.utils.ProgressListener;
import dev.potholespot.android.actions.utils.StatisticsCalulator;
import dev.potholespot.android.actions.utils.StatisticsDelegate;
import dev.potholespot.android.breadcrumbs.BreadcrumbsService;
import dev.potholespot.android.breadcrumbs.BreadcrumbsService.LocalBinder;
import dev.potholespot.android.db.Pspot.TracksColumns;
import dev.potholespot.android.util.Constants;
import dev.potholespot.android.util.UnitsI18n;
import dev.potholespot.android.viewer.map.LoggerMap;
import dev.potholespot.uganda.R;

public class ShareRoute extends Activity implements StatisticsDelegate
{
   private static final String TAG = "p" + "spot.ShareTrack";

   private static final int EXPORT_TYPE_KMZ = 0;
   private static final int EXPORT_TYPE_GPX = 1;
   private static final int EXPORT_TYPE_TEXTLINE = 2;
   private static final int EXPORT_TARGET_SAVE = 0;
   private static final int EXPORT_TARGET_SEND = 1;
   private static final int EXPORT_TARGET_JOGRUN = 2;
   private static final int EXPORT_TARGET_OSM = 3;
   private static final int EXPORT_TARGET_BREADCRUMBS = 4;
   private static final int EXPORT_TARGET_TWITTER = 0;
   private static final int EXPORT_TARGET_SMS = 1;
   private static final int EXPORT_TARGET_TEXT = 2;

   private static final int PROGRESS_STEPS = 10;
   private static final int DIALOG_ERROR = Menu.FIRST + 28;
   private static final int DIALOG_CONNECTBREADCRUMBS = Menu.FIRST + 29;
   private static final int DESCRIBE = 312;

   private static File sTempBitmap;

   private RemoteViews mContentView;
   private int barProgress = 0;
   private Notification mNotification;
   private NotificationManager mNotificationManager;

   private EditText mFileNameView;
   private EditText mTweetView;
   private Spinner mShareTypeSpinner;
   private Spinner mShareTargetSpinner;
   private Uri mTrackUri;
   private BreadcrumbsService mService;
   boolean mBound = false;
   private String mErrorDialogMessage;
   private Throwable mErrorDialogException;

   private ImageView mImageView;
   private ImageButton mCloseImageView;

   private Uri mImageUri;

   @Override
   public void onCreate(final Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.sharedialog);
      final Intent service = new Intent(this, BreadcrumbsService.class);
      startService(service);

      this.mTrackUri = getIntent().getData();

      this.mFileNameView = (EditText) findViewById(R.id.fileNameField);
      this.mTweetView = (EditText) findViewById(R.id.tweetField);
      this.mImageView = (ImageView) findViewById(R.id.imageView);
      this.mCloseImageView = (ImageButton) findViewById(R.id.closeImageView);

      this.mShareTypeSpinner = (Spinner) findViewById(R.id.shareTypeSpinner);
      final ArrayAdapter<CharSequence> shareTypeAdapter = ArrayAdapter.createFromResource(this, R.array.sharetype_choices, android.R.layout.simple_spinner_item);
      shareTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      this.mShareTypeSpinner.setAdapter(shareTypeAdapter);
      this.mShareTargetSpinner = (Spinner) findViewById(R.id.shareTargetSpinner);
      this.mShareTargetSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
         {
            @Override
            public void onItemSelected(final AdapterView< ? > arg0, final View arg1, final int position, final long arg3)
            {
               if (ShareRoute.this.mShareTypeSpinner.getSelectedItemPosition() == EXPORT_TYPE_GPX && position == EXPORT_TARGET_BREADCRUMBS)
               {
                  final boolean authorized = ShareRoute.this.mService.isAuthorized();
                  if (!authorized)
                  {
                     showDialog(DIALOG_CONNECTBREADCRUMBS);
                  }
               }
               else if (ShareRoute.this.mShareTypeSpinner.getSelectedItemPosition() == EXPORT_TYPE_TEXTLINE && position != EXPORT_TARGET_SMS)
               {
                  readScreenBitmap();
               }
               else
               {
                  removeScreenBitmap();
               }
            }

            @Override
            public void onNothingSelected(final AdapterView< ? > arg0)
            { /* NOOP */
            }
         });

      this.mShareTypeSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
         {
            @Override
            public void onItemSelected(final AdapterView< ? > arg0, final View arg1, final int position, final long arg3)
            {
               adjustTargetToType(position);
            }

            @Override
            public void onNothingSelected(final AdapterView< ? > arg0)
            { /* NOOP */
            }
         });

      final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      final int lastType = prefs.getInt(Constants.EXPORT_TYPE, EXPORT_TYPE_GPX);
      this.mShareTypeSpinner.setSelection(lastType);
      adjustTargetToType(lastType);

      this.mFileNameView.setText(queryForTrackName(getContentResolver(), this.mTrackUri));

      final Button okay = (Button) findViewById(R.id.okayshare_button);
      okay.setOnClickListener(new View.OnClickListener()
         {
            @Override
            public void onClick(final View v)
            {
               v.setEnabled(false);
               share();
            }
         });

      final Button cancel = (Button) findViewById(R.id.cancelshare_button);
      cancel.setOnClickListener(new View.OnClickListener()
         {
            @Override
            public void onClick(final View v)
            {
               v.setEnabled(false);
               ShareRoute.this.finish();
            }
         });
   }

   @Override
   protected void onStart()
   {
      super.onStart();
      final Intent intent = new Intent(this, BreadcrumbsService.class);
      bindService(intent, this.mConnection, Context.BIND_AUTO_CREATE);
   }

   @Override
   protected void onResume()

   {
      super.onResume();

      // Upgrade from stored OSM username/password to OAuth authorization
      final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      if (prefs.contains(Constants.OSM_USERNAME) || prefs.contains(Constants.OSM_PASSWORD))
      {
         final Editor editor = prefs.edit();
         editor.remove(Constants.OSM_USERNAME);
         editor.remove(Constants.OSM_PASSWORD);
         editor.commit();
      }
      findViewById(R.id.okayshare_button).setEnabled(true);
      findViewById(R.id.cancelshare_button).setEnabled(true);
   }

   @Override
   protected void onStop()
   {
      if (this.mBound)
      {
         unbindService(this.mConnection);
         this.mBound = false;
         this.mService = null;
      }
      super.onStop();
   }

   @Override
   protected void onDestroy()
   {
      if (isFinishing())
      {
         final Intent service = new Intent(this, BreadcrumbsService.class);
         stopService(service);
      }
      super.onDestroy();
   }

   /**
    * @see android.app.Activity#onCreateDialog(int)
    */
   @Override
   protected Dialog onCreateDialog(final int id)
   {
      Dialog dialog = null;
      Builder builder = null;
      switch (id)
      {
         case DIALOG_ERROR:
            builder = new AlertDialog.Builder(this);
            final String exceptionMessage = this.mErrorDialogException == null ? "" : " (" + this.mErrorDialogException.getMessage() + ") ";
            builder.setIcon(android.R.drawable.ic_dialog_alert).setTitle(android.R.string.dialog_alert_title).setMessage(this.mErrorDialogMessage + exceptionMessage)
                  .setNeutralButton(android.R.string.cancel, null);
            dialog = builder.create();
            return dialog;
         case DIALOG_CONNECTBREADCRUMBS:
            builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.dialog_breadcrumbsconnect).setMessage(R.string.dialog_breadcrumbsconnect_message).setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(R.string.btn_okay, this.mBreadcrumbsDialogListener).setNegativeButton(R.string.btn_cancel, null);
            dialog = builder.create();
            return dialog;
         default:
            return super.onCreateDialog(id);
      }
   }

   /**
    * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
    */
   @Override
   protected void onPrepareDialog(final int id, final Dialog dialog)
   {
      super.onPrepareDialog(id, dialog);
      AlertDialog alert;
      switch (id)
      {
         case DIALOG_ERROR:
            alert = (AlertDialog) dialog;
            final String exceptionMessage = this.mErrorDialogException == null ? "" : " (" + this.mErrorDialogException.getMessage() + ") ";
            alert.setMessage(this.mErrorDialogMessage + exceptionMessage);
            break;
      }
   }

   private void setGpxExportTargets()
   {
      final ArrayAdapter<CharSequence> shareTargetAdapter = ArrayAdapter.createFromResource(this, R.array.sharegpxtarget_choices, android.R.layout.simple_spinner_item);
      shareTargetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      this.mShareTargetSpinner.setAdapter(shareTargetAdapter);
      final int lastTarget = PreferenceManager.getDefaultSharedPreferences(this).getInt(Constants.EXPORT_GPXTARGET, EXPORT_TARGET_SAVE);
      this.mShareTargetSpinner.setSelection(lastTarget);

      removeScreenBitmap();
   }

   private void setKmzExportTargets()
   {
      final ArrayAdapter<CharSequence> shareTargetAdapter = ArrayAdapter.createFromResource(this, R.array.sharekmztarget_choices, android.R.layout.simple_spinner_item);
      shareTargetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      this.mShareTargetSpinner.setAdapter(shareTargetAdapter);
      final int lastTarget = PreferenceManager.getDefaultSharedPreferences(this).getInt(Constants.EXPORT_KMZTARGET, EXPORT_TARGET_SEND);
      this.mShareTargetSpinner.setSelection(lastTarget);

      removeScreenBitmap();
   }

   private void setTextLineExportTargets()
   {
      final ArrayAdapter<CharSequence> shareTargetAdapter = ArrayAdapter.createFromResource(this, R.array.sharetexttarget_choices, android.R.layout.simple_spinner_item);
      shareTargetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      this.mShareTargetSpinner.setAdapter(shareTargetAdapter);
      final int lastTarget = PreferenceManager.getDefaultSharedPreferences(this).getInt(Constants.EXPORT_TXTTARGET, EXPORT_TARGET_TWITTER);
      this.mShareTargetSpinner.setSelection(lastTarget);
   }

   private void share()
   {
      final String chosenFileName = this.mFileNameView.getText().toString();
      final String textLine = this.mTweetView.getText().toString();
      final int type = (int) this.mShareTypeSpinner.getSelectedItemId();
      final int target = (int) this.mShareTargetSpinner.getSelectedItemId();

      final Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
      editor.putInt(Constants.EXPORT_TYPE, type);

      switch (type)
      {
         case EXPORT_TYPE_KMZ:
            editor.putInt(Constants.EXPORT_KMZTARGET, target);
            editor.commit();
            exportKmz(chosenFileName, target);
            break;
         case EXPORT_TYPE_GPX:
            editor.putInt(Constants.EXPORT_GPXTARGET, target);
            editor.commit();
            exportGpx(chosenFileName, target);
            break;
         case EXPORT_TYPE_TEXTLINE:
            editor.putInt(Constants.EXPORT_TXTTARGET, target);
            editor.commit();
            exportTextLine(textLine, target);
            break;
         default:
            Log.e(TAG, "Failed to determine sharing type" + type);
            break;
      }
   }

   protected void exportKmz(final String chosenFileName, final int target)
   {
      switch (target)
      {
         case EXPORT_TARGET_SEND:
            new KmzSharing(this, this.mTrackUri, chosenFileName, new ShareProgressListener(chosenFileName)).execute();
            break;
         case EXPORT_TARGET_SAVE:
            new KmzCreator(this, this.mTrackUri, chosenFileName, new ShareProgressListener(chosenFileName)).execute();
            break;
         default:
            Log.e(TAG, "Unable to determine target for sharing KMZ " + target);
            break;
      }
      ShareRoute.this.finish();
   }

   protected void exportGpx(final String chosenFileName, final int target)
   {
      switch (target)
      {
         case EXPORT_TARGET_SAVE:
            new GpxCreator(this, this.mTrackUri, chosenFileName, true, new ShareProgressListener(chosenFileName)).execute();
            ShareRoute.this.finish();
            break;
         case EXPORT_TARGET_SEND:
            new GpxSharing(this, this.mTrackUri, chosenFileName, true, new ShareProgressListener(chosenFileName)).execute();
            ShareRoute.this.finish();
            break;
         case EXPORT_TARGET_JOGRUN:
            new JogmapSharing(this, this.mTrackUri, chosenFileName, false, new ShareProgressListener(chosenFileName)).execute();
            ShareRoute.this.finish();
            break;
         case EXPORT_TARGET_OSM:
            new OsmSharing(this, this.mTrackUri, false, new ShareProgressListener(OsmSharing.OSM_FILENAME)).execute();
            ShareRoute.this.finish();
            break;
         case EXPORT_TARGET_BREADCRUMBS:
            sendToBreadcrumbs(this.mTrackUri, chosenFileName);
            break;
         default:
            Log.e(TAG, "Unable to determine target for sharing GPX " + target);
            break;
      }
   }

   protected void exportTextLine(final String message, final int target)
   {
      final String subject = "PotholeSpot";
      switch (target)
      {
         case EXPORT_TARGET_TWITTER:
            sendTweet(message);
            break;
         case EXPORT_TARGET_SMS:
            sendSMS(message);
            ShareRoute.this.finish();
            break;
         case EXPORT_TARGET_TEXT:
            sentGenericText(subject, message);
            ShareRoute.this.finish();
            break;
      }

   }

   @Override
   protected void onActivityResult(final int requestCode, final int resultCode, final Intent data)
   {
      if (resultCode != RESULT_CANCELED)
      {
         String name;
         switch (requestCode)
         {
            case DESCRIBE:
               final Uri trackUri = data.getData();
               if (data.getExtras() != null && data.getExtras().containsKey(Constants.NAME))
               {
                  name = data.getExtras().getString(Constants.NAME);
               }
               else
               {
                  name = "shareToGobreadcrumbs";
               }
               this.mService.startUploadTask(this, new ShareProgressListener(name), trackUri, name);
               finish();
               break;
            default:
               super.onActivityResult(requestCode, resultCode, data);
               break;
         }
      }
   }

   private void sendTweet(final String tweet)
   {
      final Intent intent = findTwitterClient();
      intent.putExtra(Intent.EXTRA_TEXT, tweet);
      if (this.mImageUri != null)
      {
         intent.putExtra(Intent.EXTRA_STREAM, this.mImageUri);
      }
      startActivity(intent);
      ShareRoute.this.finish();
   }

   private void sendSMS(final String msg)
   {
      final Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setType("vnd.android-dir/mms-sms");
      intent.putExtra("sms_body", msg);
      startActivity(intent);
   }

   private void sentGenericText(final String subject, final String msg)
   {
      final Intent intent = new Intent(Intent.ACTION_SEND);
      intent.setType("text/plain");
      intent.putExtra(Intent.EXTRA_SUBJECT, subject);
      intent.putExtra(Intent.EXTRA_TEXT, msg);
      if (this.mImageUri != null)
      {
         intent.putExtra(Intent.EXTRA_STREAM, this.mImageUri);
      }
      startActivity(intent);
   }

   private void sendToBreadcrumbs(final Uri mTrackUri, final String chosenFileName)
   {
      // Start a description of the track
      final Intent namingIntent = new Intent(this, DescribeRoute.class);
      namingIntent.setData(mTrackUri);
      namingIntent.putExtra(Constants.NAME, chosenFileName);
      startActivityForResult(namingIntent, DESCRIBE);
   }

   private Intent findTwitterClient()
   {
      final String[] twitterApps = {
            // package // name 
            "com.twitter.android", // official 
            "com.twidroid", // twidroyd
            "com.handmark.tweetcaster", // Tweecaster 
            "com.thedeck.android" // TweetDeck 
      };
      final Intent tweetIntent = new Intent(Intent.ACTION_SEND);
      tweetIntent.setType("text/plain");
      final PackageManager packageManager = getPackageManager();
      final List<ResolveInfo> list = packageManager.queryIntentActivities(tweetIntent, PackageManager.MATCH_DEFAULT_ONLY);
      for (final String twitterApp : twitterApps)
      {
         for (final ResolveInfo resolveInfo : list)
         {
            final String p = resolveInfo.activityInfo.packageName;
            if (p != null && p.startsWith(twitterApp))
            {
               tweetIntent.setPackage(p);
            }
         }
      }
      return tweetIntent;
   }

   private void createTweetText()
   {
      final StatisticsCalulator calculator = new StatisticsCalulator(this, new UnitsI18n(this), this);
      findViewById(R.id.tweet_progress).setVisibility(View.VISIBLE);
      calculator.execute(this.mTrackUri);
   }

   @Override
   public void finishedCalculations(final StatisticsCalulator calculated)
   {
      final String name = queryForTrackName(getContentResolver(), this.mTrackUri);
      final String distString = calculated.getDistanceText();
      final String avgSpeed = calculated.getAvgSpeedText();
      final String duration = calculated.getDurationText();
      final String tweetText = String.format(getString(R.string.tweettext, name, distString, avgSpeed, duration));
      if (this.mTweetView.getText().toString().equals(""))
      {
         this.mTweetView.setText(tweetText);
      }
      findViewById(R.id.tweet_progress).setVisibility(View.GONE);
   }

   private void adjustTargetToType(final int position)
   {
      switch (position)
      {
         case EXPORT_TYPE_KMZ:
            setKmzExportTargets();
            this.mFileNameView.setVisibility(View.VISIBLE);
            this.mTweetView.setVisibility(View.GONE);
            break;
         case EXPORT_TYPE_GPX:
            setGpxExportTargets();
            this.mFileNameView.setVisibility(View.VISIBLE);
            this.mTweetView.setVisibility(View.GONE);
            break;
         case EXPORT_TYPE_TEXTLINE:
            setTextLineExportTargets();
            this.mFileNameView.setVisibility(View.GONE);
            this.mTweetView.setVisibility(View.VISIBLE);
            createTweetText();
            break;
         default:
            break;
      }
   }

   public static void sendFile(final Context context, final Uri fileUri, final String fileContentType, final String body)
   {
      final Intent sendActionIntent = new Intent(Intent.ACTION_SEND);
      sendActionIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.email_subject));
      sendActionIntent.putExtra(Intent.EXTRA_TEXT, body);
      sendActionIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
      sendActionIntent.setType(fileContentType);
      context.startActivity(Intent.createChooser(sendActionIntent, context.getString(R.string.sender_chooser)));
   }

   public static String queryForTrackName(final ContentResolver resolver, final Uri trackUri)
   {
      Cursor trackCursor = null;
      String name = null;

      try
      {
         trackCursor = resolver.query(trackUri, new String[] { TracksColumns.NAME }, null, null, null);
         if (trackCursor.moveToFirst())
         {
            name = trackCursor.getString(0);
         }
      }

      finally
      {
         if (trackCursor != null)
         {
            trackCursor.close();
         }
      }
      return name;
   }

   /*
    * public static String queryForLabelName(ContentResolver resolver, Uri trackUri) { Cursor labelCursor = null; String name = null; //Double time = null; try { labelCursor = resolver.query(trackUri,
    * new String[] { Labels.NAME}, null, null, null); if (labelCursor.moveToFirst()) { name = labelCursor.getString(0); //time = labelCursor.getDouble(1); } } finally { if (labelCursor != null) {
    * labelCursor.close(); } } return name; }
    */

   /*
    * public static String queryForLabelTime(ContentResolver resolver, Uri trackUri) { Cursor labelCursor = null; String time = null; try { labelCursor = resolver.query(trackUri, new String[] {
    * Labels.CREATION_TIME}, null, null, null); if (labelCursor.moveToFirst()) { time = labelCursor.getString(0); } } finally { if (labelCursor != null) { labelCursor.close(); } } return time; }
    */

   /*
    * public static String queryForLocationsTime(ContentResolver resolver, Uri labelUri) { Cursor labelCursor = null; String time = null; try { labelCursor = resolver.query(labelUri, new String[] {
    * Locations.TIME}, null, null, null); if (labelCursor.moveToFirst()) { time = labelCursor.getString(0); } } finally { if (labelCursor != null) { labelCursor.close(); } } return time; }
    */

   public static Uri storeScreenBitmap(final Bitmap bm)
   {
      Uri fileUri = null;
      FileOutputStream stream = null;
      try
      {
         clearScreenBitmap();

         sTempBitmap = File.createTempFile("shareimage", ".png");
         fileUri = Uri.fromFile(sTempBitmap);
         stream = new FileOutputStream(sTempBitmap);
         bm.compress(CompressFormat.PNG, 100, stream);
      }
      catch (final IOException e)
      {
         Log.e(TAG, "Bitmap extra storing failed", e);
      }
      finally
      {
         try
         {
            if (stream != null)
            {
               stream.close();
            }
         }
         catch (final IOException e)
         {
            Log.e(TAG, "Bitmap extra close failed", e);
         }
      }
      return fileUri;
   }

   public static void clearScreenBitmap()
   {
      if (sTempBitmap != null && sTempBitmap.exists())
      {
         sTempBitmap.delete();
         sTempBitmap = null;
      }
   }

   private void readScreenBitmap()
   {
      this.mImageView.setVisibility(View.GONE);
      this.mCloseImageView.setVisibility(View.GONE);
      if (getIntent().getExtras() != null && getIntent().hasExtra(Intent.EXTRA_STREAM))
      {
         this.mImageUri = getIntent().getExtras().getParcelable(Intent.EXTRA_STREAM);
         if (this.mImageUri != null)
         {
            InputStream is = null;
            try
            {
               is = getContentResolver().openInputStream(this.mImageUri);
               this.mImageView.setImageBitmap(BitmapFactory.decodeStream(is));
               this.mImageView.setVisibility(View.VISIBLE);
               this.mCloseImageView.setVisibility(View.VISIBLE);
               this.mCloseImageView.setOnClickListener(new View.OnClickListener()
                  {

                     @Override
                     public void onClick(final View v)
                     {
                        removeScreenBitmap();
                     }
                  });
            }
            catch (final FileNotFoundException e)
            {
               Log.e(TAG, "Failed reading image from file", e);
            }
            finally
            {
               if (is != null)
               {
                  try
                  {
                     is.close();
                  }
                  catch (final IOException e)
                  {
                     Log.e(TAG, "Failed close image from file", e);
                  }
               }
            }
         }
      }
   }

   private void removeScreenBitmap()
   {
      this.mImageView.setVisibility(View.GONE);
      this.mCloseImageView.setVisibility(View.GONE);
      this.mImageUri = null;
   }

   private final ServiceConnection mConnection = new ServiceConnection()
      {
         @Override
         public void onServiceConnected(final ComponentName className, final IBinder service)
         {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            final LocalBinder binder = (LocalBinder) service;
            ShareRoute.this.mService = binder.getService();
            ShareRoute.this.mBound = true;
         }

         @Override
         public void onServiceDisconnected(final ComponentName arg0)
         {
            ShareRoute.this.mBound = false;
            ShareRoute.this.mService = null;
         }
      };

   private final OnClickListener mBreadcrumbsDialogListener = new OnClickListener()
      {
         @Override
         public void onClick(final DialogInterface dialog, final int which)
         {
            ShareRoute.this.mService.collectBreadcrumbsOauthToken();
         }
      };

   public class ShareProgressListener implements ProgressListener
   {
      private final String mFileName;
      private int mProgress;

      public ShareProgressListener(final String sharename)
      {
         this.mFileName = sharename;
      }

      public void startNotification()
      {
         final String ns = Context.NOTIFICATION_SERVICE;
         ShareRoute.this.mNotificationManager = (NotificationManager) getSystemService(ns);
         final int icon = android.R.drawable.ic_menu_save;
         final CharSequence tickerText = getString(R.string.ticker_saving) + "\"" + this.mFileName + "\"";

         ShareRoute.this.mNotification = new Notification();
         final PendingIntent contentIntent = PendingIntent.getActivity(ShareRoute.this, 0, new Intent(ShareRoute.this, LoggerMap.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
               PendingIntent.FLAG_UPDATE_CURRENT);

         ShareRoute.this.mNotification.contentIntent = contentIntent;
         ShareRoute.this.mNotification.tickerText = tickerText;
         ShareRoute.this.mNotification.icon = icon;
         ShareRoute.this.mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
         ShareRoute.this.mContentView = new RemoteViews(getPackageName(), R.layout.savenotificationprogress);
         ShareRoute.this.mContentView.setImageViewResource(R.id.icon, icon);
         ShareRoute.this.mContentView.setTextViewText(R.id.progresstext, tickerText);

         ShareRoute.this.mNotification.contentView = ShareRoute.this.mContentView;
      }

      private void updateNotification()
      {
         //         Log.d( "TAG", "Progress " + progress + " of " + goal );
         if (this.mProgress > 0 && this.mProgress < Window.PROGRESS_END)
         {
            if ((this.mProgress * PROGRESS_STEPS) / Window.PROGRESS_END != ShareRoute.this.barProgress)
            {
               ShareRoute.this.barProgress = (this.mProgress * PROGRESS_STEPS) / Window.PROGRESS_END;
               ShareRoute.this.mContentView.setProgressBar(R.id.progress, Window.PROGRESS_END, this.mProgress, false);
               ShareRoute.this.mNotificationManager.notify(R.layout.savenotificationprogress, ShareRoute.this.mNotification);
            }
         }
         else if (this.mProgress == 0)
         {
            ShareRoute.this.mContentView.setProgressBar(R.id.progress, Window.PROGRESS_END, this.mProgress, true);
            ShareRoute.this.mNotificationManager.notify(R.layout.savenotificationprogress, ShareRoute.this.mNotification);
         }
         else if (this.mProgress >= Window.PROGRESS_END)
         {
            ShareRoute.this.mContentView.setProgressBar(R.id.progress, Window.PROGRESS_END, this.mProgress, false);
            ShareRoute.this.mNotificationManager.notify(R.layout.savenotificationprogress, ShareRoute.this.mNotification);
         }
      }

      public void endNotification(final Uri file)
      {
         ShareRoute.this.mNotificationManager.cancel(R.layout.savenotificationprogress);
      }

      @Override
      public void setIndeterminate(final boolean indeterminate)
      {
         Log.w(TAG, "Unsupported indeterminate progress display");
      }

      @Override
      public void started()
      {
         startNotification();
      }

      @Override
      public void setProgress(final int value)
      {
         this.mProgress = value;
         updateNotification();
      }

      @Override
      public void finished(final Uri result)
      {
         endNotification(result);
      }

      @Override
      public void showError(final String task, final String errorDialogMessage, final Exception errorDialogException)
      {
         endNotification(null);

         ShareRoute.this.mErrorDialogMessage = errorDialogMessage;
         ShareRoute.this.mErrorDialogException = errorDialogException;
         if (!isFinishing())
         {
            showDialog(DIALOG_ERROR);
         }
         else
         {
            final Toast toast = Toast.makeText(ShareRoute.this, errorDialogMessage, Toast.LENGTH_LONG);
            toast.show();
         }
      }

   }
}
