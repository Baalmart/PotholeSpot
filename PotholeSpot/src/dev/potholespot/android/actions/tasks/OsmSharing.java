package dev.potholespot.android.actions.tasks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import dev.potholespot.uganda.R;
import dev.potholespot.android.actions.ShareRoute;
import dev.potholespot.android.actions.utils.ProgressListener;
import dev.potholespot.android.db.Pspot;
import dev.potholespot.android.db.Pspot.Media;
import dev.potholespot.android.db.Pspot.MetaData;
import dev.potholespot.android.oauth.PrepareRequestTokenActivity;
import dev.potholespot.android.util.Constants;
import dev.potholespot.android.viewer.map.LoggerMapHelper;
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

   public OsmSharing(Activity context, Uri trackUri, boolean attachments, ProgressListener listener)
   {
      super(context, trackUri, OSM_FILENAME, attachments, listener);
   }
   
   public void resumeOsmSharing(Uri fileUri, Uri trackUri)
   {
      mFileUri = fileUri;
      mTrackUri = trackUri;
      execute();
   }

   @Override
   protected Uri doInBackground(Void... params)
   {
      if( mFileUri == null )
      {
         mFileUri = super.doInBackground(params);
      }
      sendToOsm(mFileUri, mTrackUri);
      return mFileUri;
   }
   
   @Override
   protected void onPostExecute(Uri resultFilename)
   {
      super.onPostExecute(resultFilename);
      
      CharSequence text = mContext.getString(R.string.osm_success) + responseText;
      Toast toast = Toast.makeText(mContext, text, Toast.LENGTH_LONG);
      toast.show();
   }
   
   /**
    * POST a (GPX) file to the 0.6 API of the OpenStreetMap.org website
    * publishing this track to the public.
    * 
    * @param fileUri
    * @param contentType
    */
   private void sendToOsm(final Uri fileUri, final Uri trackUri)
   {
      CommonsHttpOAuthConsumer consumer = osmConnectionSetup();
      if( consumer == null )
      {
         requestOpenstreetmapOauthToken();
         handleError(mContext.getString(R.string.osm_task), null, mContext.getString(R.string.osmauth_message));
      }
      
      String visibility = PreferenceManager.getDefaultSharedPreferences(mContext).getString(Constants.OSM_VISIBILITY, "trackable");
      File gpxFile = new File(fileUri.getEncodedPath());

      String url = mContext.getString(R.string.osm_post_url);
      DefaultHttpClient httpclient = new DefaultHttpClient();
      HttpResponse response = null;
      int statusCode = 0;
      Cursor metaData = null;
      String sources = null;
      HttpEntity responseEntity = null;
      try
      {
         metaData = mContext.getContentResolver().query(Uri.withAppendedPath(trackUri, "metadata"), new String[] { MetaData.VALUE }, MetaData.KEY + " = ? ",
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
         HttpPost method = new HttpPost(url);
         
         String tags = mContext.getString(R.string.osm_tag) + " " +queryForNotes();
         
         // Build the multipart body with the upload data
         MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
         entity.addPart("file", new FileBody(gpxFile));
         entity.addPart("description", new StringBody( ShareRoute.queryForTrackName(mContext.getContentResolver(), mTrackUri)));
         entity.addPart("tags", new StringBody(tags));
         entity.addPart("visibility", new StringBody(visibility));
         method.setEntity(entity);

         // Execute the POST to OpenStreetMap
         consumer.sign(method);
         response = httpclient.execute(method);

         // Read the response
         statusCode = response.getStatusLine().getStatusCode();
         responseEntity = response.getEntity();
         InputStream stream = responseEntity.getContent();
         responseText = XmlCreator.convertStreamToString(stream);
      }
      catch (OAuthMessageSignerException e)
      {
         Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
         editor.remove(OAUTH_TOKEN);
         editor.remove(OAUTH_TOKEN_SECRET);
         editor.commit();
         
         responseText = mContext.getString(R.string.osm_failed) + e.getLocalizedMessage();
         handleError(mContext.getString(R.string.osm_task), e, responseText);
      }
      catch (OAuthExpectationFailedException e)
      {
         Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
         editor.remove(OAUTH_TOKEN);
         editor.remove(OAUTH_TOKEN_SECRET);
         editor.commit();
         
         responseText = mContext.getString(R.string.osm_failed) + e.getLocalizedMessage();
         handleError(mContext.getString(R.string.osm_task), e, responseText);
      }
      catch (OAuthCommunicationException e)
      {
         Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
         editor.remove(OAUTH_TOKEN);
         editor.remove(OAUTH_TOKEN_SECRET);
         editor.commit();
         
         responseText = mContext.getString(R.string.osm_failed) + e.getLocalizedMessage();
         handleError(mContext.getString(R.string.osm_task), e, responseText);
      }
      catch (IOException e)
      {
         responseText = mContext.getString(R.string.osm_failed) + e.getLocalizedMessage();
         handleError(mContext.getString(R.string.osm_task), e, responseText);
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
         Log.e(TAG, "Failed to upload to error code " + statusCode + " " + responseText);
         String text = mContext.getString(R.string.osm_failed) + responseText;
         if( statusCode == 401 )
         {
            Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
            editor.remove(OAUTH_TOKEN);
            editor.remove(OAUTH_TOKEN_SECRET);
            editor.commit();
         }
         
         handleError(mContext.getString(R.string.osm_task), new HttpException("Unexpected status reported by OSM"), text);
      }
   }
   
   private CommonsHttpOAuthConsumer osmConnectionSetup()
   {
      final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
      String token = prefs.getString(OAUTH_TOKEN, "");
      String secret = prefs.getString(OAUTH_TOKEN_SECRET, "");
      boolean mAuthorized = !"".equals(token) && !"".equals(secret);
      CommonsHttpOAuthConsumer consumer = null;
      if (mAuthorized)
      {
         consumer = new CommonsHttpOAuthConsumer(mContext.getString(R.string.OSM_CONSUMER_KEY), mContext.getString(R.string.OSM_CONSUMER_SECRET));
         consumer.setTokenWithSecret(token, secret);
      }
      return consumer;
   }

   private String queryForNotes()
   {
      StringBuilder tags = new StringBuilder();
      ContentResolver resolver = mContext.getContentResolver();
      Cursor mediaCursor = null;
      Uri mediaUri = Uri.withAppendedPath(mTrackUri, "media");
      try
      {
         mediaCursor = resolver.query(mediaUri, new String[] { Media.URI }, null, null, null);
         if (mediaCursor.moveToFirst())
         {
            do
            {
               Uri noteUri = Uri.parse(mediaCursor.getString(0));
               if (noteUri.getScheme().equals("content") && noteUri.getAuthority().equals(Pspot.AUTHORITY + ".string"))
               {
                  String tag = noteUri.getLastPathSegment().trim();
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
      Intent intent = new Intent(mContext.getApplicationContext(), PrepareRequestTokenActivity.class);
      intent.putExtra(PrepareRequestTokenActivity.OAUTH_TOKEN_PREF, OAUTH_TOKEN);
      intent.putExtra(PrepareRequestTokenActivity.OAUTH_TOKEN_SECRET_PREF, OAUTH_TOKEN_SECRET);

      intent.putExtra(PrepareRequestTokenActivity.CONSUMER_KEY, mContext.getString(R.string.OSM_CONSUMER_KEY));
      intent.putExtra(PrepareRequestTokenActivity.CONSUMER_SECRET, mContext.getString(R.string.OSM_CONSUMER_SECRET));
      intent.putExtra(PrepareRequestTokenActivity.REQUEST_URL, Constants.OSM_REQUEST_URL);
      intent.putExtra(PrepareRequestTokenActivity.ACCESS_URL, Constants.OSM_ACCESS_URL);
      intent.putExtra(PrepareRequestTokenActivity.AUTHORIZE_URL, Constants.OSM_AUTHORIZE_URL);

      mContext.startActivity(intent);
   }  
}
