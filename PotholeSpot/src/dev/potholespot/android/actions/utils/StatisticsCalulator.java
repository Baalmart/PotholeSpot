package dev.potholespot.android.actions.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import dev.potholespot.android.db.Pspot.TracksColumns;
import dev.potholespot.android.db.Pspot.Waypoints;
import dev.potholespot.android.db.Pspot.WaypointsColumns;
import dev.potholespot.android.util.Constants;
import dev.potholespot.android.util.UnitsI18n;

public class StatisticsCalulator extends AsyncTask<Uri, Void, Void>
{

   @SuppressWarnings("unused")
   private static final String TAG = "pspot.StatisticsCalulator";
   private final Context mContext;
   private String overallavgSpeedText = "Unknown";
   private String avgSpeedText = "Unknown";
   private String maxSpeedText = "Unknown";
   private String ascensionText = "Unknown";
   private final String minSpeedText = "Unknown";
   private String tracknameText = "Unknown";
   private String waypointsText = "Unknown";
   private String distanceText = "Unknown";
   private long mStarttime = -1;
   private long mEndtime = -1;
   private final UnitsI18n mUnits;
   private double mMaxSpeed;

   //the variables for altitude are below:
   private double mMaxAltitude;
   private double mMinAltitude;
   private double mAscension;
   private double mDistanceTraveled;
   private long mDuration;
   private double mAverageActiveSpeed;
   private final StatisticsDelegate mDelegate;

   public StatisticsCalulator(final Context ctx, final UnitsI18n units, final StatisticsDelegate delegate)
   {
      this.mContext = ctx;
      this.mUnits = units;
      this.mDelegate = delegate;
   }

