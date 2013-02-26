package com.zlab.datFM;

import android.util.Log;
import android.view.View;
import android.widget.Toast;
import jcifs.smb.*;
import java.io.*;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.UnknownHostException;

public class datFM_IO {

    /** Protocols **/
    boolean smb;
    boolean local;
    boolean acceptable;

    /** Globals **/
    String path;
    int PanelID;

    public datFM_IO(String file,int panelID){
        path=file;
        PanelID=panelID;
    }

    /** FILE **/
    public File getFileLocal(){
        File file = new File(path);
        return file;
    }
    public SmbFile getFileSmb() throws MalformedURLException {
        //---------START SMB WORKS-------------------------
        NtlmPasswordAuthentication auth = datFM.auth[PanelID];
        SmbFile f = new SmbFile(path,auth);
        //---------END SMB WORKS-------------------------
        return f;
    }
    public Long getFileSize(String filepath){
        long size=1;
        smb = filepath.startsWith("smb://");
        local = filepath.startsWith("/");

        if(local){
            size = new datFM_IO(filepath,PanelID).getFileLocal().length();
        } else if (smb){try {
            size = new datFM_IO(filepath,PanelID).getFileSmb().length();
            } catch (SmbException e) {e.printStackTrace();} catch (MalformedURLException e) {e.printStackTrace();}
        }

        return size;
    }

    /** STREAM **/
    public InputStream getInput() throws MalformedURLException, FileNotFoundException, UnknownHostException, SmbException {
        checkProtocol();
        InputStream in=null;
        if(local){
            in = new BufferedInputStream(new FileInputStream(path));
        } else if (smb){
            in = new BufferedInputStream(new SmbFileInputStream(getFileSmb()));
        }

        return in;
    }
    public OutputStream getOutput() throws FileNotFoundException, MalformedURLException, UnknownHostException, SmbException {
        checkProtocol();
        OutputStream out=null;
        if(local){
            out = new BufferedOutputStream(new FileOutputStream(path));
        } else if (smb){
            out = new BufferedOutputStream(new SmbFileOutputStream(getFileSmb()));
        }
        return out;
    }

    /** DELETE **/
    public boolean delete() throws IOException {
        boolean success;
        checkProtocol();

        if(local){
            success=delete_recursively_local(getFileLocal());
        } else if (smb){
            success=delete_recursively_smb(getFileSmb());
        } else {
            success=false;
        }

        return success;
    }
    public boolean delete_recursively_smb(SmbFile file) throws SmbException {
        if (file.isDirectory())
            for (SmbFile child : file.listFiles())
                delete_recursively_smb(child);
        file.delete();
        return !file.exists();
    }
    public boolean delete_recursively_local(File f) throws IOException {
        if (f.isDirectory())
            for (File child : f.listFiles())
                delete_recursively_local(child);
        f.delete();
        return !f.exists();
    }

