package com.zlab.datFM;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

import java.math.BigDecimal;
import java.util.ArrayList;

public class datFM_Properties_SizeCalc extends AsyncTask<ArrayList<datFM_File>, Void, String> {

    Long holder;
    ArrayList<datFM_File> paths;

    protected void onPreExecute() {
        datFM_Properties.prop_size_progress.setVisibility(View.VISIBLE);
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(ArrayList<datFM_File>... list) {
        paths = list[0];

        for (datFM_File path : paths) {
            if (path.getType().equals("dir")) {
                try {
                    get_directory(path.getPath());
                } catch (Exception e) {
                    Log.e("ERR:", e.getMessage());
                }
            } else {
                get_file(path.getPath());
            }
        }
        return null;
    }

    protected void onPostExecute(String result) {
        datFM_Properties.prop_size_progress.setVisibility(View.GONE);
        if(holder!=null){
        BigDecimal size_d = new BigDecimal(holder/1024.00/1024.00);
        BigDecimal file_size_d = size_d.setScale(3, BigDecimal.ROUND_HALF_UP);
        datFM_Properties.prop_size.setText(String.valueOf(file_size_d));
        } else {
        datFM_Properties.prop_size.setText("0.00");
        }
        super.onPostExecute(result);
    }

    void get_directory(String dir) throws JSchException, SftpException {
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
