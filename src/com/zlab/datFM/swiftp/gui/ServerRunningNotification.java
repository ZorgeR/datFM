/*
Copyright 2011-2013 Pieter Pareit

This file is part of SwiFTP.

SwiFTP is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

SwiFTP is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.zlab.datFM.swiftp.gui;

import java.net.InetAddress;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import com.zlab.datFM.datFM;
import com.zlab.datFM.swiftp.FtpServerService;
import com.zlab.datFM.R;

public class ServerRunningNotification extends BroadcastReceiver {
    private static final String TAG = ServerRunningNotification.class.getSimpleName();

    private final int NOTIFICATIONID = 7890;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive broadcast: " + intent.getAction());
        if (intent.getAction().equals(FtpServerService.ACTION_STARTED)) {
            setupNotification(context);
        } else if (intent.getAction().equals(FtpServerService.ACTION_STOPPED)) {
            clearNotification(context);
        }
    }

    private void setupNotification(Context context) {
        Log.d(TAG, "Setting up the notification");
        // Get NotificationManager reference
        String ns = Context.NOTIFICATION_SERVICE;

        NotificationManager nm = (NotificationManager) context.getSystemService(ns);

		// get ip address
        InetAddress address = FtpServerService.getLocalInetAddress();
        if (address == null) {
            Log.w(TAG, "Unable to retreive the local ip address");
            return;
        }
        String iptext = "ftp://" + address.getHostAddress() + ":"
			+ FtpServerService.getPort() + "/";
		
        // Instantiate a Notification
        //int icon = datFM.datFM_state.getResources().getIdentifier("ext_network"+"_"+ datFM.pref_theme_icons, "drawable", "com.zlab.datFM");
        int icon = R.drawable.ftp_network;

        CharSequence tickerText = String.format(context.getString(R.string.notif_server_starting),
				iptext);
        long when = System.currentTimeMillis();

//        Notification notification = new Notification(icon, tickerText, when);

        // Define Notification's message and Intent
        CharSequence contentTitle = context.getString(R.string.notif_title);
        CharSequence contentText = String.format(context.getString(R.string.notif_text),
                iptext);

        Intent notificationIntent = new Intent(context, ServerPreferenceActivity.class);
        //notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
//                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
//                notificationIntent, 0);
//        notification
//                .setLatestEventInfo(context, contentTitle, contentText, contentIntent);
//        notification.flags |= Notification.FLAG_ONGOING_EVENT;

        Notification noti = null;

         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
             noti = new Notification.Builder(context)
                     .setTicker(tickerText)
                     .setContentTitle(contentTitle)
                     .setContentText(contentText)
                     .setSmallIcon(icon)
                     .build();
        } else {
             PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);

             noti = new NotificationCompat.Builder(context).setAutoCancel(true)
                     .setDefaults(Notification.DEFAULT_ALL)
                     .setWhen(System.currentTimeMillis())
                     .setSmallIcon(icon)
                     .setTicker(tickerText)
                     .setContentTitle(contentTitle)
                     .setContentText(contentText)
                     .setDefaults(Notification.DEFAULT_LIGHTS| Notification.DEFAULT_SOUND)
                     .setContentIntent(contentIntent)
                     .setContentInfo("Info").build();
         }

        // Pass Notification to NotificationManager
        nm.notify(NOTIFICATIONID, noti);

        Log.d(TAG, "Notification setup done");
    }

    private void clearNotification(Context context) {
        Log.d(TAG, "Clearing the notifications");
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nm = (NotificationManager) context.getSystemService(ns);
        nm.cancelAll();
        Log.d(TAG, "Cleared notification");
    }
}
