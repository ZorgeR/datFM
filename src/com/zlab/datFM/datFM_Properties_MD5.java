package com.zlab.datFM;

import android.os.AsyncTask;
import android.view.View;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

public class datFM_Properties_MD5 extends AsyncTask<String, Void, String> {

    protected void onPreExecute() {
        datFM_Properties.prop_md5_progress.setVisibility(View.VISIBLE);
        datFM_Properties.prop_btn_calc_md5.setEnabled(false);
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... files) {
        String hex="";
        try{
            MessageDigest digest = MessageDigest.getInstance("MD5");
            String f = files[0];
            InputStream is = new BufferedInputStream(new datFM_IO(f,datFM.curPanel).getInput());
            byte[] buffer = new byte[8192];
            int read = 0;
            try {
                while( (read = is.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                }
                byte[] md5sum = digest.digest();
                BigInteger bigInt = new BigInteger(1, md5sum);
                String output = bigInt.toString(16);
                hex = output;
            }
            catch(IOException e) {
                throw new RuntimeException("Unable to process file for MD5", e);
            }
            finally {
                try {
                    is.close();
                }
                catch(IOException e) {
                    throw new RuntimeException("Unable to close input stream for MD5 calculation", e);
                }
            }
        } catch (Exception e){}

        return hex;
    }

    protected void onPostExecute(String result) {
        datFM_Properties.prop_md5sum.setText(result);
        if(!datFM_Properties.prop_md5sum_check.getText().toString().equals("")){
            datFM_Properties.prop_icon_md5_check.setVisibility(View.VISIBLE);
            if(datFM_Properties.prop_md5sum.getText().toString().equals(result)){
                datFM_Properties.prop_icon_md5_check.setImageResource(android.R.drawable.checkbox_on_background);
            } else {
                datFM_Properties.prop_icon_md5_check.setImageResource(android.R.drawable.presence_busy);
            }
        }
        datFM_Properties.prop_md5_progress.setVisibility(View.GONE);
        datFM_Properties.prop_icon_md5_check.setVisibility(View.VISIBLE);
        super.onPostExecute(result);
    }

}

