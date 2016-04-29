package com.prim.model;

public class Data
{
  private int[] resources;
  private String[] texts;

  public Data(String[] paramArrayOfString, int[] paramArrayOfInt)
  {
    texts = paramArrayOfString;
    resources = paramArrayOfInt;
  }

  public int[] getResources()
  {
    return resources;
  }

  public String[] getTexts()
  {
    return texts;
  }

  public void setResources(int[] paramArrayOfInt)
  {
    resources = paramArrayOfInt;
  }

  public void setTexts(String[] paramArrayOfString)
  {
    texts = paramArrayOfString;
  }
}