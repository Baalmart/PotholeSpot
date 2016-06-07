package dev.potholespot.android.actions.tasks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.ogt.http.HttpEntity;
import org.apache.ogt.http.HttpException;
import org.apache.ogt.http.HttpResponse;
import org.apache.ogt.http.client.HttpClient;
import org.apache.ogt.http.client.methods.HttpPost;
import org.apache.ogt.http.entity.mime.MultipartEntity;
import org.apache.ogt.http.entity.mime.content.FileBody;
import org.apache.ogt.http.entity.mime.content.StringBody;
import org.apache.ogt.http.impl.client.DefaultHttpClient;
import org.apache.ogt.http.util.EntityUtils;

import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import dev.potholespot.android.actions.utils.ProgressListener;
import dev.potholespot.android.util.Constants;
import dev.potholespot.uganda.R;

/**
 * ????
 * 
 * @version $Id:$
 * @author Martin Bbaale
 */
public class JogmapSharing extends GpxCreator
{

   private static final String TAG = "OGT.JogmapSharing";
   private String jogmapResponseText;

   public JogmapSharing(final Context context, final Uri trackUri, final String chosenBaseFileName, final boolean attachments, final ProgressListener listener)
   {
      super(context, trackUri, chosenBaseFileName, attachments, listener);
   }

   @Override
   protected Uri doInBackground(final Void... params)
   {
      final Uri result = super.doInBackground(params);
      sendToJogmap(result);
      return result;
   }

   @Override
   protected void onPostExecute(final Uri resultFilename)
   {
      super.onPostExecute(resultFilename);

      final CharSequence text = this.mContext.getString(R.string.osm_success) + this.jogmapResponseText;
      final Toast toast = Toast.makeText(this.mContext, text, Toast.LENGTH_LONG);
      toast.show();
   }

   private void sendToJogmap(final Uri fileUri)
   {
      final String authCode = PreferenceManager.getDefaultSharedPreferences(this.mContext).getString(Constants.JOGRUNNER_AUTH, "");
      final File gpxFile = new File(fileUri.getEncodedPath());
      final HttpClient httpclient = new DefaultHttpClient();
      URI jogmap = null;
      int statusCode = 0;
      HttpEntity responseEntity = null;
      try
      {
         jogmap = new URI(this.mContext.getString(R.string.jogmap_post_url));
         final HttpPost method = new HttpPost(jogmap);

         final MultipartEntity entity = new MultipartEntity();
         entity.addPart("id", new StringBody(authCode));
         entity.addPart("mFile", new FileBody(gpxFile));
         method.setEntity(entity);
         final HttpResponse response = httpclient.execute(method);

         statusCode = response.getStatusLine().getStatusCode();
         responseEntity = response.getEntity();
         final InputStream stream = responseEntity.getContent();
         this.jogmapResponseText = XmlCreator.convertStreamToString(stream);
      }
      catch (final IOException e)
      {
         final String text = this.mContext.getString(R.string.jogmap_failed) + e.getLocalizedMessage();
         handleError(this.mContext.getString(R.string.jogmap_task), e, text);
      }
      catch (final URISyntaxException e)
      {
         final String text = this.mContext.getString(R.string.jogmap_failed) + e.getLocalizedMessage();
         handleError(this.mContext.getString(R.string.jogmap_task), e, text);
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
      }
      if (statusCode != 200)
      {
         Log.e(TAG, "Wrong status code " + statusCode);
         this.jogmapResponseText = this.mContext.getString(R.string.jogmap_failed) + this.jogmapResponseText;
         handleError(this.mContext.getString(R.string.jogmap_task), new HttpException("Unexpected status reported by Jogmap"), this.jogmapResponseText);
      }
   }

}
