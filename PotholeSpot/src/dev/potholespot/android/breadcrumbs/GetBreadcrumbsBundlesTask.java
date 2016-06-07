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
public class GetBreadcrumbsBundlesTask extends BreadcrumbsTask
{

   final String TAG = "OGT.GetBreadcrumbsBundlesTask";
   private final OAuthConsumer mConsumer;
   private final DefaultHttpClient mHttpclient;

   private Set<Integer> mBundleIds;
   private LinkedList<Object[]> mBundles;

   /**
    * We pass the OAuth consumer and provider.
    * 
    * @param mContext Required to be able to start the intent to launch the browser.
    * @param httpclient
    * @param listener
    * @param provider The OAuthProvider object
    * @param mConsumer The OAuthConsumer object
    */
   public GetBreadcrumbsBundlesTask(final Context context, final BreadcrumbsService adapter, final ProgressListener listener, final DefaultHttpClient httpclient, final OAuthConsumer consumer)
   {
      super(context, adapter, listener);
      this.mHttpclient = httpclient;
      this.mConsumer = consumer;

   }

   /**
    * Retrieve the OAuth Request Token and present a browser to the user to authorize the token.
    */
   @Override
   protected Void doInBackground(final Void... params)
   {
      HttpEntity responseEntity = null;
      this.mBundleIds = new HashSet<Integer>();
      this.mBundles = new LinkedList<Object[]>();
      try
      {
         final HttpUriRequest request = new HttpGet("http://api.gobreadcrumbs.com/v1/bundles.xml");
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

         String bundleName = null, bundleDescription = null;
         Integer bundleId = null;
         while (eventType != XmlPullParser.END_DOCUMENT)
         {
            if (eventType == XmlPullParser.START_TAG)
            {
               tagName = xpp.getName();
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
               if ("bundle".equals(xpp.getName()) && bundleId != null)
               {
                  this.mBundles.add(new Object[] { bundleId, bundleName, bundleDescription });
               }
               tagName = null;
            }
            else if (eventType == XmlPullParser.TEXT)
            {
               if ("description".equals(tagName))
               {
                  bundleDescription = xpp.getText();
               }
               else if ("id".equals(tagName))
               {
                  bundleId = Integer.parseInt(xpp.getText());
                  this.mBundleIds.add(bundleId);
               }
               else if ("name".equals(tagName))
               {
                  bundleName = xpp.getText();
               }
            }
            eventType = xpp.next();
         }
      }
      catch (final OAuthMessageSignerException e)
      {
         this.mService.removeAuthentication();
         handleError(this.mContext.getString(R.string.taskerror_breadcrumbs_bundle), e, "Failed to sign the request with authentication signature");
      }
      catch (final OAuthExpectationFailedException e)
      {
         this.mService.removeAuthentication();
         handleError(this.mContext.getString(R.string.taskerror_breadcrumbs_bundle), e, "The request did not authenticate");
      }
      catch (final OAuthCommunicationException e)
      {
         this.mService.removeAuthentication();
         handleError(this.mContext.getString(R.string.taskerror_breadcrumbs_bundle), e, "The authentication communication failed");
      }
      catch (final IOException e)
      {
         handleError(this.mContext.getString(R.string.taskerror_breadcrumbs_bundle), e, "A problem during communication");
      }
      catch (final XmlPullParserException e)
      {
         handleError(this.mContext.getString(R.string.taskerror_breadcrumbs_bundle), e, "A problem while reading the XML data");
      }
      catch (final IllegalStateException e)
      {
         handleError(this.mContext.getString(R.string.taskerror_breadcrumbs_bundle), e, "A problem during communication");
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
               Log.w(this.TAG, "Failed closing inputstream");
            }
         }
      }
      return null;
   }

   @Override
   protected void updateTracksData(final BreadcrumbsTracks tracks)
   {
      tracks.setAllBundleIds(this.mBundleIds);

      for (final Object[] bundle : this.mBundles)
      {
         final Integer bundleId = (Integer) bundle[0];
         final String bundleName = (String) bundle[1];
         final String bundleDescription = (String) bundle[2];

         tracks.addBundle(bundleId, bundleName, bundleDescription);
      }
   }
}