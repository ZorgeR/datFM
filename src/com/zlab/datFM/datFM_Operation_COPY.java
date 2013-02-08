package com.zlab.datFM;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;

import java.io.*;
import java.net.MalformedURLException;

public class datFM_Operation_COPY extends AsyncTask<String, Void, Boolean> {

    int srcPannelID,competPannelID;
    String destDir, operation;
    public datFM activity;
    static AlertDialog dialog_operation;
    public NtlmPasswordAuthentication auth;
    int count;
    long cur_f;

    ProgressBar progr_current,progr_overal;

    public datFM_Operation_COPY(datFM a)
    {
        activity = a;
    }

    protected void onPreExecute() {
        super.onPreExecute();

        AlertDialog.Builder progr_dialog = new AlertDialog.Builder(activity);
        progr_dialog.setTitle("operation");
        LayoutInflater inflater = activity.getLayoutInflater();
        View layer = inflater.inflate(R.layout.datfm_operation,null);
        //final EditText textNewFolderName = (EditText) layer.findViewById(R.id.textNewFolderName);
        progr_current = (ProgressBar) layer.findViewById(R.id.dialog_operation_progress_current);
        progr_overal = (ProgressBar) layer.findViewById(R.id.dialog_operation_progress_overall);
        progr_overal.setMax(datFM.sel);
        progr_current.setMax(100);
        progr_dialog.setView(layer);
        dialog_operation = progr_dialog.create();
        dialog_operation.show();

    }

    protected void onProgressUpdate(Integer c) {
        progr_overal.setProgress(c);
    }

    @Override
    protected Boolean doInBackground(String... params) {
        /** params[0] = copy,move,delete
         *  params[1] = src
         *  params[2] = dst
         *  params[5] = srcPannelID
         *  params[6] = competPannelID
         **/
        count=0;

        operation       = params[0];
        destDir         = params[2];
        srcPannelID     = Integer.parseInt(params[5]);
        competPannelID  = Integer.parseInt(params[6]);

        dialog_operation.setTitle(datFM.datf_context.getResources().getString(R.string.ui_dialog_title_copy));

            for (int i=1;i<activity.selected.length;i++){
                if (activity.selected[i]){
                    datFM_FileInformation from = activity.adapter.getItem(i);
                    cur_f = from.getSize();
                    boolean success = copy_protocol(from.getPath(),destDir+"/"+from.getName());
                    if (success) {count++;onProgressUpdate(count);}
                }
            }
        return true;
    }

    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        activity.update_tab(count,operation,destDir,4);
        if(dialog_operation.isShowing()){dialog_operation.dismiss();}
    }

    private boolean copy_protocol(String src, String dest){
        boolean success;

        try {
            if(datFM.protocols[0].equals("local") && datFM.protocols[1].equals("local")){
                return root_copy(src, dest);
            } else {
                InputStream in;
                OutputStream out;

                if (datFM.protocols[srcPannelID].equals("local")){
                    in = new BufferedInputStream(new FileInputStream(src));
                } else if (datFM.protocols[srcPannelID].equals("smb")){
                    in = new BufferedInputStream(new SmbFileInputStream(smbauth(src)));
                } else {
                    return false;
                }

                if(datFM.protocols[competPannelID].equals("local")){
                    out = new BufferedOutputStream(new FileOutputStream(dest));
                } else if (datFM.protocols[competPannelID].equals("smb")){
                    out = new BufferedOutputStream(new SmbFileOutputStream(smbauth(dest)));
                } else {
                    return false;
                }

                success = IO_Stream_Worker(in, out);
            }
        } catch (Exception e) {
            e.printStackTrace();
            success=false;
        }
        return success;
    }
    private boolean IO_Stream_Worker(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[1024];
        int len;

        long one_percent = cur_f/100;
        long cnt=0;
        int cur_file_progress=0;
        while ((len = in.read(buf)) > 0){
            out.write(buf, 0, len);
            cnt=cnt+1024;
            if(cnt>one_percent){
                cur_file_progress=cur_file_progress+1;
                progr_current.setProgress(cur_file_progress);
                cnt=0;
            }
        }
        in.close();
        out.close();
        return true;
    }

    private boolean root_copy(String srcf, String destr) throws IOException{
        File src = new File(srcf);
        File dst = new File(destr);
        try {copy(srcf, destr);} catch (IOException e){/*e.printStackTrace();*/}

        if (!dst.exists()){
            if (src.isDirectory()){
                String[] commands = {"cp -rf "+"\""+src.getPath()+"\""+" "+"\""+dst.getPath()+"\"\n"};
                RunAsRoot(commands);
            } else {
                String[] commands = {"cp -f "+"\""+src.getPath()+"\""+" "+"\""+dst.getPath()+"\"\n"};
                RunAsRoot(commands);
            }
        }
        return dst.exists();
    }
    private boolean copy(String srcf, String destr) throws IOException {
        File src = new File(srcf);
        File dst = new File(destr);

        if (src.isDirectory()) {
            dst.mkdir();
            if (!src.exists() && !dst.mkdirs()) {
                throw new IOException("Cannot create dir " + dst.getAbsolutePath());}

            File[] children = src.listFiles();
            for (File ff : children) {
                copy(ff.getPath(),dst.getPath()+"/"+ff.getName());}
        } else {
            File directory = dst.getParentFile();
            if (directory != null && !directory.exists() && !directory.mkdirs()) {
                throw new IOException("Cannot create dir " + directory.getAbsolutePath());}

            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dst);
            IO_Stream_Worker(in, out);
        }

        return dst.exists();
    }

    private void RunAsRoot(String[] cmds){
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            for (String tmpCmd : cmds) {
                byte[] buf = tmpCmd.getBytes("UTF-8");
                os.write(buf,0,buf.length);
                os.writeBytes("echo \n");
            }
            os.writeBytes("exit\n");
            os.flush();
            try {
                p.waitFor();
                if (p.exitValue() != 255) {//("root");
                } else {/*("not root");*/}
            } catch (InterruptedException e) {/*e.printStackTrace();*/}
        } catch (IOException e) {/*e.printStackTrace();*/}
    }
    private SmbFile smbauth(String path) throws MalformedURLException {
        String user, pass;
        user = "zorg";
        pass = "crt3CRT";
        auth = new NtlmPasswordAuthentication("ZORGHOME", user, pass);
        SmbFile f = new SmbFile(path,auth);
        //---------START SMB WORKS-------------------------
        return f;
    }
}
