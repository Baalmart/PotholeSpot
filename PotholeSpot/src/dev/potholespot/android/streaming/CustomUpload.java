package dev.potholespot.android.streaming;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.ogt.http.HttpResponse;
import org.apache.ogt.http.StatusLine;
import org.apache.ogt.http.client.ClientProtocolException;
import org.apache.ogt.http.client.HttpClient;
import org.apache.ogt.http.client.methods.HttpGet;
import org.apache.ogt.http.impl.client.DefaultHttpClient;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import dev.potholespot.android.util.Constants;
import dev.potholespot.android.viewer.ApplicationPreferenceActivity;
import dev.potholespot.uganda.R;
import dev.potholespot.android.streaming.CustomUpload;

public class CustomUpload extends BroadcastReceiver
{
   private static final String CUSTOMUPLOAD_BACKLOG_DEFAULT = "20";
   private static CustomUpload sCustomUpload = null;
   private static final String TAG = "OGT.CustomUpload";
   private static final int NOTIFICATION_ID = R.string.customupload_failed;
   private static Queue<HttpGet> sRequestBacklog = new LinkedList<HttpGet>();

   public static synchronized void initStreaming(final Context ctx)
   {
      if (sCustomUpload != null)
      {
         shutdownStreaming(ctx);
      }
      sCustomUpload = new CustomUpload();
      sRequestBacklog = new LinkedList<HttpGet>();

      final IntentFilter filter = new IntentFilter(Constants.STREAMBROADCAST);
      ctx.registerReceiver(sCustomUpload, filter);
   }

   public static synchronized void shutdownStreaming(final Context ctx)
   {
      if (sCustomUpload != null)
      {
         ctx.unregisterReceiver(sCustomUpload);
         sCustomUpload.onShutdown();
         sCustomUpload = null;
      }
   }

   private void onShutdown()
   {
   }

   @Override
   public void onReceive(final Context context, final Intent intent)
   {
      final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
      final String prefUrl = preferences.getString(ApplicationPreferenceActivity.CUSTOMUPLOAD_URL, "http://www.example.com");
      final Integer prefBacklog = Integer.valueOf(preferences.getString(ApplicationPreferenceActivity.CUSTOMUPLOAD_BACKLOG, CUSTOMUPLOAD_BACKLOG_DEFAULT));
      final Location loc = intent.getParcelableExtra(Constants.EXTRA_LOCATION);
      final Uri trackUri = intent.getParcelableExtra(Constants.EXTRA_TRACK);
      String buildUrl = prefUrl;
      buildUrl = buildUrl.replace("@LAT@", Double.toString(loc.getLatitude()));
      buildUrl = buildUrl.replace("@LON@", Double.toString(loc.getLongitude()));
      buildUrl = buildUrl.replace("@ID@", trackUri.getLastPathSegment());
      buildUrl = buildUrl.replace("@TIME@", Long.toString(loc.getTime()));
      buildUrl = buildUrl.replace("@SPEED@", Float.toString(loc.getSpeed()));
      buildUrl = buildUrl.replace("@ACC@", Float.toString(loc.getAccuracy()));
      buildUrl = buildUrl.replace("@ALT@", Double.toString(loc.getAltitude()));
      buildUrl = buildUrl.replace("@BEAR@", Float.toString(loc.getBearing()));

      final HttpClient client = new DefaultHttpClient();
      URI uploadUri;
      try
      {
         uploadUri = new URI(buildUrl);
         final HttpGet currentRequest = new HttpGet(uploadUri);
         sRequestBacklog.add(currentRequest);
         if (sRequestBacklog.size() > prefBacklog)
         {
            sRequestBacklog.poll();
         }

         while (!sRequestBacklog.isEmpty())
         {
            final HttpGet request = sRequestBacklog.peek();
            final HttpResponse response = client.execute(request);
            sRequestBacklog.poll();
            final StatusLine status = response.getStatusLine();
            if (status.getStatusCode() != 200)
            {
               throw new IOException("Invalid response from server: " + status.toString());
            }
            clearNotification(context);
         }
      }
      catch (final URISyntaxException e)
      {
         notifyError(context, e);
      }
      catch (final ClientProtocolException e)
      {
         notifyError(context, e);
      }
      catch (final IOException e)
      {
         notifyError(context, e);
      }
   }
 
   /*
   private void notifyError(final Context context, final Exception e)
   {
      Log.e(TAG, "Custom upload failed", e);
      final String ns = Context.NOTIFICATION_SERVICE;
      final NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);

      final int icon = R.drawable.ic_maps_indicator_current_position;
      final CharSequence tickerText = context.getText(R.string.customupload_failed);
      final long when = System.currentTimeMillis();
      final Notification notification = new Notification(icon, tickerText, when);

      final Context appContext = context.getApplicationContext();
      final CharSequence contentTitle = tickerText;
      final CharSequence contentText = e.getMessage();
      final Intent notificationIntent = new Intent(context, CustomUpload.class);
      final PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
      notification.setLatestEventInfo(appContext, contentTitle, contentText, contentIntent);
      notification.flags = Notification.FLAG_AUTO_CANCEL;

      mNotificationManager.notify(NOTIFICATION_ID, notification);
   }
   */
   private void notifyError(final Context context, final Exception e)
   {      
      Log.e( TAG, "Custom upload failed", e);
      String ns = Context.NOTIFICATION_SERVICE;
      NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);
      
      int icon = R.drawable.ic_maps_indicator_current_position;
      CharSequence tickerText = context.getText(R.string.customupload_failed);
      long when = System.currentTimeMillis();
     // Notification notification = new Notification(icon, tickerText, when);
      
      Context appContext = context.getApplicationContext();
      CharSequence contentTitle = tickerText;
      CharSequence contentText = e.getMessage();
      Intent notificationIntent = new Intent(context, CustomUpload.class);
      PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
      
     
      Notification.Builder builder = new Notification.Builder(appContext);
      builder.setAutoCancel(true);
      builder.setTicker(tickerText);
      builder.setSmallIcon(icon);
      builder.setWhen(when);
      builder.setContentTitle(contentTitle);               
      builder.setContentText(contentText);
      builder.setContentIntent(contentIntent);
      
      
      Notification notification = builder.build();
      mNotificationManager.notify(NOTIFICATION_ID, notification);
   }

   private void clearNotification(final Context context)
   {
      final String ns = Context.NOTIFICATION_SERVICE;
      final NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);
      mNotificationManager.cancel(NOTIFICATION_ID);
   }

}