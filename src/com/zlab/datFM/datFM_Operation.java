package com.zlab.datFM;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ProgressBar;
import jcifs.UniAddress;
import jcifs.smb.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.UnknownHostException;

public class datFM_Operation extends AsyncTask<String, Void, Boolean> {

    int srcPannelID,competPannelID;
    String destDir, srcDir, operation, new_name, mask;
    public datFM activity;
    static AlertDialog dialog_operation;
    public NtlmPasswordAuthentication auth;
    int count;
    long cur_f;


    ProgressBar progr_current,progr_overal;

    public datFM_Operation(datFM a)
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
             *  params[3] = new_name
             *  params[4] = mask
             *  params[5] = srcPannelID
             *  params[6] = competPannelID
             **/

            operation = params[0];
            srcDir = params[1];
            destDir = params[2];
            srcPannelID = Integer.parseInt(params[5]);
            competPannelID = Integer.parseInt(params[6]);

            count=0;

            if (operation.equals("delete")){
                dialog_operation.setTitle(datFM.datf_context.getResources().getString(R.string.ui_dialog_title_delete));

                    for (int i=1;i<activity.selected.length;i++){
                        if (activity.selected[i]){
                            datFM_FileInformation from = activity.adapter.getItem(i);
                            boolean success = false;
                            try {
                                if (datFM.pref_root){
                                    success = root_delete(new File(from.getPath()));
                                } else {
                                    success = delete(new File(from.getPath()));
                                }
                            } catch (IOException e) {e.printStackTrace();}
                            if (success) {count++;onProgressUpdate(count);}
                        }
                    }
            }

            if (operation.equals("move")){
                dialog_operation.setTitle(datFM.datf_context.getResources().getString(R.string.ui_dialog_title_move));

                for (int i=1;i<activity.selected.length;i++){
                    if (activity.selected[i]){
                        datFM_FileInformation from = activity.adapter.getItem(i);
                        File move_file = new File (from.getPath());
                        boolean success = false;

                        if (datFM.pref_root){try {
                            success = root_move(from.getPath(),destDir+"/"+from.getName());
                            } catch (IOException e) {e.printStackTrace();}
                            if (success) {count++;onProgressUpdate(count);}
                        } else {
                            success = (move_file.renameTo(new File(destDir, from.getName())));
                            if (success) {count++;onProgressUpdate(count);
                            } else {
                                try {
                                    success = copy(move_file.getName(),destDir+from.getName());
                                } catch (IOException e) {e.printStackTrace();}
                                if (success) {
                                    count++;onProgressUpdate(count);
                                    try {
                                        delete(move_file);
                                    } catch (IOException e) {e.printStackTrace();}
                                }
                            }
                        }
                    }

                }
            }

            if (operation.equals("rename")){
                dialog_operation.setTitle(datFM.datf_context.getResources().getString(R.string.ui_dialog_title_rename));

                new_name = params[3];
                mask = params[4];
                int j=1;
                if (!mask.equals("true")){
                    for (int i=1;i<activity.selected.length;i++){
                        if (activity.selected[i]){
                            datFM_FileInformation from = activity.adapter.getItem(i);
                            File rename_file = new File (from.getPath());
                            boolean success = false;

                            if (datFM.pref_root){try {
                                success = root_move(from.getPath(),srcDir+"/"+new_name);
                                } catch (IOException e) {e.printStackTrace();}
                            } else {
                                success = (rename_file.renameTo(new File(srcDir, new_name)));
                            }
                            if (success) {count++;onProgressUpdate(count);}
                        }
                    }
                } else {
                    for (int i=1;i<activity.selected.length;i++){
                        if (activity.selected[i]){
                            datFM_FileInformation from = activity.adapter.getItem(i);
                            File rename_file = new File (from.getPath());
                            boolean success = false;

                            if (rename_file.isFile()){
                                int dotPos = from.getName().lastIndexOf(".")+1;
                                String ext = from.getName().substring(dotPos);
                                if (!ext.equals(from.getName())){
                                    if (datFM.pref_root){try {
                                        success = root_move(from.getPath(),srcDir+"/"+new_name+"_"+j+"."+ext);
                                    } catch (IOException e) {e.printStackTrace();}
                                    } else {
                                        success = rename_file.renameTo(new File(srcDir, new_name + "_" + j + "." + ext));
                                    }
                                } else {
                                    if (datFM.pref_root){try {
                                        success = root_move(from.getPath(),srcDir+"/"+new_name+"_"+j);
                                    } catch (IOException e) {e.printStackTrace();}
                                    } else {
                                        success = rename_file.renameTo(new File(srcDir, new_name+"_"+j));
                                    }
                                }
                            } else {
                                if (datFM.pref_root){try {
                                    success = root_move(from.getPath(),srcDir+"/"+new_name+"_"+j);
                                } catch (IOException e) {e.printStackTrace();}
                                } else {
                                    success = rename_file.renameTo(new File(srcDir, new_name+"_"+j));
                                }
                            }
                            if (success) {count++;j++;onProgressUpdate(count);}
                        }
                    }
                }
            }

            return true;
        }

        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if (operation.equals("copy")){
                activity.update_tab(count,operation,destDir,4);}

            if (operation.equals("delete")){
                activity.update_tab(count,operation,destDir,3);}

            if (operation.equals("move")){
                activity.update_tab(count, "move", destDir,2);}

            if (operation.equals("rename")){
                activity.update_tab(count,operation,srcDir,3);}

            dialog_operation.dismiss();
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
    private boolean root_move(String srcf, String destr) throws IOException {
        File src = new File(srcf);
        File dst = new File(destr);

        src.renameTo(dst);
        if(!dst.exists()){
            String[] commands = {"mv "+"\""+src.getPath()+"\""+" "+"\""+dst.getPath()+"\"\n"};
            RunAsRoot(commands);
            if (!dst.exists()){
                root_copy(srcf, destr);
                if (dst.exists()){
                    delete(src);
                    if (dst.exists()){
                        root_delete(src);}
                }
            }
        }
        return dst.exists();
    }
    private boolean root_delete(File f) throws IOException {
        delete(f);
        if (f.exists()){
            if (f.isDirectory()){
                String[] commands = {"rm -r "+"\""+f.getPath()+"\"\n"};
                RunAsRoot(commands);
            } else {
                String[] commands = {"rm "+"\""+f.getPath()+"\"\n"};
                RunAsRoot(commands);
            }
        }
        return !f.exists();
    }

    private boolean copy(String srcf, String destr) throws IOException {
        File src = new File(srcf);
        File dst = new File(destr);

         if (src.isDirectory()) {
             dst.mkdir();
             if (!src.exists() && !dst.mkdirs()) {
                throw new IOException("Cannot create dir " + dst.getAbsolutePath());}
             String[] children = src.list();
             for (int i=0; i<children.length; i++) {
                 copy(src.getPath()+children[i],dst.getPath()+children[i]);}
         } else {
             File directory = dst.getParentFile();
             if (directory != null && !directory.exists() && !directory.mkdirs()) {
             throw new IOException("Cannot create dir " + directory.getAbsolutePath());}

             InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst);
             StreamWorker(in, out);
         }

        return dst.exists();
    }
    private boolean delete(File f) throws IOException {
        if (f.isDirectory())
            for (File child : f.listFiles())
                delete(child);
        f.delete();
        return !f.exists();
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


    private boolean StreamWorker(InputStream in, OutputStream out) throws IOException {
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

}
