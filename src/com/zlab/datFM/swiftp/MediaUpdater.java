package com.zlab.datFM.swiftp;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;



/**
 * This media rescanner runs in the background. The rescan might
 * not happen immediately.
 * 
 *
 */
public enum MediaUpdater {
    INSTANCE;
    
    private final static String TAG = MediaUpdater.class.getSimpleName();

    // the systembroadcast to remount the media is only done after a little while (5s)
    private static Timer sTimer = new Timer();

    public static void notifyFileCreated(String path) {
        if (Defaults.do_mediascanner_notify) {
            Log.d(TAG, "Notifying others about new file: " + path);
            Context context = FtpServerApp.getAppContext();
            MediaScannerConnection.scanFile(context, new String[]{path}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String path, Uri uri) {
                        }
                    });
        }
    }

    public static void notifyFileDeleted(String path) {
        // The media mounted broadcast is very taxing on the system, so we only do this
        // if for 5 seconds there was no same request, otherwise we wait again.
        if (Defaults.do_mediascanner_notify) {
            Log.d(TAG, "Notifying others about deleted file: " + path);
            // the systembroadcast might have been requested already, cancel if so
            sTimer.cancel();
            // that timer is of no value any more, create a new one
            sTimer = new Timer();
            // and in 5s let it send the broadcast, might never hapen if before
            // that time it gets canceled by this code path
            sTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Log.d(TAG, "Sending ACTION_MEDIA_MOUNTED broadcast");
                    final Context context = FtpServerApp.getAppContext();
                    context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri
                            .parse("file://" + Environment.getExternalStorageDirectory())));
                }
            }, 5000);
        }
    }

}
