package com.zlab.datFM;

import android.util.Log;
import android.widget.Toast;
import com.jcraft.jsch.*;
import com.zlab.datFM.IO.plugin_SFTP;
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

    public Long getFileSize(){
        long size=1;
        checkProtocol();
        if(local){
            size = new plugin_local(path,PanelID).getFileSize();
        } else if (smb){
            size = new plugin_SMB(path,PanelID).getFileSize();
        } else if (sftp){
            size = new plugin_SFTP(path,PanelID).getFileSize();
        }

        return size;
    }
    public Long getFileSizeCustom(String file,int Panel){
        long size=1;
        checkProtocol();
        if(local){
            size = new plugin_local(file,Panel).getFileSize();
        } else if (smb){
            size = new plugin_SMB(file,Panel).getFileSize();
        } else if (sftp){
            size = new plugin_SFTP(file,Panel).getFileSize();
        }

        return size;
    }

    /** STREAM **/
    public BufferedInputStream getInput(){
        checkProtocol();
        BufferedInputStream in=null;
        if(local){
            return new plugin_local(path,PanelID).getInput();
        } else if (smb){
            return new plugin_SMB(path,PanelID).getInput();
        } else if (sftp){
            return new plugin_SFTP(path,PanelID).getInput();
        }

        return in;
    }
    public BufferedOutputStream getOutput(){
        checkProtocol();
        BufferedOutputStream out=null;
        if(local){
            return new plugin_local(path,PanelID).getOutput();
        } else if (smb){
            return new plugin_SMB(path,PanelID).getOutput();
        } else if (sftp){
            return new plugin_SFTP(path,PanelID).getOutput();
        }
        return out;
    }

    /** DELETE **/
    public boolean delete(){
        boolean success;
        checkProtocol();

        if(local){
            success = new plugin_local(path,PanelID).delete();
        } else if (smb){
            success = new plugin_SMB(path,PanelID).delete();
        } else if (sftp){
            success = new plugin_SFTP(path,PanelID).delete();
        } else {
            success=false;
        }

        return success;
    }
    /** COPY **/
    public boolean copy(String dest){
        checkProtocol();

        if(local){
            new plugin_local(path,PanelID).copy(dest);
        } else if (smb){
            new plugin_SMB(path,PanelID).copy(dest);
        } else if (sftp){
            new plugin_SFTP(path,PanelID).copy(dest);
        } else {
            return false;
        }

        return new datFM_IO(dest,CompetPanel).is_exist_custom(dest, CompetPanel);
    }
    /** RENAME **/
    public boolean rename(String new_name){
        boolean success;
        checkProtocol();

        if(local){
            success=new plugin_local(path,PanelID).rename(new_name);
        } else if (smb){
            success=new plugin_SMB(path,PanelID).rename(new_name);
        } else if (sftp){
            success=new plugin_SFTP(path,PanelID).rename(new_name);
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
            success = new plugin_SFTP(path,PanelID).mkdir();
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
        } else if (sftp){
            success = new plugin_SFTP(path,PanelID).exists();
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
        } else if (sftp){
            success = new plugin_SFTP(file,PanID).exists();
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
            success = new plugin_SFTP(path,PanelID).is_dir();
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
            filepathlist = new plugin_SFTP(path,PanelID).listFiles();
        } else {
            filepathlist=new String[] {""};
        }

        return filepathlist;
    }
    /** STREAM WORKER **/
    public void IO_Stream_Worker(String src, String dest) throws IOException {
        BufferedInputStream in = new datFM_IO(src,PanelID).getInput();
        BufferedOutputStream out = new datFM_IO(dest,CompetPanel).getOutput();

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
            name = new plugin_SFTP(path,PanelID).getName();
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
            parent = new plugin_SFTP(path,PanelID).getParent();
        } else {
            parent[0]=path.substring(0,path.lastIndexOf("/")+1);
            parent[1]="home";
            parent[2]=datFM.datFM_state.getResources().getString(R.string.fileslist_parent_directory);
        }

        return parent;
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
