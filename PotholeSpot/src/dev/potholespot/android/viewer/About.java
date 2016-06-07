package dev.potholespot.android.viewer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.widget.TextView;
import dev.potholespot.uganda.R;

public class About extends Activity
{

   private static final String TAG = "psot.About";

   @Override
   protected void onCreate(final Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.about);
      fillContentFields();

   }

   private void fillContentFields()
   {
      final TextView version = (TextView) findViewById(R.id.version);
      try
      {
         version.setText(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
      }
      catch (final NameNotFoundException e)
      {
         version.setText("");
      }
      final WebView license = (WebView) findViewById(R.id.license_body);
      license.loadUrl("file:///android_asset/license_short.html");
      final WebView contributions = (WebView) findViewById(R.id.contribution_body);
      contributions.loadUrl("file:///android_asset/contributions.html");
      /*
       * WebView notice = (WebView) findViewById(R.id.notices_body); notice.loadUrl("file:///android_asset/notices.html");
       */
   }

   public static String readRawTextFile(final Context ctx, final int resId)
   {
      final InputStream inputStream = ctx.getResources().openRawResource(resId);

      final InputStreamReader inputreader = new InputStreamReader(inputStream);
      final BufferedReader buffreader = new BufferedReader(inputreader);
      String line;
      final StringBuilder text = new StringBuilder();

      try
      {
         while ((line = buffreader.readLine()) != null)
         {
            text.append(line);
            text.append('\n');
         }
      }
      catch (final IOException e)
      {
         Log.e(TAG, "Failed to read raw text resource", e);
      }
      return text.toString();
   }

}
