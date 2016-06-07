package dev.potholespot.android.breadcrumbs;

import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import org.apache.ogt.http.conn.ClientConnectionManager;
import org.apache.ogt.http.conn.scheme.PlainSocketFactory;
import org.apache.ogt.http.conn.scheme.Scheme;
import org.apache.ogt.http.conn.scheme.SchemeRegistry;
import org.apache.ogt.http.impl.client.DefaultHttpClient;
import org.apache.ogt.http.impl.conn.tsccm.ThreadSafeClientConnManager;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import dev.potholespot.android.actions.utils.ProgressListener;
import dev.potholespot.android.oauth.PrepareRequestTokenActivity;
import dev.potholespot.android.util.Constants;
import dev.potholespot.android.util.Pair;
import dev.potholespot.uganda.R;

/**
 * ????
 * 
 * @version $Id:$
 * @author Martin Bbaale
 */
public class BreadcrumbsService extends Service implements Observer, ProgressListener
{
   public static final String OAUTH_TOKEN = "breadcrumbs_oauth_token";
   public static final String OAUTH_TOKEN_SECRET = "breadcrumbs_oauth_secret";

   private static final String TAG = "OGT.BreadcrumbsService";
   public static final String NOTIFY_DATA_SET_CHANGED = "dev.potholespot.android.intent.action.NOTIFY_DATA_SET_CHANGED";
   public static final String NOTIFY_PROGRESS_CHANGED = "dev.potholespot.android.intent.action.NOTIFY_PROGRESS_CHANGED";
   public static final String PROGRESS_INDETERMINATE = null;
   public static final String PROGRESS = null;
   public static final String PROGRESS_STATE = null;
   public static final String PROGRESS_RESULT = null;
   public static final String PROGRESS_TASK = null;
   public static final String PROGRESS_MESSAGE = null;
   public static final int PROGRESS_STARTED = 1;
   public static final int PROGRESS_FINISHED = 2;
   public static final int PROGRESS_ERROR = 3;

   private final IBinder mBinder = new LocalBinder();

   private BreadcrumbsTracks mTracks;
   private DefaultHttpClient mHttpClient;
   private OnSharedPreferenceChangeListener tokenChangedListener;
   private boolean mFinishing;
   boolean mAuthorized;
   ExecutorService mExecutor;

   @Override
   public void onCreate()
   {
      super.onCreate();
      this.mExecutor = Executors.newFixedThreadPool(1);
      final SchemeRegistry schemeRegistry = new SchemeRegistry();
      schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
      final ClientConnectionManager cm = new ThreadSafeClientConnManager(schemeRegistry);
      this.mHttpClient = new DefaultHttpClient(cm);

      this.mTracks = new BreadcrumbsTracks(getContentResolver());
      this.mTracks.addObserver(this);

      connectionSetup();
   }

   @Override
   public void onDestroy()
   {
      if (this.tokenChangedListener != null)
      {
         PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this.tokenChangedListener);
      }
      this.mAuthorized = false;
      this.mFinishing = true;
      new AsyncTask<Void, Void, Void>()
         {
            public void executeOn(final Executor executor)
            {
               if (Build.VERSION.SDK_INT >= 11)
               {
                  executeOnExecutor(executor);
               }
               else
               {
                  execute();
               }
            }

            @Override
            protected Void doInBackground(final Void... params)
            {
               BreadcrumbsService.this.mHttpClient.getConnectionManager().shutdown();
               BreadcrumbsService.this.mExecutor.shutdown();
               BreadcrumbsService.this.mHttpClient = null;
               return null;
            }
         }.executeOn(this.mExecutor);
      this.mTracks.persistCache(this);

