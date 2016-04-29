package com.prim.custom;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import com.prim.utils.TouchEffect;

import dev.baalmart.gps.R;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
@SuppressLint("NewApi")
public class CustomActivity extends FragmentActivity
  implements View.OnClickListener
{
  public static final TouchEffect TOUCH = new TouchEffect();

  public void onClick(View paramView)
  {
  }

  @Override
  protected void onCreate(Bundle paramBundle)
  {
    super.onCreate(paramBundle);
    setupActionBar();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem paramMenuItem)
  {
    /*if (paramMenuItem.getItemId() == 16908332)
      finish();*/
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

  
  //designing the action bar....
  protected void setupActionBar()
  {
    ActionBar localActionBar = getActionBar();
    if (localActionBar == null)
      return;
    localActionBar.setDisplayShowTitleEnabled(true);
    localActionBar.setNavigationMode(0);
    localActionBar.setDisplayUseLogoEnabled(true);
    localActionBar.setLogo(R.drawable.ic_car_white);
    localActionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.action_bar_bg));
    localActionBar.setDisplayHomeAsUpEnabled(true);
    localActionBar.setHomeButtonEnabled(true);
    localActionBar.setTitle(R.string.app_name);
    //localActionBar.setIcon(R.drawable.ic_action_share);
    localActionBar.setIcon(R.drawable.ic_action_share);
  }
}
