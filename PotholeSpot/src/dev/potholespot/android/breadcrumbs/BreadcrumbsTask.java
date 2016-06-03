package dev.potholespot.android.breadcrumbs;

import java.util.concurrent.Executor;

import dev.potholespot.android.actions.utils.ProgressListener;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

/**
 * ????
 * 
 * @version $Id:$
 * @author Martin Bbaale
 */
public abstract class BreadcrumbsTask extends AsyncTask<Void, Void, Void>
{
   private static final String TAG = "OGT.BreadcrumbsTask";

   private ProgressListener mListener;
   private String mErrorText;
   private Exception mException;

   protected BreadcrumbsService mService;

   private String mTask;

   protected Context mContext;

   public BreadcrumbsTask(Context context, BreadcrumbsService adapter, ProgressListener listener)
   {
      mContext = context;
      mListener = listener;
      mService = adapter;
   }

   @TargetApi(11)
   public void executeOn(Executor executor)
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

   protected void handleError(String task, Exception e, String text)
   {
      Log.e(TAG, "Received error will cancel background task " + this.getClass().getName(), e);

      mService.removeAuthentication();
      mTask = task;
      mException = e;
      mErrorText = text;
      cancel(true);
   }

   @Override
   protected void onPreExecute()
   {
      if (mListener != null)
      {
         mListener.setIndeterminate(true);
         mListener.started();
      }
   }

   @Override
   protected void onPostExecute(Void result)
   {
      this.updateTracksData(mService.getBreadcrumbsTracks());
      if (mListener != null)
      {
         mListener.finished(null);
      }
   }

   protected abstract void updateTracksData(BreadcrumbsTracks tracks);

   @Override
   protected void onCancelled()
   {
      if (mListener != null)
      {
         mListener.finished(null);
      }
      if (mListener != null && mErrorText != null && mException != null)
      {
         mListener.showError(mTask, mErrorText, mException);
      }
      else if (mException != null)
      {
         Log.e(TAG, "Incomplete error after cancellation:" + mErrorText, mException);
      }
      else
      {
         Log.e(TAG, "Incomplete error after cancellation:" + mErrorText);
      }
   }
}
