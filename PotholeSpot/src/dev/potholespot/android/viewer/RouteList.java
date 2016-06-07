package dev.potholespot.android.viewer;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import dev.potholespot.android.actions.DescribeRoute;
import dev.potholespot.android.actions.Statistics;
import dev.potholespot.android.actions.tasks.GpxParser;
import dev.potholespot.android.actions.utils.ProgressListener;
import dev.potholespot.android.adapter.BreadcrumbsAdapter;
import dev.potholespot.android.adapter.SectionedListAdapter;
import dev.potholespot.android.breadcrumbs.BreadcrumbsService;
import dev.potholespot.android.breadcrumbs.BreadcrumbsService.LocalBinder;
import dev.potholespot.android.db.DatabaseHelper;
import dev.potholespot.android.db.Pspot;
import dev.potholespot.android.db.Pspot.Tracks;
import dev.potholespot.android.db.Pspot.TracksColumns;
import dev.potholespot.android.util.Constants;
import dev.potholespot.android.util.Pair;
import dev.potholespot.android.viewer.map.CommonLoggerMap;
import dev.potholespot.android.viewer.map.LoggerMap;
import dev.potholespot.uganda.R;

public class RouteList extends ListActivity implements ProgressListener
{

   private static final String TAG = "PRIM.TrackList";
   private static final int MENU_DETELE = Menu.FIRST + 0;
   private static final int MENU_SHARE = Menu.FIRST + 1;
   private static final int MENU_RENAME = Menu.FIRST + 2;
   private static final int MENU_STATS = Menu.FIRST + 3;
   private static final int MENU_SEARCH = Menu.FIRST + 4;
   private static final int MENU_VACUUM = Menu.FIRST + 5;
   private static final int MENU_PICKER = Menu.FIRST + 6;
   private static final int MENU_BREADCRUMBS = Menu.FIRST + 7;

   public static final int DIALOG_FILENAME = Menu.FIRST + 22;
   private static final int DIALOG_RENAME = Menu.FIRST + 23;
   private static final int DIALOG_DELETE = Menu.FIRST + 24;
   private static final int DIALOG_VACUUM = Menu.FIRST + 25;
   private static final int DIALOG_IMPORT = Menu.FIRST + 26;
   private static final int DIALOG_INSTALL = Menu.FIRST + 27;
   protected static final int DIALOG_ERROR = Menu.FIRST + 28;

   private static final int PICKER_OI = Menu.FIRST + 29;
   private static final int DESCRIBE = Menu.FIRST + 30;

   private BreadcrumbsAdapter mBreadcrumbAdapter;
   private EditText mTrackNameView;
   private Uri mDialogTrackUri;
   private String mDialogCurrentName = "";
   private String mErrorDialogMessage;
   private Exception mErrorDialogException;
   private Runnable mImportAction;
   private String mImportTrackName;
   private String mErrorTask;
   /**
    * Progress listener for the background tasks uploading to gobreadcrumbs
    */
   private ProgressListener mExportListener;
   private int mPausePosition;

   private BreadcrumbsService mService;
   boolean mBound = false;

   @Override
   protected void onCreate(final Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);

      getWindow().requestFeature(Window.FEATURE_PROGRESS);
      this.setContentView(R.layout.tracklist);

      displayIntent(getIntent());

      final ListView listView = getListView();
      listView.setItemsCanFocus(true);
      // Add the context menu (the long press thing)
      registerForContextMenu(listView);

      if (savedInstanceState != null)
      {
         getListView().setSelection(savedInstanceState.getInt("POSITION"));
      }

      final IntentFilter filter = new IntentFilter();
      filter.addAction(BreadcrumbsService.NOTIFY_DATA_SET_CHANGED);
      registerReceiver(this.mReceiver, filter);

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

   @Override
   protected void onResume()
   {
      if (this.mPausePosition != 0)
      {
         getListView().setSelection(this.mPausePosition);
      }
      super.onResume();
   }

   @Override
   protected void onPause()
   {
      this.mPausePosition = getListView().getFirstVisiblePosition();
      super.onPause();
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
      unregisterReceiver(this.mReceiver);
      super.onDestroy();
   }

   @Override
   public void onNewIntent(final Intent newIntent)
   {
      displayIntent(newIntent);
   }

