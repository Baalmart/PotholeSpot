package com.potholespot.model;

import com.google.android.gms.maps.model.LatLng;

public class Place
{
  private String address;
  
  private int icon;
  private int rating;
  private String title;
  
  //the three items we are focusing on...
  private int speed;
  private int time;
  private LatLng geo;
  
  
  public Place(String paramString1, String paramString2, int paramInt1, int paramInt2)
  {
    title = paramString1;
    address = paramString2;
    icon = paramInt2;
    rating = paramInt1;
  }

  public Place(String paramString1, String paramString2, LatLng paramLatLng, int paramInt)
  {
    title = paramString1;
    address = paramString2;
    icon = paramInt;
    geo = paramLatLng;
  }

  public String getAddress()
  {
    return address;
  }

  public LatLng getGeo()
  {
    return geo;
  }

  public int getIcon()
  {
    return icon;
  }

  public int getRating()
  {
    return rating;
  }

  public String getTitle()
  {
    return title;
  }

  public void setAddress(String paramString)
  {
    address = paramString;
  }

  public void setGeo(LatLng paramLatLng)
  {
    geo = paramLatLng;
  }

  public void setIcon(int paramInt)
  {
    icon = paramInt;
  }

  public void setRating(int paramInt)
  {
    rating = paramInt;
  }

  public void setTitle(String paramString)
  {
    title = paramString;
  }
}
