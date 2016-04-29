package com.prim.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.prim.IssueDetail;
import com.prim.custom.CustomFragment;
import com.prim.model.Place;

import dev.baalmart.gps.R;

import java.util.ArrayList;
import java.util.Iterator;

public class MapViewer extends CustomFragment
  implements View.OnClickListener
{
  private GoogleMap mMap;
  private MapView mMapView; //make an instance of Google Maps
  private ArrayList<Place> pList;

  private void initMap(View paramView, Bundle paramBundle)
  {
    MapsInitializer.initialize(getActivity());
      mMapView = ((MapView)paramView.findViewById(R.id.map));
      mMapView.onCreate(paramBundle);
      return;
  }

  private void loadDummyData()
  {
    int i = getActivity().getIntent().getIntExtra("icon", 0);
    pList = new ArrayList();
    ArrayList localArrayList1 = pList;
    LatLng localLatLng1 = new LatLng(45.466700000000003D, 9.183299999999999D);
    int j;
    int k = 0;
    int m = 0;
    int n = 0;
    ArrayList localArrayList5;
    LatLng localLatLng5;
    if (i > 0)
    {
      j = i;
      localArrayList1.add(new Place("Superba Food", "1900 S Lincoln Blvd, Venice\nLos Angeles", localLatLng1, j));
      ArrayList localArrayList2 = pList;
      LatLng localLatLng2 = new LatLng(45.486800000000002D, 9.103400000000001D);
      
      if (i <= 0)     
      k = i;
      localArrayList2.add(new Place("Coffee Cafe Day", "1234 A Lincoln Blvd, Venice\nLos Angeles", localLatLng2, k));
      ArrayList localArrayList3 = pList;
      LatLng localLatLng3 = new LatLng(45.42671D, 9.1633D);
      
      if (i <= 0)
      m = i;
      localArrayList3.add(new Place("Bar-be-queue", "C-395, A-One Mall\nSydney", localLatLng3, m));
      ArrayList localArrayList4 = pList;
      LatLng localLatLng4 = new LatLng(45.4467D, 9.11331D);
      if (i <= 0)

      n = i;
      localArrayList4.add(new Place("Om Hospital", "Om Hospital road\nSydney", localLatLng4, n));
      localArrayList5 = pList;
      localLatLng5 = new LatLng(45.436700000000002D, 9.183299999999999D);
      if (i <= 0)

    while (true)
    {
      localArrayList5.add(new Place("Cinepolice Cinema", "1900 S Lincoln Blvd, Venice\nLos Angeles", localLatLng5, i));
      //return;
      j = R.drawable.icon_map_restaurant;
      k = R.drawable.icon_map_bar_copia;
      m = R.drawable.icon_map_beer; 
      n = R.drawable.icon_map_clinic;
      i = R.drawable.icon_map_film;
    } }
  }

  private void setupMapMarkers()
  {
    mMap.clear();
    LatLng[] arrayOfLatLng = new LatLng[4];
    arrayOfLatLng[0] = new LatLng(45.466700000000003D, 9.183299999999999D);
    arrayOfLatLng[1] = new LatLng(45.486800000000002D, 9.103400000000001D);
    arrayOfLatLng[2] = new LatLng(45.4467D, 9.11331D);
    arrayOfLatLng[3] = new LatLng(45.42671D, 9.1633D);
    Iterator localIterator = pList.iterator();
    while (true)
    {
      if (!localIterator.hasNext())
      {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(arrayOfLatLng[2], 11.0F));
        return;
      }
      Place localPlace = (Place)localIterator.next();
      MarkerOptions localMarkerOptions = new MarkerOptions();
      localMarkerOptions.position(localPlace.getGeo()).title(localPlace.getTitle()).snippet(localPlace.getAddress());
      localMarkerOptions.icon(BitmapDescriptorFactory.fromResource(localPlace.getIcon()));
      mMap.addMarker(localMarkerOptions);
    }
  }

  public void onClick(View paramView)
  {
  }

  @SuppressLint({"InflateParams"})
  public View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup, Bundle paramBundle)
  {
    View localView = paramLayoutInflater.inflate(R.layout.map, null);
    loadDummyData();
    initMap(localView, paramBundle);
    return localView;
  }

  public void onDestroy()
  {
    mMapView.onDestroy();
    super.onDestroy();
  }

  public void onLowMemory()
  {
    super.onLowMemory();
    mMapView.onLowMemory();
  }

  public void onPause()
  {
    mMapView.onPause();
    if (mMap != null)
      mMap.setInfoWindowAdapter(null);
    super.onPause();
  }

  public void onResume()
  {
    super.onResume();
    mMapView.onResume();
    mMap = mMapView.getMap();
    if (mMap != null)
    {
      mMap.setMyLocationEnabled(true);
      mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());
      setupMapMarkers();
      mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener()
      {
        public void onInfoWindowClick(Marker paramAnonymousMarker)
        {
          startActivity(new Intent(getActivity(), IssueDetail.class));
        }
      });
    }
  }

  public void onSaveInstanceState(Bundle paramBundle)
  {
    super.onSaveInstanceState(paramBundle);
    mMapView.onSaveInstanceState(paramBundle);
  }

  private class CustomInfoWindowAdapter
    implements GoogleMap.InfoWindowAdapter
  {
    private final View mContents = getActivity().getLayoutInflater().inflate(R.layout.map_popup, null);

    @SuppressLint({"InflateParams"})
    CustomInfoWindowAdapter()
    {
    }

    private void render(Marker paramMarker, View paramView)
    {
      String str1 = paramMarker.getTitle();
      TextView localTextView1 = (TextView)paramView.findViewById(R.id.title);
      if (str1 != null)
      {
        SpannableString localSpannableString1 = new SpannableString(str1);
        localSpannableString1.setSpan(new ForegroundColorSpan(-1), 0, localSpannableString1.length(), 0);
        localTextView1.setText(localSpannableString1);
      }
      TextView localTextView2;
      while (true)
      {
        String str2 = paramMarker.getSnippet();
        localTextView2 = (TextView)paramView.findViewById(R.id.snippet);
        if (str2 == null)
          break;
        SpannableString localSpannableString2 = new SpannableString(str2);
        localSpannableString2.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.main_blue_lt)), 0, str2.length(), 0);
        localTextView2.setText(localSpannableString2);
        //return;
        localTextView1.setText("");
      }
      localTextView2.setText("");
    }

    public View getInfoContents(Marker paramMarker)
    {
      return null;
    }

    public View getInfoWindow(Marker paramMarker)
    {
      render(paramMarker, mContents);
      return mContents;
    }
  }
}
