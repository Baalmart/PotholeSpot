package dev.ugasoft.android.gps.actions.tasks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import dev.baalmart.potholespot.R;
import dev.ugasoft.android.gps.actions.utils.ProgressListener;
import dev.ugasoft.android.gps.util.Constants;

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

   public JogmapSharing(Context context, Uri trackUri, String chosenBaseFileName, boolean attachments, ProgressListener listener)
   {
      super(context, trackUri, chosenBaseFileName, attachments, listener);
   }

   @Override
   protected Uri doInBackground(Void... params)
   {
      Uri result = super.doInBackground(params);
      sendToJogmap(result);
      return result;
   }
   
   @Override
   protected void onPostExecute(Uri resultFilename)
   {
      super.onPostExecute(resultFilename);
      
      CharSequence text = mContext.getString(R.string.osm_success) + jogmapResponseText;
      Toast toast = Toast.makeText(mContext, text, Toast.LENGTH_LONG);
      toast.show();   
   }
   
   private void sendToJogmap(Uri fileUri)
   {
      String authCode = PreferenceManager.getDefaultSharedPreferences(mContext).getString(Constants.JOGRUNNER_AUTH, "");
      File gpxFile = new File(fileUri.getEncodedPath());
      HttpClient httpclient = new DefaultHttpClient();
      URI jogmap = null;
      int statusCode = 0;
      HttpEntity responseEntity = null;
      try
      {
         jogmap = new URI(mContext.getString(R.string.jogmap_post_url));
         HttpPost method = new HttpPost(jogmap);

         MultipartEntity entity = new MultipartEntity();
         entity.addPart("id", new StringBody(authCode));
         entity.addPart("mFile", new FileBody(gpxFile));
         method.setEntity(entity);
         HttpResponse response = httpclient.execute(method);

         statusCode = response.getStatusLine().getStatusCode();
         responseEntity = response.getEntity();
         InputStream stream = responseEntity.getContent();
         jogmapResponseText = XmlCreator.convertStreamToString(stream);
      }
      catch (IOException e)
      {
         String text = mContext.getString(R.string.jogmap_failed) + e.getLocalizedMessage();
         handleError(mContext.getString(R.string.jogmap_task), e, text);
      }
      catch (URISyntaxException e)
      {
         String text = mContext.getString(R.string.jogmap_failed) + e.getLocalizedMessage();
         handleError(mContext.getString(R.string.jogmap_task), e, text);
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
      }
      if (statusCode != 200)
      {
         Log.e(TAG, "Wrong status code " + statusCode);
         jogmapResponseText = mContext.getString(R.string.jogmap_failed) + jogmapResponseText;
         handleError(mContext.getString(R.string.jogmap_task), new HttpException("Unexpected status reported by Jogmap"), jogmapResponseText);
      }
   }

   
}
