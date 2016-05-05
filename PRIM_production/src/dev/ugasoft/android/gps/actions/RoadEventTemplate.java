package dev.ugasoft.android.gps.actions;

import dev.ugasoft.android.gps.db.DatabaseHelper;

public class RoadEventTemplate
{
   public String name;
   public double[] sequence;
   public String labelName;
   public int warpDistanceThreshold;
   public double minimumDistance;
   public double longtude;
   public double latitude;

   public RoadEventTemplate()
   {
      name = "Name";
      sequence = null;
      labelName = "Label";
      warpDistanceThreshold = 0;
      minimumDistance = 0;
      longtude =0;
      latitude =0;
   }   
}
