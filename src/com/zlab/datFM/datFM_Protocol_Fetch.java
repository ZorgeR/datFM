package com.zlab.datFM;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import jcifs.UniAddress;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbSession;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class datFM_Protocol_Fetch extends AsyncTask<String, Void, List<datFM_FileInformation>> {

    List<datFM_FileInformation> dir_info,fls_info;
    ProgressDialog dialog_operation_smb;
    String path,protocol;
    int panel_ID;
    boolean fetch_err;
    String user, pass, url, domain;
    boolean success_auth=true;
    NtlmPasswordAuthentication auth;

    public datFM activity;
    public datFM_Protocol_Fetch(datFM a){activity = a;}

    protected void onPreExecute() {
        if(!datFM.protocols[datFM.curPanel].equals("local")){
            dialog_operation_smb = new ProgressDialog(datFM.datf_context);
            dialog_operation_smb.setTitle(datFM.protocols[datFM.curPanel]);
            dialog_operation_smb.setMessage("Please wait...");
            dialog_operation_smb.setIndeterminate(false);
            dialog_operation_smb.show();}
        super.onPreExecute();
    }

    protected void onProgressUpdate(Integer values) {
        // dialog_operation.setMax(values[1]);
    }

    @Override
    protected List<datFM_FileInformation> doInBackground(String... paths) {
        path = paths[0];
        protocol = paths[1];datFM.protocol=protocol;
        panel_ID = Integer.parseInt(paths[2]);datFM.id= String.valueOf(panel_ID);

        dir_info = new ArrayList<datFM_FileInformation>();
        fls_info = new ArrayList<datFM_FileInformation>();

        datFM.curPanel = panel_ID;
        if(panel_ID==0){
            datFM.competPanel=1;
        }else{
            datFM.competPanel=0;
        }

        if (protocol.equals("local")){
            fetch_local();
        } else if (protocol.equals("smb")){
            fetch_smb();
        } else {
            Log.e("UNSUPPORT PROTOCOL:", protocol);
        }

        return dir_info;
    }

    protected void onPostExecute(List<datFM_FileInformation> result) {
        super.onPostExecute(result);
        if(!datFM.protocols[panel_ID].equals("local")){
            dialog_operation_smb.dismiss();
        }
        //if(fetch_err){datFM.notify_toast("Listing error!");}

        if(!success_auth || fetch_err){
            if(!success_auth){datFM.notify_toast("Logon error!");}
            if(fetch_err){datFM.notify_toast("Connection error.");}
            logonScreenSMB();
        } else {
            datFM.datFM_state.fill_panel(dir_info, fls_info, panel_ID);
        }
        //Toast.makeText(datFM.datf_context,result.length,Toast.LENGTH_SHORT).show();
    }

    private void fetch_smb(){
        if(path.lastIndexOf("/")!=path.length()-1){path=path+"/";}
        url = path;datFM.url=url;
        user = datFM.user;
        pass = datFM.pass;
        auth = new NtlmPasswordAuthentication(datFM.domain, user, pass);

        // ------ CHECK SMB AUTH ------------ //
        if(datFM.pref_sambalogin){

        domain=url.replace("smb://","");
        if(domain.indexOf("/")!=-1)domain=domain.substring(0, domain.indexOf("/"));

        UniAddress uniaddress = null;
        try {
            if(domain!=""){uniaddress = UniAddress.getByName(domain);}
        } catch (Exception e) {success_auth=false;}
        try {
            if(domain!=""){SmbSession.logon(uniaddress, auth);}
        } catch (Exception e) {success_auth=false;}
        } else {
            success_auth=true;
        }
        //---------START SMB WORKS-------------------------
        if(success_auth || url.equals("smb://")){
            SmbFile dir = null;
            try {
                dir = new SmbFile(url, auth);
            } catch (Exception e) {
                e.printStackTrace();
                fetch_err=true;
                Log.e("ERR", "Connection error: "+e.getMessage());
            }
            //-------END SMB WORKS---------------------

            if (panel_ID ==0){
                datFM.parent_left=dir.getParent();
                datFM.curentLeftDir=dir.getPath();
            } else {
                datFM.parent_right=dir.getParent();
                datFM.curentRightDir=dir.getPath();}

            if(dir!=null){
                try{
                    for(SmbFile ff: dir.listFiles())
                    {
                        if(ff.isDirectory()){
                            String data = datFM.datf_context.getResources().getString(R.string.fileslist_directory);
                            dir_info.add(new datFM_FileInformation(ff.getName(),ff.getPath(),0,"smb","dir",data, ff.getParent()));
                        } else {
                            BigDecimal size = new BigDecimal(ff.length()/1024.00/1024.00);
                            size = size.setScale(2, BigDecimal.ROUND_HALF_UP);
                            fls_info.add(new datFM_FileInformation(ff.getName(),ff.getPath(),ff.length(),"smb","file","size: "+size+" MiB",ff.getParent()));
                        }
                    }
                }catch(Exception e) {}
            }

            Collections.sort(dir_info);
            Collections.sort(fls_info);

            dir_info.addAll(fls_info);
            //} else {
            //logonScreenSMB();
        }
    }
    private void fetch_local(){
        File dir = new File (path);
        File[] dirs;

        if (datFM.pref_root){
            dirs = dir.listFiles();
            if(dirs==null){
                dirs = root_get_content(dir);}
        } else {
            dirs = dir.listFiles();
        }

        if (panel_ID ==0){
            datFM.parent_left=dir.getParent();
            datFM.curentLeftDir=dir.getPath();
        } else {
            datFM.parent_right=dir.getParent();
            datFM.curentRightDir=dir.getPath();}


        try{
            for(File ff: dirs)
            {
                if(ff.isDirectory()){
                    String data = datFM.datf_context.getResources().getString(R.string.fileslist_directory);
                    dir_info.add(new datFM_FileInformation(ff.getName(),ff.getPath(),0,"smb","dir",data, ff.getParent()));
                } else {
                    BigDecimal size = new BigDecimal(ff.length()/1024.00/1024.00);
                    size = size.setScale(2, BigDecimal.ROUND_HALF_UP);
                    fls_info.add(new datFM_FileInformation(ff.getName(),ff.getPath(),ff.length(),"smb","file","size: "+size+" MiB",ff.getParent()));
                }
            }
        }catch(Exception e){}
        Collections.sort(dir_info);
        Collections.sort(fls_info);

        dir_info.addAll(fls_info);
    }

    private void logonScreenSMB(){
        AlertDialog.Builder action_dialog = new AlertDialog.Builder(datFM.datFM_state);
        action_dialog.setTitle("Samba logon");
        LayoutInflater inflater = datFM.datFM_state.getLayoutInflater();
        View layer = inflater.inflate(R.layout.datfm_logon_smb,null);

        final EditText domains = (EditText) layer.findViewById(R.id.logon_domain);
        final EditText names   = (EditText) layer.findViewById(R.id.logon_name);
        final EditText passs   = (EditText) layer.findViewById(R.id.logon_pass);
        domains.setText(datFM.domain);
        names.setText(datFM.user);
        passs.setText(datFM.pass);


        action_dialog.setView(layer);
        action_dialog.setPositiveButton(datFM.datFM_state.getResources().getString(R.string.ui_dialog_btn_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        datFM.domain = domains.getText().toString();
                        datFM.user = names.getText().toString();
                        datFM.pass = passs.getText().toString();
                        new datFM_Protocol_Fetch(datFM.datFM_state).execute(datFM.url, datFM.protocol, datFM.id);
                        fetch_smb();
                    }
                });
        action_dialog.setNegativeButton(datFM.datFM_state.getResources().getString(R.string.ui_dialog_btn_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        AlertDialog AboutDialog = action_dialog.create();
        AboutDialog.show();
    }

    private File[] root_get_content(File d){
        File[] dirs;
        String out = new String();
        String command = "ls \""+d.getPath()+"\"\n";

        Process p;
        try {
            p = Runtime.getRuntime().exec(new String[]{"su", "-c", "/system/bin/sh"});
            DataOutputStream stdin = new DataOutputStream(p.getOutputStream());
            byte[] buf = command.getBytes("UTF-8");
            stdin.write(buf,0,buf.length);

            stdin.writeBytes("echo \n");
            DataInputStream stdout = new DataInputStream(p.getInputStream());
            byte[] buffer = new byte[4096];
            int read;
            while(true){
                read = stdout.read(buffer);
                out += new String(buffer, 0, read);
                if(read<4096){
                    break;
                }
                // here is where you catch the error value
                //int len = out.length();
                //char suExitValue = out.charAt(len-2);
                //Toast.makeText(getApplicationContext(), String.valueOf(suExitValue), Toast.LENGTH_SHORT).show();
                //return0or1(Integer.valueOf(suExitValue), command); // 0 or 1 Method
                // end catching exit value
            }
            stdin.writeBytes("exit\n");
            stdin.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String[] dirs_array = out.split("\n");
        dirs=new File[dirs_array.length];
        for (int i=0;i<dirs_array.length;i++){
            dirs[i]=new File (d,dirs_array[i]);
        }

        return dirs;
    }

}