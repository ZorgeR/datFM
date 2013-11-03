package com.zlab.datFM.FilePicker;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;
import com.zlab.datFM.R;
import com.zlab.datFM.datFM;
import com.zlab.datFM.datFM_IO_Fetch;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class datFM_FilePicker extends ListActivity {

    private File currentDir;
    private int panel_ID;
    private datFM_FilePickerAdaptor adapter;
    @Override
    public void onCreate(Bundle savedInstanceState) {

        if (datFM.pref_theme.equals("Dark Fullscreen")){
            if(datFM.currentApiVersion < Build.VERSION_CODES.HONEYCOMB){
                this.setTheme(android.R.style.Theme_Dialog);
            } else {
                this.setTheme(android.R.style.Theme_Holo_Dialog_NoActionBar);
            }
        } else {
            if(datFM.currentApiVersion < Build.VERSION_CODES.HONEYCOMB){
                this.setTheme(android.R.style.Theme_Dialog);
            } else {
                this.setTheme(android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
            }
        }

        super.onCreate(savedInstanceState);
        panel_ID = Integer.parseInt(getIntent().getStringExtra("panel_ID"));

        currentDir = new File("/sdcard/");
        fill(currentDir);
    }
    private void fill(File f)
    {
        File[]dirs = f.listFiles();
        this.setTitle("Current Dir: "+f.getName());
        List<datFM_FilePickerHolder> dir = new ArrayList<datFM_FilePickerHolder>();
        List<datFM_FilePickerHolder>fls = new ArrayList<datFM_FilePickerHolder>();
        try{
            for(File ff: dirs)
            {
                if(ff.isDirectory())
                    dir.add(new datFM_FilePickerHolder(ff.getName(),"Folder",ff.getAbsolutePath()));
                else
                {
                    fls.add(new datFM_FilePickerHolder(ff.getName(),formatSize(ff.length()),ff.getAbsolutePath()));
                }
            }
        }catch(Exception e)
        {

        }
        Collections.sort(dir);
        Collections.sort(fls);
        dir.addAll(fls);
        if(!f.getName().equalsIgnoreCase("sdcard"))
            dir.add(0,new datFM_FilePickerHolder("..","Parent Directory",f.getParent()));
        adapter = new datFM_FilePickerAdaptor(datFM_FilePicker.this, R.layout.datfm_list,dir);
        this.setListAdapter(adapter);
    }
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        datFM_FilePickerHolder o = adapter.getItem(position);
        if(o.getData().equalsIgnoreCase("folder")||o.getData().equalsIgnoreCase("parent directory")){
            currentDir = new File(o.getPath());
            fill(currentDir);
        }
        else
        {
            onFileClick(o);
        }
    }
    private void onFileClick(datFM_FilePickerHolder o)
    {
        int dotPos = o.getName().toString().lastIndexOf(".")+1;
        String ext = o.getName().toString().substring(dotPos);

        if (    ext.toUpperCase().equals("PEM") ||
                ext.toUpperCase().equals("PPK") ||
                ext.toUpperCase().equals("CER") ||
                ext.toUpperCase().equals("PUB") ||
                ext.toUpperCase().equals("CRT")){
            Intent intent = new Intent();
            intent.putExtra("path", o.getPath());
            setResult(RESULT_OK, intent);

            datFM.sftp_pem_file[panel_ID]=o.getPath();
            datFM.pemfile.setText(o.getName());
            datFM.SFTPkeypass.setVisibility(View.VISIBLE);

            finish();
        } else {
            datFM.notify_toast("Select PEM,PPK,CER,PUB,CRT!", true);
        }
    }

    public static String formatSize(long size) {
        String suffix = " Bytes";
        BigDecimal size_comma=new BigDecimal(size);

        if (size >= 1024) {
            suffix = " KiB";
            size_comma=new BigDecimal(size/1024.00);
            size /= 1024;
            if (size >= 1024) {
                suffix = " MiB";
                size_comma=new BigDecimal(size/1024.00);
                size /= 1024;
                if (size >= 1024) {
                    suffix = " GiB";
                    size_comma=new BigDecimal(size/1024.00);
                    size /= 1024;
                }
            }
        }

        size_comma = size_comma.setScale(1, BigDecimal.ROUND_HALF_UP);
        StringBuilder resultBuffer = new StringBuilder(String.valueOf(size_comma));

        if (suffix != null) resultBuffer.append(suffix);
        return resultBuffer.toString();
    }
}
