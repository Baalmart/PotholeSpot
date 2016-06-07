package dev.potholespot.android.actions.utils;

import java.text.DateFormat;
import java.util.Date;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.AttributeSet;
import android.view.View;
import dev.potholespot.android.db.Pspot.WaypointsColumns;
import dev.potholespot.android.util.Constants;
import dev.potholespot.android.util.UnitsI18n;
import dev.potholespot.uganda.R;

/**
 * Calculate and draw graphs of track data
 * 
 * @version $Id$
 * @author Martin Bbaale
 */
public class GraphCanvas extends View
{
   @SuppressWarnings("unused")
   private static final String TAG = "OGT.GraphCanvas";
   public static final int TIMESPEEDGRAPH = 0;
   public static final int DISTANCESPEEDGRAPH = 1;
   public static final int TIMEALTITUDEGRAPH = 2;
   public static final int DISTANCEALTITUDEGRAPH = 3;
   private Uri mUri;
   private Bitmap mRenderBuffer;
   private Canvas mRenderCanvas;
   private final Context mContext;
   private UnitsI18n mUnits;
   private int mGraphType = -1;
   private long mEndTime;
   private long mStartTime;
   private double mDistance;
   private int mHeight;
   private int mWidth;
   private int mMinAxis;
   private int mMaxAxis;
   private double mMinAlititude;
   private double mMaxAlititude;
   private double mHighestSpeedNumber;
   private double mDistanceDrawn;
   private long mStartTimeDrawn;
   private long mEndTimeDrawn;
   float density = Resources.getSystem().getDisplayMetrics().density;

   private final Paint whiteText;
   private final Paint ltgreyMatrixDashed;
   private final Paint greenGraphLine;
   private final Paint dkgreyMatrixLine;
   private final Paint whiteCenteredText;
   private final Paint dkgrayLargeType;

   public GraphCanvas(final Context context, final AttributeSet attrs)
   {
      this(context, attrs, 0);
   }

   public GraphCanvas(final Context context, final AttributeSet attrs, final int defStyle)
   {
      super(context, attrs, defStyle);

      this.mContext = context;

      this.whiteText = new Paint();
      this.whiteText.setColor(Color.WHITE);
      this.whiteText.setAntiAlias(true);
      this.whiteText.setTextSize((int) (this.density * 12));

      this.whiteCenteredText = new Paint();
      this.whiteCenteredText.setColor(Color.WHITE);
      this.whiteCenteredText.setAntiAlias(true);
      this.whiteCenteredText.setTextAlign(Paint.Align.CENTER);
      this.whiteCenteredText.setTextSize((int) (this.density * 12));

      this.ltgreyMatrixDashed = new Paint();
      this.ltgreyMatrixDashed.setColor(Color.LTGRAY);
      this.ltgreyMatrixDashed.setStrokeWidth(1);
      this.ltgreyMatrixDashed.setPathEffect(new DashPathEffect(new float[] { 2, 4 }, 0));

      this.greenGraphLine = new Paint();
      this.greenGraphLine.setPathEffect(new CornerPathEffect(8));
      this.greenGraphLine.setStyle(Paint.Style.STROKE);
      this.greenGraphLine.setStrokeWidth(4);
      this.greenGraphLine.setAntiAlias(true);
      this.greenGraphLine.setColor(Color.GREEN);

      this.dkgreyMatrixLine = new Paint();
      this.dkgreyMatrixLine.setColor(Color.DKGRAY);
      this.dkgreyMatrixLine.setStrokeWidth(2);

      this.dkgrayLargeType = new Paint();
      this.dkgrayLargeType.setColor(Color.LTGRAY);
      this.dkgrayLargeType.setAntiAlias(true);
      this.dkgrayLargeType.setTextAlign(Paint.Align.CENTER);
      this.dkgrayLargeType.setTextSize((int) (this.density * 21));
      this.dkgrayLargeType.setTypeface(Typeface.DEFAULT_BOLD);
   }

