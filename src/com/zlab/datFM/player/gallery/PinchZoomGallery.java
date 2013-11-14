package com.zlab.datFM.player.gallery;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.MotionEvent;
import android.view.Window;
import android.widget.Gallery;

import android.widget.LinearLayout;
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

	public PinchZoomGallery(Context context, AttributeSet attrSet)
	{
		super(context, attrSet);
		c = context;
	}

	@Override
	public boolean onDoubleTap(MotionEvent e)
	{
		final TouchImageView imageViewGallery = (TouchImageView) inflate(c,R.layout.gallery_dialog, null);

		final int position = super.getPositionForView(getSelectedView());
		BitmapFactory.Options o2 = new BitmapFactory.Options();
		o2.inPurgeable = true;

		try
		{
            /**
			Bitmap bitmap = BitmapFactory.decodeStream(c.getAssets().open(
				"Images/"  + position + ".jpg"));
			imageViewGallery.setImageBitmap(bitmap);
                     */

            //imageViewGallery.setImageBitmap(BitmapFactory.decodeStream(new datFM_IO(datFM_photo.imglist.get(position), datFM.curPanel).getInput()));
			imageViewGallery.setMaxZoom(4f);

            //datFM_photo.gallery.getFocusedChild().setVisibility(GONE);
            //datFM_photo.gallery.addView(imageViewGallery,0);

            Dialog d = new Dialog(c, android.R.style.Theme_NoTitleBar_Fullscreen);
            d.requestWindowFeature(Window.FEATURE_NO_TITLE);
            d.setContentView(imageViewGallery);
            d.setCancelable(true);
            d.setCanceledOnTouchOutside(true);
			d.show();


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
		return false;
	}
}