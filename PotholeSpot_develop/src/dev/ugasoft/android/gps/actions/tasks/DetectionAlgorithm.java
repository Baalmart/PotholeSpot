package dev.ugasoft.android.gps.actions.tasks;

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
   public DetectionAlgorithm(double[] sample, double[] templete) {
         seq1 = sample;
         seq2 = templete;
         
         n = seq1.length;       
         m = seq2.length;
         K = 1;
         
         warpingPath = new int[n + m][2];        // max(n, m) <= K < n + m
         warpingDistance = 0.0;
         
         this.compute();
   }
   
   public void compute() {
         double accumulatedDistance = 0.0;
         
         double[][] d = new double[n][m];        // local distances
         double[][] D = new double[n][m];        // global distances
         
         for (int i = 0; i < n; i++) {
               for (int j = 0; j < m; j++) {
                     d[i][j] = distanceBetween(seq1[i], seq2[j]);
               }
         }
         
         D[0][0] = d[0][0];
         
         for (int i = 1; i < n; i++) {
               D[i][0] = d[i][0] + D[i - 1][0];
         }

         for (int j = 1; j < m; j++) {
               D[0][j] = d[0][j] + D[0][j - 1];
         }
         System.out.println("Local distance");  
         PrintD(d);
         System.out.println("Global distance"); 
         PrintD(D);
         for (int i = 1; i < n; i++) {
               for (int j = 1; j < m; j++) {
                     accumulatedDistance = Math.min(Math.min(D[i-1][j], 
                           D[i-1][j-1]), D[i][j-1]);
                     accumulatedDistance += d[i][j];
                     D[i][j] = accumulatedDistance;
               }
         }
         accumulatedDistance = D[n - 1][m - 1];
         System.out.println("Global distance after accltd distance");   
         PrintD(D);
         int i = n - 1;
         int j = m - 1;
         int minIndex = 1;
   
         warpingPath[K - 1][0] = i;
         warpingPath[K - 1][1] = j;
         
         while ((i + j) != 0) {
               if (i == 0) {
                     j -= 1;
               } else if (j == 0) {
                     i -= 1;
               } else {        // i != 0 && j != 0
                     double[] array = { D[i - 1][j], D[i][j - 1], 
                           D[i - 1][j - 1] };
                     minIndex = this.getIndexOfMinimum(array);

                     if (minIndex == 0) {
                           i -= 1;
                     } else if (minIndex == 1) {
                           j -= 1;
                     } else if (minIndex == 2) {
                           i -= 1;
                           j -= 1;
                     }
               } // end else
               K++;
               warpingPath[K - 1][0] = i;
               warpingPath[K - 1][1] = j;
         } // end while
         warpingDistance = accumulatedDistance / K;
         System.out.print("K value "+ K+"\n");
         System.out.print("Acc dist"+accumulatedDistance+"\n");
         
         this.reversePath(warpingPath);
   }
   
   /**
    * Changes the order of the warping path (increasing order)
    *
    * @param path  the warping path in reverse order
    */
   protected void reversePath(int[][] path) {
         int[][] newPath = new int[K][2];
         for (int i = 0; i < K; i++) {
               for (int j = 0; j < 2; j++) {
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
   public double getDistance() {
         return warpingDistance;
   }
   
   /**
    * Computes a distance between two points
    *
    * @param p1    the point 1
    * @param p2    the point 2
    * @return              the distance between two points
    */
   protected double distanceBetween(double p1, double p2) {
         return (p1 - p2) * (p1 - p2);
   }

   /**
    * Finds the index of the minimum element from the given array
    *
    * @param array         the array containing numeric values
    * @return                              the min value among elements
    */
   protected int getIndexOfMinimum(double[] array) {
         int index = 0;
         double val = array[0];

         for (int i = 1; i < array.length; i++) {
               if (array[i] < val) {
                     val = array[i];
                     index = i;
               }
         }
         return index;
   }

   /**
    *      Returns a string that displays the warping distance and path
    */
   public String toString() 
   {
         String retVal = "Warping Distance: " + warpingDistance + "\n";
         retVal += "Warping Path: {";
         for (int i = 0; i < K; i++) 
         {
               retVal += "(" + warpingPath[i][0] + ", " 
         +warpingPath[i][1] + ")";
               retVal += (i == K - 1) ? "}" : ", ";
               
         }
         return retVal;
   }
   
   public void PrintD(double[][] darray)
   {
      for (int i = 0; i < n; i++) 
      {
         for (int j = 0; j < m; j++) 
         {
            System.out.print(darray[i][j]+" "); 
         }
         System.out.println();   
        }
      
   }
   
   public static double[] sampleTempalte(){
    double[] smTmp =  {0,2,4,7};
   return smTmp;
   }
   
   public double[] potHoleTemplate(){
   double[] potholeTmp = {0.00, 0.00, 0.00, 0.00};
   return potholeTmp;
   }
   
   public double[] roadHumpTemplate(){
   double[] roadhumpTmp = {0.00, 0.00, 0.00, 0.00};
   return roadhumpTmp;
   }
   
   public double[] unEvenRoadTemplate(){
   double[] unEvenRoadTmp = {0.00, 0.00, 0.00, 0.00};
   return unEvenRoadTmp;
   }
   
   public void Match()
   {
   
   
   }
   /**
    * Tests this class
    *
    * @param args  ignored
    */
   public static void main(String[] args) {
      double[] n2 = sampleTempalte();
      double[] n1 ={0,2,4,6};
      DetectionAlgorithm dtw = new DetectionAlgorithm(n1, n2);
      System.out.println(dtw);
   }
}