package com.potholespot.ui;


/**
 * @author Martin Bbaale
 * 
 * **/
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.potholespot.custom.CustomFragment;

//import dev.potholespot.gps.R;
import dev.potholespot.uganda.R;

@SuppressLint({"InflateParams"})
public class Profile extends CustomFragment
{
  public View onCreateView(LayoutInflater paramLayoutInflater, 
		  ViewGroup paramViewGroup, Bundle paramBundle)
  {
    return paramLayoutInflater.inflate(R.layout.profile, null);
  }
}