   /**
    * Set the dataset for which to draw data. Also provide hints and helpers.
    * 
    * @param uri
    * @param startTime
    * @param endTime
    * @param distance
    * @param minAlititude
    * @param maxAlititude
    * @param maxSpeed
    * @param units
    */
   public void setData(final Uri uri, final StatisticsCalulator calc)
   {
      boolean rerender = false;
      if (uri.equals(this.mUri))
      {
         final double distanceDrawnPercentage = this.mDistanceDrawn / this.mDistance;
         final double duractionDrawnPercentage = (1d + this.mEndTimeDrawn - this.mStartTimeDrawn) / (1d + this.mEndTime - this.mStartTime);
         rerender = distanceDrawnPercentage < 0.99d || duractionDrawnPercentage < 0.99d;
      }
      else
      {
         if (this.mRenderBuffer == null && super.getWidth() > 0 && super.getHeight() > 0)
         {
            initRenderBuffer(super.getWidth(), super.getHeight());
         }
         rerender = true;
      }

      this.mUri = uri;
      this.mUnits = calc.getUnits();

      this.mMinAlititude = this.mUnits.conversionFromMeterToHeight(calc.getMinAltitude());
      this.mMaxAlititude = this.mUnits.conversionFromMeterToHeight(calc.getMaxAltitude());

      if (this.mUnits.isUnitFlipped())
      {
         this.mHighestSpeedNumber = 1.5 * this.mUnits.conversionFromMetersPerSecond(calc.getAverageStatisicsSpeed());
      }
      else
      {
         this.mHighestSpeedNumber = this.mUnits.conversionFromMetersPerSecond(calc.getMaxSpeed());
      }
      this.mStartTime = calc.getStarttime();
      this.mEndTime = calc.getEndtime();
      this.mDistance = calc.getDistanceTraveled();

      if (rerender)
      {
         renderGraph();
      }
   }

   public synchronized void clearData()
   {
      this.mUri = null;
      this.mUnits = null;
      this.mRenderBuffer = null;
   }

   public void setType(final int graphType)
   {
      if (this.mGraphType != graphType)
      {
         this.mGraphType = graphType;
         renderGraph();
      }
   }

   public int getType()
   {
      return this.mGraphType;
   }

   @Override
   protected synchronized void onSizeChanged(final int w, final int h, final int oldw, final int oldh)
   {
      super.onSizeChanged(w, h, oldw, oldh);
      initRenderBuffer(w, h);

      renderGraph();
   }

   private void initRenderBuffer(final int w, final int h)
   {
      this.mRenderBuffer = Bitmap.createBitmap(w, h, Config.ARGB_8888);
      this.mRenderCanvas = new Canvas(this.mRenderBuffer);
   }

   @Override
   protected synchronized void onDraw(final Canvas canvas)
   {
      super.onDraw(canvas);
      if (this.mRenderBuffer != null)
      {
         canvas.drawBitmap(this.mRenderBuffer, 0, 0, null);
      }
   }

