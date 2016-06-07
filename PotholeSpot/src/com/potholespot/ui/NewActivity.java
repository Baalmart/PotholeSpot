package com.potholespot.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import dev.potholespot.uganda.R;

@SuppressLint("InflateParams")
public class NewActivity extends Fragment
{
   private void setupView(final View paramView)
   {
      final View localView = paramView.findViewById(R.id.vSwitch);
      localView.setOnClickListener(new View.OnClickListener()
         {
            @Override
            public void onClick(final View paramAnonymousView)
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
   public View onCreateView(final LayoutInflater paramLayoutInflater, final ViewGroup paramViewGroup, final Bundle paramBundle)
   {
      final View localView = paramLayoutInflater.inflate(R.layout.new_activity, null);
      setupView(localView);
      return localView;
   }
}
