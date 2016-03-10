package com.potholespot.custom;

import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import com.potholespot.utils.TouchEffect;

public class CustomActivity extends FragmentActivity
  implements View.OnClickListener
{
  public static final TouchEffect TOUCH = new TouchEffect();

  public void onClick(View paramView)
  {
  }

  public boolean onOptionsItemSelected(MenuItem paramMenuItem)
  {
    if (paramMenuItem.getItemId() == 16908332)
      finish();
    return super.onOptionsItemSelected(paramMenuItem);
  }

  public View setClick(int paramInt)
  {
    View localView = findViewById(paramInt);
    localView.setOnClickListener(this);
    return localView;
  }

  public View setTouchNClick(int paramInt)
  {
    View localView = setClick(paramInt);
    localView.setOnTouchListener(TOUCH);
    return localView;
  }
}
