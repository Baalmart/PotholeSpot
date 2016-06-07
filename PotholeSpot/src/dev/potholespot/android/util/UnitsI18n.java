package dev.potholespot.android.util;

import java.util.Locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import dev.potholespot.uganda.R;

/**
 * Collection of methods to provide metric and imperial data based on locale or overridden by configuration
 * 
 * @version $Id$
 * @author Martin Bbaale
 */
public class UnitsI18n
{
   private final Context mContext;
   private double mConversion_from_mps_to_speed;
   private double mConversion_from_meter_to_distance;
   private double mConversion_from_meter_to_height;
   private String mSpeed_unit;
   private String mDistance_unit;
   private String mHeight_unit;
   private UnitsChangeListener mListener;
   private final OnSharedPreferenceChangeListener mPreferenceListener = new OnSharedPreferenceChangeListener()
      {
         @Override
         public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key)
         {
            if (key.equals(Constants.UNITS))
            {
               initBasedOnPreferences(sharedPreferences);
               if (UnitsI18n.this.mListener != null)
               {
                  UnitsI18n.this.mListener.onUnitsChange();
               }
            }
         }
      };
   private boolean needsUnitFlip;
   private int mUnits;

   @SuppressWarnings("unused")
   private static final String TAG = "OGT.UnitsI18n";

   public UnitsI18n(final Context ctx, final UnitsChangeListener listener)
   {
      this(ctx);
      this.mListener = listener;
   }

   public UnitsI18n(final Context ctx)
   {
      this.mContext = ctx;
      initBasedOnPreferences(PreferenceManager.getDefaultSharedPreferences(this.mContext));
   }

   private void initBasedOnPreferences(final SharedPreferences sharedPreferences)
   {
      this.mUnits = Integer.parseInt(sharedPreferences.getString(Constants.UNITS, Integer.toString(Constants.UNITS_DEFAULT)));
      switch (this.mUnits)
      {
         case (Constants.UNITS_DEFAULT):
            setToDefault();
            break;
         case (Constants.UNITS_IMPERIAL):
            setToImperial();
            break;
         case (Constants.UNITS_METRIC):
            setToMetric();
            break;
         case (Constants.UNITS_NAUTIC):
            setToMetric();
            overrideWithNautic(this.mContext.getResources());
            break;
         case (Constants.UNITS_METRICPACE):
            setToMetric();
            overrideWithPace(this.mContext.getResources());
            break;
         case (Constants.UNITS_IMPERIALPACE):
            setToImperial();
            overrideWithPaceImperial(this.mContext.getResources());
            break;
         case Constants.UNITS_IMPERIALSURFACE:
            setToImperial();
            overrideWithSurfaceImperial();
            break;
         case Constants.UNITS_METRICSURFACE:
            setToMetric();
            overrideWithSurfaceMetric();
            break;
         default:
            setToDefault();
            break;
      }
   }

   private void setToDefault()
   {
      final Resources resources = this.mContext.getResources();
      init(resources);
   }

   private void setToMetric()
   {
      final Resources resources = this.mContext.getResources();
      final Configuration config = resources.getConfiguration();
      final Locale oldLocale = config.locale;
      config.locale = new Locale("");
      resources.updateConfiguration(config, resources.getDisplayMetrics());
      init(resources);
      config.locale = oldLocale;
      resources.updateConfiguration(config, resources.getDisplayMetrics());
   }

   private void setToImperial()
   {
      final Resources resources = this.mContext.getResources();
      final Configuration config = resources.getConfiguration();
      final Locale oldLocale = config.locale;
      config.locale = Locale.US;
      resources.updateConfiguration(config, resources.getDisplayMetrics());
      init(resources);
      config.locale = oldLocale;
      resources.updateConfiguration(config, resources.getDisplayMetrics());
   }

   /**
    * Based on a given Locale prefetch the units conversions and names.
    * 
    * @param resources Resources initialized with a Locale
    */
   private void init(final Resources resources)
   {
      final TypedValue outValue = new TypedValue();
      this.needsUnitFlip = false;
      resources.getValue(R.raw.conversion_from_mps, outValue, false);
      this.mConversion_from_mps_to_speed = outValue.getFloat();
      resources.getValue(R.raw.conversion_from_meter, outValue, false);
      this.mConversion_from_meter_to_distance = outValue.getFloat();
      resources.getValue(R.raw.conversion_from_meter_to_height, outValue, false);
      this.mConversion_from_meter_to_height = outValue.getFloat();

      this.mSpeed_unit = resources.getString(R.string.speed_unitname);
      this.mDistance_unit = resources.getString(R.string.distance_unitname);
      this.mHeight_unit = resources.getString(R.string.distance_smallunitname);
   }

   private void overrideWithNautic(final Resources resources)
   {
      final TypedValue outValue = new TypedValue();
      resources.getValue(R.raw.conversion_from_mps_to_knot, outValue, false);
      this.mConversion_from_mps_to_speed = outValue.getFloat();
      resources.getValue(R.raw.conversion_from_meter_to_nauticmile, outValue, false);
      this.mConversion_from_meter_to_distance = outValue.getFloat();

      this.mSpeed_unit = resources.getString(R.string.knot_unitname);
      this.mDistance_unit = resources.getString(R.string.nautic_unitname);
   }

   private void overrideWithPace(final Resources resources)
   {
      this.needsUnitFlip = true;
      this.mSpeed_unit = resources.getString(R.string.pace_unitname);
   }

   private void overrideWithPaceImperial(final Resources resources)
   {
      this.needsUnitFlip = true;
      this.mSpeed_unit = resources.getString(R.string.pace_unitname_imperial);
   }

   private void overrideWithSurfaceImperial()
   {
      final float width = getWidthPreference();
      final Resources resources = this.mContext.getResources();
      final TypedValue outValue = new TypedValue();
      resources.getValue(R.raw.conversion_from_mps_to_acres_hour, outValue, false);
      this.mConversion_from_mps_to_speed = outValue.getFloat() * width;
      this.mSpeed_unit = resources.getString(R.string.surface_unitname_imperial);
   }

   private float getWidthPreference()
   {
      return Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(this.mContext).getString("units_implement_width", "12"));
   }

   private void overrideWithSurfaceMetric()
   {
      final float width = getWidthPreference();
      final Resources resources = this.mContext.getResources();
      final TypedValue outValue = new TypedValue();
      resources.getValue(R.raw.conversion_from_mps_to_hectare_hour, outValue, false);
      this.mConversion_from_mps_to_speed = outValue.getFloat() * width;
      this.mSpeed_unit = resources.getString(R.string.surface_unitname_metric);
   }

   public double conversionFromMeterAndMiliseconds(final double meters, final long miliseconds)
   {
      final float seconds = miliseconds / 1000f;
      return conversionFromMetersPerSecond(meters / seconds);
   }

   public double conversionFromMetersPerSecond(final double mps)
   {
      double speed = mps * this.mConversion_from_mps_to_speed;
      if (this.needsUnitFlip) // Flip from "x per hour" to "minutes per x"
      {
         if (speed > 1) // Nearly no speed return 0 as if there is no speed
         {
            speed = (1 / speed) * 60.0;
         }
         else
         {
            speed = 0;
         }
      }
      return speed;
   }

   public double conversionFromMeter(final double meters)
   {
      final double value = meters * this.mConversion_from_meter_to_distance;
      return value;
   }

   public double conversionFromLocalToMeters(final double localizedValue)
   {
      final double meters = localizedValue / this.mConversion_from_meter_to_distance;
      return meters;
   }

   public double conversionFromMeterToHeight(final double meters)
   {
      return meters * this.mConversion_from_meter_to_height;
   }

   public String getSpeedUnit()
   {
      return this.mSpeed_unit;
   }

   public String getDistanceUnit()
   {
      return this.mDistance_unit;
   }

   public String getHeightUnit()
   {
      return this.mHeight_unit;
   }

   public boolean isUnitFlipped()
   {
      return this.needsUnitFlip;
   }

   public void setUnitsChangeListener(final UnitsChangeListener unitsChangeListener)
   {
      this.mListener = unitsChangeListener;
      if (this.mListener != null)
      {
         initBasedOnPreferences(PreferenceManager.getDefaultSharedPreferences(this.mContext));
         PreferenceManager.getDefaultSharedPreferences(this.mContext).registerOnSharedPreferenceChangeListener(this.mPreferenceListener);
      }
      else
      {
         PreferenceManager.getDefaultSharedPreferences(this.mContext).unregisterOnSharedPreferenceChangeListener(this.mPreferenceListener);
      }
   }

   /**
    * Interface definition for a callback to be invoked when the preference for units changed.
    * 
    * @version $Id$
    * @author rene (c) Feb 14, 2010, Sogeti B.V.
    */
   public interface UnitsChangeListener
   {
      /**
       * Called when the unit data has changed.
       */
      void onUnitsChange();
   }

   /**
    * Format a speed using the current unit and flipping
    * 
    * @param speed
    * @param decimals format a bit larger showing decimals or seconds
    * @return
    */
   public String formatSpeed(final double speed, final boolean decimals)
   {
      String speedText;
      if (this.mUnits == Constants.UNITS_METRICPACE || this.mUnits == Constants.UNITS_IMPERIALPACE)
      {
         if (decimals)
         {
            speedText = String.format("%02d %s", (int) speed, getSpeedUnit());
         }
         else
         {
            speedText = String.format("%02d:%02d %s", (int) speed, (int) ((speed - (int) speed) * 60), // convert decimal to seconds
                  getSpeedUnit());
         }
      }
      else
      {
         if (decimals)
         {
            speedText = String.format("%.2f %s", speed, getSpeedUnit());
         }
         else
         {
            speedText = String.format("%.0f %s", speed, getSpeedUnit());
         }

      }
      return speedText;
   }
}