   private synchronized void renderGraph()
   {
      if (this.mRenderBuffer != null && this.mUri != null)
      {
         this.mRenderBuffer.eraseColor(Color.TRANSPARENT);
         switch (this.mGraphType)
         {
            case (TIMESPEEDGRAPH):
               setupSpeedAxis();
               drawGraphType();
               drawTimeAxisGraphOnCanvas(new String[] { WaypointsColumns.TIME, WaypointsColumns.SPEED }, Constants.MIN_STATISTICS_SPEED);
               drawSpeedsTexts();
               drawTimeTexts();
               break;
            case (DISTANCESPEEDGRAPH):
               setupSpeedAxis();
               drawGraphType();
               drawDistanceAxisGraphOnCanvas(new String[] { WaypointsColumns.LONGITUDE, WaypointsColumns.LATITUDE, WaypointsColumns.SPEED }, Constants.MIN_STATISTICS_SPEED);
               drawSpeedsTexts();
               drawDistanceTexts();
               break;
            case (TIMEALTITUDEGRAPH):
               setupAltitudeAxis();
               drawGraphType();
               drawTimeAxisGraphOnCanvas(new String[] { WaypointsColumns.TIME, WaypointsColumns.ALTITUDE }, -1000d);
               drawAltitudesTexts();
               drawTimeTexts();
               break;
            case (DISTANCEALTITUDEGRAPH):
               setupAltitudeAxis();
               drawGraphType();
               drawDistanceAxisGraphOnCanvas(new String[] { WaypointsColumns.LONGITUDE, WaypointsColumns.LATITUDE, WaypointsColumns.ALTITUDE }, -1000d);
               drawAltitudesTexts();
               drawDistanceTexts();
               break;
            default:
               break;
         }
         this.mDistanceDrawn = this.mDistance;
         this.mStartTimeDrawn = this.mStartTime;
         this.mEndTimeDrawn = this.mEndTime;
      }

      postInvalidate();
   }

   /**
    * @param params
    * @param minValue Minimum value of params[1] that will be drawn
    */
   private void drawDistanceAxisGraphOnCanvas(final String[] params, final double minValue)
   {
      final ContentResolver resolver = this.mContext.getContentResolver();
      final Uri segmentsUri = Uri.withAppendedPath(this.mUri, "segments");
      Uri waypointsUri = null;
      Cursor segments = null;
      Cursor waypoints = null;
      double[][] values;
      int[][] valueDepth;
      double distance = 1;
      try
      {
         segments = resolver.query(segmentsUri, new String[] { BaseColumns._ID }, null, null, null);
         final int segmentCount = segments.getCount();
         values = new double[segmentCount][this.mWidth];
         valueDepth = new int[segmentCount][this.mWidth];
         if (segments.moveToFirst())
         {
            for (int segment = 0; segment < segmentCount; segment++)
            {
               segments.moveToPosition(segment);
               final long segmentId = segments.getLong(0);
               waypointsUri = Uri.withAppendedPath(segmentsUri, segmentId + "/waypoints");
               try
               {
                  waypoints = resolver.query(waypointsUri, params, null, null, null);
                  if (waypoints.moveToFirst())
                  {
                     Location lastLocation = null;
                     Location currentLocation = null;
                     do
                     {
                        currentLocation = new Location(this.getClass().getName());
                        currentLocation.setLongitude(waypoints.getDouble(0));
                        currentLocation.setLatitude(waypoints.getDouble(1));
                        // Do no include obvious wrong 0.0 lat 0.0 long, skip to next value in while-loop
                        if (currentLocation.getLatitude() == 0.0d || currentLocation.getLongitude() == 0.0d)
                        {
                           continue;
                        }
                        if (lastLocation != null)
                        {
                           distance += lastLocation.distanceTo(currentLocation);
                        }
                        lastLocation = currentLocation;
                        final double value = waypoints.getDouble(2);
                        if (value != 0 && value > minValue && segment < values.length)
                        {
                           final int x = (int) ((distance) * (this.mWidth - 1) / this.mDistance);
                           if (x > 0 && x < valueDepth[segment].length)
                           {
                              valueDepth[segment][x]++;
                              values[segment][x] = values[segment][x] + ((value - values[segment][x]) / valueDepth[segment][x]);
                           }
                        }
                     }
                     while (waypoints.moveToNext());
                  }
               }
               finally
               {
                  if (waypoints != null)
                  {
                     waypoints.close();
                  }
               }
            }
         }
      }
      finally
      {
         if (segments != null)
         {
            segments.close();
         }
      }
      for (int segment = 0; segment < values.length; segment++)
      {
         for (int x = 0; x < values[segment].length; x++)
         {
            if (valueDepth[segment][x] > 0)
            {
               values[segment][x] = translateValue(values[segment][x]);
            }
         }
      }
      drawGraph(values, valueDepth);
   }

