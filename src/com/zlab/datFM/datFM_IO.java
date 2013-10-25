package com.zlab.datFM;

import android.util.Log;
import android.widget.Toast;
import com.jcraft.jsch.*;
import com.zlab.datFM.IO.plugin_SMB;
import com.zlab.datFM.IO.plugin_local;
import jcifs.smb.*;
import java.io.*;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Vector;

public class datFM_IO {

    /** Protocols **/
    boolean smb;
    boolean sftp;
    boolean local;
    boolean acceptable;

    /** Globals **/
    String path;
    int PanelID;
    int CompetPanel;

    public datFM_IO(String file,int panelID){
        path=file;
        PanelID=panelID;
        CompetPanel=1;
        if(PanelID==1)CompetPanel=0;
    }

    /** FILE **/
    public ChannelSftp getSFTPChannel() throws JSchException, SftpException {
        if(datFM.sftp_auth_channel[PanelID]==null){
            datFM.sftp_auth_channel[PanelID] = new ChannelSftp();
        }
        return datFM.sftp_auth_channel[PanelID];
    }
    public Vector lslist(String fpath) throws SftpException, JSchException {
       return getSFTPChannel().ls(SFTPrealpath(fpath));
    }
    public ChannelSftp.LsEntry getSFTP_element(Vector list,int id) throws SftpException, JSchException {
        return (ChannelSftp.LsEntry) list.elementAt(id);
    }

    public Long getFileSize(){
        long size=1;
        checkProtocol();
        if(local){
            size = new plugin_local(path,PanelID).getFileSize();
        } else if (smb){
            size = new plugin_SMB(path,PanelID).getFileSize();
        } else if (sftp){try {
                size = getSFTP_element(lslist(path),0).getAttrs().getSize();
            } catch (Exception e) { Log.e("SFTP ERR:", e.getMessage()); }
        }

        return size;
    }
    public Long getFileSizeCustom(String path,int Panel){
        long size=1;
        checkProtocol();
        if(local){
            size = new plugin_local(path,PanelID).getFileSize();
        } else if (smb){
            size = new plugin_SMB(path,PanelID).getFileSize();
        } else if (sftp){try {
            size = new datFM_IO(path,Panel).getSFTP_element(lslist(path),0).getAttrs().getSize();
        } catch (Exception e) { Log.e("SFTP ERR:",e.getMessage()); }
        }

        return size;
    }

    /** STREAM **/
    public InputStream getInput() throws MalformedURLException, FileNotFoundException, UnknownHostException, SmbException {
        checkProtocol();
        InputStream in=null;
        if(local){
            in = new BufferedInputStream(new plugin_local(path,PanelID).getInput());
        } else if (smb){
            in = new BufferedInputStream(new plugin_SMB(path,PanelID).getInput());
        } else if (sftp){
            try {
                in = new BufferedInputStream(getSFTPChannel().get(SFTPrealpath(path)));
            } catch (Exception e) {
                Log.e("SFTP ERR:",e.getMessage());
            }
        }

        return in;
    }
    public OutputStream getOutput() throws FileNotFoundException, MalformedURLException, UnknownHostException, SmbException {
        checkProtocol();
        OutputStream out=null;
        if(local){
            out = new BufferedOutputStream(new plugin_local(path,PanelID).getOutput());
        } else if (smb){
            out = new BufferedOutputStream(new plugin_SMB(path,PanelID).getOutput());
        } else if (sftp){
        try {
            out = new BufferedOutputStream(getSFTPChannel().put(SFTPrealpath(path)));
        } catch (Exception e) {
            Log.e("SFTP ERR:",e.getMessage());
        }
        }
        return out;
    }

    /** DELETE **/
    public boolean delete() throws IOException, JSchException, SftpException {
        boolean success;
        checkProtocol();

        if(local){
            success = new plugin_local(path,PanelID).delete();
        } else if (smb){
            success = new plugin_SMB(path,PanelID).delete();
        } else if (sftp){
            success=delete_recursively_sftp(path);
        } else {
            success=false;
        }

        return success;
    }
    public boolean delete_recursively_sftp(String fpath) throws JSchException, SftpException {
        if (getSFTPChannel().lstat(SFTPrealpath(fpath)).isDir()){
            Vector<ChannelSftp.LsEntry> children = getSFTPChannel().ls(SFTPrealpath(fpath));
            for(ChannelSftp.LsEntry ff : children){
                if(!ff.getFilename().equals(".") && !ff.getFilename().equals("..")){
                    delete_recursively_sftp(fpath+"/"+ff.getFilename());
                }
            }
            getSFTPChannel().rmdir(SFTPrealpath(fpath));
        } else {
            getSFTPChannel().rm(SFTPrealpath(fpath));
        }

        SftpATTRS sftpATTRS;
        boolean deleted=false;
        try {
            sftpATTRS = getSFTPChannel().lstat(fpath);
        } catch (Exception ex) {
            deleted = true;
        }

        return deleted;
    }

