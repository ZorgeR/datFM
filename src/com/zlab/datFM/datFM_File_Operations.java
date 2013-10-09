package com.zlab.datFM;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

import java.io.*;
import java.util.List;

public class datFM_File_Operations extends AsyncTask<String, Void, Boolean> {

    int srcPannelID,competPannelID;
    String destDir, srcDir, operation, new_name, mask,ext;
    public datFM activity;
    static AlertDialog dialog_operation;
    int count;
    static int overalMax;

    static TextView textCurrent,textOverall,textTo,textFile;
    static ProgressBar progr_current;
    static ProgressBar progr_overal;
    static LinearLayout currentLayout;
    public datFM_File_Operations(datFM a)
    {
        activity = a;
    }

    static protected Handler mHandler = new Handler();

    protected void onPreExecute() {
            super.onPreExecute();
        overalMax=datFM.sel;

        AlertDialog.Builder progr_dialog = new AlertDialog.Builder(activity);
        progr_dialog.setTitle("operation");
        LayoutInflater inflater = activity.getLayoutInflater();
        View layer = inflater.inflate(R.layout.datfm_operation,null);
        textCurrent = (TextView) layer.findViewById(R.id.textCurrent);
        textOverall = (TextView) layer.findViewById(R.id.textOverall);
        textFile = (TextView) layer.findViewById(R.id.textFile);
        textTo = (TextView) layer.findViewById(R.id.textTo);
        progr_current = (ProgressBar) layer.findViewById(R.id.dialog_operation_progress_current);
        progr_overal = (ProgressBar) layer.findViewById(R.id.dialog_operation_progress_overall);
        progr_overal.setMax(datFM.sel);
        progr_current.setMax(100);
        textCurrent.setText("("+0+"/"+100+")");
        textOverall.setText("("+0+"/"+overalMax+")");

        currentLayout = (LinearLayout) layer.findViewById(R.id.currentLayout);
        currentLayout.setVisibility(View.GONE);
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

            if(operation.equals("open_remote") || operation.equals("open_as_remote")){
                    mHandler.post(new Runnable() {
                    public void run() {
                        currentLayout.setVisibility(View.VISIBLE);
                    }
                });
                    progr_overal.setMax(1);
                    ext=params[3];
                    boolean exist = new File(destDir).exists();
                    boolean success;
                    success = exist || protocol_copy(srcDir, destDir);
                    if (success) {count++;onProgressUpdate(count);}

            } else if(operation.equals("new_folder")){
                dialog_operation.setTitle(datFM.datFM_context.getResources().getString(R.string.ui_dialog_title_copy));
                if (protocol_newfolder(srcDir)) {count++;onProgressUpdate(count);}

            } else {

                boolean[] selectionHolder = activity.selected.clone();
                List<datFM_File> adaptorHolder = activity.adapter.getAllItems();

                if(operation.equals("copy")){
                    mHandler.post(new Runnable() {
                        public void run() {
                           currentLayout.setVisibility(View.VISIBLE);
                        }
                    });

                    dialog_operation.setTitle(datFM.datFM_context.getResources().getString(R.string.ui_dialog_title_copy));
                    for (int i=1;i<selectionHolder.length;i++){
                        if (selectionHolder[i]){
                            datFM_File from = adaptorHolder.get(i);
                            boolean success = protocol_copy(from.getPath(), destDir + "/" + from.getName());
                            if (success) {count++;onProgressUpdate(count);}
                        }
                    }
                } else if (operation.equals("delete")){
                    dialog_operation.setTitle(datFM.datFM_context.getResources().getString(R.string.ui_dialog_title_delete));
                    for (int i=1;i<selectionHolder.length;i++){
                        if (selectionHolder[i]){
                            datFM_File from = adaptorHolder.get(i);
                            boolean success=false;

                            if (from.getType().equals("dir") || from.getType().equals("file") ){
                                success = protocol_delete(from.getPath());

                            } else if(from.getType().equals("smb_store_network")){
                                success = protocol_delete(activity.getFilesDir().getPath()+"/smb_data_"+from.getName());
                            } else if(from.getType().equals("sftp_store_network")){
                                success = protocol_delete(activity.getFilesDir().getPath()+"/sftp_data_"+from.getName());
                            } else if(from.getType().startsWith("fav_bookmark")){
                                success = protocol_delete(activity.getFilesDir().getPath()+"/fav_data_"+from.getName());
                            }

                            if (success) {count++;onProgressUpdate(count);}
                        }
                    }
                } else if (operation.equals("move")){
                    mHandler.post(new Runnable() {
                        public void run() {
                            currentLayout.setVisibility(View.VISIBLE);
                        }
                    });

                    dialog_operation.setTitle(datFM.datFM_context.getResources().getString(R.string.ui_dialog_title_move));
                    for (int i=1;i<selectionHolder.length;i++){
                        if (selectionHolder[i]){
                            datFM_File from = adaptorHolder.get(i);
                            boolean success = protocol_move(from.getPath(), destDir + "/" + from.getName());
                            if (success) {count++;onProgressUpdate(count);}
                        }
                    }
                } else if (operation.equals("rename")){
                    dialog_operation.setTitle(datFM.datFM_context.getResources().getString(R.string.ui_dialog_title_rename));

                    new_name = params[3];
                    mask = params[4];
                    int j=1;
                    if (!mask.equals("true")){
                        for (int i=1;i<selectionHolder.length;i++){
                            if (selectionHolder[i]){
                                datFM_File from = adaptorHolder.get(i);
                                boolean success = protocol_rename(from.getPath(), srcDir+"/"+new_name);
                                if (success) {count++;onProgressUpdate(count);}
                            }
                        }
                    } else {
                        for (int i=1;i<selectionHolder.length;i++){
                            if (selectionHolder[i]){
                                datFM_File from = adaptorHolder.get(i);
                                boolean success;
                                if (from.getType().equals("file")){
                                    int dotPos = from.getName().lastIndexOf(".")+1;
                                    String ext = from.getName().substring(dotPos);
                                    if (dotPos>0){
                                        success = protocol_rename(from.getPath(), srcDir+"/"+new_name+"_"+j+"."+ext);
                                        if (success) {count++;onProgressUpdate(count);}
                                    } else {
                                        success = protocol_rename(from.getPath(), srcDir+"/"+new_name+"_"+j);
                                        if (success) {count++;onProgressUpdate(count);}
                                    }
                                } else {
                                    success = protocol_rename(from.getPath(), srcDir+"/"+new_name+"_"+j);
                                    if (success) {count++;onProgressUpdate(count);}
                                }
                                if (success) {count++;j++;onProgressUpdate(count);}
                            }
                        }
                    }
                }

            }

            return true;
        }
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if (operation.equals("copy")){
                activity.update_tab(count,operation,destDir,competPannelID);}

