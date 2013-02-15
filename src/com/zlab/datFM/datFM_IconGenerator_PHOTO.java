package com.zlab.datFM;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;

public class datFM_IconGenerator_PHOTO extends AsyncTask<String, Void, Drawable> {
Bitmap imageBitmap;
public datFM_Adaptor activity;
public datFM_IconGenerator_PHOTO(datFM_Adaptor a)
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
                    final int THUMBNAIL_SIZE = datFM.icons_size;

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 10;

                    /** LOCAL MODE **/
                    //imageBitmap = BitmapFactory.decodeFile(filePath, options);
                    /** MULTI MODE **/
                    imageBitmap = BitmapFactory.decodeStream(new datFM_IO(filePath).getInput(), null, options);
                    imageBitmap = ThumbnailUtils.extractThumbnail(imageBitmap,THUMBNAIL_SIZE,THUMBNAIL_SIZE);

                    /** NETWORK MODE **/
                    //imageBitmap = BitmapFactory.decodeStream(new datFM_IO(filePath).getInput(), null, options);
                    //imageBitmap = Bitmap.createScaledBitmap(imageBitmap, THUMBNAIL_SIZE, THUMBNAIL_SIZE, false);

                    icon = new BitmapDrawable(imageBitmap);
                    //if (prop_icon_file==null){prop_icon_file=activity.getContext().getResources().getDrawable(R.drawable.ext_pdf);}
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
        activity.notifyDataSetChanged();}
}


}