package com.zlab.datFM.player.gallery;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Gallery;

import android.widget.LinearLayout;
import android.widget.Toast;
import com.zlab.datFM.R;
import com.zlab.datFM.datFM;
import com.zlab.datFM.datFM_IO;
import com.zlab.datFM.player.datFM_photo;

import java.io.BufferedInputStream;

/**
 * @author Artyom Kiriliyk
 * -------------------
 * Extends Android Gallery to open image onDoubleTap.
 */
public class PinchZoomGallery extends Gallery implements OnDoubleTapListener
{
	private Context c;
    private boolean HQ=false;
    private Dialog d;
    public static View mDecorView;

	public PinchZoomGallery(Context context, AttributeSet attrSet)
	{
		super(context, attrSet);
		c = context;
	}

	@Override
	public boolean onDoubleTap(MotionEvent e)
	{
        if(!HQ){openHQ();}else{closeHQ();}
		return true;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e)
	{
		return false;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e)
	{
        if(!HQ){openHQ();}else{closeHQ();}
		return true;
	}

    private void closeHQ(){
        d.dismiss();
    }

    private void openHQ(){
        final TouchImageView imageViewGallery = (TouchImageView) inflate(c,R.layout.gallery_dialog, null);

        imageViewGallery.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                /**closeHQ();**/

            }
        });

        final int position = super.getPositionForView(getSelectedView());
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inPurgeable = true;

        try
        {
            imageViewGallery.setMaxZoom(4f);
            imageViewGallery.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(c,new datFM_IO(datFM_photo.imglist.get(position),datFM.curPanel).getName(),Toast.LENGTH_LONG).show();
                }
            });

            d = new Dialog(c, android.R.style.Theme_NoTitleBar_Fullscreen);
            d.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    HQ=false;
                }
            });
            d.requestWindowFeature(Window.FEATURE_NO_TITLE);
            d.setContentView(imageViewGallery);
            d.setCancelable(true);
            d.setCanceledOnTouchOutside(true);
            d.show();

            mDecorView = d.getWindow().getDecorView();

            if(datFM.currentApiVersion>18){hideSystemUI();}
            datFM_photo.loader_start();

            new Thread() {
                @Override
                public void run() {
                    final Bitmap img = BitmapFactory.decodeStream(new datFM_IO(datFM_photo.imglist.get(position), datFM.curPanel).getInput());
                    datFM_photo.datFM_photo_state.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            imageViewGallery.setImageBitmap(img);
                            datFM_photo.adialog.dismiss();
                        }
                    });
                }
            }.start();
        }
        catch (Exception ex)
        {
            Log.e("Exception", ex.getLocalizedMessage());
        }
        HQ=true;
    }

    public static void hideSystemUI() {
        mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    public static void showSystemUI() {
        mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
}