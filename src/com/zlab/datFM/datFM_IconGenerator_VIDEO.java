package com.zlab.datFM;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;

public class datFM_IconGenerator_VIDEO extends AsyncTask<String, Void, Drawable> {
    Bitmap videoBitmap;
    public datFM_Adaptor activity;
    public datFM_IconGenerator_VIDEO(datFM_Adaptor a)
    {
        activity = a;
    }
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Drawable doInBackground(String... params) {
        String filePath = params[0];
        int id = Integer.parseInt(params[1]);
        Drawable icon;

        try
        {
            final int THUMBNAIL_SIZE = 64;

            videoBitmap = ThumbnailUtils.createVideoThumbnail(filePath, MediaStore.Video.Thumbnails.MICRO_KIND);
            videoBitmap = Bitmap.createScaledBitmap(videoBitmap, THUMBNAIL_SIZE, THUMBNAIL_SIZE, false);
            icon = new BitmapDrawable(videoBitmap);

            //if (prop_icon_file==null){prop_icon_file=activity.getContext().getResources().getDrawable(R.drawable.ext_pdf);}
            datFM.cache_paths[id]=filePath;
            datFM.cache_icons[id]=icon;
        }
        catch(Exception ex) {
            icon=activity.getContext().getResources().getDrawable(R.drawable.ext_unknown);
        }
        return icon;
    }

    protected void onPostExecute(Drawable result) {
        /** **/
        if(!datFM.scroll){
            activity.notifyDataSetChanged();datFM.icon_buffer_lock=true;datFM.cache_counter++;}
    }


}