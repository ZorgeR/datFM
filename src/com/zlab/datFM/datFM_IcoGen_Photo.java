package com.zlab.datFM;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;

import java.io.InputStream;

public class datFM_IcoGen_Photo extends AsyncTask<String, Void, Drawable> {
Bitmap imageBitmap;
public datFM_File_ListAdaptor activity;
public datFM_IcoGen_Photo(datFM_File_ListAdaptor a)
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
                    Drawable icon=null;

                try
                {
                    final int THUMBNAIL_SIZE = datFM.icons_size;

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 10;

                    /** LOCAL MODE **/
                    //imageBitmap = BitmapFactory.decodeFile(filePath, options);
                    /** MULTI MODE **/
                    InputStream io = new datFM_IO(filePath,activity.PanelID).getInput();
                    imageBitmap = BitmapFactory.decodeStream(io, null, options);
                    imageBitmap = ThumbnailUtils.extractThumbnail(imageBitmap,THUMBNAIL_SIZE,THUMBNAIL_SIZE);
                    io.close();
                    /** NETWORK MODE **/
                    //imageBitmap = BitmapFactory.decodeStream(new datFM_IO(filePath).getInput(), null, options);
                    //imageBitmap = Bitmap.createScaledBitmap(imageBitmap, THUMBNAIL_SIZE, THUMBNAIL_SIZE, false);

                    icon = new BitmapDrawable(imageBitmap);
                    //if (prop_icon_file==null){prop_icon_file=activity.getContext().getResources().getDrawable(R.drawable.ext_pdf);}
                } catch(Exception ex) {
                }

                if (icon==null){icon=activity.getContext().getResources().getDrawable(
                                     activity.getContext().getResources().getIdentifier("ext_photo"+"_"+datFM.pref_theme_icons, "drawable", "com.zlab.datFM"));
                                //icon=activity.getContext().getResources().getDrawable(R.drawable.ext_unknown_human_o2);
                }
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