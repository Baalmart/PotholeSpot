package com.potholespot.ui;


import dev.baalmart.gps.R;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

public class History extends Fragment
{
  private void setupView(View paramView)
  {
    ((ListView)paramView).setAdapter(new HistoryAdapter());
  }

  public View onCreateView(LayoutInflater paramLayoutInflater, 
		  ViewGroup paramViewGroup, Bundle paramBundle)
  {
    View localView = paramLayoutInflater.inflate(R.layout.listview, null);
    setupView(localView);
    return localView;
  }

  private class HistoryAdapter extends BaseAdapter
  {
    private HistoryAdapter()
    {
    }

    public int getCount()
    {
      return 20;
    }

    public Object getItem(int paramInt)
    {
      return null;
    }

    public long getItemId(int paramInt)
    {
      return paramInt;
    }

    public View getView(int paramInt, View paramView, ViewGroup paramViewGroup)
    {
      if (paramView == null)
        paramView = getLayoutInflater(null).inflate(R.layout.history_item, null);
      return paramView;
    }
  }
}
