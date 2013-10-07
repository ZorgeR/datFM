package com.zlab.datFM;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.util.Log;
import android.widget.Toast;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

import java.io.File;
import java.io.IOException;

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

        getPreferenceManager()
                .findPreference("pref_do_backup")
                .setOnPreferenceClickListener(
                        new Preference.OnPreferenceClickListener() {
                            public boolean onPreferenceClick(Preference preference) {
                                File settings_file = new File("/data/data/com.zlab.datFM/shared_prefs/com.zlab.datFM_preferences.xml");
                                File backup_file = new File(Environment.getExternalStorageDirectory().getPath()+"/Android/data/datFM/com.zlab.datFM_preferences.xml");

                                if(backup_file.exists()){
                                    try {
                                        new datFM_IO(backup_file.getPath(),0).delete();
                                        new datFM_IO(backup_file.getParentFile().getPath()+"/files",0).delete();
                                    } catch (Exception e) {
                                        Log.e("ERROR:",e.getMessage());
                                    }
                                } else {
                                    if(!backup_file.getParentFile().exists()){
                                        backup_file.getParentFile().mkdir();
                                    }
                                }

                                try {
                                    new datFM_IO(settings_file.getPath(),0).copy(backup_file.getPath());
                                    new datFM_IO(backup_file.getParentFile()+"/files",0).mkdir();
                                    new datFM_IO(settings_file.getParentFile().getParentFile().getPath()+"/files",0).copy(backup_file.getParentFile().getPath()+"/files");
                                } catch (Exception e) {
                                    Log.e("ERROR:",e.getMessage());
                                }

                                if(backup_file.exists()){
                                    Toast.makeText(datFM.datFM_context,getResources().getString(R.string.notify_backup_done),Toast.LENGTH_SHORT).show();
                                }else{
                                    Toast.makeText(datFM.datFM_context,getResources().getString(R.string.notify_backup_error),Toast.LENGTH_SHORT).show();
                                }
                                return true;
                            }
                        });

        getPreferenceManager()
                .findPreference("pref_restore_backup")
                .setOnPreferenceClickListener(
                        new Preference.OnPreferenceClickListener() {
                            public boolean onPreferenceClick(Preference preference) {
                                File settings_file = new File("/data/data/com.zlab.datFM/shared_prefs/com.zlab.datFM_preferences.xml");
                                File backup_file = new File(Environment.getExternalStorageDirectory().getPath()+"/Android/data/datFM/com.zlab.datFM_preferences.xml");

                                if(!backup_file.exists()){
                                    Toast.makeText(datFM.datFM_context,getResources().getString(R.string.notify_backup_notfound),Toast.LENGTH_SHORT).show();
                                } else {
                                    try {
                                        new datFM_IO(backup_file.getPath(),0).copy(settings_file.getPath());
                                        new datFM_IO(backup_file.getParentFile().getPath()+"/files",0).copy(settings_file.getParentFile().getParentFile().getPath()+"/files");
                                    } catch (Exception e) {
                                        Log.e("ERROR:",e.getMessage());
                                    }
                                    Toast.makeText(datFM.datFM_context,getResources().getString(R.string.notify_backup_restore_done),Toast.LENGTH_LONG).show();
                                }
                                return true;
                            }
                        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}
