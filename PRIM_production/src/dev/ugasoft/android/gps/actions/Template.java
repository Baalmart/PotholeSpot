package dev.ugasoft.android.gps.actions;

import dev.ugasoft.android.gps.db.DatabaseHelper;

public class Template
{
   public String name;
   public double[] sequence;
   public String labelName;
   public int warpDistanceThreshold;
   public double minimumDistance;

   public Template()
   {
      name = "Name";
      sequence = null;
      labelName = "Label";
      warpDistanceThreshold = 0;
      minimumDistance = 0;
   }
   
   public void InsertInputSequence(DatabaseHelper dbm){
      
      
   }
   
}
