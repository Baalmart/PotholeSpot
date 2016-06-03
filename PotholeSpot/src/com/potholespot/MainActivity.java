package com.potholespot;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.potholespot.custom.CustomActivity;
import com.potholespot.ui.History;
import com.potholespot.ui.NewActivity;
import com.potholespot.ui.Routes;
import com.potholespot.ui.Workout;

import dev.potholespot.uganda.R;
import dev.potholespot.android.viewer.map.CommonLoggerMap;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
@SuppressLint("NewApi")
public class MainActivity extends CustomActivity
{
  private View currentTab;
  private ViewPager pager;

  @SuppressWarnings("deprecation")
private void initPager()
  {
    pager = ((ViewPager)findViewById(R.id.pager));
    pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener()
    {
      @Override
      public void onPageScrollStateChanged(int paramAnonymousInt)
      {
      }

      @Override
      public void onPageScrolled(int paramAnonymousInt1, 
    		  float paramAnonymousFloat, int paramAnonymousInt2)
      {
      }

      @Override
      public void onPageSelected(int paramAnonymousInt)
      {
        MainActivity.this.setCurrentTab(paramAnonymousInt);
      }
    });
    pager.setAdapter(new DummyPageAdapter(getSupportFragmentManager()));
  }

  private void initTabs()
  {
    findViewById(R.id.tab1).setOnClickListener(this);
    findViewById(R.id.tab2).setOnClickListener(this);
    findViewById(R.id.tab3).setOnClickListener(this);
    findViewById(R.id.tab4).setOnClickListener(this);
    //setCurrentTab(0);
  }

  //so the problem is inside this method below:
  
  private void setCurrentTab(int paramInt)
  {
    if (currentTab != null)
      currentTab.setEnabled(true);
    if (paramInt == 0)
      currentTab = findViewById(R.id.tab1);
    
    while (true)
    {
      currentTab.setEnabled(false);
      getActionBar().setTitle(((Button)currentTab).getText().toString());
      //return;
      
      try 
      {
      if (paramInt == 1)
        currentTab = findViewById(R.id.tab2);
      else if (paramInt == 2)
        currentTab = findViewById(R.id.tab3);
      else
        currentTab = findViewById(R.id.tab4);
      }
      catch(Exception e)
      {         
         e.printStackTrace();
      }
    }
  }

  @Override
public void onClick(View paramView)
  {
    super.onClick(paramView);
    if (paramView.getId() == R.id.tab1)
      pager.setCurrentItem(0, true);
/*    do
    {
      //return;
      if (paramView.getId() == R.id.tab2)
      {
        pager.setCurrentItem(1, true);
        return;
      }
      if (paramView.getId() == R.id.tab3)
      {
        pager.setCurrentItem(2, true);
        return;
      }
    }
    while (paramView.getId() != R.id.tab4);
    pager.setCurrentItem(3, true);*/
    

    if (paramView.getId() == R.id.tab2)
    {
      pager.setCurrentItem(1, true);
      return;
    }
    if (paramView.getId() == R.id.tab3)
    {
      pager.setCurrentItem(2, true);
      return;
    }
  
  if (paramView.getId() == R.id.tab4)
  {
      pager.setCurrentItem(3, true);
      return;
  }
  }

  @Override
protected void onCreate(Bundle paramBundle)
  {
    super.onCreate(paramBundle);
    setContentView(R.layout.activity_main);
    setupActionBar();
    initTabs();
    initPager();
  }

  @Override
public boolean onCreateOptionsMenu(Menu paramMenu)
  {
    super.onCreateOptionsMenu(paramMenu);
    getMenuInflater().inflate(R.menu.main, paramMenu);
    return true;
  }

  @Override
public boolean onOptionsItemSelected(MenuItem paramMenuItem)
  {
    if (paramMenuItem.getItemId() == R.id.menu_setting)
    {
      startActivity(new Intent(this, Setting.class));
      return true;
    }
    
    if (paramMenuItem.getItemId() == R.id.menu_map)
    {
      startActivity(new Intent(this, CommonLoggerMap.class));
      return true;
    }
    
    
    return super.onOptionsItemSelected(paramMenuItem);
  }


@SuppressWarnings("deprecation")
protected void setupActionBar()
  {
    ActionBar localActionBar = getActionBar();
    localActionBar.setDisplayShowTitleEnabled(true);
    localActionBar.setNavigationMode(0);
    localActionBar.setDisplayUseLogoEnabled(true);
    localActionBar.setLogo(R.drawable.signage_map);
    localActionBar.setDisplayHomeAsUpEnabled(false);
    localActionBar.setHomeButtonEnabled(false);
  }

  private class DummyPageAdapter extends FragmentPagerAdapter
  {
    public DummyPageAdapter(FragmentManager arg2)
    {
      super(arg2);
    }

    @Override
   public int getCount()
    {
      return 4;
    }

    @Override
   public Fragment getItem(int paramInt)
    {
      if (paramInt == 0)
        return new NewActivity();
      if (paramInt == 1)
        return new Routes();
      if (paramInt == 2)
        return new Workout();
      return new History();
    }
    
  }
}
