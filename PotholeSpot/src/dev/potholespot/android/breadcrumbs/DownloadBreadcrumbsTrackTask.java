package dev.potholespot.android.breadcrumbs;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

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
import dev.potholespot.android.actions.tasks.GpxParser;
import dev.potholespot.android.actions.tasks.XmlCreator;
import dev.potholespot.android.actions.utils.ProgressListener;
import dev.potholespot.android.adapter.BreadcrumbsAdapter;
import dev.potholespot.android.db.Pspot.MetaDataColumns;
import dev.potholespot.android.db.Pspot.Tracks;
import dev.potholespot.android.util.Pair;
import dev.potholespot.uganda.R;

/**
 * An asynchronous task that communicates with Twitter to retrieve a request token. (OAuthGetRequestToken) After receiving the request token from Twitter, pop a browser to the user to authorize the
 * Request Token. (OAuthAuthorizeToken)
 */
public class DownloadBreadcrumbsTrackTask extends GpxParser
{

   final String TAG = "OGT.GetBreadcrumbsTracksTask";
   private final BreadcrumbsService mAdapter;
   private final OAuthConsumer mConsumer;
   private final DefaultHttpClient mHttpclient;
   private final Pair<Integer, Integer> mTrack;

   /**
    * Constructor: create a new DownloadBreadcrumbsTrackTask.
    * 
    * @param context
    * @param progressListener
    * @param adapter
    * @param httpclient
    * @param consumer
    * @param track
    */
   public DownloadBreadcrumbsTrackTask(final Context context, final ProgressListener progressListener, final BreadcrumbsService adapter, final DefaultHttpClient httpclient,
         final OAuthConsumer consumer, final Pair<Integer, Integer> track)
   {
      super(context, progressListener);
      this.mAdapter = adapter;
      this.mHttpclient = httpclient;
      this.mConsumer = consumer;
      this.mTrack = track;
   }

   /**
    * Retrieve the OAuth Request Token and present a browser to the user to authorize the token.
    */
   @Override
   protected Uri doInBackground(final Uri... params)
   {
      determineProgressGoal(null);

      Uri trackUri = null;
      final String trackName = this.mAdapter.getBreadcrumbsTracks().getValueForItem(this.mTrack, BreadcrumbsTracks.NAME);
      HttpEntity responseEntity = null;
      try
      {
         final HttpUriRequest request = new HttpGet("http://api.gobreadcrumbs.com/v1/tracks/" + this.mTrack.second + "/placemarks.gpx");
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
         trackUri = importTrack(stream, trackName);
      }
      catch (final OAuthMessageSignerException e)
      {
         handleError(e, this.mContext.getString(R.string.error_importgpx_xml));
      }
      catch (final OAuthExpectationFailedException e)
      {
         handleError(e, this.mContext.getString(R.string.error_importgpx_xml));
      }
      catch (final OAuthCommunicationException e)
      {
         handleError(e, this.mContext.getString(R.string.error_importgpx_xml));
      }
      catch (final IOException e)
      {
         handleError(e, this.mContext.getString(R.string.error_importgpx_xml));
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
      return trackUri;
   }

   @Override
   protected void onPostExecute(final Uri result)
   {
      super.onPostExecute(result);

      final long ogtTrackId = Long.parseLong(result.getLastPathSegment());
      final Uri metadataUri = Uri.withAppendedPath(ContentUris.withAppendedId(Tracks.CONTENT_URI, ogtTrackId), "metadata");

      final BreadcrumbsTracks tracks = this.mAdapter.getBreadcrumbsTracks();
      final Integer bcTrackId = this.mTrack.second;
      final Integer bcBundleId = tracks.getBundleIdForTrackId(bcTrackId);
      //TODO Integer bcActivityId = tracks.getActivityIdForBundleId(bcBundleId);
      final String bcDifficulty = tracks.getValueForItem(this.mTrack, BreadcrumbsTracks.DIFFICULTY);
      final String bcRating = tracks.getValueForItem(this.mTrack, BreadcrumbsTracks.RATING);
      final String bcPublic = tracks.getValueForItem(this.mTrack, BreadcrumbsTracks.ISPUBLIC);
      final String bcDescription = tracks.getValueForItem(this.mTrack, BreadcrumbsTracks.DESCRIPTION);

      final ArrayList<ContentValues> metaValues = new ArrayList<ContentValues>();
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
      final ContentResolver resolver = this.mContext.getContentResolver();
      resolver.bulkInsert(metadataUri, metaValues.toArray(new ContentValues[1]));

      tracks.addSyncedTrack(ogtTrackId, this.mTrack.second);

   }

   private ContentValues buildContentValues(final String key, final String value)
   {
      final ContentValues contentValues = new ContentValues();
      contentValues.put(MetaDataColumns.KEY, key);
      contentValues.put(MetaDataColumns.VALUE, value);
      return contentValues;
   }

}