package dev.potholespot.android.breadcrumbs;

import java.util.concurrent.Executor;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import dev.potholespot.android.actions.utils.ProgressListener;

/**
 * ????
 * 
 * @version $Id:$
 * @author Martin Bbaale
 */
public abstract class BreadcrumbsTask extends AsyncTask<Void, Void, Void>
{
   private static final String TAG = "OGT.BreadcrumbsTask";

   private final ProgressListener mListener;
   private String mErrorText;
   private Exception mException;

   protected BreadcrumbsService mService;

   private String mTask;

   protected Context mContext;

   public BreadcrumbsTask(final Context context, final BreadcrumbsService adapter, final ProgressListener listener)
   {
      this.mContext = context;
      this.mListener = listener;
      this.mService = adapter;
   }

   @TargetApi(11)
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

   protected void handleError(final String task, final Exception e, final String text)
   {
      Log.e(TAG, "Received error will cancel background task " + this.getClass().getName(), e);

      this.mService.removeAuthentication();
      this.mTask = task;
      this.mException = e;
      this.mErrorText = text;
      cancel(true);
   }

   @Override
   protected void onPreExecute()
   {
      if (this.mListener != null)
      {
         this.mListener.setIndeterminate(true);
         this.mListener.started();
      }
   }

   @Override
   protected void onPostExecute(final Void result)
   {
      updateTracksData(this.mService.getBreadcrumbsTracks());
      if (this.mListener != null)
      {
         this.mListener.finished(null);
      }
   }

   protected abstract void updateTracksData(BreadcrumbsTracks tracks);

   @Override
   protected void onCancelled()
   {
      if (this.mListener != null)
      {
         this.mListener.finished(null);
      }
      if (this.mListener != null && this.mErrorText != null && this.mException != null)
      {
         this.mListener.showError(this.mTask, this.mErrorText, this.mException);
      }
      else if (this.mException != null)
      {
         Log.e(TAG, "Incomplete error after cancellation:" + this.mErrorText, this.mException);
      }
      else
      {
         Log.e(TAG, "Incomplete error after cancellation:" + this.mErrorText);
      }
   }
}
