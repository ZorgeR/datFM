package com.zlab.datFM.player.gallery;

import java.io.BufferedInputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
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
	//private AssetManager assetManager;
	private String[] fileList;
    //private Bitmap imageBitmap;
    private boolean ONLY_ONCE=true;

	/** Simple Constructor saving the 'parent' context. */
	public ImageAdapter(Context c)
	{
		this.context = c;
		//assetManager = context.getAssets();

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
        final int pos = position;

        if(datFM_photo.imageBitmapHQ[position]==null){
            new Thread() {
                @Override
                public void run() {

                    if(pos>1)datFM_photo.imageBitmapHQ[pos-2]=null;
                    if(pos<fileList.length-2)datFM_photo.imageBitmapHQ[pos+2]=null;

                    BitmapFactory.Options optionsHQ = new BitmapFactory.Options();
                    optionsHQ.inSampleSize = 10;

                    if(!datFM_photo.gallery_scrolled){
                        datFM_photo.gallery_scrolled=true;

                    Bitmap imgHQ = BitmapFactory.decodeStream(new BufferedInputStream(new datFM_IO(fileList[pos], datFM.curPanel).getInput()), null, optionsHQ);
                    Bitmap imgLQ = Bitmap.createScaledBitmap(imgHQ,imgHQ.getWidth()/2,imgHQ.getHeight()/2,false);

                    datFM_photo.imageBitmapHQ[pos]=imgHQ;
                    datFM_photo.imageBitmapLQ[pos]=imgLQ;
                    }
                    datFM_photo.datFM_photo_state.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            datFM_photo.imAdapt.notifyDataSetChanged();
                            if(ONLY_ONCE){datFM_photo.adialog.dismiss();ONLY_ONCE=false;
                                Toast.makeText(context,"Tap -> HQ + Pinch / Zoom",Toast.LENGTH_LONG).show();}
                        }
                    });
                }
            }.start();
        }

        if(datFM_photo.imageBitmapHQ[position]!=null){
            i.setImageBitmap(datFM_photo.imageBitmapHQ[position]);
        }else if(datFM_photo.imageBitmapLQ[position]==null){
            i.setImageDrawable(datFM.datFM_context.getResources().getDrawable(android.R.drawable.ic_popup_sync));
        } else {
            i.setImageBitmap(datFM_photo.imageBitmapLQ[position]);
        }

		i.setPadding(1, 1, 1, 1);
		/* Image should be scaled as width/height are set. */
		i.setScaleType(ImageView.ScaleType.FIT_CENTER);
		/* Set the Width/Height of the ImageView. */
		i.setLayoutParams(new Gallery.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

		return i;
	}
}