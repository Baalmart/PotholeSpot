package dev.potholespot.android.actions.tasks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.ogt.http.HttpEntity;
import org.apache.ogt.http.HttpException;
import org.apache.ogt.http.HttpResponse;
import org.apache.ogt.http.client.methods.HttpPost;
import org.apache.ogt.http.entity.mime.HttpMultipartMode;
import org.apache.ogt.http.entity.mime.MultipartEntity;
import org.apache.ogt.http.entity.mime.content.FileBody;
import org.apache.ogt.http.entity.mime.content.StringBody;
import org.apache.ogt.http.impl.client.DefaultHttpClient;
import org.apache.ogt.http.util.EntityUtils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import dev.potholespot.android.actions.ShareRoute;
import dev.potholespot.android.actions.utils.ProgressListener;
import dev.potholespot.android.db.Pspot;
import dev.potholespot.android.db.Pspot.MediaColumns;
import dev.potholespot.android.db.Pspot.MetaDataColumns;
import dev.potholespot.android.oauth.PrepareRequestTokenActivity;
import dev.potholespot.android.util.Constants;
import dev.potholespot.android.viewer.map.LoggerMapHelper;
import dev.potholespot.uganda.R;

/**
 * ????
 * 
 * @version $Id:$
 * @author Martin Bbaale
 */
public class OsmSharing extends GpxCreator
{

   public static final String OAUTH_TOKEN = "openstreetmap_oauth_token";
   public static final String OAUTH_TOKEN_SECRET = "openstreetmap_oauth_secret";
   private static final String TAG = "OGT.OsmSharing";
   public static final String OSM_FILENAME = "OSM_Trace";
   private String responseText;
   private Uri mFileUri;

   public OsmSharing(final Activity context, final Uri trackUri, final boolean attachments, final ProgressListener listener)
   {
      super(context, trackUri, OSM_FILENAME, attachments, listener);
   }

   public void resumeOsmSharing(final Uri fileUri, final Uri trackUri)
   {
      this.mFileUri = fileUri;
      this.mTrackUri = trackUri;
      execute();
   }

   @Override
   protected Uri doInBackground(final Void... params)
   {
      if (this.mFileUri == null)
      {
         this.mFileUri = super.doInBackground(params);
      }
      sendToOsm(this.mFileUri, this.mTrackUri);
      return this.mFileUri;
   }

   @Override
   protected void onPostExecute(final Uri resultFilename)
   {
      super.onPostExecute(resultFilename);

      final CharSequence text = this.mContext.getString(R.string.osm_success) + this.responseText;
      final Toast toast = Toast.makeText(this.mContext, text, Toast.LENGTH_LONG);
      toast.show();
   }

