package com.prim.ui;


/**
 * @author Martin Bbaale
 * 
 * **/
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.prim.custom.CustomFragment;

import dev.baalmart.gps.R;

public class Settings extends CustomFragment
{
  @SuppressLint({"InflateParams"})
  public View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup, Bundle paramBundle)
  {
    View localView = paramLayoutInflater.inflate(R.layout.settings_prim, null);
    setTouchNClick(localView.findViewById(R.id.lbl1));
    setTouchNClick(localView.findViewById(R.id.lbl2));
    setTouchNClick(localView.findViewById(R.id.lbl3));
    setTouchNClick(localView.findViewById(R.id.lbl4));
    setTouchNClick(localView.findViewById(R.id.lbl5));
    return localView;
  }
}
