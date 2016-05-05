package dev.ugasoft.android.gps.actions;

import java.util.ArrayList;
import java.util.Iterator;

import android.hardware.SensorEvent;

import dev.ugasoft.android.gps.db.*;

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
   private double[] segmentedInputStream;

   /**
    * Constructor
    * 
    * @param DatabaseHelper
    */
   public PotholeSpotLabel(DatabaseHelper dbh)
   {
      RoadEventTemplate template;
      templates = new ArrayList<RoadEventTemplate>();
      segmentedInputStream = new double[10];
      dbm = dbh;
      //Pothole Templates
      template = new RoadEventTemplate();
      template.name = "Template-1-Pothole";
      template.sequence = new double[] { 0.00, 0.00, 0.00, 0.00 };
      template.labelName = "Pothole";
      templates.add(template);

      template = new RoadEventTemplate();
      template.name = "Template-2-Pothole";
      template.sequence = new double[] { 0.00, 0.00, 0.00, 0.00 };
      template.labelName = "Pothole";
      templates.add(template);

      //Roadhump Templates
      template = new RoadEventTemplate();
      template.name = "Template-1-Roadhump";
      template.sequence = new double[] { 0.00, 0.00, 0.00, 0.00 };
      template.labelName = "Roadhump";
      templates.add(template);

      template = new RoadEventTemplate();
      template.name = "Template-2-Roadhump";
      template.sequence = new double[] { 0.00, 0.00, 0.00, 0.00 };
      template.labelName = "Roadhump";
      templates.add(template);

      template = new RoadEventTemplate();
      template.name = "Template-3-Roadhump";
      template.sequence = new double[] { 0.00, 0.00, 0.00, 0.00 };
      template.labelName = "Roadhump";
      templates.add(template);

      //UnevenRoad Templates
      template = new RoadEventTemplate();
      template.name = "Template-1-UnevenRoad";
      template.sequence = new double[] { 0.00, 0.00, 0.00, 0.00 };
      template.labelName = "UnevenRoad";
      templates.add(template);

      template = new RoadEventTemplate();
      template.name = "Template-2-UnevenRoad";
      template.sequence = new double[] { 0.00, 0.00, 0.00, 0.00 };
      template.labelName = "UnevenRoad";
      templates.add(template);
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
   private RoadEventTemplate match(double[] roadEventStream)
   {
      roadEventSequence = roadEventStream;
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
               TagSpotLabel(matchedTemplate);
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
      int count = segmentedInputStream.length;
      if (count == 10)
      {
         match(segmentedInputStream);
         //segmentedInputStream
         segmentStream(x, y, z);
      }
      else if (count < 10)
         segmentedInputStream[count] = x;
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
      this.dbm.insert_PotholeSpotDtw(x, y, z, tag, longtude, latitude);
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

}
