package com.zlab.datFM.player;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;
import com.zlab.datFM.R;
import com.zlab.datFM.datFM;
import com.zlab.datFM.datFM_IO;
import com.zlab.datFM.player.gallery.ImageAdapter;
import com.zlab.datFM.player.gallery.PinchZoomGallery;
import com.zlab.datFM.stream.Streamer;

import java.util.ArrayList;

public class datFM_photo extends Activity {

    static public ArrayList<String> imglist;
    public static Activity datFM_photo_context;
    static public Bitmap imageBitmapLQ[];
    static public Bitmap imageBitmapHQ[];

    public static AlertDialog adialog;
    public static String ImageURL;
    public static PinchZoomGallery gallery;
    public static ImageAdapter imAdapt;
    public static datFM_photo datFM_photo_state;
    public static boolean gallery_scrolled=false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        datFM_photo_context = this;
        datFM_photo_state = ((datFM_photo) datFM_photo_context);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.datfm_player_photo_gallery);
        ImageURL = getIntent().getExtras().getString("MediaURL");
        gallery = (PinchZoomGallery) findViewById(R.id.gallery);
        gallery.setSpacing(15);

        gallery.setCallbackDuringFling(false);

        gallery.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {
                gallery_scrolled=false;
                datFM.datFM_state.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(datFM.curPanel==0){
                            for (int i=0;i<datFM.listLeft.getCount();i++){
                                if (new datFM_IO(imglist.get(position),datFM.curPanel).getName().equals(datFM.adapterLeft.getItem(i).getName())){
                                    datFM.listLeft.setSelection(i);
                                }}
                        } else {
                            for (int i=0;i<datFM.listRight.getCount();i++){
                                if (new datFM_IO(imglist.get(position),datFM.curPanel).getName().equals(datFM.adapterRight.getItem(i).getName())){
                                    datFM.listRight.setSelection(i);}}
                        }
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        loader_start();
        startPlayer();
    }

    public static void loader_start(){
        AlertDialog.Builder builder = new AlertDialog.Builder(datFM_photo.datFM_photo_context);
        ProgressBar pbar = new ProgressBar(datFM_photo.datFM_photo_context);
        builder.setView(pbar);
        adialog = builder.create();
        adialog.show();
    }


    void startPlayer(){
        /** iView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);     **/

        new Thread() {
            @Override
            public void run() {
                String parentdir = new datFM_IO(ImageURL,datFM.curPanel).getParent()[0];
                String[] flist = new datFM_IO(parentdir,datFM.curPanel).listFiles();

                imglist = new ArrayList<String>();
                for(String ff : flist){
                    if(Streamer.mediaType(ff).equals("image/*")){
                        imglist.add(ff);
                    }
                }

                imageBitmapLQ = new Bitmap[imglist.size()];
                imageBitmapHQ = new Bitmap[imglist.size()];

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        datFM_photo.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imAdapt=new ImageAdapter(datFM_photo.this);
                                gallery.setAdapter(imAdapt);
                                for(int i=0;i<imglist.size();i++){
                                    if(imglist.get(i).equals(ImageURL)){
                                        gallery.setSelection(i);
                                    }
                                }
                            }
                        });
                    }
                });
                }
            }.start();
    }
    public Bitmap decodeSampleBitmapFromFile(String filePath, int reqWidth,int reqHeight) {

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
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
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
