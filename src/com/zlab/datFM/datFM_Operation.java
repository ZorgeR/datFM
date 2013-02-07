package com.zlab.datFM;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import java.io.*;

public class datFM_Operation extends AsyncTask<String, Void, Boolean> {

    int count;
    String destDir, srcDir, operation, new_name, mask;
    public datFM activity;
    ProgressDialog dialog_operation;

    public datFM_Operation(datFM a)
    {
        activity = a;
    }

    protected void onPreExecute() {
            super.onPreExecute();
            dialog_operation = new ProgressDialog(datFM.datf_context);
            dialog_operation.setTitle("Working...");
            dialog_operation.setMessage(datFM.datf_context.getResources().getString(R.string.ui_dialog_header_please_wait));
            dialog_operation.setIndeterminate(false);
            dialog_operation.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog_operation.setMax(datFM.sel);
            dialog_operation.show();
            }

    protected void onProgressUpdate(Integer values) {
            dialog_operation.setProgress(values);
            //dialog_operation.setMax(values[1]);
    }

        @Override
        protected Boolean doInBackground(String... params) {
            /** params[0] = copy,move,delete
             *  params[1] = src
             *  params[2] = dst
             *  params[3] = new_name
             *  params[4] = mask
             **/

            operation = params[0];
            srcDir = params[1];
            destDir = params[2];

            count=0;

            if (operation.equals("copy")){
                dialog_operation.setTitle(datFM.datf_context.getResources().getString(R.string.ui_dialog_title_copy));

                for (int i=1;i<activity.selected.length;i++){
                    if (activity.selected[i]){
                        datFM_FileInformation from = activity.adapter.getItem(i);
                        boolean success=false;
                        try {
                            if (datFM.pref_root){
                                success = root_copy(new File(from.getPath()), new File(destDir, from.getName()));
                            } else {
                                //success = (copy(new File(from.getPath()),new File(destDir, from.getName())));
                                success = copy_protocol(from.getPath(),destDir+"/"+from.getName());
                            }
                        } catch (IOException e) {e.printStackTrace();}
                        if (success) {count++;onProgressUpdate(count);}
                    }
                }
            }

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
                            success = root_move(move_file,new File (destDir, from.getName()));
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
                                success = root_move(rename_file,new File(srcDir, new_name));
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
                                        success = root_move(rename_file,new File(srcDir, new_name+"_"+j+"."+ext));
                                    } catch (IOException e) {e.printStackTrace();}
                                    } else {
                                        success = rename_file.renameTo(new File(srcDir, new_name + "_" + j + "." + ext));
                                    }
                                } else {
                                    if (datFM.pref_root){try {
                                        success = root_move(rename_file,new File(srcDir, new_name+"_"+j));
                                    } catch (IOException e) {e.printStackTrace();}
                                    } else {
                                        success = rename_file.renameTo(new File(srcDir, new_name+"_"+j));
                                    }
                                }
                            } else {
                                if (datFM.pref_root){try {
                                    success = root_move(rename_file,new File(srcDir, new_name+"_"+j));
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

    private boolean root_copy(File src, File dst) throws IOException{
        try {copy(src.getPath(), dst.getPath());} catch (IOException e){/*e.printStackTrace();*/}

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
    private boolean root_move(File src, File dst) throws IOException {
        src.renameTo(dst);
        if(!dst.exists()){
            String[] commands = {"mv "+"\""+src.getPath()+"\""+" "+"\""+dst.getPath()+"\"\n"};
            RunAsRoot(commands);
            if (!dst.exists()){
                root_copy(src, dst);
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
             // make sure the directory we plan to store the recording in exists
             File directory = dst.getParentFile();
             if (directory != null && !directory.exists() && !directory.mkdirs()) {
                throw new IOException("Cannot create dir " + directory.getAbsolutePath());}

             InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst);

             // Copy the bits from instream to outstream
             byte[] buf = new byte[1024];
             int len;
             while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);}
             in.close();
             out.close();
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

    private boolean copy_protocol(String src, String dest){
        try {

        if(datFM.protocols[datFM.curPanel].equals("local") && datFM.protocols[datFM.competPanel].equals("local")){
            copy(src,dest);
        } else if(datFM.protocols[datFM.curPanel].equals("local") && datFM.protocols[datFM.competPanel].equals("smb")){
            InputStream in = new FileInputStream(src);
            jcifs.smb.SmbFileOutputStream out_smb = new jcifs.smb.SmbFileOutputStream(dest);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0){
            out_smb.write(buf, 0, len);}

            in.close();
            out_smb.close();
        } else if(datFM.protocols[datFM.curPanel].equals("smb") && datFM.protocols[datFM.competPanel].equals("local")){
            jcifs.smb.SmbFileInputStream in_smb = new jcifs.smb.SmbFileInputStream(src);
            OutputStream out = new FileOutputStream(dest);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in_smb.read(buf)) > 0){
                out.write(buf, 0, len);}

            in_smb.close();
            out.close();
        } else if(datFM.protocols[datFM.curPanel].equals("smb") && datFM.protocols[datFM.competPanel].equals("smb")){
            jcifs.smb.SmbFileInputStream in_smb = new jcifs.smb.SmbFileInputStream(src);
            jcifs.smb.SmbFileOutputStream out_smb = new jcifs.smb.SmbFileOutputStream(dest);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in_smb.read(buf)) > 0){
                out_smb.write(buf, 0, len);}

            in_smb.close();
            out_smb.close();
        }

        return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
                 /*
    private InputStream FetchFile(String path) throws FileNotFoundException {
        return in;
    }

    private OutputStream WriteFile(String path) throws FileNotFoundException {
        return out;
    }
            */
}