   private void updateCalculations(final Uri trackUri)
   {
      this.mStarttime = -1;
      this.mEndtime = -1;
      this.mMaxSpeed = 0;
      this.mAverageActiveSpeed = 0;
      this.mMaxAltitude = 0;
      this.mMinAltitude = 0;
      this.mAscension = 0;
      this.mDistanceTraveled = 0f;
      this.mDuration = 0;
      long duration = 1;
      double ascension = 0;

      final ContentResolver resolver = this.mContext.getContentResolver();

      Cursor waypointsCursor = null;
      try
      {
         waypointsCursor = resolver.query(Uri.withAppendedPath(trackUri, "waypoints"), new String[] { "max  (" + Waypoints.TABLE + "." + WaypointsColumns.SPEED + ")",
               "max  (" + Waypoints.TABLE + "." + WaypointsColumns.ALTITUDE + ")", "min  (" + Waypoints.TABLE + "." + WaypointsColumns.ALTITUDE + ")",
               "count(" + Waypoints.TABLE + "." + BaseColumns._ID + ")" }, null, null, null);
         if (waypointsCursor.moveToLast())
         {
            this.mMaxSpeed = waypointsCursor.getDouble(0);
            this.mMaxAltitude = waypointsCursor.getDouble(1);
            this.mMinAltitude = waypointsCursor.getDouble(2);
            final long nrWaypoints = waypointsCursor.getLong(3);
            this.waypointsText = nrWaypoints + "";
         }
         waypointsCursor.close();
         waypointsCursor = resolver.query(Uri.withAppendedPath(trackUri, "waypoints"), new String[] { "avg  (" + Waypoints.TABLE + "." + WaypointsColumns.SPEED + ")" }, Waypoints.TABLE + "."
               + WaypointsColumns.SPEED + "  > ?", new String[] { "" + Constants.MIN_STATISTICS_SPEED }, null);
         if (waypointsCursor.moveToLast())
         {
            this.mAverageActiveSpeed = waypointsCursor.getDouble(0);
         }
      }
      finally
      {
         if (waypointsCursor != null)
         {
            waypointsCursor.close();
         }
      }
      Cursor trackCursor = null;
      try
      {
         trackCursor = resolver.query(trackUri, new String[] { TracksColumns.NAME }, null, null, null);
         if (trackCursor.moveToLast())
         {
            this.tracknameText = trackCursor.getString(0);
         }
      }
      finally
      {
         if (trackCursor != null)
         {
            trackCursor.close();
         }
      }
      Cursor segments = null;
      Location lastLocation = null;
      Location lastAltitudeLocation = null;
      Location currentLocation = null;
      try
      {
         final Uri segmentsUri = Uri.withAppendedPath(trackUri, "segments");
         segments = resolver.query(segmentsUri, new String[] { BaseColumns._ID }, null, null, null);
         if (segments.moveToFirst())
         {
            do
            {
               final long segmentsId = segments.getLong(0);
               Cursor waypoints = null;
               try
               {
                  final Uri waypointsUri = Uri.withAppendedPath(segmentsUri, segmentsId + "/waypoints");
                  waypoints = resolver.query(waypointsUri, new String[] { BaseColumns._ID, WaypointsColumns.TIME, WaypointsColumns.LONGITUDE, WaypointsColumns.LATITUDE, WaypointsColumns.ALTITUDE },
                        null, null, null);
                  if (waypoints.moveToFirst())
                  {
                     do
                     {
                        if (this.mStarttime < 0)
                        {
                           this.mStarttime = waypoints.getLong(1);
                        }
                        currentLocation = new Location(this.getClass().getName());
                        currentLocation.setTime(waypoints.getLong(1));
                        currentLocation.setLongitude(waypoints.getDouble(2));
                        currentLocation.setLatitude(waypoints.getDouble(3));
                        currentLocation.setAltitude(waypoints.getDouble(4));

                        // Do no include obvious wrong 0.0 lat 0.0 long, skip to next value in while-loop
                        if (currentLocation.getLatitude() == 0.0d || currentLocation.getLongitude() == 0.0d)
                        {
                           continue;
                        }

                        if (lastLocation != null)
                        {
                           final float travelPart = lastLocation.distanceTo(currentLocation);
                           final long timePart = currentLocation.getTime() - lastLocation.getTime();
                           this.mDistanceTraveled += travelPart;
                           duration += timePart;
                        }
                        if (currentLocation.hasAltitude())
                        {
                           if (lastAltitudeLocation != null)
                           {
                              if (currentLocation.getTime() - lastAltitudeLocation.getTime() > 5 * 60 * 1000) // more then a 5m of climbing
                              {
                                 if (currentLocation.getAltitude() > lastAltitudeLocation.getAltitude() + 1) // more then 1m climb
                                 {
                                    ascension += currentLocation.getAltitude() - lastAltitudeLocation.getAltitude();
                                    lastAltitudeLocation = currentLocation;
                                 }
                                 else
                                 {
                                    lastAltitudeLocation = currentLocation;
                                 }
                              }
                           }
                           else
                           {
                              lastAltitudeLocation = currentLocation;
                           }
                        }
                        lastLocation = currentLocation;
                        this.mEndtime = lastLocation.getTime();
                     }
                     while (waypoints.moveToNext());
                     this.mDuration = this.mEndtime - this.mStarttime;
                  }
               }
               finally
               {
                  if (waypoints != null)
                  {
                     waypoints.close();
                  }
               }
               lastLocation = null;
            }
            while (segments.moveToNext());
         }
      }
      finally
      {
         if (segments != null)
         {
            segments.close();
         }
      }
      final double maxSpeed = this.mUnits.conversionFromMetersPerSecond(this.mMaxSpeed);
      final double overallavgSpeedfl = this.mUnits.conversionFromMeterAndMiliseconds(this.mDistanceTraveled, this.mDuration);
      final double avgSpeedfl = this.mUnits.conversionFromMeterAndMiliseconds(this.mDistanceTraveled, duration);
      final double traveled = this.mUnits.conversionFromMeter(this.mDistanceTraveled);
      this.avgSpeedText = this.mUnits.formatSpeed(avgSpeedfl, true);
      this.overallavgSpeedText = this.mUnits.formatSpeed(overallavgSpeedfl, true);
      this.maxSpeedText = this.mUnits.formatSpeed(maxSpeed, true);
      this.distanceText = String.format("%.2f %s", traveled, this.mUnits.getDistanceUnit());
      this.ascensionText = String.format("%.0f %s", ascension, this.mUnits.getHeightUnit());
   }

