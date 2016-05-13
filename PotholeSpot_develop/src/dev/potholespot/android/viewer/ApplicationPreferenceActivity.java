package dev.potholespot.android.viewer;

import java.util.regex.Pattern;

import dev.potholespot.uganda.R;
import dev.potholespot.android.util.Constants;
import dev.potholespot.android.util.UnitsI18n;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

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
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);

      addPreferencesFromResource(R.layout.settings_gps);

      ListPreference precision = (ListPreference) findPreference(PRECISION_PREFERENCE);
      time = (EditTextPreference) findPreference(CUSTOMPRECISIONTIME_PREFERENCE);
      distance = (EditTextPreference) findPreference(CUSTOMPRECISIONDISTANCE_PREFERENCE);
      implentWidth = (EditTextPreference) findPreference(UNITS_IMPLEMENT_WIDTH_PREFERENCE);
      streambroadcast_distance = (EditTextPreference) findPreference(STREAMBROADCAST_PREFERENCE);
      custumupload_backlog = (EditTextPreference) findPreference(CUSTOMUPLOAD_BACKLOG);

      setEnabledCustomValues(precision.getValue());
      precision.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
      {
         @Override
         public boolean onPreferenceChange(Preference preference, Object newValue)
         {
            setEnabledCustomValues(newValue);
            return true;
         }
      });
      implentWidth.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
      {
         @Override
         public boolean onPreferenceChange(Preference preference, Object newValue)
         {
            String fpExpr = "\\d{1,4}([,\\.]\\d+)?";
            return Pattern.matches(fpExpr, newValue.toString());
         }
      });
      streambroadcast_distance.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
      {
         @Override
         public boolean onPreferenceChange(Preference preference, Object newValue)
         {
            String fpExpr = "\\d{1,5}";
            boolean matches = Pattern.matches(fpExpr, newValue.toString());
            if (matches)
            {
               Editor editor = getPreferenceManager().getSharedPreferences().edit();
               double value = new UnitsI18n(ApplicationPreferenceActivity.this).conversionFromLocalToMeters(Integer.parseInt(newValue.toString()));
               editor.putFloat("streambroadcast_distance_meter", (float) value);
               editor.commit();
            }
            return matches;
         }
      });
      custumupload_backlog.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
      {
         @Override
         public boolean onPreferenceChange(Preference preference, Object newValue)
         {
            String fpExpr = "\\d{1,3}";
            return Pattern.matches(fpExpr, newValue.toString());
         }
      });
   }

   private void setEnabledCustomValues(Object newValue)
   {
      boolean customPresicion = Integer.toString(Constants.LOGGING_CUSTOM).equals(newValue);
      time.setEnabled(customPresicion);
      distance.setEnabled(customPresicion);
   }
}
