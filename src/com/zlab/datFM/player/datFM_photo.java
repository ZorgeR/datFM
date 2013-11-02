package com.zlab.datFM.player;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import com.zlab.datFM.R;
import com.zlab.datFM.datFM;
import com.zlab.datFM.datFM_IO;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class datFM_photo extends Activity {

    //String ImageURL;
    ImageView iView;
    Activity mActivity;
    Bitmap imageBitmap;
    Drawable pic;
    public static String ImageURL;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.datfm_player_photo);

        ImageURL = getIntent().getExtras().getString("MediaURL");

        iView = (ImageView) findViewById(R.id.datfm_player_photo_imageview);


        iView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            }
        });

        startPlayer();
    }
    void startPlayer(){
        //iView.setImageBitmap(BitmapFactory.decodeStream(new datFM_IO(ImageURL,datFM.curPanel).getInput()));
        iView.setImageURI(Uri.parse(ImageURL));
        iView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        //iView.setImageBitmap(decodeSampleBitmapFromFile(ImageURL,64,64));
                     /*
        final int THUMBNAIL_SIZE = 64;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 10;

        /** LOCAL MODE **
        //imageBitmap = BitmapFactory.decodeFile(filePath, options);
        /** MULTI MODE **
        InputStream io = new BufferedInputStream(new datFM_IO(ImageURL,datFM.curPanel).getInput());
        imageBitmap = BitmapFactory.decodeStream(io, null, options);
        imageBitmap = ThumbnailUtils.extractThumbnail(imageBitmap, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
        try {
            io.close();
        } catch (IOException e) {
            Log.e("ERR", "MSG");
        }
        pic = new BitmapDrawable(imageBitmap);
        iView.invalidateDrawable(pic);
          */


    }
    public Bitmap decodeSampleBitmapFromFile(String filePath, int reqWidth,
                                                    int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        //BitmapFactory.decodeFile(filePath,options);
        BitmapFactory.decodeStream(new datFM_IO(filePath,datFM.curPanel).getInput(),null,options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth,reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        //return BitmapFactory.decodeFile(filePath,options);
        return BitmapFactory.decodeStream(new datFM_IO(filePath, datFM.curPanel).getInput(), null, options);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

}
