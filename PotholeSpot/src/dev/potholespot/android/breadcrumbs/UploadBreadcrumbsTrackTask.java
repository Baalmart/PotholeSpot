package dev.potholespot.android.breadcrumbs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.ogt.http.Header;
import org.apache.ogt.http.HttpEntity;
import org.apache.ogt.http.HttpResponse;
import org.apache.ogt.http.client.HttpClient;
import org.apache.ogt.http.client.methods.HttpPost;
import org.apache.ogt.http.entity.mime.HttpMultipartMode;
import org.apache.ogt.http.entity.mime.MultipartEntity;
import org.apache.ogt.http.entity.mime.content.FileBody;
import org.apache.ogt.http.entity.mime.content.StringBody;
import org.apache.ogt.http.util.EntityUtils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import dev.potholespot.android.actions.tasks.GpxCreator;
import dev.potholespot.android.actions.tasks.XmlCreator;
import dev.potholespot.android.actions.utils.ProgressListener;
import dev.potholespot.android.adapter.BreadcrumbsAdapter;
import dev.potholespot.android.db.Pspot.MetaDataColumns;
import dev.potholespot.uganda.R;

/**
 * An asynchronous task that communicates with Twitter to retrieve a request token. (OAuthGetRequestToken) After receiving the request token from Twitter, pop a browser to the user to authorize the
 * Request Token. (OAuthAuthorizeToken)
 */
public class UploadBreadcrumbsTrackTask extends GpxCreator
{

   final String TAG = "OGT.UploadBreadcrumbsTrackTask";
   private final BreadcrumbsService mService;
   private final OAuthConsumer mConsumer;
   private final HttpClient mHttpClient;
   private String mActivityId;
   private String mBundleId;
   private String mDescription;
   private String mIsPublic;
   private String mBundleName;
   private String mBundleDescription;
   private boolean mIsBundleCreated;
   private final List<File> mPhotoUploadQueue;

   /**
    * Constructor: create a new UploadBreadcrumbsTrackTask.
    * 
    * @param context
    * @param adapter
    * @param listener
    * @param httpclient
    * @param consumer
    * @param trackUri
    * @param name
    */
   public UploadBreadcrumbsTrackTask(final Context context, final BreadcrumbsService adapter, final ProgressListener listener, final HttpClient httpclient, final OAuthConsumer consumer,
         final Uri trackUri, final String name)
   {
      super(context, trackUri, name, true, listener);
      this.mService = adapter;
      this.mHttpClient = httpclient;
      this.mConsumer = consumer;
      this.mPhotoUploadQueue = new LinkedList<File>();
   }

