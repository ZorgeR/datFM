/*******************************************************************************
 * Copyright (c) 2012-2013 Pieter Pareit.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *     Pieter Pareit - initial API and implementation
 ******************************************************************************/

package com.zlab.datFM.swiftp.gui;

import java.io.File;
import java.net.InetAddress;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;
import com.zlab.datFM.swiftp.FtpServerApp;
import com.zlab.datFM.swiftp.FtpServerService;
import com.zlab.datFM.swiftp.Globals;
import com.zlab.datFM.R;


/**
 * This is the main activity for swiftp, it enables the user to start the server service
 * and allows the users to change the settings.
 * 
 */
public class ServerPreferenceActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {

    private static String TAG = ServerPreferenceActivity.class.getSimpleName();

    EditTextPreference mPassWordPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.swiftp_preferences);

        final SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(this);
        Resources resources = getResources();

        CheckBoxPreference running_state = (CheckBoxPreference) findPreference("running_state");
        if (FtpServerService.isRunning() == true) {
            running_state.setChecked(true);
            // Fill in the FTP server address
            InetAddress address = FtpServerService.getLocalInetAddress();
            if (address == null) {
                Log.v(TAG, "Unable to retreive wifi ip address");
                running_state.setSummary(R.string.cant_get_url);
                return;
            }
            String iptext = "ftp://" + address.getHostAddress() + ":"
                    + FtpServerService.getPort() + "/";
            String summary = resources
                    .getString(R.string.running_summary_started, iptext);
            running_state.setSummary(summary);
        } else {
            running_state.setChecked(false);
        }
        running_state.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue) {
                    startServer();
                } else {
                    stopServer();
                }
                return true;
            }
        });

        PreferenceScreen prefScreen = (PreferenceScreen) findPreference("preference_screen");
        Preference marketVersionPref = findPreference("market_version");
        marketVersionPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // start the market at our application
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("market://details?id=be.ppareit.swiftp"));
                try {
                    // this can fail if there is no market installed
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to lauch the market.");
                    e.printStackTrace();
                }
                return false;
            }
        });
        if (FtpServerApp.isFreeVersion() == false) {
            prefScreen.removePreference(marketVersionPref);
        }

        EditTextPreference username_pref = (EditTextPreference) findPreference("username");
        username_pref.setSummary(settings.getString("username",
                resources.getString(R.string.username_default)));
        username_pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String newUsername = (String) newValue;
                if (preference.getSummary().equals(newUsername))
                    return false;
                if (!newUsername.matches("[a-zA-Z0-9]+")) {
                    Toast.makeText(ServerPreferenceActivity.this,
                            R.string.username_validation_error, Toast.LENGTH_LONG).show();
                    return false;
                }
                preference.setSummary(newUsername);
                stopServer();
                return true;
            }
        });

        mPassWordPref = (EditTextPreference) findPreference("password");
        String password = resources.getString(R.string.password_default);
        password = settings.getString("password", password);
        mPassWordPref.setSummary(transformPassword(password));
        mPassWordPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String newPassword = (String) newValue;
                preference.setSummary(transformPassword(newPassword));
                stopServer();
                return true;
            }
        });

        EditTextPreference portnum_pref = (EditTextPreference) findPreference("portNum");
        portnum_pref.setSummary(settings.getString("portNum",
                resources.getString(R.string.portnumber_default)));
        portnum_pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String newPortnumString = (String) newValue;
                if (preference.getSummary().equals(newPortnumString))
                    return false;
                int portnum = 0;
                try {
                    portnum = Integer.parseInt(newPortnumString);
                } catch (Exception e) {
                }
                if (portnum <= 0 || 65535 < portnum) {
                    Toast.makeText(ServerPreferenceActivity.this,
                            R.string.port_validation_error, Toast.LENGTH_LONG).show();
                    return false;
                }
                preference.setSummary(newPortnumString);
                stopServer();
                return true;
            }
        });

        EditTextPreference chroot_pref = (EditTextPreference) findPreference("chrootDir");
        chroot_pref.setSummary(settings.getString("chrootDir",
                resources.getString(R.string.chroot_default)));
        chroot_pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String newChroot = (String) newValue;
                if (preference.getSummary().equals(newChroot))
                    return false;
                // now test the new chroot directory
                File chrootTest = new File(newChroot);
                if (!chrootTest.isDirectory() || !chrootTest.canRead())
                    return false;
                preference.setSummary(newChroot);
                stopServer();
                return true;
            }
        });

        final CheckBoxPreference wakelock_pref = (CheckBoxPreference) findPreference("stayAwake");
        wakelock_pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                stopServer();
                return true;
            }
        });

        Preference help = findPreference("help");
        help.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Log.v(TAG, "On preference help clicked");
                Context context = ServerPreferenceActivity.this;
                AlertDialog ad = new AlertDialog.Builder(context)
                        .setTitle(R.string.help_dlg_title)
                        .setMessage(R.string.help_dlg_message)
                        .setPositiveButton(R.string.ok, null).create();
                ad.show();
                Linkify.addLinks((TextView) ad.findViewById(android.R.id.message),
                        Linkify.ALL);
                return true;
            }
        });

        Preference about = findPreference("about");
        about.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog ad = new AlertDialog.Builder(ServerPreferenceActivity.this)
                        .setTitle(R.string.about_dlg_title)
                        .setMessage(R.string.about_dlg_message)
                        .setPositiveButton(getText(R.string.ok), null).create();
                ad.show();
                Linkify.addLinks((TextView) ad.findViewById(android.R.id.message),
                        Linkify.ALL);
                return true;
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (key.equals("show_password")) {
			Context context = FtpServerApp.getAppContext();
            Resources res = context.getResources();
            String password = res.getString(R.string.password_default);
            password = sp.getString("password", password);
            mPassWordPref.setSummary(transformPassword(password));
        }
    }

    private void startServer() {
		sendBroadcast(new Intent(FtpServerService.ACTION_START_FTPSERVER));
    }

    private void stopServer() {
		sendBroadcast(new Intent(FtpServerService.ACTION_STOP_FTPSERVER));
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "onResume");
        super.onResume();

        // make this class listen for preference changes
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

        Log.v(TAG, "Registering the FTP server actions");
        IntentFilter filter = new IntentFilter();
        filter.addAction(FtpServerService.ACTION_STARTED);
        filter.addAction(FtpServerService.ACTION_STOPPED);
        filter.addAction(FtpServerService.ACTION_FAILEDTOSTART);
        registerReceiver(ftpServerReceiver, filter);
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "onPause");
        super.onPause();

        Log.v(TAG, "Unregistering the FTPServer actions");
        unregisterReceiver(ftpServerReceiver);

        // unregister the listener
        SharedPreferences sprefs = getPreferenceScreen().getSharedPreferences();
        sprefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * This receiver will check FTPServer.ACTION* messages and will update the button,
     * running_state, if the server is running and will also display at what url the
     * server is running.
     */
    BroadcastReceiver ftpServerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "FTPServerService action received: " + intent.getAction());
            CheckBoxPreference running_state = (CheckBoxPreference) findPreference("running_state");
            if (intent.getAction().equals(FtpServerService.ACTION_STARTED)) {
                running_state.setChecked(true);
                // Fill in the FTP server address
                InetAddress address = FtpServerService.getLocalInetAddress();
                if (address == null) {
                    Log.v(TAG, "Unable to retreive local ip address");
                    running_state.setSummary(R.string.cant_get_url);
                    return;
                }
                String iptext = "ftp://" + address.getHostAddress() + ":"
                        + FtpServerService.getPort() + "/";
                Resources resources = getResources();
                String summary = resources.getString(R.string.running_summary_started,
                        iptext);
                running_state.setSummary(summary);
            } else if (intent.getAction().equals(FtpServerService.ACTION_STOPPED)) {
                running_state.setChecked(false);
                running_state.setSummary(R.string.running_summary_stopped);
            } else if (intent.getAction().equals(FtpServerService.ACTION_FAILEDTOSTART)) {
                running_state.setChecked(false);
                running_state.setSummary(R.string.running_summary_failed);
            }
        }
    };

    static private String transformPassword(String password) {
		Context context = FtpServerApp.getAppContext();
        SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(context);
        Resources res = context.getResources();
        boolean showPassword = res.getString(R.string.show_password_default)
				.equals("true") ? true : false;
        showPassword = sp.getBoolean("show_password", showPassword);
        if (showPassword == true)
            return password;
        else {
            StringBuilder sb = new StringBuilder(password.length());
            for (int i = 0; i < password.length(); ++i)
                sb.append('*');
            return sb.toString();
        }
    }

}
