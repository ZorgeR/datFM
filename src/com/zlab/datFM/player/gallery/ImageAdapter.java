package com.zlab.datFM.player.gallery;

import java.io.BufferedInputStream;
import java.io.IOException;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import com.zlab.datFM.R;
import com.zlab.datFM.datFM;
import com.zlab.datFM.datFM_IO;
import com.zlab.datFM.player.datFM_photo;

/**
 * @author Artyom Kiriliyk
 * -------------------
 * Custom adapter
 */
public class ImageAdapter extends BaseAdapter
{
	private Context context;
	private AssetManager assetManager;
	private String[] fileList;
    private Bitmap imageBitmap;
    private boolean ONLY_ONCE=true;

	/** Simple Constructor saving the 'parent' context. */
	public ImageAdapter(Context c)
	{
		this.context = c;
		assetManager = context.getAssets();

		try
		{
			//fileList = assetManager.list("Images");
            fileList = new String[datFM_photo.imglist.size()];
            datFM_photo.imglist.toArray(fileList);
		}
		catch (Exception e)
		{
			Log.e("Exception", e.getLocalizedMessage());
		}
	}

	/** Returns the amount of images we have defined. */
	@Override
	public int getCount()
	{
		return fileList.length;
	}

	@Override
	public Object getItem(int position)
	{
		return position;
	}

	/* Use the array-Positions as unique IDs */
	@Override
	public long getItemId(int position)
	{
		return position;
	}

	/**
	 * Returns a new ImageView to be displayed, depending on the position passed.
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		final ImageView i = new ImageView(context);

            /*
			Bitmap bitmap = BitmapFactory.decodeStream(context.getAssets().open(
				"Images/" + position + ".jpg"));
			i.setImageBitmap(bitmap);   */
        ///**
            //if(i.isShown()){

        if(datFM_photo.imageBitmap[position]==null){
            i.setImageDrawable(datFM.datFM_context.getResources().getDrawable(android.R.drawable.ic_popup_sync));
            final int pos = position;
            new Thread() {
                @Override
                public void run() {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 10;
                        imageBitmap = BitmapFactory.decodeStream(new BufferedInputStream(new datFM_IO(fileList[pos], datFM.curPanel).getInput()), null, options);
                        imageBitmap = ThumbnailUtils.extractThumbnail(imageBitmap, 320, 240);
                        datFM_photo.imageBitmap[pos]=imageBitmap;

                    datFM_photo.datFM_photo_state.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            datFM_photo.imAdapt.notifyDataSetChanged();
                            if(ONLY_ONCE){datFM_photo.adialog.dismiss();ONLY_ONCE=false;}
                        }
                    });
                }
            }.start();
        } else {
            i.setImageBitmap(datFM_photo.imageBitmap[position]);
        }
                 /*
        if(datFM_photo.imageBitmap[position]==null){
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 10;
                imageBitmap = BitmapFactory.decodeStream(new BufferedInputStream(new datFM_IO(fileList[position], datFM.curPanel).getInput()), null, options);
                imageBitmap = ThumbnailUtils.extractThumbnail(imageBitmap, 320, 240);
                datFM_photo.imageBitmap[position]=imageBitmap;
        }             */
         //       i.setImageBitmap(imageBitmap);
        //} else {
        //}
            //}
         //*/


            //i.setImageBitmap(BitmapFactory.decodeStream(new datFM_IO(fileList[position],datFM.curPanel).getInput()));



        /*
        final int pos=position;
        new Thread() {
            @Override
            public void run() {
                final Bitmap bmp = BitmapFactory.decodeStream(new datFM_IO(fileList[pos],datFM.curPanel).getInput());
                i.setImageBitmap(bmp);
                datFM_photo.adialog.dismiss();
            }
        }.start();
                    */

		i.setPadding(1, 1, 1, 1);
		/* Image should be scaled as width/height are set. */
		i.setScaleType(ImageView.ScaleType.FIT_CENTER);
		/* Set the Width/Height of the ImageView. */
		i.setLayoutParams(new Gallery.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

		return i;
	}
}