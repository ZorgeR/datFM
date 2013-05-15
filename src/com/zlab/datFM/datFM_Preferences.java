package com.zlab.datFM;

import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;

public class datFM_Preferences extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        //noinspection deprecation
        addPreferencesFromResource(R.xml.datfm_preferences);


        int currentApiVersion = android.os.Build.VERSION.SDK_INT;

        if (currentApiVersion >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){

            final SwitchPreference pref_show_single_panel = (SwitchPreference) getPreferenceManager().findPreference("pref_show_single_panel");
            final SwitchPreference pref_show_single_navbar = (SwitchPreference) getPreferenceManager().findPreference("pref_show_single_navbar");

            pref_show_single_panel.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue.toString().equals("true")) {
                        pref_show_single_navbar.setChecked(true);
                    } else {
                        // do nothing
                    }
                    return true;
                }
            });
        } else{
            final CheckBoxPreference pref_show_single_panel = (CheckBoxPreference) getPreferenceManager().findPreference("pref_show_single_panel");
            final CheckBoxPreference pref_show_single_navbar = (CheckBoxPreference) getPreferenceManager().findPreference("pref_show_single_navbar");

            pref_show_single_panel.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue.toString().equals("true")) {
                        pref_show_single_navbar.setChecked(true);
                    } else {
                        // do nothing
                    }
                    return true;
                }
            });
        }

    }
}