   /**
    * POST a (GPX) file to the 0.6 API of the OpenStreetMap.org website publishing this track to the public.
    * 
    * @param fileUri
    * @param contentType
    */
   private void sendToOsm(final Uri fileUri, final Uri trackUri)
   {
      final CommonsHttpOAuthConsumer consumer = osmConnectionSetup();
      if (consumer == null)
      {
         requestOpenstreetmapOauthToken();
         handleError(this.mContext.getString(R.string.osm_task), null, this.mContext.getString(R.string.osmauth_message));
      }

      final String visibility = PreferenceManager.getDefaultSharedPreferences(this.mContext).getString(Constants.OSM_VISIBILITY, "trackable");
      final File gpxFile = new File(fileUri.getEncodedPath());

      final String url = this.mContext.getString(R.string.osm_post_url);
      final DefaultHttpClient httpclient = new DefaultHttpClient();
      HttpResponse response = null;
      int statusCode = 0;
      Cursor metaData = null;
      String sources = null;
      HttpEntity responseEntity = null;
      try
      {
         metaData = this.mContext.getContentResolver().query(Uri.withAppendedPath(trackUri, "metadata"), new String[] { MetaDataColumns.VALUE }, MetaDataColumns.KEY + " = ? ",
               new String[] { Constants.DATASOURCES_KEY }, null);
         if (metaData.moveToFirst())
         {
            sources = metaData.getString(0);
         }
         if (sources != null && sources.contains(LoggerMapHelper.GOOGLE_PROVIDER))
         {
            throw new IOException("Unable to upload track with materials derived from Google Maps.");
         }

         // The POST to the create node
         final HttpPost method = new HttpPost(url);

         final String tags = this.mContext.getString(R.string.osm_tag) + " " + queryForNotes();

         // Build the multipart body with the upload data
         final MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
         entity.addPart("file", new FileBody(gpxFile));
         entity.addPart("description", new StringBody(ShareRoute.queryForTrackName(this.mContext.getContentResolver(), this.mTrackUri)));
         entity.addPart("tags", new StringBody(tags));
         entity.addPart("visibility", new StringBody(visibility));
         method.setEntity(entity);

         // Execute the POST to OpenStreetMap
         consumer.sign(method);
         response = httpclient.execute(method);

         // Read the response
         statusCode = response.getStatusLine().getStatusCode();
         responseEntity = response.getEntity();
         final InputStream stream = responseEntity.getContent();
         this.responseText = XmlCreator.convertStreamToString(stream);
      }
      catch (final OAuthMessageSignerException e)
      {
         final Editor editor = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
         editor.remove(OAUTH_TOKEN);
         editor.remove(OAUTH_TOKEN_SECRET);
         editor.commit();

         this.responseText = this.mContext.getString(R.string.osm_failed) + e.getLocalizedMessage();
         handleError(this.mContext.getString(R.string.osm_task), e, this.responseText);
      }
      catch (final OAuthExpectationFailedException e)
      {
         final Editor editor = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
         editor.remove(OAUTH_TOKEN);
         editor.remove(OAUTH_TOKEN_SECRET);
         editor.commit();

         this.responseText = this.mContext.getString(R.string.osm_failed) + e.getLocalizedMessage();
         handleError(this.mContext.getString(R.string.osm_task), e, this.responseText);
      }
      catch (final OAuthCommunicationException e)
      {
         final Editor editor = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
         editor.remove(OAUTH_TOKEN);
         editor.remove(OAUTH_TOKEN_SECRET);
         editor.commit();

         this.responseText = this.mContext.getString(R.string.osm_failed) + e.getLocalizedMessage();
         handleError(this.mContext.getString(R.string.osm_task), e, this.responseText);
      }
      catch (final IOException e)
      {
         this.responseText = this.mContext.getString(R.string.osm_failed) + e.getLocalizedMessage();
         handleError(this.mContext.getString(R.string.osm_task), e, this.responseText);
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
               Log.e(TAG, "Failed to close the content stream", e);
            }
         }
         if (metaData != null)
         {
            metaData.close();
         }
      }

      if (statusCode != 200)
      {
         Log.e(TAG, "Failed to upload to error code " + statusCode + " " + this.responseText);
         final String text = this.mContext.getString(R.string.osm_failed) + this.responseText;
         if (statusCode == 401)
         {
            final Editor editor = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
            editor.remove(OAUTH_TOKEN);
            editor.remove(OAUTH_TOKEN_SECRET);
            editor.commit();
         }

         handleError(this.mContext.getString(R.string.osm_task), new HttpException("Unexpected status reported by OSM"), text);
      }
   }

   private CommonsHttpOAuthConsumer osmConnectionSetup()
   {
      final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);
      final String token = prefs.getString(OAUTH_TOKEN, "");
      final String secret = prefs.getString(OAUTH_TOKEN_SECRET, "");
      final boolean mAuthorized = !"".equals(token) && !"".equals(secret);
      CommonsHttpOAuthConsumer consumer = null;
      if (mAuthorized)
      {
         consumer = new CommonsHttpOAuthConsumer(this.mContext.getString(R.string.OSM_CONSUMER_KEY), this.mContext.getString(R.string.OSM_CONSUMER_SECRET));
         consumer.setTokenWithSecret(token, secret);
      }
      return consumer;
   }

   private String queryForNotes()
   {
      final StringBuilder tags = new StringBuilder();
      final ContentResolver resolver = this.mContext.getContentResolver();
      Cursor mediaCursor = null;
      final Uri mediaUri = Uri.withAppendedPath(this.mTrackUri, "media");
      try
      {
         mediaCursor = resolver.query(mediaUri, new String[] { MediaColumns.URI }, null, null, null);
         if (mediaCursor.moveToFirst())
         {
            do
            {
               final Uri noteUri = Uri.parse(mediaCursor.getString(0));
               if (noteUri.getScheme().equals("content") && noteUri.getAuthority().equals(Pspot.AUTHORITY + ".string"))
               {
                  final String tag = noteUri.getLastPathSegment().trim();
                  if (!tag.contains(" "))
                  {
                     if (tags.length() > 0)
                     {
                        tags.append(" ");
                     }
                     tags.append(tag);
                  }
               }
            }
            while (mediaCursor.moveToNext());
         }
      }
      finally
      {
         if (mediaCursor != null)
         {
            mediaCursor.close();
         }
      }
      return tags.toString();
   }

   public void requestOpenstreetmapOauthToken()
   {
      final Intent intent = new Intent(this.mContext.getApplicationContext(), PrepareRequestTokenActivity.class);
      intent.putExtra(PrepareRequestTokenActivity.OAUTH_TOKEN_PREF, OAUTH_TOKEN);
      intent.putExtra(PrepareRequestTokenActivity.OAUTH_TOKEN_SECRET_PREF, OAUTH_TOKEN_SECRET);

      intent.putExtra(PrepareRequestTokenActivity.CONSUMER_KEY, this.mContext.getString(R.string.OSM_CONSUMER_KEY));
      intent.putExtra(PrepareRequestTokenActivity.CONSUMER_SECRET, this.mContext.getString(R.string.OSM_CONSUMER_SECRET));
      intent.putExtra(PrepareRequestTokenActivity.REQUEST_URL, Constants.OSM_REQUEST_URL);
      intent.putExtra(PrepareRequestTokenActivity.ACCESS_URL, Constants.OSM_ACCESS_URL);
      intent.putExtra(PrepareRequestTokenActivity.AUTHORIZE_URL, Constants.OSM_AUTHORIZE_URL);

      this.mContext.startActivity(intent);
   }
}