   /*
    * (non-Javadoc)
    * @see android.app.ListActivity#onRestoreInstanceState(android.os.Bundle)
    */
   @Override
   protected void onRestoreInstanceState(final Bundle state)
   {
      super.onRestoreInstanceState(state);
      this.mDialogTrackUri = state.getParcelable("URI");
      this.mDialogCurrentName = state.getString("NAME");
      this.mDialogCurrentName = this.mDialogCurrentName != null ? this.mDialogCurrentName : "";
      getListView().setSelection(state.getInt("POSITION"));
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
    */
   @Override
   protected void onSaveInstanceState(final Bundle outState)
   {
      super.onSaveInstanceState(outState);
      outState.putParcelable("URI", this.mDialogTrackUri);
      outState.putString("NAME", this.mDialogCurrentName);
      outState.putInt("POSITION", getListView().getFirstVisiblePosition());
   }

   @Override
   public boolean onCreateOptionsMenu(final Menu menu)
   {
      final boolean result = super.onCreateOptionsMenu(menu);

      menu.add(Menu.NONE, MENU_SEARCH, Menu.NONE, android.R.string.search_go).setIcon(android.R.drawable.ic_search_category_default).setAlphabeticShortcut(SearchManager.MENU_KEY);
      menu.add(Menu.NONE, MENU_VACUUM, Menu.NONE, R.string.menu_vacuum).setIcon(android.R.drawable.ic_menu_crop);
      menu.add(Menu.NONE, MENU_PICKER, Menu.NONE, R.string.menu_picker).setIcon(android.R.drawable.ic_menu_add);
      menu.add(Menu.NONE, MENU_BREADCRUMBS, Menu.NONE, R.string.dialog_breadcrumbsconnect).setIcon(android.R.drawable.ic_menu_revert);
      return result;
   }

   @Override
   public boolean onOptionsItemSelected(final MenuItem item)
   {
      boolean handled = false;
      switch (item.getItemId())
      {
         case MENU_SEARCH:
            onSearchRequested();
            handled = true;
            break;
         case MENU_VACUUM:
            showDialog(DIALOG_VACUUM);
            break;
         case MENU_PICKER:
            try
            {
               final Intent intent = new Intent("org.openintents.action.PICK_FILE");
               intent.putExtra("org.openintents.extra.TITLE", getString(R.string.dialog_import_picker));
               intent.putExtra("org.openintents.extra.BUTTON_TEXT", getString(R.string.menu_picker));
               startActivityForResult(intent, PICKER_OI);
            }
            catch (final ActivityNotFoundException e)
            {
               showDialog(DIALOG_INSTALL);
            }
            break;
         case MENU_BREADCRUMBS:
            this.mService.removeAuthentication();
            this.mService.clearAllCache();
            this.mService.collectBreadcrumbsOauthToken();
            break;
         default:
            handled = super.onOptionsItemSelected(item);
      }
      return handled;
   }

   @Override
   protected void onListItemClick(final ListView listView, final View view, final int position, final long id)
   {
      super.onListItemClick(listView, view, position, id);

      final Object item = listView.getItemAtPosition(position);
      if (item instanceof String)
      {
         if (Constants.BREADCRUMBS_CONNECT.equals(item))
         {
            this.mService.collectBreadcrumbsOauthToken();
         }
      }
      else if (item instanceof Pair< ? , ? >)
      {
         @SuppressWarnings("unchecked")
         final Pair<Integer, Integer> track = (Pair<Integer, Integer>) item;
         if (track.first == Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE)
         {
            final TextView tv = (TextView) view.findViewById(R.id.listitem_name);
            this.mImportTrackName = tv.getText().toString();
            this.mImportAction = new Runnable()
               {
                  @Override
                  public void run()
                  {
                     RouteList.this.mService.startDownloadTask(RouteList.this, RouteList.this, track);
                  }
               };
            showDialog(DIALOG_IMPORT);
         }
      }
      else
      {
         final Intent intent = new Intent();
         final Uri trackUri = ContentUris.withAppendedId(Tracks.CONTENT_URI, id);
         intent.setData(trackUri);
         final ComponentName caller = getCallingActivity();
         if (caller != null)
         {
            setResult(RESULT_OK, intent);
            finish();
         }
         else
         {
            intent.setClass(this, CommonLoggerMap.class);
            startActivity(intent);
         }
      }
   }

   @Override
   public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenu.ContextMenuInfo menuInfo)
   {
      if (menuInfo instanceof AdapterView.AdapterContextMenuInfo)
      {
         final AdapterView.AdapterContextMenuInfo itemInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
         final TextView textView = (TextView) itemInfo.targetView.findViewById(R.id.listitem_name);
         if (textView != null)
         {
            menu.setHeaderTitle(textView.getText());
         }

         final Object listItem = getListAdapter().getItem(itemInfo.position);
         if (listItem instanceof Cursor)
         {
            menu.add(0, MENU_STATS, 0, R.string.menu_statistics);
            menu.add(0, MENU_SHARE, 0, R.string.menu_shareTrack);
            menu.add(0, MENU_RENAME, 0, R.string.menu_renameTrack);
            menu.add(0, MENU_DETELE, 0, R.string.menu_deleteTrack);
         }
      }
   }

