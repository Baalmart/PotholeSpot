package com.potholespot.ui;

import dev.potholespot.uganda.R;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

@SuppressLint("InflateParams")
public class NewActivity extends Fragment
{
  private void setupView(View paramView)
  {
    final View localView = paramView.findViewById(R.id.vSwitch);
    localView.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View paramAnonymousView)
      {
        if (localView.getTag() == null)
        {
          localView.setBackgroundResource(R.drawable.swith_up_right);
          localView.setTag("gps");
          return;
        }
        localView.setBackgroundResource(R.drawable.swith_up_left);
        localView.setTag(null);
      }
    });
  }

  @Override
public View onCreateView(LayoutInflater paramLayoutInflater, 
		  ViewGroup paramViewGroup, Bundle paramBundle)
  {
    View localView = paramLayoutInflater.inflate(R.layout.new_activity, null);
    setupView(localView);
    return localView;
  }
}
