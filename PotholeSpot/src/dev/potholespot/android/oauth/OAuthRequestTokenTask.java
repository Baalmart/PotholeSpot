package dev.potholespot.android.oauth;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import dev.potholespot.android.util.Constants;

/**
 * An asynchronous task that communicates with Twitter to retrieve a request token. (OAuthGetRequestToken) After receiving the request token from Twitter, pop a browser to the user to authorize the
 * Request Token. (OAuthAuthorizeToken)
 */
public class OAuthRequestTokenTask extends AsyncTask<Void, Void, Void>
{

   final String TAG = "OGT.OAuthRequestTokenTask";
   private final Context context;
   private final OAuthProvider provider;
   private final OAuthConsumer consumer;

   /**
    * We pass the OAuth consumer and provider.
    * 
    * @param context Required to be able to start the intent to launch the browser.
    * @param provider The OAuthProvider object
    * @param consumer The OAuthConsumer object
    */
   public OAuthRequestTokenTask(final Context context, final OAuthConsumer consumer, final OAuthProvider provider)
   {
      this.context = context;
      this.consumer = consumer;
      this.provider = provider;
   }

   /**
    * Retrieve the OAuth Request Token and present a browser to the user to authorize the token.
    */
   @Override
   protected Void doInBackground(final Void... params)
   {
      try
      {
         final String url = this.provider.retrieveRequestToken(this.consumer, Constants.OAUTH_CALLBACK_URL);
         final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
         intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_FROM_BACKGROUND);
         this.context.startActivity(intent);
      }
      catch (final Exception e)
      {
         Log.e(this.TAG, "Failed to start token request ", e);
         final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.OAUTH_CALLBACK_URL));
         intent.putExtra("ERROR", e.toString());
         this.context.startActivity(intent);
      }

      return null;
   }

}