   @Override
   public boolean onContextItemSelected(final MenuItem item)
   {
      boolean handled = false;
      AdapterView.AdapterContextMenuInfo info;
      try
      {
         info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
      }
      catch (final ClassCastException e)
      {
         Log.e(TAG, "Bad menuInfo", e);
         return handled;
      }

      final Object listItem = getListAdapter().getItem(info.position);
      if (listItem instanceof Cursor)
      {
         final Cursor cursor = (Cursor) listItem;
         this.mDialogTrackUri = ContentUris.withAppendedId(Tracks.CONTENT_URI, cursor.getLong(0));
         this.mDialogCurrentName = cursor.getString(1);
         this.mDialogCurrentName = this.mDialogCurrentName != null ? this.mDialogCurrentName : "";
         switch (item.getItemId())
         {
            case MENU_DETELE:
            {
               showDialog(DIALOG_DELETE);
               handled = true;
               break;
            }
            case MENU_SHARE:
            {
               final Intent actionIntent = new Intent(Intent.ACTION_RUN);
               actionIntent.setDataAndType(this.mDialogTrackUri, Tracks.CONTENT_ITEM_TYPE);
               actionIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
               startActivity(Intent.createChooser(actionIntent, getString(R.string.share_track)));
               handled = true;
               break;
            }
            case MENU_RENAME:
            {
               showDialog(DIALOG_RENAME);
               handled = true;
               break;
            }
            case MENU_STATS:
            {
               final Intent actionIntent = new Intent(this, Statistics.class);
               actionIntent.setData(this.mDialogTrackUri);
               startActivity(actionIntent);
               handled = true;
               break;
            }
            default:
               handled = super.onContextItemSelected(item);
               break;
         }
      }
      return handled;
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onCreateDialog(int)
    */
   @Override
   protected Dialog onCreateDialog(final int id)
   {
      Dialog dialog = null;
      Builder builder = null;
      switch (id)
      {
         case DIALOG_RENAME:
            final LayoutInflater factory = LayoutInflater.from(this);
            final View view = factory.inflate(R.layout.namedialog, null);
            this.mTrackNameView = (EditText) view.findViewById(R.id.nameField);
            builder = new AlertDialog.Builder(this).setTitle(R.string.dialog_routename_title).setMessage(R.string.dialog_routename_message).setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(R.string.btn_okay, this.mRenameOnClickListener).setNegativeButton(R.string.btn_cancel, null).setView(view);
            dialog = builder.create();
            return dialog;
         case DIALOG_DELETE:
            builder = new AlertDialog.Builder(RouteList.this).setTitle(R.string.dialog_delete_title).setIcon(android.R.drawable.ic_dialog_alert).setNegativeButton(android.R.string.cancel, null)
                  .setPositiveButton(android.R.string.ok, this.mDeleteOnClickListener);
            dialog = builder.create();
            final String messageFormat = getResources().getString(R.string.dialog_delete_message);
            final String message = String.format(messageFormat, "");
            ((AlertDialog) dialog).setMessage(message);
            return dialog;
         case DIALOG_VACUUM:
            builder = new AlertDialog.Builder(RouteList.this).setTitle(R.string.dialog_vacuum_title).setMessage(R.string.dialog_vacuum_message).setIcon(android.R.drawable.ic_dialog_alert)
                  .setNegativeButton(android.R.string.cancel, null).setPositiveButton(android.R.string.ok, this.mVacuumOnClickListener);
            dialog = builder.create();
            return dialog;
         case DIALOG_IMPORT:
            builder = new AlertDialog.Builder(RouteList.this).setTitle(R.string.dialog_import_title).setMessage(getString(R.string.dialog_import_message, this.mImportTrackName))
                  .setIcon(android.R.drawable.ic_dialog_alert).setNegativeButton(android.R.string.cancel, null).setPositiveButton(android.R.string.ok, this.mImportOnClickListener);
            dialog = builder.create();
            return dialog;
         case DIALOG_INSTALL:
            builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.dialog_nooipicker).setMessage(R.string.dialog_nooipicker_message).setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(R.string.btn_install, this.mOiPickerDialogListener).setNegativeButton(R.string.btn_cancel, null);
            dialog = builder.create();
            return dialog;
         case DIALOG_ERROR:
            builder = new AlertDialog.Builder(this);
            builder.setIcon(android.R.drawable.ic_dialog_alert).setTitle(android.R.string.dialog_alert_title).setMessage(this.mErrorDialogMessage).setNeutralButton(android.R.string.cancel, null);
            dialog = builder.create();
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
      super.onPrepareDialog(id, dialog);
      AlertDialog alert;
      String message;
      switch (id)
      {
         case DIALOG_RENAME:
            this.mTrackNameView.setText(this.mDialogCurrentName);
            this.mTrackNameView.setSelection(0, this.mDialogCurrentName.length());
            break;
         case DIALOG_DELETE:
            alert = (AlertDialog) dialog;
            final String messageFormat = getResources().getString(R.string.dialog_delete_message);
            message = String.format(messageFormat, this.mDialogCurrentName);
            alert.setMessage(message);
            break;
         case DIALOG_ERROR:
            alert = (AlertDialog) dialog;
            message = "Failed task:\n" + this.mErrorTask;
            message += "\n\n";
            message += "Reason:\n" + this.mErrorDialogMessage;
            if (this.mErrorDialogException != null)
            {
               message += " (" + this.mErrorDialogException.getMessage() + ") ";
            }
            alert.setMessage(message);
            break;
         case DIALOG_IMPORT:
            alert = (AlertDialog) dialog;
            alert.setMessage(getString(R.string.dialog_import_message, this.mImportTrackName));
            break;
      }
   }

   @Override
   protected void onActivityResult(final int requestCode, final int resultCode, final Intent data)
   {
      if (resultCode != RESULT_CANCELED)
      {
         switch (requestCode)
         {
            case PICKER_OI:
               new GpxParser(RouteList.this, RouteList.this).execute(data.getData());
               break;
            case DESCRIBE:
               final Uri trackUri = data.getData();
               String name;
               if (data.getExtras() != null && data.getExtras().containsKey(Constants.NAME))
               {
                  name = data.getExtras().getString(Constants.NAME);
               }
               else
               {
                  name = "shareToGobreadcrumbs";
               }
               this.mService.startUploadTask(RouteList.this, this.mExportListener, trackUri, name);
               break;
            default:
               super.onActivityResult(requestCode, resultCode, data);
               break;
         }
      }
      else
      {
         if (requestCode == DESCRIBE)
         {
            this.mBreadcrumbAdapter.notifyDataSetChanged();
         }
      }
   }

   private void displayIntent(final Intent intent)
   {
      final String queryAction = intent.getAction();
      final String orderby = TracksColumns.CREATION_TIME + " DESC";
      Cursor tracksCursor = null;
      if (Intent.ACTION_SEARCH.equals(queryAction))
      {
         // Got to SEARCH a query for tracks, make a list
         tracksCursor = doSearchWithIntent(intent);
      }
      else if (Intent.ACTION_VIEW.equals(queryAction))
      {
         final Uri uri = intent.getData();
         if ("content".equals(uri.getScheme()) && Pspot.AUTHORITY.equals(uri.getAuthority()))
         {
            // Got to VIEW a single track, instead hand it of to the LoggerMap
            final Intent notificationIntent = new Intent(this, LoggerMap.class);
            notificationIntent.setData(uri);
            startActivity(notificationIntent);
            finish();
         }
         else if (uri.getScheme().equals("file") || uri.getScheme().equals("content"))
         {

            this.mImportTrackName = uri.getLastPathSegment();
            // Got to VIEW a GPX filename
            this.mImportAction = new Runnable()
               {
                  @Override
                  public void run()
                  {
                     new GpxParser(RouteList.this, RouteList.this).execute(uri);
                  }
               };
            showDialog(DIALOG_IMPORT);
            tracksCursor = managedQuery(Tracks.CONTENT_URI, new String[] { BaseColumns._ID, TracksColumns.NAME, TracksColumns.CREATION_TIME }, null, null, orderby);
         }
         else
         {
            Log.e(TAG, "Unable to VIEW " + uri);
         }
      }
      else
      {
         // Got to nothing, make a list of everything
         tracksCursor = managedQuery(Tracks.CONTENT_URI, new String[] { BaseColumns._ID, TracksColumns.NAME, TracksColumns.CREATION_TIME }, null, null, orderby);
      }
      displayCursor(tracksCursor);

   }

   private void displayCursor(final Cursor tracksCursor)
   {
      final SectionedListAdapter sectionedAdapter = new SectionedListAdapter(this);

      final String[] fromColumns = new String[] { TracksColumns.NAME, TracksColumns.CREATION_TIME, BaseColumns._ID };
      final int[] toItems = new int[] { R.id.listitem_name, R.id.listitem_from, R.id.bcSyncedCheckBox };
      final SimpleCursorAdapter trackAdapter = new SimpleCursorAdapter(this, R.layout.trackitem, tracksCursor, fromColumns, toItems);

      this.mBreadcrumbAdapter = new BreadcrumbsAdapter(this, this.mService);
      sectionedAdapter.addSection("Local", trackAdapter);
      sectionedAdapter.addSection("www.gobreadcrumbs.com", this.mBreadcrumbAdapter);

      // Enrich the track adapter with Breadcrumbs adapter data 
      trackAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder()
         {
            @Override
            public boolean setViewValue(final View view, final Cursor cursor, final int columnIndex)
            {
               if (columnIndex == 0)
               {
                  final long trackId = cursor.getLong(0);
                  final String trackName = cursor.getString(1);
                  // Show the check if Breadcrumbs is online
                  final CheckBox checkbox = (CheckBox) view;
                  final ProgressBar progressbar = (ProgressBar) ((View) view.getParent()).findViewById(R.id.bcExportProgress);
                  if (RouteList.this.mService != null && RouteList.this.mService.isAuthorized())
                  {
                     checkbox.setVisibility(View.VISIBLE);

                     // Disable the checkbox if marked online
                     final boolean isOnline = RouteList.this.mService.isLocalTrackSynced(trackId);
                     checkbox.setEnabled(!isOnline);

                     // Check the checkbox if determined synced
                     final boolean isSynced = RouteList.this.mService.isLocalTrackSynced(trackId);
                     checkbox.setOnCheckedChangeListener(null);
                     checkbox.setChecked(isSynced);
                     checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener()
                        {
                           @Override
                           public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked)
                           {
                              if (isChecked)
                              {
                                 // Start a description of the track
                                 final Intent namingIntent = new Intent(RouteList.this, DescribeRoute.class);
                                 namingIntent.setData(ContentUris.withAppendedId(Tracks.CONTENT_URI, trackId));
                                 namingIntent.putExtra(Constants.NAME, trackName);
                                 RouteList.this.mExportListener = new ProgressListener()
                                    {
                                       @Override
                                       public void setIndeterminate(final boolean indeterminate)
                                       {
                                          progressbar.setIndeterminate(indeterminate);
                                       }

                                       @Override
                                       public void started()
                                       {
                                          checkbox.setVisibility(View.INVISIBLE);
                                          progressbar.setVisibility(View.VISIBLE);
                                       }

                                       @Override
                                       public void finished(final Uri result)
                                       {
                                          checkbox.setVisibility(View.VISIBLE);
                                          progressbar.setVisibility(View.INVISIBLE);
                                          progressbar.setIndeterminate(false);
                                       }

                                       @Override
                                       public void setProgress(final int value)
                                       {
                                          progressbar.setProgress(value);
                                       }

                                       @Override
                                       public void showError(final String task, final String errorMessage, final Exception exception)
                                       {
                                          RouteList.this.showError(task, errorMessage, exception);
                                       }
                                    };
                                 startActivityForResult(namingIntent, DESCRIBE);
                              }
                           }
                        });
                  }
                  else
                  {
                     checkbox.setVisibility(View.INVISIBLE);
                     checkbox.setOnCheckedChangeListener(null);
                  }
                  return true;
               }
               return false;
            }
         });

      setListAdapter(sectionedAdapter);
   }

   private Cursor doSearchWithIntent(final Intent queryIntent)
   {
      final String queryString = queryIntent.getStringExtra(SearchManager.QUERY);
      final Cursor cursor = managedQuery(Tracks.CONTENT_URI, new String[] { BaseColumns._ID, TracksColumns.NAME, TracksColumns.CREATION_TIME }, "name LIKE ?",
            new String[] { "%" + queryString + "%" }, null);
      return cursor;
   }

   /*******************************************************************/
   /** ProgressListener interface and UI actions (non-Javadoc) **/
   /*******************************************************************/

   @Override
   public void setIndeterminate(final boolean indeterminate)
   {
      setProgressBarIndeterminate(indeterminate);
   }

   @Override
   public void started()
   {
      setProgressBarVisibility(true);
      setProgress(Window.PROGRESS_START);
   }

   @Override
   public void finished(final Uri result)
   {
      setProgressBarVisibility(false);
      setProgressBarIndeterminate(false);
   }

   @Override
   public void showError(final String task, final String errorDialogMessage, final Exception errorDialogException)
   {
      this.mErrorTask = task;
      this.mErrorDialogMessage = errorDialogMessage;
      this.mErrorDialogException = errorDialogException;
      Log.e(TAG, errorDialogMessage, errorDialogException);
      if (!isFinishing())
      {
         showDialog(DIALOG_ERROR);
      }
      setProgressBarVisibility(false);
      setProgressBarIndeterminate(false);
   }

   private final ServiceConnection mConnection = new ServiceConnection()
      {
         @Override
         public void onServiceConnected(final ComponentName className, final IBinder service)
         {
            final LocalBinder binder = (LocalBinder) service;
            RouteList.this.mService = binder.getService();
            RouteList.this.mBound = true;
            RouteList.this.mBreadcrumbAdapter.setService(RouteList.this.mService);
         }

         @Override
         public void onServiceDisconnected(final ComponentName arg0)
         {
            RouteList.this.mBound = false;
            RouteList.this.mService = null;
         }
      };

   private final OnClickListener mDeleteOnClickListener = new DialogInterface.OnClickListener()
      {
         @Override
         public void onClick(final DialogInterface dialog, final int which)
         {
            getContentResolver().delete(RouteList.this.mDialogTrackUri, null, null);
         }
      };
   private final OnClickListener mRenameOnClickListener = new DialogInterface.OnClickListener()
      {
         @Override
         public void onClick(final DialogInterface dialog, final int which)
         {
            //         Log.d( TAG, "Context item selected: "+mDialogUri+" with name "+mDialogCurrentName );

            final String trackName = RouteList.this.mTrackNameView.getText().toString();
            final ContentValues values = new ContentValues();
            values.put(TracksColumns.NAME, trackName);
            RouteList.this.getContentResolver().update(RouteList.this.mDialogTrackUri, values, null, null);
         }
      };
   private final OnClickListener mVacuumOnClickListener = new DialogInterface.OnClickListener()
      {
         @Override
         public void onClick(final DialogInterface dialog, final int which)
         {
            final DatabaseHelper helper = new DatabaseHelper(RouteList.this);
            helper.vacuum();
         }
      };
   private final OnClickListener mImportOnClickListener = new DialogInterface.OnClickListener()
      {
         @Override
         public void onClick(final DialogInterface dialog, final int which)
         {
            RouteList.this.mImportAction.run();
         }
      };
   private final DialogInterface.OnClickListener mOiPickerDialogListener = new DialogInterface.OnClickListener()
      {
         @Override
         public void onClick(final DialogInterface dialog, final int which)
         {
            Uri oiDownload = Uri.parse("market://details?id=org.openintents.filemanager");
            Intent oiAboutIntent = new Intent(Intent.ACTION_VIEW, oiDownload);
            try
            {
               startActivity(oiAboutIntent);
            }
            catch (final ActivityNotFoundException e)
            {
               oiDownload = Uri.parse("http://openintents.googlecode.com/files/FileManager-1.1.3.apk");
               oiAboutIntent = new Intent(Intent.ACTION_VIEW, oiDownload);
               startActivity(oiAboutIntent);
            }
         }
      };
   private final BroadcastReceiver mReceiver = new BroadcastReceiver()
      {
         @Override
         public void onReceive(final Context context, final Intent intent)
         {
            if (BreadcrumbsService.NOTIFY_DATA_SET_CHANGED.equals(intent.getAction()))
            {
               RouteList.this.mBreadcrumbAdapter.updateItemList();
            }
         }
      };
}
