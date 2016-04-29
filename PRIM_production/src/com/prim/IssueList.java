package com.prim;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;

import com.prim.custom.CustomActivity;
import com.prim.ui.MapViewer;
import com.prim.ui.Places;

import dev.baalmart.gps.R;


@SuppressLint("NewApi")
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class IssueList extends CustomActivity
{
  private View currentTab;
  private ViewPager pager;
  private SearchView searchView;

  @SuppressWarnings("deprecation")
private void initPager()
  {
    pager = ((ViewPager)findViewById(R.id.pager));
    pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener()
    {
      public void onPageScrollStateChanged(int paramAnonymousInt)
      {
      }

      public void onPageScrolled(int paramAnonymousInt1, float paramAnonymousFloat, int paramAnonymousInt2)
      {
      }

      public void onPageSelected(int paramAnonymousInt)
      {
        IssueList.this.setCurrentTab(paramAnonymousInt);
      }
    });
    pager.setAdapter(new DummyPageAdapter(getSupportFragmentManager()));
  }

  private void initTabs()
  {
    findViewById(R.id.tab1).setOnClickListener(this);
    findViewById(R.id.tab2).setOnClickListener(this);
    setCurrentTab(0);
  }

  private void setCurrentTab(int paramInt)
  {
    if (currentTab != null)
      currentTab.setEnabled(true);
    if (paramInt == 0)
      currentTab = findViewById(R.id.tab1);
    while (true)
    {
      currentTab.setEnabled(false);
      
      if (paramInt == 1)
        currentTab = findViewById(R.id.tab2);
    }    
    //return;
  }
  @Override
  public void onClick(View paramView)
  {
    super.onClick(paramView);
    if (paramView.getId() == R.id.tab1)
      pager.setCurrentItem(0, true);
    while (paramView.getId() != R.id.tab2)
      return;
    pager.setCurrentItem(1, true);
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
@SuppressLint("NewApi")
  @Override
protected void onCreate(Bundle paramBundle)
  {
    super.onCreate(paramBundle);
    setContentView(R.layout.place_list);
    getActionBar().setTitle(getIntent().getStringExtra("title"));
    initTabs();
    initPager();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu paramMenu)
  {
    super.onCreateOptionsMenu(paramMenu);
    getMenuInflater().inflate(R.menu.search_exp, paramMenu);
    if (getActionBar().getTitle() == getString(R.string.nearby))
      paramMenu.findItem(R.id.menu_loc).setVisible(false);
    setupSearchView(paramMenu);
    return true;
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  @Override
public boolean onOptionsItemSelected(MenuItem paramMenuItem)
  {
    if (paramMenuItem.getItemId() == R.id.menu_loc)
    {
      startActivity(new Intent(this, IssueList.class).putExtra("title", getString(R.string.nearby)));
      return true;
    }
    return super.onOptionsItemSelected(paramMenuItem);
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
protected void setupSearchView(Menu paramMenu)
  {
    searchView = ((SearchView)paramMenu.findItem(R.id.menu_search).getActionView());
    searchView.setIconifiedByDefault(true);
    searchView.setQueryHint("Search...");
    searchView.requestFocusFromTouch();
    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
    {
      public boolean onQueryTextChange(String paramAnonymousString)
      {
        return false;
      }

      public boolean onQueryTextSubmit(String paramAnonymousString)
      {
        return true;
      }
    });
    setupSearchViewTheme(searchView);
  }

  protected void setupSearchViewTheme(SearchView paramSearchView)
  {
    ((EditText)paramSearchView.findViewById(paramSearchView.getContext().getResources().getIdentifier("android:id/search_src_text", null, null))).setHintTextColor(getResources().getColor(R.color.white));
    ((ImageView)paramSearchView.findViewById(paramSearchView.getContext().getResources().getIdentifier("android:id/search_close_btn", null, null))).setImageResource(R.drawable.ic_close);
    ((ImageView)paramSearchView.findViewById(paramSearchView.getContext().getResources().getIdentifier("android:id/search_mag_icon", null, null))).setImageResource(R.drawable.ic_search_small);
    paramSearchView.findViewById(paramSearchView.getContext().getResources().getIdentifier("android:id/search_plate", null, null)).setBackgroundResource(R.drawable.edittext_bg_white);
    try
    {
      ((ImageView)paramSearchView.findViewById(paramSearchView.getContext().getResources().getIdentifier("android:id/search_button", null, null))).setImageResource(R.drawable.icon_search);
      return;
    }
    catch (Exception localException)
    {
      localException.printStackTrace();
    }
  }

  private class DummyPageAdapter extends FragmentPagerAdapter
  {
    public DummyPageAdapter(FragmentManager arg2)
    {
      super(arg2);
    }

    public int getCount()
    {
      return 2;
    }

    public Fragment getItem(int paramInt)
    {
      if (paramInt == 0)
        return new MapViewer();
      
      if (paramInt == 1)
        return new Places();
      return null;
    }
  }
}