   /**
    * Get the overallavgSpeedText.
    * 
    * @return Returns the overallavgSpeedText as a String.
    */
   public String getOverallavgSpeedText()
   {
      return this.overallavgSpeedText;
   }

   /**
    * Get the avgSpeedText.
    * 
    * @return Returns the avgSpeedText as a String.
    */
   public String getAvgSpeedText()
   {
      return this.avgSpeedText;
   }

   /**
    * Get the maxSpeedText.
    * 
    * @return Returns the maxSpeedText as a String.
    */
   public String getMaxSpeedText()
   {
      return this.maxSpeedText;
   }

   /**
    * Get the minSpeedText.
    * 
    * @return Returns the minSpeedText as a String.
    */
   public String getMinSpeedText()
   {
      return this.minSpeedText;
   }

   /**
    * Get the tracknameText.
    * 
    * @return Returns the tracknameText as a String.
    */
   public String getTracknameText()
   {
      return this.tracknameText;
   }

   /**
    * Get the waypointsText.
    * 
    * @return Returns the waypointsText as a String.
    */
   public String getWaypointsText()
   {
      return this.waypointsText;
   }

   /**
    * Get the distanceText.
    * 
    * @return Returns the distanceText as a String.
    */
   public String getDistanceText()
   {
      return this.distanceText;
   }

   /**
    * Get the starttime.
    * 
    * @return Returns the starttime as a long.
    */
   public long getStarttime()
   {
      return this.mStarttime;
   }

   /**
    * Get the endtime.
    * 
    * @return Returns the endtime as a long.
    */
   public long getEndtime()
   {
      return this.mEndtime;
   }

   /**
    * Get the maximum speed.
    * 
    * @return Returns the maxSpeeddb as m/s in a double.
    */
   public double getMaxSpeed()
   {
      return this.mMaxSpeed;
   }

   /**
    * Get the min speed.
    * 
    * @return Returns the average speed as m/s in a double.
    */
   public double getAverageStatisicsSpeed()
   {
      return this.mAverageActiveSpeed;
   }

   /**
    * Get the maxAltitude.
    * 
    * @return Returns the maxAltitude as a double.
    */
   public double getMaxAltitude()
   {
      return this.mMaxAltitude;
   }

   /**
    * Get the minAltitude.
    * 
    * @return Returns the minAltitude as a double.
    */
   public double getMinAltitude()
   {
      return this.mMinAltitude;
   }

   /**
    * Get the total ascension in m.
    * 
    * @return Returns the ascension as a double.
    */
   public double getAscension()
   {
      return this.mAscension;
   }

   public CharSequence getAscensionText()
   {
      return this.ascensionText;
   }

   /**
    * Get the distanceTraveled.
    * 
    * @return Returns the distanceTraveled as a float.
    */
   public double getDistanceTraveled()
   {
      return this.mDistanceTraveled;
   }

   /**
    * Get the mUnits.
    * 
    * @return Returns the mUnits as a UnitsI18n.
    */
   public UnitsI18n getUnits()
   {
      return this.mUnits;
   }

   public String getDurationText()
   {
      final long s = this.mDuration / 1000;
      final String duration = String.format("%dh:%02dm:%02ds", s / 3600, (s % 3600) / 60, (s % 60));

      return duration;
   }

   @Override
   protected Void doInBackground(final Uri... params)
   {
      updateCalculations(params[0]);
      return null;
   }

   @Override
   protected void onPostExecute(final Void result)
   {
      super.onPostExecute(result);
      if (this.mDelegate != null)
      {
         this.mDelegate.finishedCalculations(this);
      }

   }
}