   private void drawTimeAxisGraphOnCanvas(final String[] params, final double minValue)
   {
      final ContentResolver resolver = this.mContext.getContentResolver();
      final Uri segmentsUri = Uri.withAppendedPath(this.mUri, "segments");
      Uri waypointsUri = null;
      Cursor segments = null;
      Cursor waypoints = null;
      final long duration = 1 + this.mEndTime - this.mStartTime;
      double[][] values;
      int[][] valueDepth;
      try
      {
         segments = resolver.query(segmentsUri, new String[] { BaseColumns._ID }, null, null, null);
         final int segmentCount = segments.getCount();
         values = new double[segmentCount][this.mWidth];
         valueDepth = new int[segmentCount][this.mWidth];
         if (segments.moveToFirst())
         {
            for (int segment = 0; segment < segmentCount; segment++)
            {
               segments.moveToPosition(segment);
               final long segmentId = segments.getLong(0);
               waypointsUri = Uri.withAppendedPath(segmentsUri, segmentId + "/waypoints");
               try
               {
                  waypoints = resolver.query(waypointsUri, params, null, null, null);
                  if (waypoints.moveToFirst())
                  {
                     do
                     {
                        final long time = waypoints.getLong(0);
                        final double value = waypoints.getDouble(1);
                        if (value != 0 && value > minValue && segment < values.length)
                        {
                           final int x = (int) ((time - this.mStartTime) * (this.mWidth - 1) / duration);
                           if (x > 0 && x < valueDepth[segment].length)
                           {
                              valueDepth[segment][x]++;
                              values[segment][x] = values[segment][x] + ((value - values[segment][x]) / valueDepth[segment][x]);
                           }
                        }
                     }
                     while (waypoints.moveToNext());
                  }
               }
               finally
               {
                  if (waypoints != null)
                  {
                     waypoints.close();
                  }
               }
            }

         }
      }
      finally
      {
         if (segments != null)
         {
            segments.close();
         }
      }
      for (int p = 0; p < values.length; p++)
      {
         for (int x = 0; x < values[p].length; x++)
         {
            if (valueDepth[p][x] > 0)
            {
               values[p][x] = translateValue(values[p][x]);
            }
         }
      }
      drawGraph(values, valueDepth);
   }

   private void setupAltitudeAxis()
   {
      this.mMinAxis = -4 + 4 * (int) (this.mMinAlititude / 4);
      this.mMaxAxis = 4 + 4 * (int) (this.mMaxAlititude / 4);

      this.mWidth = this.mRenderCanvas.getWidth() - 5;
      this.mHeight = this.mRenderCanvas.getHeight() - 10;
   }

   private void setupSpeedAxis()
   {
      this.mMinAxis = 0;
      this.mMaxAxis = 4 + 4 * (int) (this.mHighestSpeedNumber / 4);

      this.mWidth = this.mRenderCanvas.getWidth() - 5;
      this.mHeight = this.mRenderCanvas.getHeight() - 10;
   }

   private void drawAltitudesTexts()
   {
      this.mRenderCanvas.drawText(String.format("%d %s", this.mMinAxis, this.mUnits.getHeightUnit()), 8, this.mHeight, this.whiteText);
      this.mRenderCanvas.drawText(String.format("%d %s", (this.mMaxAxis + this.mMinAxis) / 2, this.mUnits.getHeightUnit()), 8, 5 + this.mHeight / 2, this.whiteText);
      this.mRenderCanvas.drawText(String.format("%d %s", this.mMaxAxis, this.mUnits.getHeightUnit()), 8, 15, this.whiteText);
   }

