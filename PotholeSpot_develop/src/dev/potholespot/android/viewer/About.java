package dev.potholespot.android.viewer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import dev.baalmart.potholespot.R;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.widget.TextView;

public class About extends Activity
{

   private static final String TAG = "psot.About";

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.about);
      fillContentFields();

   }

   private void fillContentFields()
   {
      TextView version = (TextView) findViewById(R.id.version);
      try
      {
         version.setText(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
      }
      catch (NameNotFoundException e)
      {
         version.setText("");
      }
      WebView license = (WebView) findViewById(R.id.license_body);
      license.loadUrl("file:///android_asset/license_short.html");
      WebView contributions = (WebView) findViewById(R.id.contribution_body);
      contributions.loadUrl("file:///android_asset/contributions.html");
     /* WebView notice = (WebView) findViewById(R.id.notices_body);
      notice.loadUrl("file:///android_asset/notices.html");*/
   }

   public static String readRawTextFile(Context ctx, int resId)
   {
      InputStream inputStream = ctx.getResources().openRawResource(resId);

      InputStreamReader inputreader = new InputStreamReader(inputStream);
      BufferedReader buffreader = new BufferedReader(inputreader);
      String line;
      StringBuilder text = new StringBuilder();

      try
      {
         while ((line = buffreader.readLine()) != null)
         {
            text.append(line);
            text.append('\n');
         }
      }
      catch (IOException e)
      {
         Log.e(TAG, "Failed to read raw text resource", e);
      }
      return text.toString();
   }

}
