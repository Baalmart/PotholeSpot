package com.potholespot;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.os.Build;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.potholespot.custom.CustomActivity;

import dev.potholespot.uganda.R;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class Setting extends CustomActivity
{
  private boolean isRunning;

  private void setDummyContents()
  {
    isRunning = true;
    //setTouchNClick(R.id.btnDemo);
    new Thread(new Runnable()
    {
      @Override
      public void run()
      {
        final ProgressBar localProgressBar = (ProgressBar)findViewById(R.id.progressBar1);
        final TextView localTextView1 = (TextView)findViewById(R.id.lblProgress1);
        final TextView localTextView2 = (TextView)findViewById(R.id.lblProgress2);
        while (true)
        {
          if (!isRunning)
            return;
          try
          {
            Thread.sleep(1000L);
            runOnUiThread(new Runnable()
            {
              @Override
            public void run()
              {
                int i = 5 + localProgressBar.getProgress();
                if (i > 100)
                  i = 0;
                localProgressBar.setProgress(i);
                localTextView1.setText(i + "MB/100MB");
                localTextView2.setText(i + "%");
              }
            });
          }
          catch (Exception localException)
          {
            while (true)
              localException.printStackTrace();
          }
        }
      }
    }).start();
  }

  @Override
protected void onCreate(Bundle paramBundle)
  {
    super.onCreate(paramBundle);
    setContentView(R.layout.settings);
    setupActionBar();
    setDummyContents();
  }

  @Override
protected void onDestroy()
  {
    super.onDestroy();
    isRunning = false;
  }

  @SuppressWarnings("deprecation")
protected void setupActionBar()
  {
    ActionBar localActionBar = getActionBar();
    localActionBar.setDisplayShowTitleEnabled(true);
    localActionBar.setTitle("Settings");
    localActionBar.setNavigationMode(0);
    localActionBar.setDisplayUseLogoEnabled(true);
    localActionBar.setLogo(R.drawable.ic_gear);
    localActionBar.setDisplayHomeAsUpEnabled(true);
    localActionBar.setHomeButtonEnabled(true);
  }
}