    /** COPY **/
    public boolean copy(String dest) throws IOException, SftpException, JSchException {
        checkProtocol();

        if(local){
            new plugin_local(path,PanelID).copy(dest);
        } else if (smb){
            new plugin_SMB(path,PanelID).copy(dest);
            //copy_recursively_smb(getFileSmb(),dest);
        } else if (sftp){
            copy_recursively_sftp(path,dest);
        } else {
            return false;
        }

        return new datFM_IO(dest,CompetPanel).is_exist_custom(dest, CompetPanel);
    }
    private void copy_recursively_sftp(String fpath, String dest) throws JSchException, SftpException {
        if (getSFTPChannel().lstat(SFTPrealpath(fpath)).isDir()) {
            new datFM_IO(dest,PanelID).mkdir();

            updateOverallBar(datFM_File_Operations.progr_overal.getProgress() + 1,fpath,dest);

            Vector<ChannelSftp.LsEntry> children = getSFTPChannel().ls(SFTPrealpath(fpath));
            datFM_File_Operations.overalMax= datFM_File_Operations.progr_overal.getMax()+children.size();
            datFM_File_Operations.progr_overal.setMax(datFM_File_Operations.overalMax);

            for(ChannelSftp.LsEntry ff : children){
                if(!ff.getFilename().equals(".") && !ff.getFilename().equals("..")){
                    copy_recursively_sftp(fpath+"/"+ff.getFilename(),dest+"/"+ff.getFilename()
                    );
                }
            }
        } else {
            try {
                updateOverallBar(datFM_File_Operations.progr_overal.getProgress(), fpath, dest);
                IO_Stream_Worker(fpath, dest);
                updateOverallBar(datFM_File_Operations.progr_overal.getProgress() + 1, fpath, dest);
            } catch (Exception e){}
        }
    }

    /** RENAME **/
    public boolean rename(String new_name) throws IOException {
        boolean success;
        checkProtocol();

        if(local){
            success=new plugin_local(path,PanelID).rename(new_name);
        } else if (smb){
            /**success=rename_smb(new_name);**/
            success=new plugin_SMB(path,PanelID).rename(new_name);
        } else {
            success=false;
        }

        return success;
    }

    /** MKDIR **/
    public boolean mkdir(){
        boolean success=false;
        checkProtocol();
        if(local){
            success = new plugin_local(path,PanelID).mkdir();
        } else if (smb){
            success = new plugin_SMB(path,PanelID).mkdir();
        } else if (sftp){
            try {
                getSFTPChannel().mkdir(SFTPrealpath(path));
                success = !getSFTPChannel().ls(SFTPrealpath(path)).isEmpty();
            } catch (Exception e) {
                Log.e("SFTP: ERR",e.getMessage());
            }
        } else {
            success=false;
        }
        return success;
    }

