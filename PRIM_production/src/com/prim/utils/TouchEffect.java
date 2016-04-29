package com.prim.utils;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class TouchEffect
  implements View.OnTouchListener
{
  @SuppressLint({"ClickableViewAccessibility"})
  public boolean onTouch(View paramView, MotionEvent paramMotionEvent)
  
  {
    if (paramMotionEvent.getAction() == 0)
      paramView.setAlpha(0.7F);
    while (true)
    {    
      if ((paramMotionEvent.getAction() == 1) || (paramMotionEvent.getAction() == 3))
        paramView.setAlpha(1.0F);
      return false;
    }
  }
  
}

