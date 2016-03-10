package com.potholespot.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.potholespot.custom.CustomActivity;

import dev.baalmart.potholespot.R;

public class Workout extends Fragment
  implements View.OnClickListener
{
  private void setupView(View paramView)
  {
    View localView1 = paramView.findViewById(R.id.pause);
    localView1.setOnClickListener(this);
    localView1.setOnTouchListener(CustomActivity.TOUCH);
    View localView2 = paramView.findViewById(R.id.finish);
    localView2.setOnClickListener(this);
    localView2.setOnTouchListener(CustomActivity.TOUCH);
  }

  public void onClick(View paramView)
  {
  }

  public View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup, 
		  Bundle paramBundle)
  {
    View localView = paramLayoutInflater.inflate(R.layout.workout, null);
    setupView(localView);
    return localView;
  }
}
