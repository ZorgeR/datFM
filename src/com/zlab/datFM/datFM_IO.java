package com.zlab.datFM;

import android.util.Log;
import jcifs.smb.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.UnknownHostException;

public class datFM_IO {

    /** Protocols **/
    boolean smb;
    boolean local;
    boolean acceptable;

    /** Globals **/
    String path;

    public datFM_IO(String file){
        path=file;
    }

    /** FILE **/
    public File getFileLocal(){
        File file = new File(path);
        return file;
    }
    public SmbFile getFileSmb() throws MalformedURLException {
        //---------START SMB WORKS-------------------------
        String user, pass;
        user = datFM.user;
        pass = datFM.pass;
        NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(datFM.domain, user, pass);
        SmbFile f = new SmbFile(path,auth);
        //---------END SMB WORKS-------------------------
        return f;
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
        boolean success;
        checkProtocol();

        if(local){
            success=copy_recursively_local(getFileLocal(),dest);
        } else if (smb){
            success=copy_recursively_smb(getFileSmb(),dest);
        } else {
            success=false;
        }

        return success;
    }
    public boolean copy_recursively_smb(SmbFile file,String dest) throws IOException {
        if (file.isDirectory()) {
            new datFM_IO(dest).mkdir();
            SmbFile[] children = file.listFiles();
            for (SmbFile ff : children) {
                copy_recursively_smb(ff, dest+"/"+ff.getName());}
        } else {
            IO_Stream_Worker(file.getPath(), dest);
        };
        return true;
    }
    public boolean copy_recursively_local(File file,String dest) throws IOException {
        if (file.isDirectory()) {
            new datFM_IO(dest).mkdir();
            File[] children = file.listFiles();
            for (File ff : children) {
                copy_recursively_local(ff, dest+"/"+ff.getName());}
        } else {
            IO_Stream_Worker(file.getPath(), dest);
        };
        return true;
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

    /** STREAM WORKER **/
    private boolean IO_Stream_Worker(String src, String dest) throws IOException {
        InputStream in = new datFM_IO(src).getInput();
        OutputStream out = new datFM_IO(dest).getOutput();

        byte[] buf = new byte[1024];
        int len;

        long one_percent = datFM_FileOperation.cur_f/100;
        long cnt=0;
        int cur_file_progress=0;
        while ((len = in.read(buf)) > 0){
            out.write(buf, 0, len);
            cnt=cnt+1024;
            if(cnt>one_percent){
                cur_file_progress=cur_file_progress+1;
                datFM_FileOperation.progr_current.setProgress(cur_file_progress);
                cnt=0;
            }
        }
        in.close();
        out.close();
        return true;
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

                    name_in_ui_parser=name_in_ui_parser.substring(name_in_ui_parser.indexOf("/"));

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
