package com.potholespot.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import dev.potholespot.uganda.R;

@SuppressLint("InflateParams")
public class Routes extends Fragment implements View.OnClickListener
{
   private View tab;

   private void setupView(final View paramView)
   {
      ((ListView) paramView.findViewById(R.id.list)).setAdapter(new RouteAdapter());
      this.tab = paramView.findViewById(R.id.tabNear);
      this.tab.setOnClickListener(this);
      paramView.findViewById(R.id.tabFav).setOnClickListener(this);
   }

   @Override
   public void onClick(final View paramView)
   {
      this.tab.setEnabled(true);
      this.tab = paramView;
      this.tab.setEnabled(false);
   }

   @Override
   public View onCreateView(final LayoutInflater paramLayoutInflater, final ViewGroup paramViewGroup, final Bundle paramBundle)
   {
      final View localView = paramLayoutInflater.inflate(R.layout.routes, null);
      setupView(localView);
      return localView;
   }

   private class RouteAdapter extends BaseAdapter
   {
      private RouteAdapter()
      {
      }

      @Override
      public int getCount()
      {
         return 10;
      }

      @Override
      public Object getItem(final int paramInt)
      {
         return null;
      }

      @Override
      public long getItemId(final int paramInt)
      {
         return paramInt;
      }

      @Override
      public View getView(final int paramInt, View paramView, final ViewGroup paramViewGroup)
      {
         if (paramView == null)
         {
            paramView = getLayoutInflater(null).inflate(R.layout.route_item, null);
         }
         return paramView;
      }
   }
}
