package com.zlab.datFM;

import android.os.AsyncTask;
import android.view.View;

import java.math.BigDecimal;
import java.util.ArrayList;

public class datFM_FileProperties_Builder extends AsyncTask<ArrayList<datFM_FileInfo>, Void, String> {

    Long holder;
    ArrayList<datFM_FileInfo> paths;

    protected void onPreExecute() {
        datFM_FileProperties.prop_size_progress.setVisibility(View.VISIBLE);
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(ArrayList<datFM_FileInfo>... list) {
        paths = list[0];

        for (int i=0;i<paths.size();i++){
            if(paths.get(i).getType().equals("dir")){
                get_directory(paths.get(i).getPath());
            } else {
                get_file(paths.get(i).getPath());
            }
        }
        return null;
    }

    protected void onPostExecute(String result) {
        datFM_FileProperties.prop_size_progress.setVisibility(View.GONE);
        if(holder!=null){
        BigDecimal size_d = new BigDecimal(holder/1024.00/1024.00);
        BigDecimal file_size_d = size_d.setScale(3, BigDecimal.ROUND_HALF_UP);
        datFM_FileProperties.prop_size.setText(String.valueOf(file_size_d));
        } else {
        datFM_FileProperties.prop_size.setText("0.00");
        }
        super.onPostExecute(result);
    }

    void get_directory(String dir){
        if (new datFM_IO(dir, datFM.curPanel).is_dir()){
            String [] dir_list = new datFM_IO(dir, datFM.curPanel).get_dir_list();
            if(dir_list!=null)
            for(String f : dir_list){
                get_directory(f);
            }
        } else {
            get_file(dir);
        }
    }
    void get_file(String file){
        if(holder==null){
            holder = new datFM_IO(file,datFM.curPanel).getFileSize();
        } else {
            holder=holder+new datFM_IO(file,datFM.curPanel).getFileSize();
        }
        //onProgressUpdate(holder);
    }

}
