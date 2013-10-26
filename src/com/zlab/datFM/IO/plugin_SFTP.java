package com.zlab.datFM.IO;

import android.util.Log;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.zlab.datFM.R;
import com.zlab.datFM.datFM;
import com.zlab.datFM.datFM_IO;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

public class plugin_SFTP {

    private String path;
    private int PanelID;
    private int CompetPanel;

    public plugin_SFTP(String file, int id){
        path=file;
        PanelID=id;
        CompetPanel=1;if(PanelID==1)CompetPanel=0;
    }

    public ChannelSftp getFile(){
        if(datFM.sftp_auth_channel[PanelID]==null){
            datFM.sftp_auth_channel[PanelID] = new ChannelSftp();
        }
        return datFM.sftp_auth_channel[PanelID];
    }
    public Vector lslist(String fpath) throws SftpException, JSchException {
        return getFile().ls(SFTPrealpath(fpath));
    }
    public ChannelSftp.LsEntry getSFTP_element(Vector list,int id) throws SftpException, JSchException {
        return (ChannelSftp.LsEntry) list.elementAt(id);
    }

    public Long getFileSize(){
        long size;
        try{
            size = getSFTP_element(lslist(path),0).getAttrs().getSize();
        } catch (Exception e){
            size=0;
            Log.e("datFM err: ", "Can't get file size - " + path);
        }
        return size;
    }

    /** Stream worker **/
    public InputStream getInput() {
        InputStream in;
        try {
            in = getFile().get(SFTPrealpath(path));
        } catch (SftpException e) {
            in = null;
            Log.e("datFM err: ","Can't get input - "+path);
        }

        return in;
    }
    public OutputStream getOutput() {
        OutputStream out;
        try {
            out = getFile().put(SFTPrealpath(path));
        } catch (SftpException e) {
            out = null;
            Log.e("datFM err: ","Can't get output - "+path);
        }
        return out;
    }

    /** Operation worker **/
    /** COPY **/
    public void copy(String dest) {
        if (is_dir()) {
            new datFM_IO(dest,CompetPanel).mkdir();
            Vector<ChannelSftp.LsEntry> children = null;
            try {
                children = getFile().ls(SFTPrealpath(path));
                for(ChannelSftp.LsEntry ff : children){
                    if(!ff.getFilename().equals(".") && !ff.getFilename().equals("..")){
                        new plugin_SFTP(path+"/"+ff.getFilename(),PanelID).copy(dest+"/"+ff.getFilename());
                    }
                }
            } catch (SftpException e) {
                Log.e("datFM err: ", "Error while copy - "+path);
            }
        } else {
            try {
                new datFM_IO(path,PanelID).IO_Stream_Worker(path, dest);
            } catch (Exception e){
                Log.e("datFM err: ","SFTP copy IO error - "+path+" to "+dest);
            }
        }
    }
    /** DELETE **/
    public boolean delete() {
        boolean success;
        try{
            success=delete_recursively(SFTPrealpath(path));
        } catch (Exception e){
            success = false;
            Log.e("datFM err: ","File not found - "+path);
        }
        return success;
    }
    public boolean delete_recursively(String fpath){
        try{
            if (getFile().lstat(fpath).isDir()){
                Vector<ChannelSftp.LsEntry> children = getFile().ls(fpath);
                for(ChannelSftp.LsEntry ff : children){
                    if(!ff.getFilename().equals(".") && !ff.getFilename().equals("..")){
                        delete_recursively(fpath + "/" + ff.getFilename());
                    }
                }
                getFile().rmdir(fpath);
            } else {
                getFile().rm(fpath);
            }

            SftpATTRS sftpATTRS;
            boolean deleted=false;
            try {
                sftpATTRS = getFile().lstat(fpath);
            } catch (Exception ex) {
                deleted = true;
            }
            return deleted;
        } catch (Exception e){
            Log.e("datFM err: ", "Error while delete - "+fpath);
            return false;
        }
    }

    /** RENAME **/
    public boolean rename(String new_name){
        try{
            getFile().rename(SFTPrealpath(path), SFTPrealpath(new_name));
            return new plugin_SFTP(new_name,PanelID).exists();
        } catch (Exception e){
            Log.e("datFM err: ", "Error while rename - "+path+" to "+new_name);
            return false;
        }
    }
    /** MKDIR **/
    public boolean mkdir(){
        try{
            getFile().mkdir(SFTPrealpath(path));
            return getFile().lstat(SFTPrealpath(path)).isDir();
        } catch (Exception e){
            Log.e("datFM err: ", "Error while mkdir - "+path);
            return false;
        }
    }
    /** EXISTS **/
    public boolean exists(){
        try{
            SftpATTRS sftpATTRS;
            sftpATTRS = getFile().lstat(SFTPrealpath(path));
            return true;
        } catch (Exception e){
            Log.e("datFM err: ", "Error while exists - "+path);
            return false;
        }
    }
    /** IS DIR **/
    public boolean is_dir() {
        try{
            return getFile().lstat(SFTPrealpath(path)).isDir();
        } catch (Exception e){
            Log.e("datFM err: ", "Error while is_dir - "+path);
            return false;
        }
    }
    /** getDir list **/
    public String[] listFiles(){
        String[] filepathlist;
        try {
            int count=0;
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
        return parent;
    }
    public String getPath(){
        try {
            return getFile().realpath(path);
        } catch (SftpException e) {
            Log.e("datFM err: ", "Can't get realpath.");
            return "";
        }
    }
    public String SFTPrealpath(String spath){
        String hostname = spath.replace("sftp://","");
        if(hostname.contains("/")){hostname = hostname.substring(0, hostname.indexOf("/"));}
        return spath.replace("sftp://"+hostname,"");
    }

}
