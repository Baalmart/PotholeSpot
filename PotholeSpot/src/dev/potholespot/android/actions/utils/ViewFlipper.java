package dev.potholespot.android.actions.utils;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;

/**
 * Work around based on input from the comment section of <a href="http://code.google.com/p/android/issues/detail?can=2&q=6191&colspec=ID%20Type%20Status%20Owner%20Summary%20Stars&id=6191">Issue
 * 6191</a>
 * 
 * @version $Id$
 * @author Martin Bbaale
 */
public class ViewFlipper extends android.widget.ViewFlipper
{
   private static final String TAG = "OGT.ViewFlipper";

   public ViewFlipper(final Context context)
   {
      super(context);
   }

   public ViewFlipper(final Context context, final AttributeSet attrs)
   {
      super(context, attrs);
   }

   /**
    * On api level 7 unexpected exception occur during orientation switching. These are java.lang.IllegalArgumentException: Receiver not registered: android.widget.ViewFlipper$id exceptions. On level
    * 7, 8 and 9 devices these are ignored.
    */
   @Override
   protected void onDetachedFromWindow()
   {
      if (Build.VERSION.SDK_INT > 7)
      {
         try
         {
            super.onDetachedFromWindow();
         }
         catch (final IllegalArgumentException e)
         {
            Log.w(TAG, "Android project issue 6191 workaround.");
            /* Quick catch and continue on api level 7+, the Eclair 2.1 / 2.2 */
         }
         finally
         {
            super.stopFlipping();
         }
      }
      else
      {
         super.onDetachedFromWindow();
      }
   }
}
