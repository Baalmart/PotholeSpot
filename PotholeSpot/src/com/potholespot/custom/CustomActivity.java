package com.potholespot.custom;

import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;

import com.potholespot.utils.TouchEffect;

public class CustomActivity extends FragmentActivity implements View.OnClickListener
{
   public static final TouchEffect TOUCH = new TouchEffect();

   @Override
   public void onClick(final View paramView)
   {
   }

   @Override
   public boolean onOptionsItemSelected(final MenuItem paramMenuItem)
   {
      if (paramMenuItem.getItemId() == 16908332)
      {
         finish();
      }
      return super.onOptionsItemSelected(paramMenuItem);
   }

   public View setClick(final int paramInt)
   {
      final View localView = findViewById(paramInt);
      localView.setOnClickListener(this);
      return localView;
   }

   public View setTouchNClick(final int paramInt)
   {
      final View localView = setClick(paramInt);
      localView.setOnTouchListener(TOUCH);
      return localView;
   }
}
