package com.prim.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.prim.IssueDetail;
import com.prim.model.Place;

import dev.baalmart.gps.R;

import java.util.ArrayList;

@SuppressLint({"InflateParams"})
public class Places extends Fragment
{
  private ArrayList<Place> pList;

  private void loadDummyData()
  {
    int i = getActivity().getIntent().getIntExtra("icon1", 0);
    ArrayList localArrayList = new ArrayList();
    int j;
    int k = 0;
    int m = 0;
    int n = 0;
    int i1 = 0;
    int i2 = 0;
    
    
    if (i > 0)
    {
      j = i;
      localArrayList.add(new Place("Superba Food", "100 m", 3, j));
      if (i <= 0)
      
      k = i;
      localArrayList.add(new Place("Coffee Cafe Day", "110 m", 2, k));
      if (i <= 0)
       
      m = i;
      localArrayList.add(new Place("Bar-be-queue", "120 m", 5, m));
      if (i <= 0)
      
      n = i;
      localArrayList.add(new Place("Om Hospital", "200 m", 1, n));
      if (i <= 0)
       
      i1 = i;
      localArrayList.add(new Place("Cinepolice Cinema", "330 m", 0, i1));
      if (i <= 0)
   
      i2 = i;
      label157: localArrayList.add(new Place("The World Bank", "350 m", 5, i2));
      if (i <= 0)
   
   // }
    while (true)
    {
      localArrayList.add(new Place("J-Star Hotel", "410 m", 3, i));
      pList = new ArrayList(localArrayList);
      pList.addAll(localArrayList);
      pList.addAll(localArrayList);
     // return;
      j = R.drawable.icon_list_restaurent;
      k = R.drawable.icon_list_coffee;   
      m = R.drawable.icon_list_beer;  
      n = R.drawable.icon_list_clinic;      
      i1 = R.drawable.icon_list_film;    
      i2 = R.drawable.icon_list_bank;  
      i = R.drawable.icon_list_hotel;
    } }
  }

  private void setupView(View paramView)
  {
    loadDummyData();
    ListView localListView = (ListView)paramView;
    localListView.setAdapter(new PlaceAdapter());
    localListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
    {
      public void onItemClick(AdapterView<?> paramAnonymousAdapterView, View paramAnonymousView, int paramAnonymousInt, long paramAnonymousLong)
      {
        startActivity(new Intent(getActivity(), IssueDetail.class));
      }
    });
  }

  public View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup, Bundle paramBundle)
  {
    View localView = paramLayoutInflater.inflate(R.layout.list, null);
    setupView(localView);
    return localView;
  }

  private class PlaceAdapter extends BaseAdapter
  {
    private PlaceAdapter()
    {
    }

    public int getCount()
    {
      return pList.size();
    }

    public Place getItem(int paramInt)
    {
      return (Place)pList.get(paramInt);
    }

    public long getItemId(int paramInt)
    {
      return paramInt;
    }

    public View getView(int paramInt, View paramView, ViewGroup paramViewGroup)
    {
      if (paramView == null)
        paramView = getLayoutInflater(null).inflate(R.layout.place_item, null);
      Place localPlace = getItem(paramInt);
      ((TextView)paramView.findViewById(R.id.lbl1)).setText(localPlace.getTitle());
      ((TextView)paramView.findViewById(R.id.lbl2)).setText(localPlace.getAddress());
      ((ImageView)paramView.findViewById(R.id.img)).setImageResource(localPlace.getIcon());
      LinearLayout localLinearLayout = (LinearLayout)paramView.findViewById(R.id.rating);
      int i = 0;
      if (i >= localLinearLayout.getChildCount())
        return paramView;
      TextView localTextView = (TextView)localLinearLayout.getChildAt(i);
      Resources localResources = getResources();
      if (i <= localPlace.getRating());
      for (int j = R.color.main_red; ; j = R.color.side_menu_divider)
      {
        localTextView.setTextColor(localResources.getColor(j));
        i++;
        break;
      }
	return localTextView;
    }
  }
}
