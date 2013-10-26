package com.zlab.datFM.IO;

import android.util.Log;
import com.zlab.datFM.R;
import com.zlab.datFM.datFM;
import com.zlab.datFM.datFM_IO;
import jcifs.smb.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.UnknownHostException;

public class plugin_SMB {
    private String path;
    private int PanelID;
    private int CompetPanel;

    public plugin_SMB(String file, int id){
        path=file;
        PanelID=id;
        CompetPanel=1;if(PanelID==1)CompetPanel=0;
    }

    public SmbFile getFile(){
        if(datFM.smb_auth_session[PanelID]==null){
            datFM.smb_auth_session[PanelID] = new NtlmPasswordAuthentication(null, null, null);
        }
        SmbFile smbfile;
        try {
            smbfile = new SmbFile(path,datFM.smb_auth_session[PanelID]);
        } catch (MalformedURLException e) {
            smbfile = null;
            Log.e("datFM err: ", "Can't connect to file "+path);
        }
        return smbfile;
    }

    public Long getFileSize(){
        long size;
        try{
            size = getFile().length();
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
                in = new SmbFileInputStream(getFile());
            } catch (SmbException e) {
                in = null;
                Log.e("datFM err: ","SmbException - "+path);
            } catch (MalformedURLException e) {
                in = null;
                Log.e("datFM err: ","MalformedURLException - "+path);
            } catch (UnknownHostException e) {
                in = null;
                Log.e("datFM err: ","UnknownHostException - "+path);
            }

        return in;
    }
    public OutputStream getOutput() {
        OutputStream out;
            try {
                out = new SmbFileOutputStream(getFile());
            } catch (SmbException e) {
                out = null;
                Log.e("datFM err: ","SmbException - "+path);
            } catch (MalformedURLException e) {
                out = null;
                Log.e("datFM err: ","MalformedURLException - "+path);
            } catch (UnknownHostException e) {
                out = null;
                Log.e("datFM err: ","UnknownHostException - "+path);
            }
        return out;
    }

    /** Operation worker **/
    /** COPY **/
    public void copy(String dest) {
        if (is_dir()) {
            new datFM_IO(dest,CompetPanel).mkdir();
            try {
                for (SmbFile ff : getFile().listFiles()) {
                    new plugin_SMB(ff.getPath(),PanelID).copy(dest+"/"+ff.getName());}
            } catch (SmbException e) {
                Log.e("datFM err: ", "Can't do listing of "+path);
            }
        } else {
            try {
                new datFM_IO(path,PanelID).IO_Stream_Worker(path, dest);
            } catch (Exception e){
                Log.e("datFM err: ","SmbFile copy IO error - "+path+" to "+dest);
            }
        }
    }
    /** DELETE **/
    public boolean delete() {
        boolean success;
        try{
            success=delete_recursively(getFile());
        } catch (Exception e){
            success = false;
            Log.e("datFM err: ","File not found - "+path);
        }
        return success;
    }
    public boolean delete_recursively(SmbFile f){
        try{
            if (f.isDirectory())
                for (SmbFile child : f.listFiles())
                    delete_recursively(child);
            f.delete();
            return !f.exists();
        } catch (Exception e){
            Log.e("datFM err: ", "Error while delete - "+f.getPath());
            return false;
        }
    }

    /** RENAME **/
    public boolean rename(String new_name){
        try{
            getFile().renameTo(new plugin_SMB(new_name,PanelID).getFile());
            return new plugin_SMB(new_name,PanelID).exists();
        } catch (Exception e){
            Log.e("datFM err: ", "Error while rename - "+path+" to "+new_name);
            return false;
        }
    }
    /** MKDIR **/
    public boolean mkdir(){
        try{
            getFile().mkdir();
            return getFile().isDirectory();
        } catch (Exception e){
            Log.e("datFM err: ", "Error while mkdir - "+path);
            return false;
        }
    }
    /** EXISTS **/
    public boolean exists(){
        try{
            return getFile().exists();
        } catch (Exception e){
            Log.e("datFM err: ", "Error while exists - "+path);
            return false;
        }
    }
    /** IS DIR **/
    public boolean is_dir() {
        try{
            return getFile().isDirectory();
        } catch (Exception e){
            Log.e("datFM err: ", "Error while is_dir - "+path);
            return false;
        }
    }
    /** getDir list **/
    public String[] listFiles(){
        String[] filepathlist;

        try {
            SmbFile[] smbfilelist=getFile().listFiles();
            filepathlist = new String[smbfilelist.length];
            for(int i=0;i<smbfilelist.length;i++){
                filepathlist[i]=smbfilelist[i].getPath();
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

        return path.substring(path.lastIndexOf("/") + 1);
    }
    public String[] getParent(){
        String[] parent=new String[3];
        if(path.equals("smb://")){
            parent[0]="datFM://samba";
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
        return getFile().getPath();
    }

}
