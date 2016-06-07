package dev.potholespot.android.actions.tasks;

/*A class that will handle the algorithm of the potholeSpot Detection Algorithm*/

public class DetectionAlgorithm

{
   protected double[] seq1;
   protected double[] seq2;
   protected int[][] warpingPath;

   protected int n;
   protected int m;
   protected int K;

   protected double warpingDistance;

   /**
    * Constructor
    * 
    * @param query
    * @param templete
    */
   public DetectionAlgorithm(final double[] sample, final double[] templete)
   {
      this.seq1 = sample;
      this.seq2 = templete;

      this.n = this.seq1.length;
      this.m = this.seq2.length;
      this.K = 1;

      this.warpingPath = new int[this.n + this.m][2]; // max(n, m) <= K < n + m
      this.warpingDistance = 0.0;

      compute();
   }

   public void compute()
   {
      double accumulatedDistance = 0.0;

      final double[][] d = new double[this.n][this.m]; // local distances
      final double[][] D = new double[this.n][this.m]; // global distances

      for (int i = 0; i < this.n; i++)
      {
         for (int j = 0; j < this.m; j++)
         {
            d[i][j] = distanceBetween(this.seq1[i], this.seq2[j]);
         }
      }

      D[0][0] = d[0][0];

      for (int i = 1; i < this.n; i++)
      {
         D[i][0] = d[i][0] + D[i - 1][0];
      }

      for (int j = 1; j < this.m; j++)
      {
         D[0][j] = d[0][j] + D[0][j - 1];
      }
      System.out.println("Local distance");
      PrintD(d);
      System.out.println("Global distance");
      PrintD(D);
      for (int i = 1; i < this.n; i++)
      {
         for (int j = 1; j < this.m; j++)
         {
            accumulatedDistance = Math.min(Math.min(D[i - 1][j], D[i - 1][j - 1]), D[i][j - 1]);
            accumulatedDistance += d[i][j];
            D[i][j] = accumulatedDistance;
         }
      }
      accumulatedDistance = D[this.n - 1][this.m - 1];
      System.out.println("Global distance after accltd distance");
      PrintD(D);
      int i = this.n - 1;
      int j = this.m - 1;
      int minIndex = 1;

      this.warpingPath[this.K - 1][0] = i;
      this.warpingPath[this.K - 1][1] = j;

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
            final double[] array = { D[i - 1][j], D[i][j - 1], D[i - 1][j - 1] };
            minIndex = getIndexOfMinimum(array);

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
         this.K++;
         this.warpingPath[this.K - 1][0] = i;
         this.warpingPath[this.K - 1][1] = j;
      } // end while
      this.warpingDistance = accumulatedDistance / this.K;
      System.out.print("K value " + this.K + "\n");
      System.out.print("Acc dist" + accumulatedDistance + "\n");

      reversePath(this.warpingPath);
   }

   /**
    * Changes the order of the warping path (increasing order)
    * 
    * @param path the warping path in reverse order
    */
   protected void reversePath(final int[][] path)
   {
      final int[][] newPath = new int[this.K][2];
      for (int i = 0; i < this.K; i++)
      {
         for (int j = 0; j < 2; j++)
         {
            newPath[i][j] = path[this.K - i - 1][j];
         }
      }
      this.warpingPath = newPath;
   }

   /**
    * Returns the warping distance
    * 
    * @return
    */
   public double getDistance()
   {
      return this.warpingDistance;
   }

   /**
    * Computes a distance between two points
    * 
    * @param p1 the point 1
    * @param p2 the point 2
    * @return the distance between two points
    */
   protected double distanceBetween(final double p1, final double p2)
   {
      return (p1 - p2) * (p1 - p2);
   }

   /**
    * Finds the index of the minimum element from the given array
    * 
    * @param array the array containing numeric values
    * @return the min value among elements
    */
   protected int getIndexOfMinimum(final double[] array)
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
      String retVal = "Warping Distance: " + this.warpingDistance + "\n";
      retVal += "Warping Path: {";
      for (int i = 0; i < this.K; i++)
      {
         retVal += "(" + this.warpingPath[i][0] + ", " + this.warpingPath[i][1] + ")";
         retVal += (i == this.K - 1) ? "}" : ", ";

      }
      return retVal;
   }

   public void PrintD(final double[][] darray)
   {
      for (int i = 0; i < this.n; i++)
      {
         for (int j = 0; j < this.m; j++)
         {
            System.out.print(darray[i][j] + " ");
         }
         System.out.println();
      }

   }

   public static double[] sampleTempalte()
   {
      final double[] smTmp = { 0, 2, 4, 7 };
      return smTmp;
   }

   public double[] potHoleTemplate()
   {
      final double[] potholeTmp = { 0.00, 0.00, 0.00, 0.00 };
      return potholeTmp;
   }

   public double[] roadHumpTemplate()
   {
      final double[] roadhumpTmp = { 0.00, 0.00, 0.00, 0.00 };
      return roadhumpTmp;
   }

   public double[] unEvenRoadTemplate()
   {
      final double[] unEvenRoadTmp = { 0.00, 0.00, 0.00, 0.00 };
      return unEvenRoadTmp;
   }

   public void Match()
   {

   }

   /**
    * Tests this class
    * 
    * @param args ignored
    */
   public static void main(final String[] args)
   {
      final double[] n2 = sampleTempalte();
      final double[] n1 = { 0, 2, 4, 6 };
      final DetectionAlgorithm dtw = new DetectionAlgorithm(n1, n2);
      System.out.println(dtw);
   }
}