    /** COPY **/
    public boolean copy(String dest) throws IOException {
        checkProtocol();

        if(local){
            copy_recursively_local(getFileLocal(),dest);
        } else if (smb){
            copy_recursively_smb(getFileSmb(),dest);
        } else {
            return false;
        }

        return file_exist(dest);
    }
    public boolean copy_recursively_smb(SmbFile file,String dest) throws IOException {
        if (file.isDirectory()) {
            new datFM_IO(dest,PanelID).mkdir();

            updateOverallBar(datFM_FileOperation.progr_overal.getProgress() + 1,file.getPath(),dest);

            SmbFile[] children = file.listFiles();
            datFM_FileOperation.overalMax=datFM_FileOperation.progr_overal.getMax()+children.length;
            datFM_FileOperation.progr_overal.setMax(datFM_FileOperation.overalMax);
            for (SmbFile ff : children) {
                copy_recursively_smb(ff, dest+"/"+ff.getName());}
        } else {
            try {
                updateOverallBar(datFM_FileOperation.progr_overal.getProgress(), file.getPath(), dest);
                IO_Stream_Worker(file.getPath(), dest);
                updateOverallBar(datFM_FileOperation.progr_overal.getProgress() + 1, file.getPath(), dest);
            } catch (Exception e){}
        }
        return file_exist(dest);
    }
    public boolean copy_recursively_local(File file,String dest) throws IOException {
        if (file.isDirectory()) {
            new datFM_IO(dest,PanelID).mkdir();

            updateOverallBar(datFM_FileOperation.progr_overal.getProgress() + 1, file.getPath(), dest);

            File[] children = file.listFiles();
            datFM_FileOperation.overalMax=datFM_FileOperation.progr_overal.getMax()+children.length;
            datFM_FileOperation.progr_overal.setMax(datFM_FileOperation.overalMax);
            for (File ff : children) {
                copy_recursively_local(ff, dest+"/"+ff.getName());}
        } else {
            try {
                updateOverallBar(datFM_FileOperation.progr_overal.getProgress(), file.getPath(), dest);
                IO_Stream_Worker(file.getPath(), dest);
                updateOverallBar(datFM_FileOperation.progr_overal.getProgress() + 1, file.getPath(), dest);
            } catch (Exception e){}
        }
        return file_exist(dest);
    }

    /** RENAME **/
    public boolean rename(String new_name) throws IOException {
        boolean success;
        checkProtocol();

        if(local){
            success=rename_local(getFileLocal(), new_name);
        } else if (smb){
            success=rename_smb(getFileSmb(), new_name);
        } else {
            success=false;
        }

        return success;
    }
    public boolean rename_smb(SmbFile file,String new_name) throws IOException {
        SmbFile dest=new SmbFile(file.getParent()+"/"+new_name);
        file.renameTo(dest);
        return dest.exists();
    }
    public boolean rename_local(File file,String new_name) throws IOException {
        return file.renameTo(new File(file.getParent()+"/"+new_name));
    }

    /** MKDIR **/
    public boolean mkdir(){
        boolean success;
        checkProtocol();
        if(local){
            getFileLocal().mkdir();
            success= getFileLocal().exists();
        } else if (smb){
            try {
                getFileSmb().mkdir();
                success = getFileSmb().exists();
            } catch (Exception e) {e.printStackTrace();success=false;Log.e("ERR",e.getMessage());}
        } else {
            success=false;
        }
        return success;
    }

    /** EXIST **/
    public boolean dir_exist(){
        boolean success;
        checkProtocol();

        if(local){
            success= getFileLocal().exists();
        } else if (smb){
            try {success=getFileSmb().exists();
            } catch (Exception e) {success=false;}
        } else {
            success=false;
        }
        return success;
    }
    public boolean file_exist(String file){
        boolean success;
        boolean smb_ = file.startsWith("smb://");
        boolean local_ = file.startsWith("/");

        if(local_){
            success=new datFM_IO(file,PanelID).getFileLocal().exists();
        } else if (smb_){
            try {
                success=new datFM_IO(file,PanelID).getFileSmb().exists();
            } catch (Exception e) {success=false;}
        } else {
            success=false;
        }
        return success;
    }

    /** STREAM WORKER **/
    private void IO_Stream_Worker(String src, String dest) throws IOException {
        InputStream in = new datFM_IO(src,PanelID).getInput();
        int compPanel=1;
        if(PanelID==1)compPanel=0;
        OutputStream out = new datFM_IO(dest,compPanel).getOutput();

        byte[] buf = new byte[1024];
        int len;

        long fullsize = getFileSize(src);
        long one_percent = fullsize/100;
        long cnt=0;
        int cur_file_progress=0;
        while ((len = in.read(buf)) > 0){
            out.write(buf, 0, len);
            cnt=cnt+1024;
            if(cnt>one_percent){
                cur_file_progress=cur_file_progress+1;
                datFM_FileOperation.progr_current.setProgress(cur_file_progress);
                updateCurrentBar(cur_file_progress,fullsize,cur_file_progress*one_percent);
                cnt=0;
            }
        }
        in.close();
        out.close();
    }

