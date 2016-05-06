package dev.potholespot.android.breadcrumbs;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import dev.baalmart.potholespot.R;
import dev.potholespot.android.actions.tasks.GpxParser;
import dev.potholespot.android.actions.tasks.XmlCreator;
import dev.potholespot.android.actions.utils.ProgressListener;
import dev.potholespot.android.adapter.BreadcrumbsAdapter;
import dev.potholespot.android.db.Pspot.MetaData;
import dev.potholespot.android.db.Pspot.Tracks;
import dev.potholespot.android.util.Pair;
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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

/**
 * An asynchronous task that communicates with Twitter to retrieve a request
 * token. (OAuthGetRequestToken) After receiving the request token from Twitter,
 * pop a browser to the user to authorize the Request Token.
 * (OAuthAuthorizeToken)
 */
public class DownloadBreadcrumbsTrackTask extends GpxParser
{

   final String TAG = "OGT.GetBreadcrumbsTracksTask";
   private BreadcrumbsService mAdapter;
   private OAuthConsumer mConsumer;
   private DefaultHttpClient mHttpclient;
   private Pair<Integer, Integer> mTrack;


   /**
    * 
    * Constructor: create a new DownloadBreadcrumbsTrackTask.
    * @param context
    * @param progressListener
    * @param adapter
    * @param httpclient
    * @param consumer
    * @param track
    */
   public DownloadBreadcrumbsTrackTask(Context context, ProgressListener progressListener, BreadcrumbsService adapter, DefaultHttpClient httpclient,
         OAuthConsumer consumer, Pair<Integer, Integer> track)
   {
      super(context, progressListener);
      mAdapter = adapter;
      mHttpclient = httpclient;
      mConsumer = consumer;
      mTrack = track;
   }

   /**
    * Retrieve the OAuth Request Token and present a browser to the user to
    * authorize the token.
    */
   @Override
   protected Uri doInBackground(Uri... params)
   {
      determineProgressGoal(null);

      Uri trackUri = null;
      String trackName = mAdapter.getBreadcrumbsTracks().getValueForItem(mTrack, BreadcrumbsTracks.NAME);
      HttpEntity responseEntity = null;
      try
      {
         HttpUriRequest request = new HttpGet("http://api.gobreadcrumbs.com/v1/tracks/" + mTrack.second + "/placemarks.gpx");
         if (isCancelled())
         {
            throw new IOException("Fail to execute request due to canceling");
         }
         mConsumer.sign(request);
         if( BreadcrumbsAdapter.DEBUG )
         {
            Log.d( TAG, "Execute request: "+request.getURI() );
            for( Header header : request.getAllHeaders() )
            {
               Log.d( TAG, "   with header: "+header.toString());
            }
         }
         HttpResponse response = mHttpclient.execute(request);
         responseEntity = response.getEntity();
         InputStream is = responseEntity.getContent();
         InputStream stream = new BufferedInputStream(is, 8192);
         if( BreadcrumbsAdapter.DEBUG )
         {
            stream = XmlCreator.convertStreamToLoggedStream(TAG, stream);
         }
         trackUri = importTrack(stream, trackName);
      }
      catch (OAuthMessageSignerException e)
      {
         handleError(e, mContext.getString(R.string.error_importgpx_xml));
      }
      catch (OAuthExpectationFailedException e)
      {
         handleError(e, mContext.getString(R.string.error_importgpx_xml));
      }
      catch (OAuthCommunicationException e)
      {
         handleError(e, mContext.getString(R.string.error_importgpx_xml));
      }
      catch (IOException e)
      {
         handleError(e, mContext.getString(R.string.error_importgpx_xml));
      }
      finally
      {
         if (responseEntity != null)
         {
            try
            {
               EntityUtils.consume(responseEntity);
            }
            catch (IOException e)
            {
               Log.e( TAG, "Failed to close the content stream", e);
            }
         }
      }
      return trackUri;
   }

   @Override
   protected void onPostExecute(Uri result)
   {
      super.onPostExecute(result);

      long ogtTrackId = Long.parseLong(result.getLastPathSegment());
      Uri metadataUri = Uri.withAppendedPath(ContentUris.withAppendedId(Tracks.CONTENT_URI, ogtTrackId), "metadata");

      BreadcrumbsTracks tracks = mAdapter.getBreadcrumbsTracks();
      Integer bcTrackId = mTrack.second;
      Integer bcBundleId = tracks.getBundleIdForTrackId(bcTrackId);
      //TODO Integer bcActivityId = tracks.getActivityIdForBundleId(bcBundleId);
      String bcDifficulty = tracks.getValueForItem(mTrack, BreadcrumbsTracks.DIFFICULTY);
      String bcRating = tracks.getValueForItem(mTrack, BreadcrumbsTracks.RATING);
      String bcPublic = tracks.getValueForItem(mTrack, BreadcrumbsTracks.ISPUBLIC);
      String bcDescription = tracks.getValueForItem(mTrack, BreadcrumbsTracks.DESCRIPTION);

      ArrayList<ContentValues> metaValues = new ArrayList<ContentValues>();
      if (bcTrackId != null)
      {
         metaValues.add(buildContentValues(BreadcrumbsTracks.TRACK_ID, Long.toString(bcTrackId)));
      }
      if (bcDescription != null)
      {
         metaValues.add(buildContentValues(BreadcrumbsTracks.DESCRIPTION, bcDescription));
      }
      if (bcDifficulty != null)
      {
         metaValues.add(buildContentValues(BreadcrumbsTracks.DIFFICULTY, bcDifficulty));
      }
      if (bcRating != null)
      {
         metaValues.add(buildContentValues(BreadcrumbsTracks.RATING, bcRating));
      }
      if (bcPublic != null)
      {
         metaValues.add(buildContentValues(BreadcrumbsTracks.ISPUBLIC, bcPublic));
      }
      if (bcBundleId != null)
      {
         metaValues.add(buildContentValues(BreadcrumbsTracks.BUNDLE_ID, Integer.toString(bcBundleId)));
      }
//      if (bcActivityId != null)
//      {
//         metaValues.add(buildContentValues(BreadcrumbsTracks.ACTIVITY_ID, Integer.toString(bcActivityId)));
//      }
      ContentResolver resolver = mContext.getContentResolver();
      resolver.bulkInsert(metadataUri, metaValues.toArray(new ContentValues[1]));
      
      tracks.addSyncedTrack(ogtTrackId, mTrack.second);
      
   }

   private ContentValues buildContentValues(String key, String value)
   {
      ContentValues contentValues = new ContentValues();
      contentValues.put(MetaData.KEY, key);
      contentValues.put(MetaData.VALUE, value);
      return contentValues;
   }

}