   private void drawSpeedsTexts()
   {
      this.mRenderCanvas.drawText(String.format("%d %s", this.mMinAxis, this.mUnits.getSpeedUnit()), 8, this.mHeight, this.whiteText);
      this.mRenderCanvas.drawText(String.format("%d %s", (this.mMaxAxis + this.mMinAxis) / 2, this.mUnits.getSpeedUnit()), 8, 3 + this.mHeight / 2, this.whiteText);
      this.mRenderCanvas.drawText(String.format("%d %s", this.mMaxAxis, this.mUnits.getSpeedUnit()), 8, 7 + this.whiteText.getTextSize(), this.whiteText);
   }

   private void drawGraphType()
   {
      //float density = Resources.getSystem().getDisplayMetrics().density;
      String text;
      switch (this.mGraphType)
      {
         case (TIMESPEEDGRAPH):
            text = this.mContext.getResources().getString(R.string.graphtype_timespeed);
            break;
         case (DISTANCESPEEDGRAPH):
            text = this.mContext.getResources().getString(R.string.graphtype_distancespeed);
            break;
         case (TIMEALTITUDEGRAPH):
            text = this.mContext.getResources().getString(R.string.graphtype_timealtitude);
            break;
         case (DISTANCEALTITUDEGRAPH):
            text = this.mContext.getResources().getString(R.string.graphtype_distancealtitude);
            break;
         default:
            text = "UNKNOWN GRAPH TYPE";
            break;
      }
      this.mRenderCanvas.drawText(text, 5 + this.mWidth / 2, 5 + this.mHeight / 8, this.dkgrayLargeType);

   }

   private void drawTimeTexts()
   {
      final DateFormat timeInstance = android.text.format.DateFormat.getTimeFormat(getContext().getApplicationContext());
      final String start = timeInstance.format(new Date(this.mStartTime));
      final String half = timeInstance.format(new Date((this.mEndTime + this.mStartTime) / 2));
      final String end = timeInstance.format(new Date(this.mEndTime));

      Path yAxis;
      yAxis = new Path();
      yAxis.moveTo(5, 5 + this.mHeight / 2);
      yAxis.lineTo(5, 5);
      this.mRenderCanvas.drawTextOnPath(String.format(start), yAxis, 0, this.whiteCenteredText.getTextSize(), this.whiteCenteredText);
      yAxis = new Path();
      yAxis.moveTo(5 + this.mWidth / 2, 5 + this.mHeight / 2);
      yAxis.lineTo(5 + this.mWidth / 2, 5);
      this.mRenderCanvas.drawTextOnPath(String.format(half), yAxis, 0, -3, this.whiteCenteredText);
      yAxis = new Path();
      yAxis.moveTo(5 + this.mWidth - 1, 5 + this.mHeight / 2);
      yAxis.lineTo(5 + this.mWidth - 1, 5);
      this.mRenderCanvas.drawTextOnPath(String.format(end), yAxis, 0, -3, this.whiteCenteredText);
   }

   private void drawDistanceTexts()
   {
      final String start = String.format("%.0f %s", this.mUnits.conversionFromMeter(0), this.mUnits.getDistanceUnit());
      final String half = String.format("%.0f %s", this.mUnits.conversionFromMeter(this.mDistance) / 2, this.mUnits.getDistanceUnit());
      final String end = String.format("%.0f %s", this.mUnits.conversionFromMeter(this.mDistance), this.mUnits.getDistanceUnit());

      Path yAxis;
      yAxis = new Path();
      yAxis.moveTo(5, 5 + this.mHeight / 2);
      yAxis.lineTo(5, 5);
      this.mRenderCanvas.drawTextOnPath(String.format(start), yAxis, 0, this.whiteText.getTextSize(), this.whiteCenteredText);
      yAxis = new Path();
      yAxis.moveTo(5 + this.mWidth / 2, 5 + this.mHeight / 2);
      yAxis.lineTo(5 + this.mWidth / 2, 5);
      this.mRenderCanvas.drawTextOnPath(String.format(half), yAxis, 0, -3, this.whiteCenteredText);
      yAxis = new Path();
      yAxis.moveTo(5 + this.mWidth - 1, 5 + this.mHeight / 2);
      yAxis.lineTo(5 + this.mWidth - 1, 5);
      this.mRenderCanvas.drawTextOnPath(String.format(end), yAxis, 0, -3, this.whiteCenteredText);
   }

