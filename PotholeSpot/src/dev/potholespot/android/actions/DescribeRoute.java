package dev.potholespot.android.actions;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import dev.potholespot.android.breadcrumbs.BreadcrumbsService;
import dev.potholespot.android.breadcrumbs.BreadcrumbsService.LocalBinder;
import dev.potholespot.android.breadcrumbs.BreadcrumbsTracks;
import dev.potholespot.android.db.Pspot.MetaDataColumns;
import dev.potholespot.android.util.Constants;
import dev.potholespot.android.util.Pair;
import dev.potholespot.uganda.R;

/**
 * Empty Activity that pops up the dialog to describe the track
 */
public class DescribeRoute extends Activity
{
   private static final int DIALOG_TRACKDESCRIPTION = 42;

   protected static final String TAG = "OGT.DescribeTrack";

   private static final String ACTIVITY_ID = "ACTIVITY_ID";

   private static final String BUNDLE_ID = "BUNDLE_ID";

   private Spinner mActivitySpinner;
   private Spinner mBundleSpinner;
   private EditText mDescriptionText;
   private CheckBox mPublicCheck;
   private Button mOkayButton;
   private boolean paused;
   private Uri mTrackUri;
   private ProgressBar mProgressSpinner;

   private AlertDialog mDialog;
   private BreadcrumbsService mService;
   boolean mBound = false;

   @Override
   protected void onCreate(final Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);

      setVisible(false);
      this.paused = false;

