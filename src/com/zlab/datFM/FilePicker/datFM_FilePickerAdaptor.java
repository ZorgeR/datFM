package com.zlab.datFM.FilePicker;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.zlab.datFM.R;
import com.zlab.datFM.datFM;

import java.util.List;

public class datFM_FilePickerAdaptor extends ArrayAdapter<datFM_FilePickerHolder> {

    private Context c;
    private int id;
    private List<datFM_FilePickerHolder> items;

    public datFM_FilePickerAdaptor(Context context, int textViewResourceId,
                            List<datFM_FilePickerHolder> objects) {
        super(context, textViewResourceId, objects);
        c = context;
        id = textViewResourceId;
        items = objects;
    }
    public datFM_FilePickerHolder getItem(int i)
    {
        return items.get(i);
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(id, null);
        }
        final datFM_FilePickerHolder o = items.get(position);
        if (o != null) {
            TextView t1 = (TextView) v.findViewById(R.id.textFileName);
            TextView t2 = (TextView) v.findViewById(R.id.textFileDiscription);
            ImageView i1 = (ImageView) v.findViewById(R.id.imgFileIcon);

            t2.setTextSize(12);
            t1.setTypeface(datFM.font_typeface, datFM.font_style);
            t2.setTypeface(datFM.font_typeface, datFM.font_style);

            if(t1!=null)
                t1.setText(o.getName());
            if(t2!=null)
                t2.setText(o.getData());
            int dotPos = o.getName().toString().lastIndexOf(".")+1;
            String ext = o.getName().toString().substring(dotPos);

            if (o.getData().equals("Folder") || o.getData().equals("Parent Directory")){
                i1.setImageResource(R.drawable.ext_folder_quartz);
            } else if (
                    ext.toUpperCase().equals("PEM") ||
                    ext.toUpperCase().equals("PPK") ||
                    ext.toUpperCase().equals("CER") ||
                    ext.toUpperCase().equals("PUB") ||
                    ext.toUpperCase().equals("CRT") ){
                i1.setImageResource(R.drawable.datfm_filepicker_cert);
            } else {
                i1.setImageResource(R.drawable.ext_unknown_quartz);
            }
        }
        return v;
    }
}
