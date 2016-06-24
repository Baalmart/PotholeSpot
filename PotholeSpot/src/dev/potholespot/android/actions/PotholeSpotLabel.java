package dev.potholespot.android.actions;

import java.util.ArrayList;
import android.util.Log;

import dev.potholespot.android.db.*;
import dev.potholespot.android.db.Pspot.PotholeSpotDtw;

public class PotholeSpotLabel
{
   private DatabaseHelper dbm;

   private double[] roadEventSequence;
   private double[] templateSequence;
   private int[][] warpingPath;

   private int n;
   private int m;
   private int K;

   private double warpingDistance;
   private ArrayList<RoadEventTemplate> templates;
   private ArrayList<Double> segmentedInputStream;

   /**
    * Constructor
    * 
    * @param DatabaseHelper
    */
   public PotholeSpotLabel(DatabaseHelper dbh)
   {
      RoadEventTemplate template;
      templates = new ArrayList<RoadEventTemplate>();
      segmentedInputStream = new ArrayList<Double>();
      segmentedInputStream.clear();
      dbm = dbh;
      
      //Pothole Templates
      template = new RoadEventTemplate();
      template.name = "Template-1-Pothole";
      template.sequence = new double[] { 8.6323,10.281,9.1506,9.3051,9.681,9.3003,9.28995,9.432,9.1447,9.9994,9.7683,11.159,11.769,12.066,10.336,7.4963,8.7233,8.2492,10.197,10.489,9.8893,9.9108,9.1387,8.9986,8.418 };
      template.labelName = "Pothole";
      templates.add(template);

      /*template = new RoadEventTemplate();
      template.name = "Template-2-Pothole";
      template.sequence = new double[] { -5.00, -2.00, 0.00, 0.00 };
      template.labelName = "Pothole";
      templates.add(template);*/

      //Roadhump Templates
      template = new RoadEventTemplate();
      template.name = "Template-1-Roadhump";
      template.sequence = new double[] { 8.9328,7.8925,9.4272,9.4834,8.1487,9.8928,8.2732,9.0549,7.7955,7.9679,7.9322,6.3315,8.8502,8.8957,8.8238,10.002,9.5086,7.6375,11.842,9.2189,9.8545,9.171,7.2975,7.0282};
      template.labelName = "Roadhump";
      templates.add(template);
      /*
      
      template = new RoadEventTemplate();
      template.name = "Template-2-Roadhump";
      template.sequence = new double[] { 0.00, 0.00, 0.00, 0.00 };
      template.labelName = "Roadhump";
      templates.add(template);

      template = new RoadEventTemplate();
      template.name = "Template-3-Roadhump";
      template.sequence = new double[] { 0.00, 0.00, 0.00, 0.00 };
      template.labelName = "Roadhump";
      templates.add(template);*/

      //UnevenRoad Templates
      template = new RoadEventTemplate();
      template.name = "Template-1-UnevenRoad";
      template.sequence = new double[] { 9.1279,7.5214,7.4963,8.1487,7.9356,7.683,8.7496,9.2117,8.102,8.8095,7.8937,9.8545,7.9679,8.7975,8.4659,9.0884,7.0198,8.9376,8.8544,8.15};
      template.labelName = "UnevenRoad";
      templates.add(template);

      /*
      template = new RoadEventTemplate();
      template.name = "Template-2-UnevenRoad";
      template.sequence = new double[] { 0.00, 0.00, 0.00, 0.00 };
      template.labelName = "UnevenRoad";
      templates.add(template);*/
   }

   private void match()
   {
      double accumulatedDistance = 0.0;

      double[][] d = new double[n][m]; // local distances
      double[][] D = new double[n][m]; // global distances

      for (int i = 0; i < n; i++)
      {
         for (int j = 0; j < m; j++)
         {
            d[i][j] = distanceBetween(roadEventSequence[i], templateSequence[j]);
         }
      }

      D[0][0] = d[0][0];

      for (int i = 1; i < n; i++)
      {
         D[i][0] = d[i][0] + D[i - 1][0];
      }

      for (int j = 1; j < m; j++)
      {
         D[0][j] = d[0][j] + D[0][j - 1];
      }

      for (int i = 1; i < n; i++)
      {
         for (int j = 1; j < m; j++)
         {
            accumulatedDistance = Math.min(Math.min(D[i - 1][j], D[i - 1][j - 1]), D[i][j - 1]);
            accumulatedDistance += d[i][j];
            D[i][j] = accumulatedDistance;
         }
      }
      accumulatedDistance = D[n - 1][m - 1];

      int i = n - 1;
      int j = m - 1;
      int minIndex = 1;

      warpingPath[K - 1][0] = i;
      warpingPath[K - 1][1] = j;

      while ((i + j) != 0)
      {
         if (i == 0)
         {
            j -= 1;
         }
         else if (j == 0)
         {
            i -= 1;
         }
         else
         { // i != 0 && j != 0
            double[] array = { D[i - 1][j], D[i][j - 1], D[i - 1][j - 1] };
            minIndex = this.getIndexOfMinimum(array);

            if (minIndex == 0)
            {
               i -= 1;
            }
            else if (minIndex == 1)
            {
               j -= 1;
            }
            else if (minIndex == 2)
            {
               i -= 1;
               j -= 1;
            }
         } // end else
         K++;
         warpingPath[K - 1][0] = i;
         warpingPath[K - 1][1] = j;
      } // end while
      warpingDistance = accumulatedDistance / K;

      this.reversePath(warpingPath);
   }

