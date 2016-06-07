package dev.potholespot.android.breadcrumbs;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.ogt.http.Header;
import org.apache.ogt.http.HttpEntity;
import org.apache.ogt.http.HttpResponse;
import org.apache.ogt.http.client.methods.HttpGet;
import org.apache.ogt.http.client.methods.HttpUriRequest;
import org.apache.ogt.http.impl.client.DefaultHttpClient;
import org.apache.ogt.http.util.EntityUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.util.Log;
import dev.potholespot.android.actions.tasks.XmlCreator;
import dev.potholespot.android.actions.utils.ProgressListener;
import dev.potholespot.android.adapter.BreadcrumbsAdapter;
import dev.potholespot.uganda.R;

/**
 * An asynchronous task that communicates with Twitter to retrieve a request token. (OAuthGetRequestToken) After receiving the request token from Twitter, pop a browser to the user to authorize the
 * Request Token. (OAuthAuthorizeToken)
 */
public class GetBreadcrumbsTracksTask extends BreadcrumbsTask
{

   final String TAG = "OGT.GetBreadcrumbsTracksTask";
   private final OAuthConsumer mConsumer;
   private final DefaultHttpClient mHttpclient;
   private final Integer mBundleId;
   private LinkedList<Object[]> mTracks;

   /**
    * We pass the OAuth consumer and provider.
    * 
    * @param mContext Required to be able to start the intent to launch the browser.
    * @param httpclient
    * @param provider The OAuthProvider object
    * @param mConsumer The OAuthConsumer object
    */
   public GetBreadcrumbsTracksTask(final Context context, final BreadcrumbsService adapter, final ProgressListener listener, final DefaultHttpClient httpclient, final OAuthConsumer consumer,
         final Integer bundleId)
   {
      super(context, adapter, listener);
      this.mHttpclient = httpclient;
      this.mConsumer = consumer;
      this.mBundleId = bundleId;
   }

   /**
    * Retrieve the OAuth Request Token and present a browser to the user to authorize the token.
    */
   @Override
   protected Void doInBackground(final Void... params)
   {
      this.mTracks = new LinkedList<Object[]>();
      HttpEntity responseEntity = null;
      try
      {

         final HttpUriRequest request = new HttpGet("http://api.gobreadcrumbs.com/v1/bundles/" + this.mBundleId + "/tracks.xml");
         if (isCancelled())
         {
            throw new IOException("Fail to execute request due to canceling");
         }
         this.mConsumer.sign(request);
         if (BreadcrumbsAdapter.DEBUG)
         {
            Log.d(this.TAG, "Execute request: " + request.getURI());
            for (final Header header : request.getAllHeaders())
            {
               Log.d(this.TAG, "   with header: " + header.toString());
            }
         }
         final HttpResponse response = this.mHttpclient.execute(request);
         responseEntity = response.getEntity();
         final InputStream is = responseEntity.getContent();
         InputStream stream = new BufferedInputStream(is, 8192);
         if (BreadcrumbsAdapter.DEBUG)
         {
            stream = XmlCreator.convertStreamToLoggedStream(this.TAG, stream);
         }

         final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
         factory.setNamespaceAware(true);
         final XmlPullParser xpp = factory.newPullParser();
         xpp.setInput(stream, "UTF-8");

         String tagName = null;
         int eventType = xpp.getEventType();

         String trackName = null, description = null, difficulty = null, startTime = null, endTime = null, trackRating = null, isPublic = null;
         Integer trackId = null, bundleId = null;
         final Integer totalTime = null;
         Float lat = null, lng = null;
         final Float totalDistance = null;
         while (eventType != XmlPullParser.END_DOCUMENT)
         {
            if (eventType == XmlPullParser.START_TAG)
            {
               tagName = xpp.getName();
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
               if ("track".equals(xpp.getName()) && trackId != null && bundleId != null)
               {
                  this.mTracks.add(new Object[] { trackId, trackName, bundleId, description, difficulty, startTime, endTime, isPublic, lat, lng, totalDistance, totalTime, trackRating });
               }
               tagName = null;
            }
            else if (eventType == XmlPullParser.TEXT)
            {
               if ("bundle-id".equals(tagName))
               {
                  bundleId = Integer.parseInt(xpp.getText());
               }
               else if ("description".equals(tagName))
               {
                  description = xpp.getText();
               }
               else if ("difficulty".equals(tagName))
               {
                  difficulty = xpp.getText();
               }
               else if ("start-time".equals(tagName))
               {
                  startTime = xpp.getText();
               }
               else if ("end-time".equals(tagName))
               {
                  endTime = xpp.getText();
               }
               else if ("id".equals(tagName))
               {
                  trackId = Integer.parseInt(xpp.getText());
               }
               else if ("is-public".equals(tagName))
               {
                  isPublic = xpp.getText();
               }
               else if ("lat".equals(tagName))
               {
                  lat = Float.parseFloat(xpp.getText());
               }
               else if ("lng".equals(tagName))
               {
                  lng = Float.parseFloat(xpp.getText());
               }
               else if ("name".equals(tagName))
               {
                  trackName = xpp.getText();
               }
               else if ("track-rating".equals(tagName))
               {
                  trackRating = xpp.getText();
               }
            }
            eventType = xpp.next();
         }
      }
      catch (final OAuthMessageSignerException e)
      {
         this.mService.removeAuthentication();
         handleError(this.mContext.getString(R.string.taskerror_breadcrumbs_track), e, "Failed to sign the request with authentication signature");
      }
      catch (final OAuthExpectationFailedException e)
      {
         this.mService.removeAuthentication();
         handleError(this.mContext.getString(R.string.taskerror_breadcrumbs_track), e, "The request did not authenticate");
      }
      catch (final OAuthCommunicationException e)
      {
         this.mService.removeAuthentication();
         handleError(this.mContext.getString(R.string.taskerror_breadcrumbs_track), e, "The authentication communication failed");
      }
      catch (final IOException e)
      {
         handleError(this.mContext.getString(R.string.taskerror_breadcrumbs_track), e, "A problem during communication");
      }
      catch (final XmlPullParserException e)
      {
         handleError(this.mContext.getString(R.string.taskerror_breadcrumbs_track), e, "A problem while reading the XML data");
      }
      finally
      {
         if (responseEntity != null)
         {
            try
            {
               EntityUtils.consume(responseEntity);
            }
            catch (final IOException e)
            {
               Log.e(this.TAG, "Failed to close the content stream", e);
            }
         }
      }
      return null;
   }

   @Override
   protected void updateTracksData(final BreadcrumbsTracks tracks)
   {

      final Set<Integer> mTracksIds = new HashSet<Integer>();
      for (final Object[] track : this.mTracks)
      {
         mTracksIds.add((Integer) track[0]);
      }
      tracks.setAllTracksForBundleId(this.mBundleId, mTracksIds);

      for (final Object[] track : this.mTracks)
      {
         final Integer trackId = (Integer) track[0];
         final String trackName = (String) track[1];
         final Integer bundleId = (Integer) track[2];
         final String description = (String) track[3];
         final String difficulty = (String) track[4];
         final String startTime = (String) track[5];
         final String endTime = (String) track[6];
         final String isPublic = (String) track[7];
         final Float lat = (Float) track[8];
         final Float lng = (Float) track[9];
         final Float totalDistance = (Float) track[10];
         final Integer totalTime = (Integer) track[11];
         final String trackRating = (String) track[12];

         tracks.addTrack(trackId, trackName, bundleId, description, difficulty, startTime, endTime, isPublic, lat, lng, totalDistance, totalTime, trackRating);
      }

   }
}