package dev.ugasoft.android.gps.streaming;

import dev.ugasoft.android.gps.util.Constants;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class StreamUtils
{
   @SuppressWarnings("unused")
   private static final String TAG = "PRIM.StreamUtils";

   /**
    * Initialize all appropriate stream listeners
    * @param ctx
    */
   public static void initStreams(final Context ctx)
   {
      SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
      boolean streams_enabled = sharedPreferences.getBoolean(Constants.BROADCAST_STREAM, false);
      if (streams_enabled && sharedPreferences.getBoolean("VOICEOVER_ENABLED", false))
      {
         VoiceOver.initStreaming(ctx);
      }
      if (streams_enabled && sharedPreferences.getBoolean("CUSTOMUPLOAD_ENABLED", false))
      {
         CustomUpload.initStreaming(ctx);
      }
   }
   
   /**
    * Shutdown all stream listeners
    * 
    * @param ctx
    */
   public static void shutdownStreams(Context ctx)
   {
      VoiceOver.shutdownStreaming(ctx);
      CustomUpload.shutdownStreaming(ctx);
   }
}
