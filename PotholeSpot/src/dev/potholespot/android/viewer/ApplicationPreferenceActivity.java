package dev.potholespot.android.viewer;

import java.util.regex.Pattern;

import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import dev.potholespot.android.util.Constants;
import dev.potholespot.android.util.UnitsI18n;
import dev.potholespot.uganda.R;

public class ApplicationPreferenceActivity extends PreferenceActivity
{

   public static final String STREAMBROADCAST_PREFERENCE = "streambroadcast_distance";
   public static final String UNITS_IMPLEMENT_WIDTH_PREFERENCE = "units_implement_width";
   public static final String CUSTOMPRECISIONDISTANCE_PREFERENCE = "customprecisiondistance";
   public static final String CUSTOMPRECISIONTIME_PREFERENCE = "customprecisiontime";
   public static final String PRECISION_PREFERENCE = "precision";
   public static final String CUSTOMUPLOAD_BACKLOG = "CUSTOMUPLOAD_BACKLOG";
   public static final String CUSTOMUPLOAD_URL = "CUSTOMUPLOAD_URL";

   private EditTextPreference time;
   private EditTextPreference distance;
   private EditTextPreference implentWidth;

   private EditTextPreference streambroadcast_distance;
   private EditTextPreference custumupload_backlog;

   @SuppressWarnings("deprecation")
   @Override
   protected void onCreate(final Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);

      addPreferencesFromResource(R.layout.settings_gps);

      final ListPreference precision = (ListPreference) findPreference(PRECISION_PREFERENCE);
      this.time = (EditTextPreference) findPreference(CUSTOMPRECISIONTIME_PREFERENCE);
      this.distance = (EditTextPreference) findPreference(CUSTOMPRECISIONDISTANCE_PREFERENCE);
      this.implentWidth = (EditTextPreference) findPreference(UNITS_IMPLEMENT_WIDTH_PREFERENCE);
      this.streambroadcast_distance = (EditTextPreference) findPreference(STREAMBROADCAST_PREFERENCE);
      this.custumupload_backlog = (EditTextPreference) findPreference(CUSTOMUPLOAD_BACKLOG);

      setEnabledCustomValues(precision.getValue());
      precision.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
         {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue)
            {
               setEnabledCustomValues(newValue);
               return true;
            }
         });
      this.implentWidth.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
         {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue)
            {
               final String fpExpr = "\\d{1,4}([,\\.]\\d+)?";
               return Pattern.matches(fpExpr, newValue.toString());
            }
         });
      this.streambroadcast_distance.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
         {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue)
            {
               final String fpExpr = "\\d{1,5}";
               final boolean matches = Pattern.matches(fpExpr, newValue.toString());
               if (matches)
               {
                  final Editor editor = getPreferenceManager().getSharedPreferences().edit();
                  final double value = new UnitsI18n(ApplicationPreferenceActivity.this).conversionFromLocalToMeters(Integer.parseInt(newValue.toString()));
                  editor.putFloat("streambroadcast_distance_meter", (float) value);
                  editor.commit();
               }
               return matches;
            }
         });
      this.custumupload_backlog.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
         {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue)
            {
               final String fpExpr = "\\d{1,3}";
               return Pattern.matches(fpExpr, newValue.toString());
            }
         });
   }

   private void setEnabledCustomValues(final Object newValue)
   {
      final boolean customPresicion = Integer.toString(Constants.LOGGING_CUSTOM).equals(newValue);
      this.time.setEnabled(customPresicion);
      this.distance.setEnabled(customPresicion);
   }
}