            if (operation.equals("delete")){
                activity.update_tab(count,operation,destDir,srcPannelID);}

            if (operation.equals("move")){
                activity.update_tab(count, "move", destDir,2);}

            if (operation.equals("rename")){
                activity.update_tab(count,operation,srcDir,srcPannelID);}

            if (operation.equals("new_folder")){
                activity.update_tab(count,"new_folder",srcDir,srcPannelID);}

            if (operation.equals("open_remote")){
                activity.openFile(destDir,"",ext);
            }

            if (operation.equals("open_as_remote")){
                activity.openFileAs(destDir, "", ext);
            }

            dialog_operation.dismiss();
        }

    /** Protocol Operation **/
    private boolean protocol_copy(String src, String dest){
        boolean success;
        try {
            if(datFM.protocols[0].equals("local") && datFM.protocols[1].equals("local")){
                return root_copy(src, dest);
            } else {
                success = new datFM_IO(src,srcPannelID).copy(dest);
            }
        } catch (Exception e) {
            e.printStackTrace();
            success=false;
        }
        return success;
    }
    private boolean protocol_move(String src, String dest){
        boolean success;
        try {
            if(datFM.protocols[srcPannelID].equals("local") && datFM.protocols[competPannelID].equals("local")){
                return root_move(src, dest);
            } else {
                success = protocol_copy(src, dest);
                if(success){
                    protocol_delete(src);}
            }
        } catch (Exception e) {
            e.printStackTrace();
            success=false;
        }
        return success;
    }
    private boolean protocol_delete(String src){
        boolean success;
        try {
            if(datFM.protocols[srcPannelID].equals("local")){
                return root_delete(src);
            } else {
                success = new datFM_IO(src,srcPannelID).delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
            success=false;
        }
        return success;
    }
    private boolean protocol_rename(String src,String new_name){
        boolean success=false;
        try {success=new datFM_IO(src,srcPannelID).rename(new_name);
        } catch (IOException e) {e.printStackTrace();}
        return success;
    }
    private boolean protocol_newfolder(String newfolder){
        boolean success=false;
        if(!new datFM_IO(newfolder,srcPannelID).is_exist()){
            success=new datFM_IO(newfolder,srcPannelID).mkdir();
            if (!success && datFM.pref_root){
                    File newdir_path = new File(newfolder);
                    root_newfolder(newdir_path.getPath());
            }

        } else {
            for (int i=1;i<1000;i++){
                String dirs = newfolder+" "+"("+i+")";
                if (!new datFM_IO(dirs,srcPannelID).is_exist()){
                    if (datFM.pref_root){
                        success=new datFM_IO(dirs,srcPannelID).mkdir();
                        if(!success){
                            File newdir_path = new File(dirs);
                            root_newfolder(newdir_path.getPath());}
                    } else {
                        success=new datFM_IO(dirs,srcPannelID).mkdir();
                    }
                    break;
                }
            }
        }
        return success;
    }
    /** Operation **/
    /* TODO - При операциях с root файлами использовать тип HOOK_ROOT_FILES, за место File */
    /* TODO - Исравить проверку выполнения для root операций и для создания нового каталога. */
    private boolean root_delete(String file) throws IOException, SftpException, JSchException {
        boolean success = new datFM_IO(file,srcPannelID).delete();
        if (!success && datFM.pref_root){
            File f = new File(file);
            if (f.isDirectory()){
                String[] commands = {"rm -r "+"\""+f.getPath()+"\"\n"};
                RunAsRoot(commands);
            } else {
                String[] commands = {"rm "+"\""+f.getPath()+"\"\n"};
                RunAsRoot(commands);
            }
            return !f.exists();
        }
        return success;
    }
    private boolean root_copy(String srcf, String destr) throws IOException, JSchException, SftpException {
        boolean success = new datFM_IO(srcf,srcPannelID).copy(destr);
        if (!success && datFM.pref_root){
            File src = new File(srcf);
            File dst = new File(destr);
            if (src.isDirectory()){
                String[] commands = {"cp -rf "+"\""+src.getPath()+"\""+" "+"\""+dst.getPath()+"\"\n"};
                RunAsRoot(commands);
            } else {
                String[] commands = {"cp -f "+"\""+src.getPath()+"\""+" "+"\""+dst.getPath()+"\"\n"};
                RunAsRoot(commands);
            }
            return dst.exists();
        }
        return success;
    }
    private boolean root_move(String srcf, String destr) throws IOException {
        boolean success;
        File src = new File(srcf);
        File dst = new File(destr);
        success = src.renameTo(dst);

        if (!success){
            if(datFM.pref_root){
                if(!dst.exists()){
                    String[] commands = {"mv "+"\""+src.getPath()+"\""+" "+"\""+dst.getPath()+"\"\n"};
                    RunAsRoot(commands);
                    if (!dst.exists()){
                        if (protocol_copy(srcf, destr)){
                            protocol_delete(srcf);
                        }
                    }
                }
                success = dst.exists();
            } else {
                if (protocol_copy(srcf, destr)) {
                    protocol_delete(srcf);
                    success=true;
                }
            }
        }
        return success;
    }
    private void    root_newfolder(String path){
        String[] commands = {"mkdir \""+path+"\"\n","chmod 775 \""+path+"\"\n"};
        RunAsRoot(commands);
    }

    /** other **/
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
}