   private double translateValue(double val)
   {
      switch (this.mGraphType)
      {
         case (TIMESPEEDGRAPH):
         case (DISTANCESPEEDGRAPH):
            val = this.mUnits.conversionFromMetersPerSecond(val);
            break;
         case (TIMEALTITUDEGRAPH):
         case (DISTANCEALTITUDEGRAPH):
            val = this.mUnits.conversionFromMeterToHeight(val);
            break;
         default:
            break;
      }
      return val;

   }

   private void drawGraph(final double[][] values, final int[][] valueDepth)
   {
      // Matrix
      // Horizontals
      this.mRenderCanvas.drawLine(5, 5, 5 + this.mWidth, 5, this.ltgreyMatrixDashed); // top
      this.mRenderCanvas.drawLine(5, 5 + this.mHeight / 4, 5 + this.mWidth, 5 + this.mHeight / 4, this.ltgreyMatrixDashed); // 2nd
      this.mRenderCanvas.drawLine(5, 5 + this.mHeight / 2, 5 + this.mWidth, 5 + this.mHeight / 2, this.ltgreyMatrixDashed); // middle
      this.mRenderCanvas.drawLine(5, 5 + this.mHeight / 4 * 3, 5 + this.mWidth, 5 + this.mHeight / 4 * 3, this.ltgreyMatrixDashed); // 3rd
      // Verticals
      this.mRenderCanvas.drawLine(5 + this.mWidth / 4, 5, 5 + this.mWidth / 4, 5 + this.mHeight, this.ltgreyMatrixDashed); // 2nd
      this.mRenderCanvas.drawLine(5 + this.mWidth / 2, 5, 5 + this.mWidth / 2, 5 + this.mHeight, this.ltgreyMatrixDashed); // middle
      this.mRenderCanvas.drawLine(5 + this.mWidth / 4 * 3, 5, 5 + this.mWidth / 4 * 3, 5 + this.mHeight, this.ltgreyMatrixDashed); // 3rd
      this.mRenderCanvas.drawLine(5 + this.mWidth - 1, 5, 5 + this.mWidth - 1, 5 + this.mHeight, this.ltgreyMatrixDashed); // right

      // The line
      Path mPath;
      int emptyValues = 0;
      mPath = new Path();
      for (int p = 0; p < values.length; p++)
      {
         int start = 0;
         while (valueDepth[p][start] == 0 && start < values[p].length - 1)
         {
            start++;
         }
         mPath.moveTo((float) start + 5, 5f + (float) (this.mHeight - ((values[p][start] - this.mMinAxis) * this.mHeight) / (this.mMaxAxis - this.mMinAxis)));
         for (int x = start; x < values[p].length; x++)
         {
            final double y = this.mHeight - ((values[p][x] - this.mMinAxis) * this.mHeight) / (this.mMaxAxis - this.mMinAxis);
            if (valueDepth[p][x] > 0)
            {
               if (emptyValues > this.mWidth / 10)
               {
                  mPath.moveTo((float) x + 5, (float) y + 5);
               }
               else
               {
                  mPath.lineTo((float) x + 5, (float) y + 5);
               }
               emptyValues = 0;
            }
            else
            {
               emptyValues++;
            }
         }
      }
      this.mRenderCanvas.drawPath(mPath, this.greenGraphLine);

      // Axis's
      this.mRenderCanvas.drawLine(5, 5, 5, 5 + this.mHeight, this.dkgreyMatrixLine);
      this.mRenderCanvas.drawLine(5, 5 + this.mHeight, 5 + this.mWidth, 5 + this.mHeight, this.dkgreyMatrixLine);
   }

}
