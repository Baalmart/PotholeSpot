package com.potholespot.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.potholespot.custom.CustomActivity;

import dev.potholespot.uganda.R;

public class Workout extends Fragment implements View.OnClickListener
{
   private void setupView(final View paramView)
   {
      final View localView1 = paramView.findViewById(R.id.pause);
      localView1.setOnClickListener(this);
      localView1.setOnTouchListener(CustomActivity.TOUCH);
      final View localView2 = paramView.findViewById(R.id.finish);
      localView2.setOnClickListener(this);
      localView2.setOnTouchListener(CustomActivity.TOUCH);
   }

   @Override
   public void onClick(final View paramView)
   {
   }

   @Override
   public View onCreateView(final LayoutInflater paramLayoutInflater, final ViewGroup paramViewGroup, final Bundle paramBundle)
   {
      final View localView = paramLayoutInflater.inflate(R.layout.workout, null);
      setupView(localView);
      return localView;
   }
}
