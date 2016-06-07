package com.potholespot.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import dev.potholespot.uganda.R;

public class History extends Fragment
{
   private void setupView(final View paramView)
   {
      ((ListView) paramView).setAdapter(new HistoryAdapter());
   }

   @Override
   public View onCreateView(final LayoutInflater paramLayoutInflater, final ViewGroup paramViewGroup, final Bundle paramBundle)
   {
      final View localView = paramLayoutInflater.inflate(R.layout.listview, null);
      setupView(localView);
      return localView;
   }

   private class HistoryAdapter extends BaseAdapter
   {
      private HistoryAdapter()
      {
      }

      @Override
      public int getCount()
      {
         return 20;
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
            paramView = getLayoutInflater(null).inflate(R.layout.history_item, null);
         }
         return paramView;
      }
   }
}
