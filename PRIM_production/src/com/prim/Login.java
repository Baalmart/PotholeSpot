package com.prim;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;

import com.prim.custom.CustomActivity;

import dev.baalmart.gps.R;


public class Login extends CustomActivity
{
  private void setupView()
  {
    ((Button)setTouchNClick(R.id.btnReg)).setText(Html.fromHtml(getString(R.string.sign_up)));
    setTouchNClick(R.id.btnLogin);
    setTouchNClick(R.id.btnForget);
  }

  @Override
  public void onClick(View paramView)
  {
    super.onClick(paramView);
    if (paramView.getId() == R.id.btnLogin)
    {
      Intent localIntent = new Intent(this, MainActivity.class);
      localIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);  //67108864
      startActivity(localIntent);
      finish();
    }
  }

  @Override
  protected void onCreate(Bundle paramBundle)
  {
    super.onCreate(paramBundle);
    setContentView(R.layout.login);
    setupView();
  }
}
