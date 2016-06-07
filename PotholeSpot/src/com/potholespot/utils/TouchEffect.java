package com.potholespot.utils;

import android.annotation.TargetApi;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;

@TargetApi(Build.VERSION_CODES.CUPCAKE)
public class TouchEffect implements View.OnTouchListener
{
   @SuppressWarnings("deprecation")
   @Override
   public boolean onTouch(final View paramView, final MotionEvent paramMotionEvent)
   {
      if (paramMotionEvent.getAction() == 0)
      {
         final Drawable localDrawable2 = paramView.getBackground();
         localDrawable2.mutate();
         localDrawable2.setAlpha(150);
         paramView.setBackgroundDrawable(localDrawable2);
      }
      while (true)
      {
         //return false;
         if ((paramMotionEvent.getAction() == 1) || (paramMotionEvent.getAction() == 3))
         {
            final Drawable localDrawable1 = paramView.getBackground();
            localDrawable1.setAlpha(255);
            paramView.setBackgroundDrawable(localDrawable1);
         }
      }
   }
}