      super.onDestroy();
   }

   /**
    * Class used for the client Binder. Because we know this service always runs in the same process as its clients, we don't need to deal with IPC.
    */
   public class LocalBinder extends Binder
   {
      public BreadcrumbsService getService()
      {
         return BreadcrumbsService.this;
      }
   }

   /**
    * @see android.app.Service#onBind(android.content.Intent)
    */
   @Override
   public IBinder onBind(final Intent intent)
   {
      return this.mBinder;
   }

   private boolean connectionSetup()
   {
      final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      final String token = prefs.getString(OAUTH_TOKEN, "");
      final String secret = prefs.getString(OAUTH_TOKEN_SECRET, "");
      this.mAuthorized = !"".equals(token) && !"".equals(secret);
      if (this.mAuthorized)
      {
         final CommonsHttpOAuthConsumer consumer = getOAuthConsumer();
         if (this.mTracks.readCache(this))
         {
            new GetBreadcrumbsActivitiesTask(this, this, this, this.mHttpClient, consumer).executeOn(this.mExecutor);
            new GetBreadcrumbsBundlesTask(this, this, this, this.mHttpClient, consumer).executeOn(this.mExecutor);
         }
      }
      return this.mAuthorized;
   }

   public CommonsHttpOAuthConsumer getOAuthConsumer()
   {
      final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      final String token = prefs.getString(OAUTH_TOKEN, "");
      final String secret = prefs.getString(OAUTH_TOKEN_SECRET, "");
      final CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(this.getString(R.string.CONSUMER_KEY), this.getString(R.string.CONSUMER_SECRET));
      consumer.setTokenWithSecret(token, secret);
      return consumer;
   }

   public void removeAuthentication()
   {
      Log.w(TAG, "Removing Breadcrumbs OAuth tokens");
      final Editor e = PreferenceManager.getDefaultSharedPreferences(this).edit();
      e.remove(OAUTH_TOKEN);
      e.remove(OAUTH_TOKEN_SECRET);
      e.commit();
   }

   /**
    * Use a locally stored token or start the request activity to collect one
    */
   public void collectBreadcrumbsOauthToken()
   {
      if (!connectionSetup())
      {
         this.tokenChangedListener = new OnSharedPreferenceChangeListener()
            {
               @Override
               public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key)
               {
                  if (OAUTH_TOKEN.equals(key))
                  {
                     PreferenceManager.getDefaultSharedPreferences(BreadcrumbsService.this).unregisterOnSharedPreferenceChangeListener(BreadcrumbsService.this.tokenChangedListener);
                     connectionSetup();
                  }
               }
            };
         PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this.tokenChangedListener);

         final Intent i = new Intent(getApplicationContext(), PrepareRequestTokenActivity.class);
         i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         i.putExtra(PrepareRequestTokenActivity.OAUTH_TOKEN_PREF, OAUTH_TOKEN);
         i.putExtra(PrepareRequestTokenActivity.OAUTH_TOKEN_SECRET_PREF, OAUTH_TOKEN_SECRET);

         i.putExtra(PrepareRequestTokenActivity.CONSUMER_KEY, this.getString(R.string.CONSUMER_KEY));
         i.putExtra(PrepareRequestTokenActivity.CONSUMER_SECRET, this.getString(R.string.CONSUMER_SECRET));
         i.putExtra(PrepareRequestTokenActivity.REQUEST_URL, Constants.REQUEST_URL);
         i.putExtra(PrepareRequestTokenActivity.ACCESS_URL, Constants.ACCESS_URL);
         i.putExtra(PrepareRequestTokenActivity.AUTHORIZE_URL, Constants.AUTHORIZE_URL);

         this.startActivity(i);
      }
   }

   public void startDownloadTask(final Context context, final ProgressListener listener, final Pair<Integer, Integer> track)
   {
      new DownloadBreadcrumbsTrackTask(context, listener, this, this.mHttpClient, getOAuthConsumer(), track).executeOn(this.mExecutor);
   }

   public void startUploadTask(final Context context, final ProgressListener listener, final Uri trackUri, final String name)
   {
      new UploadBreadcrumbsTrackTask(context, this, listener, this.mHttpClient, getOAuthConsumer(), trackUri, name).executeOn(this.mExecutor);
   }

   public boolean isAuthorized()
   {
      return this.mAuthorized;
   }

   public void willDisplayItem(final Pair<Integer, Integer> item)
   {
      if (item.first == Constants.BREADCRUMBS_BUNDLE_ITEM_VIEW_TYPE)
      {
         if (!this.mFinishing && !this.mTracks.areTracksLoaded(item) && !this.mTracks.areTracksLoadingScheduled(item))
         {
            new GetBreadcrumbsTracksTask(this, this, this, this.mHttpClient, getOAuthConsumer(), item.second).executeOn(this.mExecutor);
            this.mTracks.addTracksLoadingScheduled(item);
         }
      }
   }

   public List<Pair<Integer, Integer>> getAllItems()
   {
      final List<Pair<Integer, Integer>> items = this.mTracks.getAllItems();

      return items;
   }

   public List<Pair<Integer, Integer>> getActivityList()
   {
      final List<Pair<Integer, Integer>> activities = this.mTracks.getActivityList();
      return activities;
   }

   public List<Pair<Integer, Integer>> getBundleList()
   {
      final List<Pair<Integer, Integer>> bundles = this.mTracks.getBundleList();

      return bundles;
   }

   public String getValueForItem(final Pair<Integer, Integer> item, final String name)
   {
      return this.mTracks.getValueForItem(item, name);
   }

   public void clearAllCache()
   {
      this.mTracks.clearAllCache(this);
   }

   protected BreadcrumbsTracks getBreadcrumbsTracks()
   {
      return this.mTracks;
   }

   public boolean isLocalTrackSynced(final long trackId)
   {
      return this.mTracks.isLocalTrackSynced(trackId);
   }

   /****
    * Observer interface
    */

   @Override
   public void update(final Observable observable, final Object data)
   {
      final Intent broadcast = new Intent();
      broadcast.setAction(BreadcrumbsService.NOTIFY_DATA_SET_CHANGED);
      getApplicationContext().sendBroadcast(broadcast);
   }

   /****
    * ProgressListener interface
    */

   @Override
   public void setIndeterminate(final boolean indeterminate)
   {
      final Intent broadcast = new Intent();
      broadcast.putExtra(BreadcrumbsService.PROGRESS_INDETERMINATE, indeterminate);
      broadcast.setAction(BreadcrumbsService.NOTIFY_PROGRESS_CHANGED);
      getApplicationContext().sendBroadcast(broadcast);
   }

   @Override
   public void started()
   {
      final Intent broadcast = new Intent();
      broadcast.putExtra(BreadcrumbsService.PROGRESS_STATE, BreadcrumbsService.PROGRESS_STARTED);
      broadcast.setAction(BreadcrumbsService.NOTIFY_PROGRESS_CHANGED);
      getApplicationContext().sendBroadcast(broadcast);
   }

   @Override
   public void setProgress(final int value)
   {
      final Intent broadcast = new Intent();
      broadcast.putExtra(BreadcrumbsService.PROGRESS, value);
      broadcast.setAction(BreadcrumbsService.NOTIFY_PROGRESS_CHANGED);
      getApplicationContext().sendBroadcast(broadcast);
   }

   @Override
   public void finished(final Uri result)
   {
      final Intent broadcast = new Intent();
      broadcast.putExtra(BreadcrumbsService.PROGRESS_STATE, BreadcrumbsService.PROGRESS_FINISHED);
      broadcast.putExtra(BreadcrumbsService.PROGRESS_RESULT, result);
      broadcast.setAction(BreadcrumbsService.NOTIFY_PROGRESS_CHANGED);
      getApplicationContext().sendBroadcast(broadcast);
   }

   @Override
   public void showError(final String task, final String errorMessage, final Exception exception)
   {
      final Intent broadcast = new Intent();
      broadcast.putExtra(BreadcrumbsService.PROGRESS_STATE, BreadcrumbsService.PROGRESS_ERROR);
      broadcast.putExtra(BreadcrumbsService.PROGRESS_TASK, task);
      broadcast.putExtra(BreadcrumbsService.PROGRESS_MESSAGE, errorMessage);
      broadcast.putExtra(BreadcrumbsService.PROGRESS_RESULT, exception);
      broadcast.setAction(BreadcrumbsService.NOTIFY_PROGRESS_CHANGED);
      getApplicationContext().sendBroadcast(broadcast);
   }
}
