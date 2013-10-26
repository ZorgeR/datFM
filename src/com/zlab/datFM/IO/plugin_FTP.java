package com.zlab.datFM.IO;

import android.util.Log;
import com.enterprisedt.net.ftp.FTPFile;
import com.zlab.datFM.R;
import com.zlab.datFM.datFM;
import com.zlab.datFM.datFM_IO;

import java.io.InputStream;
import java.io.OutputStream;

public class plugin_FTP {

    private String path;
    private int PanelID;
    private int CompetPanel;

    public plugin_FTP(String file, int id){
        path=file;
        PanelID=id;
        CompetPanel=1;if(PanelID==1)CompetPanel=0;
    }

    public FTPFile getFile(){
        FTPFile file=null;
        try {
                                                                 /**
            datFM.ftp_auth_session[PanelID].setRemoteHost(datFM.ftp_auth_transfer[PanelID].getRemoteHost());
            datFM.ftp_auth_session[PanelID].setRemotePort(datFM.ftp_auth_transfer[PanelID].getRemotePort());
            datFM.ftp_auth_session[PanelID].user(datFM.ftp_auth_transfer[PanelID].getUserName());
            datFM.ftp_auth_session[PanelID].password(datFM.ftp_auth_transfer[PanelID].getPassword());
                                                                          **/
            file = datFM.ftp_auth_session[PanelID].fileDetails(FTPrealpath(path));
        } catch (Exception e) {
            Log.e("datFM err: ", "Can't get file - "+path);
        }
        return file;
    }

    public Long getFileSize(){
        try{
            return getFile().size();
        } catch (Exception e){
            Log.e("datFM err: ", "Can't get file size - " + path);
            return null;
        }
    }

    /** Stream worker **/
    public InputStream getInput() {
        InputStream in;
        try {
                in = datFM.ftp_auth_transfer[PanelID].downloadStream(FTPrealpath(path));
        } catch (Exception e) {
                in = null;
                Log.e("datFM err: ","File not found - "+path);
        }
        return in;
    }
    public OutputStream getOutput() {
        OutputStream out;
        try {
            out = datFM.ftp_auth_transfer[PanelID].uploadStream(FTPrealpath(path));
        } catch (Exception e) {
            out = null;
            Log.e("datFM err: ","File not found - "+path);
        }
        return out;
    }

    /** Operation worker **/
    /** COPY **/
    public void copy(String dest) {
        if (is_dir()) {
            new datFM_IO(dest,CompetPanel).mkdir();
            try {
                for (FTPFile ff : datFM.ftp_auth_transfer[PanelID].directoryList(FTPrealpath(path))) {
                    new plugin_FTP(path+"/"+ff.getName(),PanelID).copy(dest+"/"+ff.getName());}
            } catch (Exception e) {
                Log.e("datFM err: ", "While list - "+path);
            }
        } else {
            try {
                new datFM_IO(path,PanelID).IO_Stream_Worker(path, dest);
            } catch (Exception e){
                Log.e("datFM err: ","File copy IO error - "+path+" to "+dest);
            }
        }
    }
    /** DELETE **/
    public boolean delete() {
        boolean success;
        try{
            success=delete_recursively(path);
        } catch (Exception e){
            success = false;
            Log.e("datFM err: ","File not found - "+path);
        }
        return success;
    }
    public boolean delete_recursively(String sfile){
        if (new plugin_FTP(sfile,PanelID).is_dir()){
            try {
                for (FTPFile child : datFM.ftp_auth_transfer[PanelID].directoryList(FTPrealpath(sfile)))
                    delete_recursively(sfile+"/"+child.getName());
                datFM.ftp_auth_transfer[PanelID].deleteDirectory(FTPrealpath(sfile));
            } catch (Exception e) {
                Log.e("datFM err: ", "Can't list directory");
                return false;
            }
        } else {
            try{
                datFM.ftp_auth_transfer[PanelID].deleteFile(FTPrealpath(sfile));
                return true;
            } catch (Exception e){
                Log.e("datFM err: ", "Error while delete - "+sfile);
                return false;
            }
        }
        return true;
    }

    /** RENAME **/
    public boolean rename(String new_name){
        try {
            datFM.ftp_auth_transfer[PanelID].rename(FTPrealpath(path), FTPrealpath(new_name));
            return true;
        } catch (Exception e) {
            Log.e("datFM err: ", "IOException");
            return false;
        }
    }
    /** MKDIR **/
    public boolean mkdir(){
        try {
            datFM.ftp_auth_transfer[PanelID].createDirectory(FTPrealpath(path));
            return true;
        } catch (Exception e) {
            Log.e("datFM err: ", "can't mkdir - "+path);
            return false;
        }
    }
    /** EXISTS **/
    public boolean exists(){
        try {
            return datFM.ftp_auth_transfer[PanelID].exists(FTPrealpath(path));
        } catch (Exception e) {
            //Log.e("datFM err: ", "No exist.");
            return false;
        }
    }
    /** IS DIR **/
    public boolean is_dir() {
        return getFile().isDir();
    }
    /** getDir list **/
    public String[] listFiles(){
        String[] filepathlist;

        try {
            FTPFile[] ftpfilelist=getFile().listFiles();
            filepathlist = new String[ftpfilelist.length];
            for(int i=0;i<ftpfilelist.length;i++){
                filepathlist[i]=path+"/"+ftpfilelist[i].getName();
            }
        } catch (Exception e) {
            Log.e("datFM err: ", "Error while listing - "+path);
            filepathlist=new String[] {""};
        }

        return filepathlist;
    }

    /** GetName in UI thread **/
    public String getName(){
        if (path.lastIndexOf("/")+1==path.length())
        {path=path.substring(0,path.lastIndexOf("/"));}

        return path.substring(path.lastIndexOf("/")+1);
    }
    public String[] getParent(){
        String[] parent=new String[3];
        if(path.equals("ftp://")){
            parent[0]="datFM://ftp";
            parent[1]="parent_dir";
            parent[2]=datFM.datFM_state.getResources().getString(R.string.fileslist_parent_directory);
        } else {
            if (path.lastIndexOf("/")+1==path.length())
            {path=path.substring(0,path.lastIndexOf("/"));}

            parent[0]=path.substring(0, path.lastIndexOf("/") + 1);
            parent[1]="parent_dir";
            parent[2]=datFM.datFM_state.getResources().getString(R.string.fileslist_parent_directory);
        }
        return parent;
    }
    public String getPath(){
        try {
            return getFile().getPath();
        } catch (Exception e) {
            Log.e("datFM err: ", "Get dir error - "+path);
            return null;
        }
    }

    public String FTPrealpath(String spath){
        String hostname = spath.replace("ftp://","");
        if(hostname.contains("/")){hostname = hostname.substring(0, hostname.indexOf("/"));}
        return spath.replace("ftp://"+hostname,"");
    }

}
