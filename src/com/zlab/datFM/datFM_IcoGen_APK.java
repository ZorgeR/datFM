package com.zlab.datFM;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

public class datFM_IcoGen_APK extends AsyncTask<String, Void, Drawable> {
    public datFM_File_ListAdaptor activity;
    public datFM_IcoGen_APK(datFM_File_ListAdaptor a)
    {
        activity = a;
    }
    protected void onPreExecute() {
        super.onPreExecute();}

    @Override
    protected Drawable doInBackground(String... params) {
        String filePath = params[0];

        int id = Integer.parseInt(params[1]);
        Drawable icon=null;
        PackageInfo packageInfo = activity.getContext().getPackageManager().getPackageArchiveInfo(filePath, PackageManager.GET_ACTIVITIES);
        if(packageInfo != null) {
            ApplicationInfo appInfo = packageInfo.applicationInfo;
            if (Build.VERSION.SDK_INT >= 8) {
                appInfo.sourceDir = filePath;
                appInfo.publicSourceDir = filePath;
            }
            icon = appInfo.loadIcon(activity.getContext().getPackageManager());
        }
        if (icon==null){
            try{
            icon=activity.getContext().getResources().getDrawable(
                    activity.getContext().getResources().getIdentifier("ext_apk"+"_"+datFM.pref_theme_icons, "drawable", "com.zlab.datFM"));
            } catch (Exception e){
                Log.e("APK PARSE ERROR: ", e.getMessage());
            }
        }
            //icon=activity.getContext().getResources().getDrawable(R.drawable.ext_apk);}

        datFM.cache_icons[id]=icon;
        return icon;
    }
    protected void onPostExecute(Drawable result) {
        super.onPostExecute(result);
        /** **/
        if(!datFM.scroll){
        activity.notifyDataSetChanged();}
    }
}
