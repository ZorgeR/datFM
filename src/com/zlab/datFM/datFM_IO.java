package com.zlab.datFM;

import android.util.Log;
import com.jcraft.jsch.*;
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
    public File getFileLocal(){
        //File file = new File(path);
        return new File(path);
    }
    public SmbFile getFileSmb() throws MalformedURLException {
        //---------START SMB WORKS-------------------------
        NtlmPasswordAuthentication auth;
        if(datFM.smb_auth_session[PanelID]!=null){
            auth = datFM.smb_auth_session[PanelID];
        } else {
            auth = new NtlmPasswordAuthentication(null, null, null);
        }

        //SmbFile f = new SmbFile(path,smb_auth_session);
        //---------END SMB WORKS-------------------------
        return new SmbFile(path,auth);
    }
    public ChannelSftp getSFTPChannel() throws JSchException, SftpException {
        if(datFM.sftp_auth_channel[PanelID]!=null){
            //sftp_auth_session = datFM.sftp_auth_session[PanelID];
        } else {
            JSch sftp_auth_session;
            sftp_auth_session = new JSch();

        String knownHostsFilename = "/sdcard/.ssh_known_hosts";
        sftp_auth_session.setKnownHosts( knownHostsFilename );
        Session session = sftp_auth_session.getSession( "root", SFTPhostname(path) );
        {
            // "interactive" version
            // can selectively update specified known_hosts file
            // need to implement UserInfo interface
            // MyUserInfo is a swing implementation provided in
            //  examples/Sftp.java in the JSch dist
            //UserInfo ui = new MyUserInfo();
            //session.setUserInfo(ui);

            // OR non-interactive version. Relies in host key being in known-hosts file
            session.setPassword( "bnm3BNM" );
        }
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();
        Channel channel = session.openChannel( "sftp" );
        channel.connect();
        ChannelSftp sftpChannel = (ChannelSftp) channel;
            datFM.sftp_auth_channel[PanelID]=sftpChannel;
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
            size = getFileLocal().length();
        } else if (smb){try {
            size = getFileSmb().length();
            } catch (SmbException e) {e.printStackTrace();} catch (MalformedURLException e) {e.printStackTrace();}
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
            size = new datFM_IO(path,Panel).getFileLocal().length();
        } else if (smb){try {
            size = new datFM_IO(path,Panel).getFileSmb().length();
        } catch (SmbException e) {e.printStackTrace();} catch (MalformedURLException e) {e.printStackTrace();}
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
            in = new BufferedInputStream(new FileInputStream(path));
        } else if (smb){
            in = new BufferedInputStream(new SmbFileInputStream(getFileSmb()));
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
            out = new BufferedOutputStream(new FileOutputStream(path));
        } else if (smb){
            out = new BufferedOutputStream(new SmbFileOutputStream(getFileSmb()));
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
            success=delete_recursively_local(getFileLocal());
        } else if (smb){
            success=delete_recursively_smb(getFileSmb());
        } else if (sftp){
            success=delete_recursively_sftp(path);
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

        SftpATTRS sftpATTRS = null;Boolean deleted = false;
        try {     sftpATTRS = getSFTPChannel().lstat(fpath);
        } catch (Exception ex) { deleted = false; }

        return deleted;
    }

    /** COPY **/
    public boolean copy(String dest) throws IOException, SftpException, JSchException {
        checkProtocol();

        if(local){
            copy_recursively_local(getFileLocal(),dest);
        } else if (smb){
            copy_recursively_smb(getFileSmb(),dest);
        } else if (sftp){
            copy_recursively_sftp(path,dest);
        } else {
            return false;
        }

        return is_exist_custom(dest, CompetPanel);
    }
    private void copy_recursively_smb(SmbFile file,String dest) throws IOException {
        if (file.isDirectory()) {
            new datFM_IO(dest,CompetPanel).mkdir();

            updateOverallBar(datFM_File_Operations.progr_overal.getProgress() + 1,file.getPath(),dest);

            SmbFile[] children = file.listFiles();
            datFM_File_Operations.overalMax= datFM_File_Operations.progr_overal.getMax()+children.length;
            datFM_File_Operations.progr_overal.setMax(datFM_File_Operations.overalMax);
            for (SmbFile ff : children) {
                copy_recursively_smb(ff, dest+"/"+ff.getName());}
        } else {
            try {
                updateOverallBar(datFM_File_Operations.progr_overal.getProgress(), file.getPath(), dest);
                IO_Stream_Worker(file.getPath(), dest);
                updateOverallBar(datFM_File_Operations.progr_overal.getProgress() + 1, file.getPath(), dest);
            } catch (Exception e){}
        }
        //return is_exist_custom(dest,CompetPanel);
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
    private void copy_recursively_local(File file,String dest) throws IOException {
        if (file.isDirectory()) {
            new datFM_IO(dest,CompetPanel).mkdir();

            if(datFM_File_Operations.progr_overal!=null)updateOverallBar(datFM_File_Operations.progr_overal.getProgress() + 1, file.getPath(), dest);

            File[] children = file.listFiles();
            if(datFM_File_Operations.progr_overal!=null)datFM_File_Operations.overalMax= datFM_File_Operations.progr_overal.getMax()+children.length;
            if(datFM_File_Operations.progr_overal!=null)datFM_File_Operations.progr_overal.setMax(datFM_File_Operations.overalMax);
            for (File ff : children) {
                copy_recursively_local(ff, dest+"/"+ff.getName());}
        } else {
            try {
                if(datFM_File_Operations.progr_overal!=null)updateOverallBar(datFM_File_Operations.progr_overal.getProgress(), file.getPath(), dest);

                IO_Stream_Worker(file.getPath(), dest);

                if(datFM_File_Operations.progr_overal!=null)updateOverallBar(datFM_File_Operations.progr_overal.getProgress() + 1, file.getPath(), dest);
            } catch (Exception e){}
        }
        //return is_exist_custom(dest,CompetPanel);
    }

    /** RENAME **/
    public boolean rename(String new_name) throws IOException {
        boolean success;
        checkProtocol();

        if(local){
            success=rename_local(new_name);
        } else if (smb){
            success=rename_smb(new_name);
        } else {
            success=false;
        }

        return success;
    }
    public boolean rename_smb(String new_name) throws IOException {
        SmbFile dest = new datFM_IO(new_name,PanelID).getFileSmb();
        getFileSmb().renameTo(dest);
        return dest.exists();
    }
    public boolean rename_local(String new_name) throws IOException {
        return getFileLocal().renameTo(new File(new_name));
    }

    /** MKDIR **/
    public boolean mkdir(){
        boolean success=false;
        checkProtocol();
        if(local){
            getFileLocal().mkdir();
            success= getFileLocal().exists();
        } else if (smb){
            try {
                getFileSmb().mkdir();
                success = getFileSmb().exists();
            } catch (Exception e) {e.printStackTrace();success=false;Log.e("ERR",e.getMessage());}
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
    public boolean is_exist(){
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
    public boolean is_exist_custom(String file, int PanID){
        boolean success;
        boolean smb_ = file.startsWith("smb://");
        boolean local_ = file.startsWith("/");

        if(local_){
            success=new datFM_IO(file,PanID).getFileLocal().exists();
        } else if (smb_){
            try {
                success=new datFM_IO(file,PanID).getFileSmb().exists();
            } catch (Exception e) {success=false;}
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
            success= getFileLocal().isDirectory();
        } else if (smb){
            try {success=getFileSmb().isDirectory();
            } catch (Exception e) {success=false;}
        } else if (sftp){
            success = getSFTPChannel().lstat(SFTPrealpath(path)).isDir();
        } else {
            success=false;
        }
        return success;
    }
    /** getDir list **/
    public String[] get_dir_list(){
        String[] filepathlist;
        checkProtocol();

        if(local){
            File[] filelist;

            if (datFM.pref_root){
                    filelist = getFileLocal().listFiles();
                if(filelist==null){
                    filelist=root_get_content(getFileLocal());
                }
            } else {
                filelist = getFileLocal().listFiles();
            }

            if(filelist!=null){
                filepathlist = new String[filelist.length];
                for(int i=0;i<filelist.length;i++){
                    filepathlist[i]=filelist[i].getPath();
                }
            } else {
                return null;
            }
        } else if (smb){
            try {
                SmbFile[] smbfilelist=getFileSmb().listFiles();
                filepathlist = new String[smbfilelist.length];
                for(int i=0;i<smbfilelist.length;i++){
                    filepathlist[i]=smbfilelist[i].getPath();
                }
            } catch (Exception e) {filepathlist=new String[] {""};}
        } else {
            filepathlist=new String[] {""};
        }

        return filepathlist;
    }
    private File[] root_get_content(File d){
        File[] dirs;
        String out = "";
        String command = "ls \""+d.getPath()+"\"\n";

        Process p;
        try {
            p = Runtime.getRuntime().exec(new String[]{"su", "-c", "/system/bin/sh"});
            DataOutputStream stdin = new DataOutputStream(p.getOutputStream());
            byte[] buf = command.getBytes("UTF-8");
            stdin.write(buf,0,buf.length);

            stdin.writeBytes("echo \n");
            DataInputStream stdout = new DataInputStream(p.getInputStream());
            byte[] buffer = new byte[4096];
            int read;
            while(true){
                read = stdout.read(buffer);
                out += new String(buffer, 0, read);
                if(read<4096){
                    break;
                }
                // here is where you catch the error value
                //int len = out.length();
                //char suExitValue = out.charAt(len-2);
                //Toast.makeText(getApplicationContext(), String.valueOf(suExitValue), Toast.LENGTH_SHORT).show();
                //return0or1(Integer.valueOf(suExitValue), command); // 0 or 1 Method
                // end catching exit value
            }
            stdin.writeBytes("exit\n");
            stdin.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String[] dirs_array = out.split("\n");
        dirs=new File[dirs_array.length];
        for (int i=0;i<dirs_array.length;i++){
            dirs[i]=new File (d,dirs_array[i]);
        }

        return dirs;
    }

    /** STREAM WORKER **/
    private void IO_Stream_Worker(String src, String dest) throws IOException {
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
            name=getFileLocal().getName();
        } else if(smb){
            if (path.lastIndexOf("/")+1==path.length())
            {path=path.substring(0,path.lastIndexOf("/"));}

            name = path.substring(path.lastIndexOf("/")+1);
        } else if(sftp){
            if (path.lastIndexOf("/")+1==path.length())
            {path=path.substring(0,path.lastIndexOf("/"));}
            name = path.substring(path.lastIndexOf("/")+1);
        } else {
           name="";
        }

        return name;
    }
    public String getRealName(){
        String name;
        checkProtocol();

        if(local){
            name=getFileLocal().getName();
        } else if(smb){
            try {
                name = getFileSmb().getName();
            } catch (MalformedURLException e) {
                name="null";
            }
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
    public String getRealParent(){
        String parent;
        checkProtocol();

        if(local){
            parent=getFileLocal().getParent();
        } else if(smb){
            try {
                parent=getFileSmb().getParent();
            } catch (MalformedURLException e) {
                parent="null";
            }
        } else {
            parent="null";
        }

        return parent;
    }

    public String SFTPrealpath(String spath){
        String hostname = spath.replace("sftp://","");
        if(hostname.contains("/")){hostname = hostname.substring(0, hostname.indexOf("/"));}
        String rpath=spath.replace("sftp://"+hostname,"");
        return rpath;
    }
    public String SFTPhostname(String spath){
        String hostname = spath.replace("sftp://","");
        if(hostname.contains("/")){hostname = hostname.substring(0, hostname.indexOf("/"));}
        return hostname;
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
