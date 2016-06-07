package dev.potholespot.android.logger;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import dev.potholespot.android.util.Constants;

/**
 * Class to interact with the service that tracks and logs the locations
 * 
 * @author Martin Bbaale
 */
public class GPSLoggerServiceManager
{
   private static final String TAG = "OGT.GPSLoggerServiceManager";
   private static final String REMOTE_EXCEPTION = "REMOTE_EXCEPTION";
   private IGPSLoggerServiceRemote mGPSLoggerRemote;
   public final Object mStartLock = new Object();
   private boolean mBound = false;
   /**
    * Class for interacting with the main interface of the service.
    */
   private ServiceConnection mServiceConnection;
   private Runnable mOnServiceConnected;

   public GPSLoggerServiceManager(final Context ctx)
   {
      ctx.startService(new Intent(Constants.SERVICENAME));
   }

   public Location getLastWaypoint()
   {
      synchronized (this.mStartLock)
      {
         Location lastWaypoint = null;
         try
         {
            if (this.mBound)
            {
               lastWaypoint = this.mGPSLoggerRemote.getLastWaypoint();
            }
            else
            {
               Log.w(TAG, "Remote interface to logging service not found. Started: " + this.mBound);
            }
         }
         catch (final RemoteException e)
         {
            Log.e(TAG, "Could get lastWaypoint GPSLoggerService.", e);
         }
         return lastWaypoint;
      }
   }

   public float getTrackedDistance()
   {
      synchronized (this.mStartLock)
      {
         float distance = 0F;
         try
         {
            if (this.mBound)
            {
               distance = this.mGPSLoggerRemote.getTrackedDistance();
            }
            else
            {
               Log.w(TAG, "Remote interface to logging service not found. Started: " + this.mBound);
            }
         }
         catch (final RemoteException e)
         {
            Log.e(TAG, "Could get tracked distance from GPSLoggerService.", e);
         }
         return distance;
      }
   }

   public int getLoggingState()
   {
      synchronized (this.mStartLock)
      {
         int logging = Constants.UNKNOWN;
         try
         {
            if (this.mBound)
            {
               logging = this.mGPSLoggerRemote.loggingState();
               //               Log.d( TAG, "mGPSLoggerRemote tells state to be "+logging );
            }
            else
            {
               Log.w(TAG, "Remote interface to logging service not found. Started: " + this.mBound);
            }
         }
         catch (final RemoteException e)
         {
            Log.e(TAG, "Could stat GPSLoggerService.", e);
         }
         return logging;
      }
   }

   public boolean isMediaPrepared()
   {
      synchronized (this.mStartLock)
      {
         boolean prepared = false;
         try
         {
            if (this.mBound)
            {
               prepared = this.mGPSLoggerRemote.isMediaPrepared();
            }
            else
            {
               Log.w(TAG, "Remote interface to logging service not found. Started: " + this.mBound);
            }
         }
         catch (final RemoteException e)
         {
            Log.e(TAG, "Could stat GPSLoggerService.", e);
         }
         return prepared;
      }
   }

   public long startGPSLogging(final String name)
   {
      synchronized (this.mStartLock)
      {
         if (this.mBound)
         {
            try
            {
               return this.mGPSLoggerRemote.startLogging();
            }
            catch (final RemoteException e)
            {
               Log.e(TAG, "Could not start GPSLoggerService.", e);
            }
         }
         return -1;
      }
   }

   public long startLogging()
   {
      synchronized (this.mStartLock)
      {
         if (this.mBound)
         {
            try
            {
               this.mGPSLoggerRemote.startLogging();
            }
            catch (final RemoteException e)
            {
               Log.e(TAG, "Could not start GPSLoggerService.", e);
            }
         }

      }
      return -1;
   }

   public void pauseGPSLogging()
   {
      synchronized (this.mStartLock)
      {
         if (this.mBound)
         {
            try
            {
               this.mGPSLoggerRemote.pauseLogging();
            }
            catch (final RemoteException e)
            {
               Log.e(TAG, "Could not start GPSLoggerService.", e);
            }
         }
      }
   }

   public long resumeGPSLogging()
   {
      synchronized (this.mStartLock)
      {
         if (this.mBound)
         {
            try
            {
               return this.mGPSLoggerRemote.resumeLogging();
            }
            catch (final RemoteException e)
            {
               Log.e(TAG, "Could not start GPSLoggerService.", e);
            }
         }
         return -1;
      }
   }

