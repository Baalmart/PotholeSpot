package com.potholespot.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.potholespot.model.*;

import dev.potholespot.uganda.R;
import java.util.ArrayList;

public class LeftNavAdapter extends BaseAdapter
{
  private Context context;
  private ArrayList<Data> items;
  private int selection;

  //instantiates a new left navigation adapter
  public LeftNavAdapter(Context paramContext, ArrayList<Data> paramArrayList)
  {
    context = paramContext; //context of this application
    items = paramArrayList; //items displayed in the nav
  }

  public int getCount()
  {
    return items.size();
  }

  public Data getItem(int paramInt)
  {
    return (Data)items.get(paramInt);
  }

  public long getItemId(int paramInt)
  {
    return paramInt;
  }

  @SuppressLint({"InflateParams"})
  public View getView(int paramInt, View paramView, ViewGroup paramViewGroup)
  {
    if (paramView == null)
      paramView = LayoutInflater.from(context).inflate(R.layout.left_nav_item, null);
    Data localData = getItem(paramInt);
    TextView localTextView = (TextView)paramView.findViewById(R.id.lbl);
    localTextView.setText(localData.getTexts()[0]);
    if (selection == paramInt)
    {
      localTextView.setTextColor(context.getResources().getColor(R.color.main_red));
      localTextView.setCompoundDrawablesWithIntrinsicBounds(localData.getResources()[1], 0, 0, 0);
      paramView.findViewById(R.id.bar).setVisibility(0);
      return paramView;
    }
    localTextView.setTextColor(-1);
    localTextView.setCompoundDrawablesWithIntrinsicBounds(localData.getResources()[0], 0, 0, 0);
    paramView.findViewById(R.id.bar).setVisibility(4);
    return paramView;
  }

  public int isSelection()
  {
    return selection;
  }

  public void setSelection(int paramInt)
  {
    selection = paramInt;
    notifyDataSetChanged();
  }
}