    private void updateCurrentBar(final int CurrentProgress,final long fullsize, final long currentsize){
        final BigDecimal sizeinMB = new BigDecimal(fullsize/1024.00/1024.00).setScale(3, BigDecimal.ROUND_HALF_UP);
        final BigDecimal CurrentSizeinMB = new BigDecimal(currentsize/1024.00/1024.00).setScale(3, BigDecimal.ROUND_HALF_UP);

        datFM_FileOperation.mHandler.post(new Runnable() {
            public void run() {
                datFM_FileOperation.textCurrent.setText(String.valueOf(CurrentSizeinMB)+"MiB / "+String.valueOf(sizeinMB)+"MiB"+" ("+CurrentProgress+"/100%)");
            }
        });
    }
    private void updateOverallBar(final int OverallProgress,final String pathtofile,final String dest){
        datFM_FileOperation.progr_overal.setProgress(OverallProgress);
        datFM_FileOperation.mHandler.post(new Runnable() {
            public void run() {
                datFM_FileOperation.textOverall.setText("("+OverallProgress+"/"+datFM_FileOperation.overalMax+")");
                datFM_FileOperation.textFile.setText(pathtofile);
                datFM_FileOperation.textTo.setText(dest);
            }
        });
    }

    /** GetName in UI thread **/
    public String getName(){
        String name;
        checkProtocol();

        if(local){
            name=getFileLocal().getName();
        } else if(smb){
            String name_in_ui_parser=path.replace("smb://","");
            while (name_in_ui_parser.contains("/")){

                    if(name_in_ui_parser.indexOf("/")==0){name_in_ui_parser=name_in_ui_parser.substring(1);}
                    String holder=name_in_ui_parser;

                    if(name_in_ui_parser.indexOf("/")!=-1){
                        name_in_ui_parser=name_in_ui_parser.substring(name_in_ui_parser.indexOf("/"));
                    } else {
                        name_in_ui_parser=name_in_ui_parser+"/";
                        break;
                    }

                    if(name_in_ui_parser.length()==1){
                        name_in_ui_parser=holder;break;
                    }
            }
            name=name_in_ui_parser.substring(0);
        } else {
           name="";
        }

        return name;
    }
    /** GetParent in UI thread **/
    public String[] getParent(){
        String[] parent=new String[3];
        checkProtocol();

        if(local){
            if(path.equals("/")){
                parent[0]="datFM://";
                parent[1]="home";
                parent[2]=datFM.datFM_state.getResources().getString(R.string.fileslist_parent_directory);
            } else {
                parent[0]=getFileLocal().getParent();
                parent[1]="parent_dir";
                parent[2]=datFM.datFM_state.getResources().getString(R.string.fileslist_parent_directory);
            }
        } else if(smb){
            if(path.equals("smb:////")){
                parent[0]="datFM://samba";
                parent[1]="parent_dir";
                parent[2]=datFM.datFM_state.getResources().getString(R.string.fileslist_parent_directory);
            } else {
                parent[0]=path.substring(0,path.lastIndexOf("/"));
                parent[0]=parent[0].substring(0,parent[0].lastIndexOf("/")+1);
                parent[1]="parent_dir";
                parent[2]=datFM.datFM_state.getResources().getString(R.string.fileslist_parent_directory);
            }
        } else {
            parent[0]=path.substring(0,path.lastIndexOf("/")+1);
            parent[1]="home";
            parent[2]=datFM.datFM_state.getResources().getString(R.string.fileslist_parent_directory);
        }

        return parent;
    }

    /** Protocol identifier **/
    public boolean checkProtocol(){
        smb = path.startsWith("smb://");
        local = path.startsWith("/");

        if(!local &&
           !smb   ){
            acceptable=false;
        } else {
            acceptable=true;
        }

        return acceptable;
    }

}
