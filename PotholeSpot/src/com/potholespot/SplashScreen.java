package com.potholespot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import dev.potholespot.uganda.R;

public class SplashScreen extends Activity
{
   private boolean isRunning;

   private void doFinish()
   {
      try
      {
         if (this.isRunning)
         {

            //CommonLoggerMap.class
            this.isRunning = false;
            final Intent localIntent = new Intent(this, MainActivity.class);
            localIntent.addFlags(67108864);
            //67108864
            startActivity(localIntent);
            finish();
         }
         return;
      }
      finally
      {
         /*
          * localObject = finally; throw localObject;
          */
      }
   }

   private void startSplash()
   {
      new Thread(new Runnable()
         {
            @Override
            public void run()
            {
               try
               {
                  Thread.sleep(3000L);
                  return;
               }
               catch (final Exception localException)
               {
                  localException.printStackTrace();
                  return;
               }
               finally
               {
                  runOnUiThread(new Runnable()
                     {
                        @Override
                        public void run()
                        {
                           SplashScreen.this.doFinish();
                        }
                     });
               }
            }
         }).start();
   }

   @Override
   public void onCreate(final Bundle paramBundle)
   {
      super.onCreate(paramBundle);
      setContentView(R.layout.splash);
      this.isRunning = true;
      startSplash();
   }

   @Override
   public boolean onKeyDown(final int paramInt, final KeyEvent paramKeyEvent)
   {
      if (paramInt == 4)
      {
         this.isRunning = false;
         finish();
         return true;
      }
      return super.onKeyDown(paramInt, paramKeyEvent);
   }
}
