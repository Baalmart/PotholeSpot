package dev.potholespot.android.oauth;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

public class RetrieveAccessTokenTask extends AsyncTask<Uri, Void, Void>
{
   private static final String TAG = "OGT.RetrieveAccessTokenTask";
   private final OAuthProvider provider;
   private final OAuthConsumer consumer;
   private final SharedPreferences prefs;
   private final String mTokenKey;
   private final String mSecretKey;

   public RetrieveAccessTokenTask(final Context context, final OAuthConsumer consumer, final OAuthProvider provider, final SharedPreferences prefs, final String tokenKey, final String secretKey)
   {
      this.consumer = consumer;
      this.provider = provider;
      this.prefs = prefs;
      this.mTokenKey = tokenKey;
      this.mSecretKey = secretKey;
   }

   /**
    * Retrieve the oauth_verifier, and store the oauth and oauth_token_secret for future API calls.
    */
   @Override
   protected Void doInBackground(final Uri... params)
   {
      final Uri uri = params[0];
      final String oauth_verifier = uri.getQueryParameter(OAuth.OAUTH_VERIFIER);

      try
      {
         this.provider.retrieveAccessToken(this.consumer, oauth_verifier);

         final Editor edit = this.prefs.edit();
         edit.putString(this.mTokenKey, this.consumer.getToken());
         edit.putString(this.mSecretKey, this.consumer.getTokenSecret());
         edit.commit();

         Log.i(TAG, "OAuth - Access Token Retrieved and stored to " + this.mTokenKey + " and " + this.mSecretKey);
      }
      catch (final Exception e)
      {
         Log.e(TAG, "OAuth - Access Token Retrieval Error", e);
      }

      return null;
   }
}