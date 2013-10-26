package com.zlab.datFM;

import android.util.Log;
import android.widget.Toast;
import com.jcraft.jsch.*;
import com.zlab.datFM.IO.plugin_FTP;
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
    boolean ftp;
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
        } else if (ftp){
            size = new plugin_FTP(path,PanelID).getFileSize();
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
        } else if (ftp){
            size = new plugin_FTP(file,Panel).getFileSize();
        }

        return size;
    }

    /** STREAM **/
    public InputStream getInput(){
        checkProtocol();
        InputStream in=null;
        if(local){
            return new plugin_local(path,PanelID).getInput();
        } else if (smb){
            return new plugin_SMB(path,PanelID).getInput();
        } else if (sftp){
            return new plugin_SFTP(path,PanelID).getInput();
        } else if (ftp){
            return new plugin_FTP(path,PanelID).getInput();
        }

        return in;
    }
    public OutputStream getOutput(){
        checkProtocol();
        OutputStream out=null;
        if(local){
            return new plugin_local(path,PanelID).getOutput();
        } else if (smb){
            return new plugin_SMB(path,PanelID).getOutput();
        } else if (sftp){
            return new plugin_SFTP(path,PanelID).getOutput();
        } else if (ftp){
            return new plugin_FTP(path,PanelID).getOutput();
        }
        return out;
    }

    /** DELETE **/
    public boolean delete(){
        checkProtocol();

        if(local){
            return new plugin_local(path,PanelID).delete();
        } else if (smb){
            return new plugin_SMB(path,PanelID).delete();
        } else if (sftp){
            return new plugin_SFTP(path,PanelID).delete();
        } else if (ftp){
            return new plugin_FTP(path,PanelID).delete();
        }

        return false;
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
        } else if (ftp){
            new plugin_FTP(path,PanelID).copy(dest);
        }

        return new datFM_IO(dest,CompetPanel).is_exist_custom(dest, CompetPanel);
    }
    /** RENAME **/
    public boolean rename(String new_name){
        checkProtocol();

        if(local){
            return new plugin_local(path,PanelID).rename(new_name);
        } else if (smb){
            return new plugin_SMB(path,PanelID).rename(new_name);
        } else if (sftp){
            return new plugin_SFTP(path,PanelID).rename(new_name);
        } else if (ftp){
            return new plugin_FTP(path,PanelID).rename(new_name);
        }

        return false;
    }

    /** MKDIR **/
    public boolean mkdir(){
        checkProtocol();
        if(local){
            return new plugin_local(path,PanelID).mkdir();
        } else if (smb){
            return new plugin_SMB(path,PanelID).mkdir();
        } else if (sftp){
            return new plugin_SFTP(path,PanelID).mkdir();
        } else if (ftp){
            return new plugin_FTP(path,PanelID).mkdir();
        }
        return false;
    }

    /** EXIST **/
    public boolean exists(){
        checkProtocol();

        if(local){
            return new plugin_local(path,PanelID).exists();
        } else if (smb){
            return new plugin_SMB(path,PanelID).exists();
        } else if (sftp){
            return new plugin_SFTP(path,PanelID).exists();
        } else if (ftp){
            return new plugin_FTP(path,PanelID).exists();
        }
        return false;
    }
    public boolean is_exist_custom(String file, int PanID){
        checkProtocol();

        if(local){
            return new plugin_local(file,PanID).exists();
        } else if (smb){
            return new plugin_SMB(file,PanID).exists();
        } else if (sftp){
            return new plugin_SFTP(file,PanID).exists();
        } else if (ftp){
            return new plugin_FTP(file,PanID).exists();
        }
        return false;
    }
    /** IS DIR **/
    public boolean is_dir() {
        checkProtocol();

        if(local){
            return new plugin_local(path,PanelID).is_dir();
        } else if (smb){
            return new plugin_SMB(path,PanelID).is_dir();
        } else if (sftp){
            return new plugin_SFTP(path,PanelID).is_dir();
        } else if (ftp){
            return new plugin_FTP(path,PanelID).is_dir();
        }
        return false;
    }
    /** getDir list **/
    public String[] listFiles(){
        checkProtocol();

        if(local){
            return new plugin_local(path,PanelID).listFiles();
        } else if (smb){
            return new plugin_SMB(path,PanelID).listFiles();
        } else if (sftp){
            return new plugin_SFTP(path,PanelID).listFiles();
        } else if (ftp){
            return new plugin_FTP(path,PanelID).listFiles();
        }

        return new String[] {""};
    }
    /** STREAM WORKER **/
    public void IO_Stream_Worker(String src, String dest) throws IOException {
        InputStream in = new BufferedInputStream(new datFM_IO(src,PanelID).getInput());
        OutputStream out = new BufferedOutputStream(new datFM_IO(dest,CompetPanel).getOutput());

        byte[] buf = new byte[1024];
        int len;

        long fullsize = getFileSizeCustom(src,PanelID);
        long one_percent = fullsize/100;
        long cnt=0;
        int cur_file_progress=0;
        while ((len = in.read(buf)) > 0){
            out.write(buf, 0, len);
            cnt=cnt+(1024);
            if(cnt>one_percent){
                cur_file_progress=cur_file_progress+1;
                if(datFM_File_Operations.progr_overal!=null)datFM_File_Operations.progr_current.setProgress(cur_file_progress);
                updateCurrentBar(cur_file_progress,fullsize,cur_file_progress*one_percent);
                cnt=0;
            }
        }
        in.close();
        out.flush();
        out.close();

        /** Check FTP transaction **/
                 /*
        if(datFM.ftp_auth_transfer[PanelID]!=null)datFM.ftp_auth_transfer[PanelID].completePendingCommand();
        if(datFM.ftp_auth_transfer[CompetPanel]!=null)datFM.ftp_auth_transfer[CompetPanel].completePendingCommand();
                         */
               /***
        if(datFM.ftp_auth_transfer[PanelID]!=null)if(!datFM.ftp_auth_transfer[PanelID].completePendingCommand()){
            Log.e("datFM err:","FTP transaction error. SRC="+path+", DST="+dest);
        }else{Log.e("sec","sec");}

        if(datFM.ftp_auth_transfer[CompetPanel]!=null)if(!datFM.ftp_auth_transfer[CompetPanel].completePendingCommand()){
            Log.e("datFM err:","FTP transaction error. SRC="+path+", DST="+dest);
        }else{Log.e("sec","sec");}
        */
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
        checkProtocol();

        if(local){
            return new plugin_local(path,PanelID).getName();
        } else if(smb){
            return new plugin_SMB(path,PanelID).getName();
        } else if(sftp){
            return new plugin_SFTP(path,PanelID).getName();
        } else if(ftp){
            return new plugin_FTP(path,PanelID).getName();
        }

        return null;
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
        } else if(ftp){
            parent = new plugin_FTP(path,PanelID).getParent();
        } else {
            parent[0]=path.substring(0,path.lastIndexOf("/")+1);
            parent[1]="home";
            parent[2]=datFM.datFM_state.getResources().getString(R.string.fileslist_parent_directory);
        }

        return parent;
    }

    /** Protocol identifier **/
    public boolean checkProtocol(){
        local = path.startsWith("/");
        smb = path.startsWith("smb://");
        sftp = path.startsWith("sftp://");
        ftp = path.startsWith("ftp://");

        if(!local && !smb && !sftp && !ftp ){
            acceptable=false;
        } else {
            acceptable=true;
        }

        return acceptable;
    }

}