   /**
    * Changes the order of the warping path (increasing order)
    * 
    * @param path the warping path in reverse order
    */
   protected void reversePath(int[][] path)
   {
      int[][] newPath = new int[K][2];
      for (int i = 0; i < K; i++)
      {
         for (int j = 0; j < 2; j++)
         {
            newPath[i][j] = path[K - i - 1][j];
         }
      }
      warpingPath = newPath;
   }

   /**
    * Returns the warping distance
    * 
    * @return
    */
   public double getDistance()
   {
      return warpingDistance;
   }

   /**
    * Computes a distance between two points
    * 
    * @param p1 the point 1
    * @param p2 the point 2
    * @return the distance between two points
    */
   protected double distanceBetween(double p1, double p2)
   {
      return (p1 - p2) * (p1 - p2);
   }

   /**
    * Finds the index of the minimum element from the given array
    * 
    * @param array the array containing numeric values
    * @return the min value among elements
    */
   protected int getIndexOfMinimum(double[] array)
   {
      int index = 0;
      double val = array[0];

      for (int i = 1; i < array.length; i++)
      {
         if (array[i] < val)
         {
            val = array[i];
            index = i;
         }
      }
      return index;
   }

   /**
    * Returns a string that displays the warping distance and path
    */
   @Override
   public String toString()
   {
      String retVal = "Warping Distance: " + warpingDistance + "\n";
      retVal += "Warping Path: {";
      for (int i = 0; i < K; i++)
      {
         retVal += "(" + warpingPath[i][0] + ", " + warpingPath[i][1] + ")";
         retVal += (i == K - 1) ? "}" : ", ";
      }
      return retVal;
   }

   /**
    * Matches the an Incoming input stream to several templates stored.
    * @return a template whose matching with Incoming stream resulted into the minimum distance 
    * @param roadEventStream , an incoming stream of 10 values in total.
    * @author Judas Tadeo, PotholeSpot-Uganda Project
    * */
   private RoadEventTemplate match(ArrayList<Double> roadEventStream)
   {
      roadEventSequence = new double[10];
      for(int i = 0; i <10; i++)
      {
         roadEventSequence[i]  = roadEventStream.get(i);
      }
      
      //roadEventSequence = roadEventStream;
      RoadEventTemplate template = null;
      RoadEventTemplate matchedTemplate = null;
      for (int i = 0; i < templates.size(); i++)
      {
         template = templates.get(i);
         templateSequence = template.sequence;

         n = roadEventSequence.length;
         m = templateSequence.length;
         K = 1;

         warpingPath = new int[n + m][2]; // max(n, m) <= K < n + m
         warpingDistance = 0.0;

         this.match();

         template.minimumDistance = warpingDistance;
      }

      for (int i = 0; i < templates.size(); i++)
      {
         matchedTemplate = templates.get(i);

         if (i >= 1)
         {
            if (matchedTemplate.minimumDistance > templates.get(i - 1).minimumDistance)
            {
               matchedTemplate = templates.get(i - 1);
               //TagSpotLabel(matchedTemplate);
            }
         }
      }
      return matchedTemplate;
   }

   /**
    * @category The method receives streams of xyz values and segments them into a sequence of 10. 
    * Further analyzes them to remove stream due to noise
    * @param x, accelerometer value X.
    * @param y, accelerometer value Y.
    * @param z, Accelerometer value z.
    * @return void.
    * @author Judas Tadeo, PotholeSpot-Uganda Project.
    * */
   public void segmentStream(double x, double y, double z)
   {
      int count = segmentedInputStream.size();
      if (count == 10)
      {
         RoadEventTemplate temp = match(segmentedInputStream);
         saveToDatabase(temp,segmentedInputStream);
      }
      else if (count < 10)
      {
         segmentedInputStream.add(count, z);
      }
   }