    /** EXIST **/
    public boolean exists(){
        boolean success;
        checkProtocol();

        if(local){
            success = new plugin_local(path,PanelID).exists();
        } else if (smb){
            success = new plugin_SMB(path,PanelID).exists();
        } else {
            success=false;
        }
        return success;
    }
    public boolean is_exist_custom(String file, int PanID){
        boolean success;
        checkProtocol();

        if(local){
            success = new plugin_local(file,PanID).exists();
        } else if (smb){
            success = new plugin_SMB(file,PanID).exists();
        } else if (sftp){try {
            SftpATTRS sftpATTRS = null;
                      sftpATTRS = getSFTPChannel().lstat(SFTPrealpath(file));
                      success = true;
            } catch (Exception ex) { success = false; }
        } else {
            success=false;
        }
        return success;
    }
    /** IS DIR **/
    public boolean is_dir() throws SftpException, JSchException {
        boolean success;
        checkProtocol();

        if(local){
            success = new plugin_local(path,PanelID).is_dir();
        } else if (smb){
            success = new plugin_SMB(path,PanelID).is_dir();
        } else if (sftp){
            success = getSFTPChannel().lstat(SFTPrealpath(path)).isDir();
        } else {
            success=false;
        }
        return success;
    }
    /** getDir list **/
    public String[] listFiles(){
        String[] filepathlist;
        checkProtocol();

        if(local){
            filepathlist = new plugin_local(path,PanelID).listFiles();
        } else if (smb){
            filepathlist = new plugin_SMB(path,PanelID).listFiles();
        } else if (sftp){
            try {
                int count=0;
                //filepathlist = new String[lslist(path).size()-2];
                for(int i=0;i<lslist(path).size();i++){
                    if(!getSFTP_element(lslist(path),i).getFilename().equals(".") && !getSFTP_element(lslist(path),i).getFilename().equals(".."))
                    {
                        count++;
                    }
                }
                filepathlist = new String[count];
                count=0;
                for(int i=0;i<lslist(path).size();i++){
                    if(!getSFTP_element(lslist(path),i).getFilename().equals(".") && !getSFTP_element(lslist(path),i).getFilename().equals(".."))
                    {
                        filepathlist[count]=path+"/"+getSFTP_element(lslist(path),i).getFilename();count++;
                    }
                }
            } catch (Exception e) {filepathlist=new String[] {""};}
        } else {
            filepathlist=new String[] {""};
        }

        return filepathlist;
    }
    /** STREAM WORKER **/
    public void IO_Stream_Worker(String src, String dest) throws IOException {
        InputStream in = new datFM_IO(src,PanelID).getInput();
        OutputStream out = new datFM_IO(dest,CompetPanel).getOutput();

        byte[] buf = new byte[1024];
        int len;

        long fullsize = getFileSizeCustom(src,PanelID);
        long one_percent = fullsize/100;
        long cnt=0;
        int cur_file_progress=0;
        while ((len = in.read(buf)) > 0){
            out.write(buf, 0, len);
            cnt=cnt+1024;
            if(cnt>one_percent){
                cur_file_progress=cur_file_progress+1;
                if(datFM_File_Operations.progr_overal!=null)datFM_File_Operations.progr_current.setProgress(cur_file_progress);
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

        datFM_File_Operations.mHandler.post(new Runnable() {
            public void run() {
                if(datFM_File_Operations.progr_overal!=null)datFM_File_Operations.textCurrent.setText(String.valueOf(CurrentSizeinMB)+"MiB / "+String.valueOf(sizeinMB)+"MiB"+" ("+CurrentProgress+"/100%)");
            }
        });
    }
    private void updateOverallBar(final int OverallProgress,final String pathtofile,final String dest){
        if(datFM_File_Operations.progr_overal!=null){
            datFM_File_Operations.progr_overal.setProgress(OverallProgress);
            datFM_File_Operations.mHandler.post(new Runnable() {
                public void run() {
                    datFM_File_Operations.textOverall.setText("("+OverallProgress+"/"+ datFM_File_Operations.overalMax+")");
                    datFM_File_Operations.textFile.setText(pathtofile);
                    datFM_File_Operations.textTo.setText(dest);
                }
            });
        }
    }

    /** GetName in UI thread **/
    public String getName(){
        String name;
        checkProtocol();

        if(local){
            name = new plugin_local(path,PanelID).getName();
        } else if(smb){
            name = new plugin_SMB(path,PanelID).getName();
        } else if(sftp){
            if (path.lastIndexOf("/")+1==path.length())
            {path=path.substring(0,path.lastIndexOf("/"));}
            name = path.substring(path.lastIndexOf("/")+1);
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
            parent = new plugin_local(path,PanelID).getParent();
        } else if(smb){
            parent = new plugin_SMB(path,PanelID).getParent();
        } else if(sftp){
                if(path.equals("sftp://")){
                    parent[0]="datFM://sftp";
                    parent[1]="parent_dir";
                    parent[2]=datFM.datFM_state.getResources().getString(R.string.fileslist_parent_directory);
                } else {
                    if (path.lastIndexOf("/")+1==path.length())
                    {path=path.substring(0,path.lastIndexOf("/"));}

                    parent[0]=path.substring(0,path.lastIndexOf("/")+1);
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

    public String SFTPrealpath(String spath){
        String hostname = spath.replace("sftp://","");
        if(hostname.contains("/")){hostname = hostname.substring(0, hostname.indexOf("/"));}
        String rpath=spath.replace("sftp://"+hostname,"");
        return rpath;
    }

    /** Protocol identifier **/
    public boolean checkProtocol(){
        sftp = path.startsWith("sftp://");
        smb = path.startsWith("smb://");
        local = path.startsWith("/");

        if(!local && !smb && !sftp ){
            acceptable=false;
        } else {
            acceptable=true;
        }

        return acceptable;
    }

}
