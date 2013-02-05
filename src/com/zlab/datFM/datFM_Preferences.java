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
        addPreferencesFromResource(R.xml.datfm_preferences);

        int currentApiVersion = android.os.Build.VERSION.SDK_INT;

        if (currentApiVersion >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
            final SwitchPreference pref_root = (SwitchPreference) getPreferenceManager().findPreference("pref_root");

            pref_root.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue.toString().equals("true")) {
                    } else {
                    }
                    return true;
                }
            });
        } else{
            final CheckBoxPreference pref_root = (CheckBoxPreference) getPreferenceManager().findPreference("pref_root");
            pref_root.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue.toString().equals("true")) {
                    } else {
                    }
                    return true;
                }
            });
        }



    }
}
