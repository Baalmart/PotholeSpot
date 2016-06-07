package dev.potholespot.android.streaming;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import dev.potholespot.android.util.Constants;
import dev.potholespot.uganda.R;

public class VoiceOver extends BroadcastReceiver implements TextToSpeech.OnInitListener
{
   private static VoiceOver sVoiceOver = null;
   private static final String TAG = "PRIM.VoiceOver";

   public static synchronized void initStreaming(final Context ctx)
   {
      if (sVoiceOver != null)
      {
         shutdownStreaming(ctx);
      }
      sVoiceOver = new VoiceOver(ctx);

      final IntentFilter filter = new IntentFilter(Constants.STREAMBROADCAST);
      ctx.registerReceiver(sVoiceOver, filter);
   }

   public static synchronized void shutdownStreaming(final Context ctx)
   {
      if (sVoiceOver != null)
      {
         ctx.unregisterReceiver(sVoiceOver);
         sVoiceOver.onShutdown();
         sVoiceOver = null;
      }
   }

   private final TextToSpeech mTextToSpeech;
   private int mVoiceStatus = -1;
   private final Context mContext;

   public VoiceOver(final Context ctx)
   {
      this.mContext = ctx.getApplicationContext();
      this.mTextToSpeech = new TextToSpeech(this.mContext, this);
   }

   @Override
   public void onInit(final int status)
   {
      this.mVoiceStatus = status;
   }

   private void onShutdown()
   {
      this.mVoiceStatus = -1;
      this.mTextToSpeech.shutdown();
   }

   @Override
   public void onReceive(final Context context, final Intent intent)
   {
      if (this.mVoiceStatus == TextToSpeech.SUCCESS)
      {
         final int meters = intent.getIntExtra(Constants.EXTRA_DISTANCE, 0);
         final int minutes = intent.getIntExtra(Constants.EXTRA_TIME, 0);
         final String myText = context.getString(R.string.voiceover_speaking, minutes, meters);
         this.mTextToSpeech.speak(myText, TextToSpeech.QUEUE_ADD, null);
      }
      else
      {
         Log.w(TAG, "Voice stream failed TTS not ready");
      }
   }
}