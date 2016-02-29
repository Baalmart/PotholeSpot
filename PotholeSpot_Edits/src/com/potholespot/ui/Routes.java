package com.potholespot.ui;

import dev.baalmart.pothole.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

public class Routes extends Fragment
  implements View.OnClickListener
{
  private View tab;

  private void setupView(View paramView)
  {
    ((ListView)paramView.findViewById(R.id.list)).setAdapter(new RouteAdapter());
    tab = paramView.findViewById(R.id.tabNear);
    tab.setOnClickListener(this);
    paramView.findViewById(R.id.tabFav).setOnClickListener(this);
  }

  public void onClick(View paramView)
  {
    tab.setEnabled(true);
    tab = paramView;
    tab.setEnabled(false);
  }

  public View onCreateView(LayoutInflater paramLayoutInflater, 
		  ViewGroup paramViewGroup, Bundle paramBundle)
  {
    View localView = paramLayoutInflater.inflate(R.layout.routes, null);
    setupView(localView);
    return localView;
  }

  private class RouteAdapter extends BaseAdapter
  {
    private RouteAdapter()
    {
    }

    public int getCount()
    {
      return 10;
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
        paramView = getLayoutInflater(null).inflate(R.layout.route_item, null);
      return paramView;
    }
  }
}