   /**
    * @category, The method saves a matched or an identified Road Event to SQLlite Database with its label also called tag
    * @param template, is the matched Template whose location must also be found.
    * @return, void.
    * @author Judas Tadeo, PotholeSpot-Uganda Project
    * */
   private void TagSpotLabel(RoadEventTemplate template)
   {
      float x, y, z;
      double longtude, latitude;
      double[] location;
      long logtime = 000;
      String tag;

      tag = template.labelName;
      location = searchLocation(logtime);
      longtude = location[0];
      latitude = location[1];
      x = 0; y = 0; z = 0;
     // this.dbm.insert_PotholeSpotDtw(x, y, z, tag, longtude, latitude);
   }

   /**
    * @category The method  location of a Road Event based on the time the event was logged
    * @param logtime, the time at which the event was logged
    * @return void.
    * @author Judas Tadeo, PotholeSpot-Uganda Project
    * */
   public double[] searchLocation(long logtime)
   {
      double[] loc = new double[2];
      loc[0] = 0.00;
      loc[1] = 0.00;
      return loc;
   }
   
   public double[] locationUpdates(double longtude, double latitude){
      double[] location = new double[2];
      location[0] = longtude;
      location[1] = latitude;
      return location;
   }
   
   float[] lowPass(double x, double y, double z) {

      float[] filteredValues = new float[3];

      /*filteredValues[0] = x * a + filteredValues[0] * (1.0f � a);
      filteredValues[1] = y * a + filteredValues[1] * (1.0f � a);
      filteredValues[2] = z * a + filteredValues[2] * (1.0f � a);*/

      return filteredValues;

      }
   
   private float[] highPass(float x, float y, float z) {

      float[] filteredValues = new float[3];

    /*  gravity[0] = ALPHA * gravity[0] + (1 � ALPHA) * x;
      gravity[1] = ALPHA * gravity[1] + (1 � ALPHA) * y;
      gravity[2] = ALPHA * gravity[2] + (1 � ALPHA) * z;

      filteredValues[0] = x � gravity[0];
      filteredValues[1] = y � gravity[1];
      filteredValues[2] = z � gravity[2];*/

      return filteredValues;

      }
   
   private void saveToDatabase(RoadEventTemplate template, ArrayList<Double> segmentedInputStream){
            
      if(template != null){
        
      String queryStoreAccelerationValuesDTW ="Insert into "+PotholeSpotDtw.TABLE+" (";
      
      queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+PotholeSpotDtw.SEQ1+",";
      queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+PotholeSpotDtw.SEQ2+",";
      queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+PotholeSpotDtw.SEQ3+","; 
      queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+PotholeSpotDtw.SEQ4+","; 
      queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+PotholeSpotDtw.SEQ5+","; 
      queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+PotholeSpotDtw.SEQ6+","; 
      queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+PotholeSpotDtw.SEQ7+","; 
      queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+PotholeSpotDtw.SEQ8+","; 
      queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+PotholeSpotDtw.SEQ9+","; 
      queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+PotholeSpotDtw.SEQ10+","; 
      queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+PotholeSpotDtw.LATITUDE+",";
      queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+PotholeSpotDtw.LONGITUDE+",";
      queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+PotholeSpotDtw.TAG;
      //queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+PotholeSpotDtw.TIME;
       
      queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+" ) VALUES ( ";
      
      queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+"'"+segmentedInputStream.get(0)+"' , ";
      queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+"'"+segmentedInputStream.get(1)+"' , ";
      queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+"'"+segmentedInputStream.get(2)+"', "; 
      queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+"'"+segmentedInputStream.get(3)+"', ";
      queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+"'"+segmentedInputStream.get(6)+"', ";
      queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+"'"+segmentedInputStream.get(5)+"', ";
      queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+"'"+segmentedInputStream.get(6)+"', ";
      queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+"'"+segmentedInputStream.get(7)+"', ";
      queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+"'"+segmentedInputStream.get(8)+"', ";
      queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+"'"+segmentedInputStream.get(9)+"', ";
      queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+"'"+0+"', ";
      queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+"'"+0+"', ";
      queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+"'"+template.labelName+"' ) ";
      //queryStoreAccelerationValuesDTW=queryStoreAccelerationValuesDTW+"'"+Long.valueOf(System.currentTimeMillis()) +"' ) "; 
      Log.d("Insert Query", queryStoreAccelerationValuesDTW);
         
         this.dbm.getData(queryStoreAccelerationValuesDTW);
         segmentedInputStream.clear();
      }
   }
}
