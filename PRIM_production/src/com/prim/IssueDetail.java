package com.prim;

import android.app.ActionBar;
import android.os.Bundle;

import com.prim.custom.CustomActivity;

import dev.baalmart.gps.R;

public class IssueDetail extends CustomActivity
{

  @Override
  protected void onCreate(Bundle paramBundle)
  {
    super.onCreate(paramBundle);
    setContentView(R.layout.place_details);
    getActionBar().setTitle("issue details");
  }
}
