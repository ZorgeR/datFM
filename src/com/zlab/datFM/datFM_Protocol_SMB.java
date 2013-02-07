package com.zlab.datFM;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import jcifs.UniAddress;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbSession;

import java.math.BigDecimal;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class datFM_Protocol_SMB extends AsyncTask<String, Void, List<datFM_Information>> {

    List<datFM_Information> dir_info;
    List<datFM_Information> fls_info;
    ProgressDialog dialog_operation_smb;


    public datFM activity;
        public datFM_Protocol_SMB(datFM a){activity = a;}

        protected void onPreExecute() {
            dialog_operation_smb = new ProgressDialog(datFM.datf_context);
            dialog_operation_smb.setTitle("Working...");
            dialog_operation_smb.setMessage("fetch smb dir");
            dialog_operation_smb.setIndeterminate(false);
            dialog_operation_smb.show();
            super.onPreExecute();
        }

        protected void onProgressUpdate(Integer values) {
            // dialog_operation.setMax(values[1]);
        }

        @Override
        protected List<datFM_Information> doInBackground(String... paths) {

            dir_info = new ArrayList<datFM_Information>();
            fls_info = new ArrayList<datFM_Information>();

                boolean seccess_auth=true;
                UniAddress domain=null;
                NtlmPasswordAuthentication auth;
                String ipaddress, user, pass, url;
                //ipaddress = paths.toString();
                ipaddress = "10.32.3.32";
                url = "smb://" + ipaddress + "/Обмен/";
                user = "shamsievan";
                pass = "crt3CRT";
                auth = new NtlmPasswordAuthentication("corp", user, pass);

            try {
                domain = UniAddress.getByName(ipaddress);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                Log.e("ERR", "Uri parse error:"+e.getMessage());
            }

            try {
                SmbSession.logon(domain, auth);
            } catch (SmbException e) {
                e.printStackTrace();
                seccess_auth=false;
                Log.e("ERR", "Connection error: "+e.getMessage());
            }
                //---------START SMB WORKS-------------------------
            if(seccess_auth){
                    SmbFile dir = null;
                    try {
                        dir = new SmbFile(url, auth);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("ERR", "Connection error 2: "+e.getMessage());
                    }
                //-------END SMB WORKS---------------------

                if(dir!=null){
                    try{
                        for(SmbFile ff: dir.listFiles())
                        {
                            if(ff.isDirectory())
                                dir_info.add(new datFM_Information(ff.getName(),datFM.datf_context.getResources().getString(R.string.fileslist_directory),ff.getUncPath()));
                            else
                            {
                                BigDecimal size = new BigDecimal(ff.length()/1024.00/1024.00);
                                size = size.setScale(2, BigDecimal.ROUND_HALF_UP);
                                fls_info.add(new datFM_Information(ff.getName(),"size: "+size+" MiB",ff.getUncPath()));
                            }
                        }
                    }catch(Exception e)
                    {
                    }
                }
                Collections.sort(dir_info);
                Collections.sort(fls_info);
                dir_info.addAll(fls_info);
            }

            return dir_info;
        }

        protected void onPostExecute(List<datFM_Information> result) {
            super.onPostExecute(result);
            dialog_operation_smb.dismiss();
            datFM.datFM_state.fill_smb(dir_info,fls_info,datFM.curPanel);
            //Toast.makeText(datFM.datf_context,result.length,Toast.LENGTH_SHORT).show();
        }
}