      this.mTrackUri = getIntent().getData();
      final Intent service = new Intent(this, BreadcrumbsService.class);
      startService(service);
   }

   @Override
   protected void onStart()
   {
      super.onStart();
      final Intent intent = new Intent(this, BreadcrumbsService.class);
      bindService(intent, this.mConnection, Context.BIND_AUTO_CREATE);
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
         showDialog(DIALOG_TRACKDESCRIPTION);
      }
      else
      {
         Log.e(TAG, "Describing track without a track URI supplied.");
         finish();
      }
   }

   @Override
   protected void onPause()
   {
      super.onPause();
      this.paused = true;
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

   @Override
   protected Dialog onCreateDialog(final int id)
   {
      LayoutInflater factory = null;
      View view = null;
      Builder builder = null;
      switch (id)
      {
         case DIALOG_TRACKDESCRIPTION:
            builder = new AlertDialog.Builder(this);
            factory = LayoutInflater.from(this);
            view = factory.inflate(R.layout.describedialog, null);
            this.mActivitySpinner = (Spinner) view.findViewById(R.id.activity);
            this.mBundleSpinner = (Spinner) view.findViewById(R.id.bundle);
            this.mDescriptionText = (EditText) view.findViewById(R.id.description);
            this.mPublicCheck = (CheckBox) view.findViewById(R.id.public_checkbox);
            this.mProgressSpinner = (ProgressBar) view.findViewById(R.id.progressSpinner);
            builder.setTitle(R.string.dialog_description_title).setMessage(R.string.dialog_description_message).setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(R.string.btn_okay, this.mTrackDescriptionDialogListener).setNegativeButton(R.string.btn_cancel, this.mTrackDescriptionDialogListener).setView(view);
            this.mDialog = builder.create();
            setUiEnabled();

            this.mDialog.setOnDismissListener(new OnDismissListener()
               {
                  @Override
                  public void onDismiss(final DialogInterface dialog)
                  {
                     if (!DescribeRoute.this.paused)
                     {
                        finish();
                     }
                  }
               });
            return this.mDialog;
         default:
            return super.onCreateDialog(id);
      }
   }

   @Override
   protected void onPrepareDialog(final int id, final Dialog dialog)
   {
      switch (id)
      {
         case DIALOG_TRACKDESCRIPTION:
            setUiEnabled();
            connectBreadcrumbs();
            break;
         default:
            super.onPrepareDialog(id, dialog);
            break;
      }
   }

   private void connectBreadcrumbs()
   {
      if (this.mService != null && !this.mService.isAuthorized())
      {
         this.mService.collectBreadcrumbsOauthToken();
      }
   }

   private void saveBreadcrumbsPreference(final int activityPosition, final int bundlePosition)
   {
      final Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
      editor.putInt(ACTIVITY_ID, activityPosition);
      editor.putInt(BUNDLE_ID, bundlePosition);
      editor.commit();
   }

   private void loadBreadcrumbsPreference()
   {
      final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

      int activityPos = prefs.getInt(ACTIVITY_ID, 0);
      activityPos = activityPos < this.mActivitySpinner.getCount() ? activityPos : 0;
      this.mActivitySpinner.setSelection(activityPos);

      int bundlePos = prefs.getInt(BUNDLE_ID, 0);
      bundlePos = bundlePos < this.mBundleSpinner.getCount() ? bundlePos : 0;
      this.mBundleSpinner.setSelection(bundlePos);
   }

   private ContentValues buildContentValues(final String key, final String value)
   {
      final ContentValues contentValues = new ContentValues();
      contentValues.put(MetaDataColumns.KEY, key);
      contentValues.put(MetaDataColumns.VALUE, value);
      return contentValues;
   }

   private void setUiEnabled()
   {
      final boolean enabled = this.mService != null && this.mService.isAuthorized();
      if (this.mProgressSpinner != null)
      {
         if (enabled)
         {
            this.mProgressSpinner.setVisibility(View.GONE);
         }
         else
         {
            this.mProgressSpinner.setVisibility(View.VISIBLE);
         }
      }

      if (this.mDialog != null)
      {
         this.mOkayButton = this.mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
      }
      for (final View view : new View[] { this.mActivitySpinner, this.mBundleSpinner, this.mDescriptionText, this.mPublicCheck, this.mOkayButton })
      {
         if (view != null)
         {
            view.setEnabled(enabled);
         }
      }
      if (enabled)
      {
         this.mActivitySpinner.setAdapter(getActivityAdapter());
         this.mBundleSpinner.setAdapter(getBundleAdapter());
         loadBreadcrumbsPreference();
      }
   }

   public SpinnerAdapter getActivityAdapter()
   {
      final List<Pair<Integer, Integer>> activities = this.mService.getActivityList();
      final ArrayAdapter<Pair<Integer, Integer>> adapter = new ArrayAdapter<Pair<Integer, Integer>>(this, android.R.layout.simple_spinner_item, activities);
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      return adapter;
   }

   public SpinnerAdapter getBundleAdapter()
   {
      final List<Pair<Integer, Integer>> bundles = this.mService.getBundleList();
      final ArrayAdapter<Pair<Integer, Integer>> adapter = new ArrayAdapter<Pair<Integer, Integer>>(this, android.R.layout.simple_spinner_item, bundles);
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      return adapter;
   }

   private final ServiceConnection mConnection = new ServiceConnection()
      {
         @Override
         public void onServiceConnected(final ComponentName className, final IBinder service)
         {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            final LocalBinder binder = (LocalBinder) service;
            DescribeRoute.this.mService = binder.getService();
            DescribeRoute.this.mBound = true;
            setUiEnabled();
         }

         @Override
         public void onServiceDisconnected(final ComponentName arg0)
         {
            DescribeRoute.this.mService = null;
            DescribeRoute.this.mBound = false;
         }
      };

   private final DialogInterface.OnClickListener mTrackDescriptionDialogListener = new DialogInterface.OnClickListener()
      {
         @Override
         public void onClick(final DialogInterface dialog, final int which)
         {
            switch (which)
            {
               case DialogInterface.BUTTON_POSITIVE:
                  final Uri metadataUri = Uri.withAppendedPath(DescribeRoute.this.mTrackUri, "metadata");
                  final Integer activityId = ((Pair<Integer, Integer>) DescribeRoute.this.mActivitySpinner.getSelectedItem()).second;
                  final Integer bundleId = ((Pair<Integer, Integer>) DescribeRoute.this.mBundleSpinner.getSelectedItem()).second;
                  saveBreadcrumbsPreference(DescribeRoute.this.mActivitySpinner.getSelectedItemPosition(), DescribeRoute.this.mBundleSpinner.getSelectedItemPosition());
                  final String description = DescribeRoute.this.mDescriptionText.getText().toString();
                  final String isPublic = Boolean.toString(DescribeRoute.this.mPublicCheck.isChecked());
                  final ContentValues[] metaValues = { buildContentValues(BreadcrumbsTracks.ACTIVITY_ID, activityId.toString()), buildContentValues(BreadcrumbsTracks.BUNDLE_ID, bundleId.toString()),
                        buildContentValues(BreadcrumbsTracks.DESCRIPTION, description), buildContentValues(BreadcrumbsTracks.ISPUBLIC, isPublic), };
                  getContentResolver().bulkInsert(metadataUri, metaValues);
                  final Intent data = new Intent();
                  data.setData(DescribeRoute.this.mTrackUri);
                  if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(Constants.NAME))
                  {
                     data.putExtra(Constants.NAME, getIntent().getExtras().getString(Constants.NAME));
                  }
                  setResult(RESULT_OK, data);
                  break;
               case DialogInterface.BUTTON_NEGATIVE:
                  break;
               default:
                  Log.e(TAG, "Unknown option ending dialog:" + which);
                  break;
            }
            finish();
         }
      };
}