   public void stopGPSLogging()
   {
      synchronized (this.mStartLock)
      {
         if (this.mBound)
         {
            try
            {
               this.mGPSLoggerRemote.stopLogging();
            }
            catch (final RemoteException e)
            {
               Log.e(GPSLoggerServiceManager.REMOTE_EXCEPTION, "Could not stop GPSLoggerService.", e);
            }
         }
         else
         {
            Log.e(TAG, "No GPSLoggerRemote service connected to this manager");
         }
      }
   }

   public void storeDerivedDataSource(final String datasource)
   {
      synchronized (this.mStartLock)
      {
         if (this.mBound)
         {
            try
            {
               this.mGPSLoggerRemote.storeDerivedDataSource(datasource);
            }
            catch (final RemoteException e)
            {
               Log.e(GPSLoggerServiceManager.REMOTE_EXCEPTION, "Could not send datasource to GPSLoggerService.", e);
            }
         }
         else
         {
            Log.e(TAG, "No GPSLoggerRemote service connected to this manager");
         }
      }
   }

   public void storeMediaUri(final Uri mediaUri)
   {
      synchronized (this.mStartLock)
      {
         if (this.mBound)
         {
            try
            {
               this.mGPSLoggerRemote.storeMediaUri(mediaUri);
            }
            catch (final RemoteException e)
            {
               Log.e(GPSLoggerServiceManager.REMOTE_EXCEPTION, "Could not send media to GPSLoggerService.", e);
            }
         }
         else
         {
            Log.e(TAG, "No GPSLoggerRemote service connected to this manager");
         }
      }
   }

   /**
    * Means by which an Activity lifecycle aware object hints about binding and unbinding
    * 
    * @param onServiceConnected Run on main thread after the service is bound
    */
   public void startup(final Context context, final Runnable onServiceConnected)
   {
      //      Log.d( TAG, "connectToGPSLoggerService()" );
      synchronized (this.mStartLock)
      {
         if (!this.mBound)
         {
            this.mOnServiceConnected = onServiceConnected;
            this.mServiceConnection = new ServiceConnection()
               {
                  @Override
                  public void onServiceConnected(final ComponentName className, final IBinder service)
                  {
                     synchronized (GPSLoggerServiceManager.this.mStartLock)
                     {
                        //                     Log.d( TAG, "onServiceConnected() "+ Thread.currentThread().getId() );
                        GPSLoggerServiceManager.this.mGPSLoggerRemote = IGPSLoggerServiceRemote.Stub.asInterface(service);
                        GPSLoggerServiceManager.this.mBound = true;
                     }
                     if (GPSLoggerServiceManager.this.mOnServiceConnected != null)
                     {
                        GPSLoggerServiceManager.this.mOnServiceConnected.run();
                        GPSLoggerServiceManager.this.mOnServiceConnected = null;
                     }
                  }

                  @Override
                  public void onServiceDisconnected(final ComponentName className)
                  {
                     synchronized (GPSLoggerServiceManager.this.mStartLock)
                     {
                        //                     Log.d( TAG, "onServiceDisconnected()"+ Thread.currentThread().getId() );
                        GPSLoggerServiceManager.this.mBound = false;
                     }
                  }
               };
            context.bindService(new Intent(Constants.SERVICENAME), this.mServiceConnection, Context.BIND_AUTO_CREATE);
         }
         else
         {
            Log.w(TAG, "Attempting to connect whilst already connected");
         }
      }
   }

   /**
    * Means by which an Activity lifecycle aware object hints about binding and unbinding
    */
   public void shutdown(final Context context)
   {
      //      Log.d( TAG, "disconnectFromGPSLoggerService()" );
      synchronized (this.mStartLock)
      {
         try
         {
            if (this.mBound)
            {
               //               Log.d( TAG, "unbindService()"+this.mServiceConnection );
               context.unbindService(this.mServiceConnection);
               GPSLoggerServiceManager.this.mGPSLoggerRemote = null;
               this.mServiceConnection = null;
               this.mBound = false;
            }
         }
         catch (final IllegalArgumentException e)
         {
            Log.w(TAG, "Failed to unbind a service, prehaps the service disapearded?", e);
         }
      }
   }
}