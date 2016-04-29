package dev.ugasoft.android.gps.actions;

import java.util.ArrayList;
import java.util.Iterator;

import android.hardware.SensorEvent;

import dev.ugasoft.android.gps.db.*;

public class PotholeSpotLabel
{

   private String tag;
   private long id;

   private DatabaseHelper dbm;

   private double[] seq1;
   private double[] seq2;
   private int[][] warpingPath;

   private int n;
   private int m;
   private int K;

   private double warpingDistance;
   private ArrayList<Template> templates;

   /**
    * Constructor
    * 
    * @param DatabaseHelper
    */
   public PotholeSpotLabel(DatabaseHelper dbh)
   {
      Template template;
      templates = new ArrayList<Template>();
      dbm = dbh;
      //Pothole Templates
      template = new Template();
      template.name = "Template-1-Pothole";
      template.sequence = new double[] { 0.00, 0.00, 0.00, 0.00 };
      template.labelName = "Pothole";
      templates.add(template);

      template = new Template();
      template.name = "Template-2-Pothole";
      template.sequence = new double[] { 0.00, 0.00, 0.00, 0.00 };
      template.labelName = "Pothole";
      templates.add(template);

      //Roadhump Templates
      template = new Template();
      template.name = "Template-1-Roadhump";
      template.sequence = new double[] { 0.00, 0.00, 0.00, 0.00 };
      template.labelName = "Roadhump";
      templates.add(template);

      template = new Template();
      template.name = "Template-2-Roadhump";
      template.sequence = new double[] { 0.00, 0.00, 0.00, 0.00 };
      template.labelName = "Roadhump";
      templates.add(template);

      template = new Template();
      template.name = "Template-3-Roadhump";
      template.sequence = new double[] { 0.00, 0.00, 0.00, 0.00 };
      template.labelName = "Roadhump";
      templates.add(template);

      //UnevenRoad Templates
      template = new Template();
      template.name = "Template-1-UnevenRoad";
      template.sequence = new double[] { 0.00, 0.00, 0.00, 0.00 };
      template.labelName = "UnevenRoad";
      templates.add(template);

      template = new Template();
      template.name = "Template-2-UnevenRoad";
      template.sequence = new double[] { 0.00, 0.00, 0.00, 0.00 };
      template.labelName = "UnevenRoad";
      templates.add(template);
   }

   public void match()
   {
      double accumulatedDistance = 0.0;

      double[][] d = new double[n][m]; // local distances
      double[][] D = new double[n][m]; // global distances

      for (int i = 0; i < n; i++)
      {
         for (int j = 0; j < m; j++)
         {
            d[i][j] = distanceBetween(seq1[i], seq2[j]);
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

   public Template match(double[] inputSequence)
   {
      seq1 = inputSequence;
      Template template = null;
      Template matchedTemplate = null;
      for (int i = 0; i < templates.size(); i++)
      {
         template = templates.get(i);
         seq2 = template.sequence;

         n = seq1.length;
         m = seq2.length;
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
               matchedTemplate = templates.get(i - 1);
         }
      }

      return matchedTemplate;
   }

   public double[] segmentSequece(double[] inputSequence)
   {

      double[] segmentedSq = null;

      return segmentedSq;
   }

   private ArrayList<PotholeSpotLabel> LoadUnTaggedLabels()
   {

      ArrayList<PotholeSpotLabel> labels = this.dbm.getUntaggedLabels();
      return labels;
   }

   public void TagSpotLabel(DatabaseHelper dbm)
   {
      this.dbm = dbm;

      ArrayList<PotholeSpotLabel> labels = LoadUnTaggedLabels();

      for (Iterator<PotholeSpotLabel> lbl = labels.iterator(); lbl.hasNext();)
      {
         PotholeSpotLabel lable = lbl.next();
         //TagSpotLabel(lable);
      }
      updateSpotLabelTag(labels);
   }

   public void TagSpotLabel(SensorEvent event)
   {
      //this.dbm = dbm;

      //mLastRecordedEvent = ((MainActivity)getActivity()).getmLastRecordedEvent();

      ArrayList<PotholeSpotLabel> labels = LoadUnTaggedLabels();

      for (Iterator<PotholeSpotLabel> lbl = labels.iterator(); lbl.hasNext();)
      {
         PotholeSpotLabel lable = lbl.next();
         // TagSpotLabel(lable);
      }
      updateSpotLabelTag(labels);
   }

   public void TagSpotLabel(Template template)
   {

      // this.dbm.UpdateLabelTag(label);

   }

   public void updateSpotLabelTag(ArrayList<PotholeSpotLabel> labels)
   {

      for (PotholeSpotLabel label : labels)
      {
         this.dbm.UpdateLabelTag(label);
      }
   }

   /*
    * private void speedFilter(ArrayList<PotholeSpotLabel> labels){ } private void highPassFilter(ArrayList<PotholeSpotLabel> labels){ } private ArrayList<PotholeSpotLabel>
    * zPeakFilter(ArrayList<PotholeSpotLabel> labels){ return labels; }
    */

   public void setID(long id)
   {
      this.id = id;
   }

   public void setTag(String tag)
   {
      this.tag = tag;
   }

   public String getTag()
   {
      return this.tag;
   }

   public long getID()
   {
      return this.id;
   }

}
