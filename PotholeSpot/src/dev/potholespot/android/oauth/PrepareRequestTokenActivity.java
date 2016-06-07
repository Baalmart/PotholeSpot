package dev.potholespot.android.oauth;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TextView;
import dev.potholespot.android.util.Constants;
import dev.potholespot.uganda.R;

/**
 * Prepares a OAuthConsumer and OAuthProvider OAuthConsumer is configured with the consumer key & consumer secret. Both key and secret are retrieved from the extras in the Intent OAuthProvider is
 * configured with the 3 OAuth endpoints. These are retrieved from the extras in the Intent. Execute the OAuthRequestTokenTask to retrieve the request, and authorize the request. After the request is
 * authorized, a callback is made here and this activity finishes to return to the last Activity on the stack.
 */
public class PrepareRequestTokenActivity extends Activity
{
   /**
    * Name of the Extra in the intent holding the consumer secret
    */
   public static final String CONSUMER_SECRET = "CONSUMER_SECRET";
   /**
    * Name of the Extra in the intent holding the consumer key
    */
   public static final String CONSUMER_KEY = "CONSUMER_KEY";
   /**
    * Name of the Extra in the intent holding the authorizationWebsiteUrl
    */
   public static final String AUTHORIZE_URL = "AUTHORIZE_URL";
   /**
    * Name of the Extra in the intent holding the accessTokenEndpointUrl
    */
   public static final String ACCESS_URL = "ACCESS_URL";
   /**
    * Name of the Extra in the intent holding the requestTokenEndpointUrl
    */
   public static final String REQUEST_URL = "REQUEST_URL";
   /**
    * String value of the key in the DefaultSharedPreferences in which to store the permission token
    */
   public static final String OAUTH_TOKEN_PREF = "OAUTH_TOKEN";
   /**
    * String value of the key in the DefaultSharedPreferences in which to store the permission secret
    */
   public static final String OAUTH_TOKEN_SECRET_PREF = "OAUTH_TOKEN_SECRET";

   final String TAG = "OGT.PrepareRequestTokenActivity";

   private OAuthConsumer consumer;
   private OAuthProvider provider;

   private String mTokenKey;

   private String mSecretKey;
   private OAuthRequestTokenTask mTask;

   @Override
   public void onCreate(final Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.oauthentication);

      final String key = getIntent().getStringExtra(CONSUMER_KEY);
      final String secret = getIntent().getStringExtra(CONSUMER_SECRET);

      final String requestUrl = getIntent().getStringExtra(REQUEST_URL);
      final String accessUrl = getIntent().getStringExtra(ACCESS_URL);
      final String authUrl = getIntent().getStringExtra(AUTHORIZE_URL);

      final TextView tv = (TextView) findViewById(R.id.detail);
      tv.setText(requestUrl);

      this.mTokenKey = getIntent().getStringExtra(OAUTH_TOKEN_PREF);
      this.mSecretKey = getIntent().getStringExtra(OAUTH_TOKEN_SECRET_PREF);

      this.consumer = new CommonsHttpOAuthConsumer(key, secret);
      this.provider = new CommonsHttpOAuthProvider(requestUrl, accessUrl, authUrl);

      this.mTask = new OAuthRequestTokenTask(this, this.consumer, this.provider);
      this.mTask.execute();
   }

   @Override
   protected void onResume()
   {
      super.onResume();

      // Will not be called if onNewIntent() was called with callback scheme
      final Status status = this.mTask.getStatus();
      if (status != Status.RUNNING)
      {
         finish();
      }
   }

   /**
    * Called when the OAuthRequestTokenTask finishes (user has authorized the request token). The callback URL will be intercepted here.
    */
   @Override
   public void onNewIntent(final Intent intent)
   {
      super.onNewIntent(intent);
      final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      final Uri uri = intent.getData();
      if (uri != null && uri.getScheme().equals(Constants.OAUTH_CALLBACK_SCHEME))
      {
         Log.i(this.TAG, "Callback received : " + uri);
         Log.i(this.TAG, "Retrieving Access Token");
         new RetrieveAccessTokenTask(this, this.consumer, this.provider, prefs, this.mTokenKey, this.mSecretKey).execute(uri);
         finish();
      }
   }
}