   /**
    * Retrieve the OAuth Request Token and present a browser to the user to authorize the token.
    */
   @Override
   protected Uri doInBackground(final Void... params)
   {
      // Leave room in the progressbar for uploading
      determineProgressGoal();
      this.mProgressAdmin.setUpload(true);

      // Build GPX file
      final Uri gpxFile = exportGpx();

      if (isCancelled())
      {
         final String text = this.mContext.getString(R.string.ticker_failed) + " \"http://api.gobreadcrumbs.com/v1/tracks\" " + this.mContext.getString(R.string.error_buildxml);
         handleError(this.mContext.getString(R.string.taskerror_breadcrumbs_upload), new IOException("Fail to execute request due to canceling"), text);
      }

      // Collect GPX Import option params
      this.mActivityId = null;
      this.mBundleId = null;
      this.mDescription = null;
      this.mIsPublic = null;

      final Uri metadataUri = Uri.withAppendedPath(this.mTrackUri, "metadata");
      Cursor cursor = null;
      try
      {
         cursor = this.mContext.getContentResolver().query(metadataUri, new String[] { MetaDataColumns.KEY, MetaDataColumns.VALUE }, null, null, null);
         if (cursor.moveToFirst())
         {
            do
            {
               final String key = cursor.getString(0);
               if (BreadcrumbsTracks.ACTIVITY_ID.equals(key))
               {
                  this.mActivityId = cursor.getString(1);
               }
               else if (BreadcrumbsTracks.BUNDLE_ID.equals(key))
               {
                  this.mBundleId = cursor.getString(1);
               }
               else if (BreadcrumbsTracks.DESCRIPTION.equals(key))
               {
                  this.mDescription = cursor.getString(1);
               }
               else if (BreadcrumbsTracks.ISPUBLIC.equals(key))
               {
                  this.mIsPublic = cursor.getString(1);
               }
            }
            while (cursor.moveToNext());
         }
      }
      finally
      {
         if (cursor != null)
         {
            cursor.close();
         }
      }
      if ("-1".equals(this.mActivityId))
      {
         final String text = "Unable to upload without a activity id stored in meta-data table";
         final IllegalStateException e = new IllegalStateException(text);
         handleError(this.mContext.getString(R.string.taskerror_breadcrumbs_upload), e, text);
      }

      int statusCode = 0;
      String responseText = null;
      Uri trackUri = null;
      HttpEntity responseEntity = null;
      try
      {
         if ("-1".equals(this.mBundleId))
         {
            this.mBundleDescription = "";//mContext.getString(R.string.breadcrumbs_bundledescription);
            this.mBundleName = this.mContext.getString(R.string.app_name);
            this.mBundleId = createOpenGpsTrackerBundle();
         }

         final String gpxString = XmlCreator.convertStreamToString(this.mContext.getContentResolver().openInputStream(gpxFile));

         final HttpPost method = new HttpPost("http://api.gobreadcrumbs.com:80/v1/tracks");
         if (isCancelled())
         {
            throw new IOException("Fail to execute request due to canceling");
         }
         // Build the multipart body with the upload data
         final MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
         entity.addPart("import_type", new StringBody("GPX"));
         //entity.addPart("gpx",         new FileBody(gpxFile));
         entity.addPart("gpx", new StringBody(gpxString));
         entity.addPart("bundle_id", new StringBody(this.mBundleId));
         entity.addPart("activity_id", new StringBody(this.mActivityId));
         entity.addPart("description", new StringBody(this.mDescription));
         //         entity.addPart("difficulty",  new StringBody("3"));
         //         entity.addPart("rating",      new StringBody("4"));
         entity.addPart("public", new StringBody(this.mIsPublic));
         method.setEntity(entity);

         // Execute the POST to OpenStreetMap
         this.mConsumer.sign(method);
         if (BreadcrumbsAdapter.DEBUG)
         {
            Log.d(this.TAG, "HTTP Method " + method.getMethod());
            Log.d(this.TAG, "URI scheme " + method.getURI().getScheme());
            Log.d(this.TAG, "Host name " + method.getURI().getHost());
            Log.d(this.TAG, "Port " + method.getURI().getPort());
            Log.d(this.TAG, "Request path " + method.getURI().getPath());

            Log.d(this.TAG, "Consumer Key: " + this.mConsumer.getConsumerKey());
            Log.d(this.TAG, "Consumer Secret: " + this.mConsumer.getConsumerSecret());
            Log.d(this.TAG, "Token: " + this.mConsumer.getToken());
            Log.d(this.TAG, "Token Secret: " + this.mConsumer.getTokenSecret());

            Log.d(this.TAG, "Execute request: " + method.getURI());
            for (final Header header : method.getAllHeaders())
            {
               Log.d(this.TAG, "   with header: " + header.toString());
            }
         }
         final HttpResponse response = this.mHttpClient.execute(method);
         this.mProgressAdmin.addUploadProgress();

         statusCode = response.getStatusLine().getStatusCode();
         responseEntity = response.getEntity();
         final InputStream stream = responseEntity.getContent();
         responseText = XmlCreator.convertStreamToString(stream);

         if (BreadcrumbsAdapter.DEBUG)
         {
            Log.d(this.TAG, "Upload Response: " + responseText);
         }

         final Pattern p = Pattern.compile(">([0-9]+)</id>");
         final Matcher m = p.matcher(responseText);
         if (m.find())
         {
            final Integer trackId = Integer.valueOf(m.group(1));
            trackUri = Uri.parse("http://api.gobreadcrumbs.com/v1/tracks/" + trackId + "/placemarks.gpx");
            for (final File photo : this.mPhotoUploadQueue)
            {
               uploadPhoto(photo, trackId);
            }
         }

      }
      catch (final OAuthMessageSignerException e)
      {
         this.mService.removeAuthentication();
         handleError(this.mContext.getString(R.string.taskerror_breadcrumbs_upload), e, "Failed to sign the request with authentication signature");
      }
      catch (final OAuthExpectationFailedException e)
      {
         this.mService.removeAuthentication();
         handleError(this.mContext.getString(R.string.taskerror_breadcrumbs_upload), e, "The request did not authenticate");
      }
      catch (final OAuthCommunicationException e)
      {
         this.mService.removeAuthentication();
         handleError(this.mContext.getString(R.string.taskerror_breadcrumbs_upload), e, "The authentication communication failed");
      }
      catch (final IOException e)
      {
         handleError(this.mContext.getString(R.string.taskerror_breadcrumbs_upload), e, "A problem during communication");
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

      if (statusCode == 200 || statusCode == 201)
      {
         if (trackUri == null)
         {
            handleError(this.mContext.getString(R.string.taskerror_breadcrumbs_upload), new IOException("Unable to retrieve URI from response"), responseText);
         }
      }
      else
      {
         //mAdapter.removeAuthentication();

         handleError(this.mContext.getString(R.string.taskerror_breadcrumbs_upload), new IOException("Status code: " + statusCode), responseText);
      }
      return trackUri;
   }

   private String createOpenGpsTrackerBundle() throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException, IOException
   {
      final HttpPost method = new HttpPost("http://api.gobreadcrumbs.com/v1/bundles.xml");
      if (isCancelled())
      {
         throw new IOException("Fail to execute request due to canceling");
      }

      final MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
      entity.addPart("name", new StringBody(this.mBundleName));
      entity.addPart("activity_id", new StringBody(this.mActivityId));
      entity.addPart("description", new StringBody(this.mBundleDescription));
      method.setEntity(entity);

      this.mConsumer.sign(method);
      final HttpResponse response = this.mHttpClient.execute(method);
      final HttpEntity responseEntity = response.getEntity();
      final InputStream stream = responseEntity.getContent();
      final String responseText = XmlCreator.convertStreamToString(stream);
      final Pattern p = Pattern.compile(">([0-9]+)</id>");
      final Matcher m = p.matcher(responseText);
      String bundleId = null;
      if (m.find())
      {
         bundleId = m.group(1);

         final ContentValues values = new ContentValues();
         values.put(MetaDataColumns.KEY, BreadcrumbsTracks.BUNDLE_ID);
         values.put(MetaDataColumns.VALUE, bundleId);
         final Uri metadataUri = Uri.withAppendedPath(this.mTrackUri, "metadata");

         this.mContext.getContentResolver().insert(metadataUri, values);
         this.mIsBundleCreated = true;
      }
      else
      {
         final String text = "Unable to upload (yet) without a bunld id stored in meta-data table";
         final IllegalStateException e = new IllegalStateException(text);
         handleError(this.mContext.getString(R.string.taskerror_breadcrumbs_upload), e, text);
      }
      return bundleId;
   }

   /**
    * Queue's media
    * 
    * @param inputFilePath
    * @return file path relative to the export dir
    * @throws IOException
    */
   @Override
   protected String includeMediaFile(final String inputFilePath) throws IOException
   {
      final File source = new File(inputFilePath);
      if (source.exists())
      {
         this.mProgressAdmin.setPhotoUpload(source.length());
         this.mPhotoUploadQueue.add(source);
      }
      return source.getName();
   }

   private void uploadPhoto(final File photo, final Integer trackId) throws IOException, OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException
   {
      final HttpPost request = new HttpPost("http://api.gobreadcrumbs.com/v1/photos.xml");
      if (isCancelled())
      {
         throw new IOException("Fail to execute request due to canceling");
      }

      final MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
      entity.addPart("name", new StringBody(photo.getName()));
      entity.addPart("track_id", new StringBody(Integer.toString(trackId)));
      //entity.addPart("description", new StringBody(""));
      entity.addPart("file", new FileBody(photo));
      request.setEntity(entity);

      this.mConsumer.sign(request);
      if (BreadcrumbsAdapter.DEBUG)
      {
         Log.d(this.TAG, "Execute request: " + request.getURI());
         for (final Header header : request.getAllHeaders())
         {
            Log.d(this.TAG, "   with header: " + header.toString());
         }
      }
      final HttpResponse response = this.mHttpClient.execute(request);
      final HttpEntity responseEntity = response.getEntity();
      final InputStream stream = responseEntity.getContent();
      final String responseText = XmlCreator.convertStreamToString(stream);

      this.mProgressAdmin.addPhotoUploadProgress(photo.length());

      Log.i(this.TAG, "Uploaded photo " + responseText);
   }

   @Override
   protected void onPostExecute(final Uri result)
   {
      final BreadcrumbsTracks tracks = this.mService.getBreadcrumbsTracks();
      final Uri metadataUri = Uri.withAppendedPath(this.mTrackUri, "metadata");
      final List<String> segments = result.getPathSegments();
      final Integer bcTrackId = Integer.valueOf(segments.get(segments.size() - 2));

      final ArrayList<ContentValues> metaValues = new ArrayList<ContentValues>();

      metaValues.add(buildContentValues(BreadcrumbsTracks.TRACK_ID, Long.toString(bcTrackId)));
      if (this.mDescription != null)
      {
         metaValues.add(buildContentValues(BreadcrumbsTracks.DESCRIPTION, this.mDescription));
      }
      if (this.mIsPublic != null)
      {
         metaValues.add(buildContentValues(BreadcrumbsTracks.ISPUBLIC, this.mIsPublic));
      }
      metaValues.add(buildContentValues(BreadcrumbsTracks.BUNDLE_ID, this.mBundleId));
      metaValues.add(buildContentValues(BreadcrumbsTracks.ACTIVITY_ID, this.mActivityId));

      // Store in OGT provider
      final ContentResolver resolver = this.mContext.getContentResolver();
      resolver.bulkInsert(metadataUri, metaValues.toArray(new ContentValues[1]));

      // Store in Breadcrumbs adapter
      tracks.addSyncedTrack(Long.valueOf(this.mTrackUri.getLastPathSegment()), bcTrackId);
      if (this.mIsBundleCreated)
      {
         this.mService.getBreadcrumbsTracks().addBundle(Integer.parseInt(this.mBundleId), this.mBundleName, this.mBundleDescription);
      }
      //"http://api.gobreadcrumbs.com/v1/tracks/" + trackId + "/placemarks.gpx"
      this.mService.getBreadcrumbsTracks().addTrack(bcTrackId, this.mName, Integer.valueOf(this.mBundleId), this.mDescription, null, null, null, this.mIsPublic, null, null, null, null, null);

      super.onPostExecute(result);
   }

   private ContentValues buildContentValues(final String key, final String value)
   {
      final ContentValues contentValues = new ContentValues();
      contentValues.put(MetaDataColumns.KEY, key);
      contentValues.put(MetaDataColumns.VALUE, value);
      return contentValues;
   }

}