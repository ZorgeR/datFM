package com.zlab.datFM;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import com.enterprisedt.net.ftp.*;
import com.jcraft.jsch.*;
import com.zlab.datFM.FilePicker.datFM_FilePicker;
import com.zlab.datFM.IO.plugin_FTP;
import jcifs.UniAddress;
import jcifs.smb.*;
import java.io.*;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


public class datFM_IO_Fetch extends AsyncTask<String, Void, List<datFM_File>> {

    List<datFM_File> dir_info,fls_info;
    ProgressDialog dialog_operation_remote;
    String path,protocol;
    int panel_ID;
    String url, hostname,user;
    boolean fetch_err=false;
    boolean smb_success_auth =true;
    boolean sftp_success_auth =true;
    boolean sftp_session_auth =true;
    boolean ftp_success_auth = true;
    boolean valid_url=true;
    boolean strict_host_key=true;
    //String sftp_session_pass=null;
    //KnownHosts KH_file;

    //public static Button pemfile;
    //public static LinearLayout SFTPkeypass;

    public datFM activity;
    public datFM_IO_Fetch(datFM a){activity = a;}

    protected void onPreExecute() {
        if(!datFM.protocols[datFM.curPanel].equals("local") &&
           !datFM.protocols[datFM.curPanel].equals("datfm")){
            dialog_operation_remote = new ProgressDialog(activity);
            dialog_operation_remote.setTitle(datFM.protocols[datFM.curPanel]);
            dialog_operation_remote.setMessage("Please wait...");
            dialog_operation_remote.setIndeterminate(false);
            dialog_operation_remote.show();
        }
        super.onPreExecute();
    }
    @Override
    protected List<datFM_File> doInBackground(String... paths) {
        path = paths[0];
        protocol = paths[1];
        panel_ID = Integer.parseInt(paths[2]);

        dir_info = new ArrayList<datFM_File>();
        fls_info = new ArrayList<datFM_File>();

        datFM.curPanel = panel_ID;
        if(panel_ID==0){
            datFM.competPanel=1;
        }else{
            datFM.competPanel=0;
        }

        if (protocol.equals("local")){
            fetch_local();
        } else if (protocol.equals("smb")){
            fetch_smb();
        } else if (protocol.equals("sftp")){
            try {
                fetch_sftp();
            } catch (Exception e) {if(e.getMessage()!=null)Log.e("SFTP ERROR:", e.getMessage());
            }
        } else if (protocol.equals("ftp")){
            fetch_ftp();
        } else if (protocol.equals("apps")){
            fetch_apps();
        } else if (protocol.equals("datfm")){
            fetch_datFM();
        } else {
            Log.e("UNSUPPORT PROTOCOL:", protocol);
        }

        return dir_info;
    }
    protected void onPostExecute(List<datFM_File> result) {
        super.onPostExecute(result);
        if(!datFM.protocols[panel_ID].equals("local") && dialog_operation_remote !=null){
            dialog_operation_remote.dismiss();
        }
            if(protocol.equals("smb") && (!smb_success_auth || fetch_err)){
                if(!smb_success_auth){datFM.notify_toast(datFM.datFM_state.getResources().getString(R.string.notify_logon_error),true);}
                if(fetch_err){datFM.notify_toast(datFM.datFM_state.getResources().getString(R.string.notify_connection_error),true);}
                if(datFM.pref_sambalogin){
                    datFM.datFM_state.fill_new("datFM://samba", panel_ID);
                    logonScreenSMB();
                }
            } else if(protocol.equals("sftp")&& (!sftp_success_auth||!sftp_session_auth)){
                if(!strict_host_key){
                    logonScreenSFTPStrict();
                } else {
                    if(!sftp_session_auth){
                        Toast.makeText(datFM.datFM_context,"SFTP host error.", Toast.LENGTH_SHORT).show();
                    } else if (!sftp_success_auth){
                        Toast.makeText(datFM.datFM_context,"SFTP logon error.", Toast.LENGTH_SHORT).show();
                    }
                    datFM.datFM_state.fill_new("datFM://sftp", panel_ID);
                    logonScreenSFTP();
                }
            } else if(protocol.equals("ftp") && !ftp_success_auth){
                    Toast.makeText(datFM.datFM_context,"FTP logon error.", Toast.LENGTH_SHORT).show();
                    datFM.datFM_state.fill_new("datFM://ftp", panel_ID);
                    logonScreenFTP();
            } else {
                    datFM.datFM_state.fill_panel(dir_info, fls_info, panel_ID);
            }
    }

    private void fetch_smb(){
        if(path.lastIndexOf("/")!=path.length()-1){path=path+"/";}
        url = path;
        datFM.url=url;

        if(datFM.smb_auth_session[panel_ID]==null){
            datFM.smb_auth_session[panel_ID] = new NtlmPasswordAuthentication(null, null, null);
        }

        // ------ CHECK SMB AUTH ------------ //
        if(datFM.pref_sambalogin){
            hostname = url;
            if(!hostname.equals("smb://")){
                hostname = hostname.replace("smb://","");
                if(hostname.contains("/")){hostname = hostname.substring(0, hostname.indexOf("/"));}
            }

            if(!hostname.equals("")){
                UniAddress uniaddress = null;
                try {
                        uniaddress = UniAddress.getByName(hostname);
                } catch (UnknownHostException e) {
                        valid_url=false;
                }
                if(valid_url){
                    try {
                        SmbSession.logon(uniaddress, datFM.smb_auth_session[panel_ID]);
                    } catch (SmbAuthException e ) {
                        smb_success_auth =false;
                    } catch(SmbException e ) {
                        fetch_err=true;
                    }
                } else {
                    smb_success_auth =false;
                }
            } else {
                valid_url=false;
                smb_success_auth =false;
            }
        }
        //---------START SMB WORKS-------------------------
            try {
                SmbFile dir = new SmbFile(url, datFM.smb_auth_session[panel_ID]);

                if (panel_ID ==0){
                    datFM.parent_left=dir.getParent();
                    if(dir.getPath().equals("smb:////")){
                        datFM.curentLeftDir=dir.getPath().substring(0,dir.getPath().length()-2);
                    } else {
                        datFM.curentLeftDir=dir.getPath().substring(0,dir.getPath().length()-1);
                    }
                } else {
                    datFM.parent_right=dir.getParent();
                    if(dir.getPath().equals("smb:////")){
                        datFM.curentRightDir=dir.getPath().substring(0, dir.getPath().length() - 2);
                    } else {
                        datFM.curentRightDir=dir.getPath().substring(0, dir.getPath().length() - 1);
                    }
                }

                if(dir!=null){
                    try{
                        for(SmbFile ff: dir.listFiles()){
                            if(!datFM.pref_show_hide && (ff.getName().endsWith("$/") || ff.getName().startsWith("."))){} else {
                                if(ff.isDirectory()){
                                    String data = datFM.datFM_context.getResources().getString(R.string.fileslist_directory);
                                    String name = ff.getName().substring(0,ff.getName().length()-1);
                                    Long date = ff.getLastModified();
                                    dir_info.add(new datFM_File(name,ff.getPath(),0,"smb","dir",data, ff.getParent(), date));
                                } else {
                                    String data = formatSize(ff.length());
                                    Long date = ff.getLastModified();

                                    if(datFM.pref_show_date){
                                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm',' d MMM");
                                        data = data+" / "+sdf.format(date);
                                    }

                                    fls_info.add(new datFM_File(ff.getName(),ff.getPath(),ff.length(),"smb","file",data,ff.getParent(),date));
                                }
                            }
                        }
                    } catch (Exception e) {
                        //fetch_err=true;
                    }
                }

                Collections.sort(dir_info);
                Collections.sort(fls_info);

                dir_info.addAll(fls_info);
                if(dir_info.size()>0){
                    smb_success_auth =true;
                    fetch_err=false;
                }

            } catch (MalformedURLException e) {
                fetch_err=true;
            }
        //-------END SMB WORKS---------------------
    }
    private void fetch_ftp(){
        if(path.lastIndexOf("/")!=path.length()-1){path=path+"/";}
        url = path;
        datFM.url=url;

        hostname = url;
        if(!hostname.equals("ftp://")){
            hostname = hostname.replace("ftp://","");
            if(hostname.contains("/")){hostname = hostname.substring(0, hostname.indexOf("/"));}
        }

        if(datFM.ftp_auth_transfer[panel_ID]==null){
            datFM.ftp_auth_transfer[panel_ID] = new FileTransferClient();
        }

        if((datFM.ftp_auth_transfer[panel_ID].getRemoteHost()!=null && !datFM.ftp_auth_transfer[panel_ID].getRemoteHost().isEmpty())){
            if(!datFM.ftp_auth_transfer[panel_ID].getRemoteHost().equals(hostname)){
                try {
                    datFM.ftp_auth_transfer[panel_ID].setRemoteHost(hostname);
                    //datFM.ftp_auth_transfer[panel_ID].setRemotePort(21);
                    //datFM.ftp_auth_transfer[panel_ID].setUserName("");
                    //datFM.ftp_auth_transfer[panel_ID].setPassword("");
                } catch (Exception e) {
                    ftp_success_auth=false;
                    Log.e("datFM err: ", "FTP configuration error: "+e.getMessage());
                }
            }
            if(!datFM.ftp_auth_transfer[panel_ID].isConnected()){
                try {
                    /** Passive Mode **/
                    datFM.ftp_auth_transfer[panel_ID].getAdvancedFTPSettings().setConnectMode(FTPConnectMode.PASV);
                    datFM.ftp_auth_transfer[panel_ID].setContentType(FTPTransferType.BINARY);

                    /** UTF 8 **/
                    datFM.ftp_auth_transfer[panel_ID].getAdvancedSettings().setControlEncoding("UTF-8");

                    //if(datFM.ftp_auth_transfer[panel_ID].isConnected())datFM.ftp_auth_transfer[panel_ID].disconnect();
                    datFM.ftp_auth_transfer[panel_ID].connect();
                } catch (Exception e) {
                    ftp_success_auth=false;
                    Log.e("datFM err: ", "FTP connect error: "+e.getMessage());
                }
            }
        }

        //---------START FTP WORKS-------------------------
        FTPFile dir = new plugin_FTP(url,panel_ID).getFile();

        if(!datFM.ftp_auth_transfer[panel_ID].isConnected()){
            ftp_success_auth=false;
        } else {

            if (panel_ID ==0){
                datFM.parent_left=new plugin_FTP(url,panel_ID).getParent()[0];
                datFM.curentLeftDir=url;
            } else {
                datFM.parent_right=new plugin_FTP(url,panel_ID).getParent()[0];
                datFM.curentRightDir=url;
            }

            if(dir!=null){
                try{
                    String realpath=url.replace("ftp://"+hostname,"");

                    FTPFile[] list;

                    if(datFM.pref_show_hide){
                        list = datFM.ftp_auth_transfer[panel_ID].directoryList("-a "+realpath);
                    } else {
                        list = datFM.ftp_auth_transfer[panel_ID].directoryList(realpath);
                    }

                    for(FTPFile ff: list){
                        if((!datFM.pref_show_hide && ff.getName().startsWith(".")) ||
                                (ff.getName().equals(".") || ff.getName().equals(".."))){} else {
                            if(ff.isDir()){
                                String data = datFM.datFM_context.getResources().getString(R.string.fileslist_directory);
                                Long date = ff.lastModified().getTime();
                                dir_info.add(new datFM_File(ff.getName(),url+ff.getName(),0,"smb","dir",data, url, date));
                            } else {
                                String data = formatSize(ff.size());
                                Long date = ff.lastModified().getTime();

                                if(datFM.pref_show_date){
                                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm',' d MMM");
                                    data = data+" / "+sdf.format(date);
                                }

                                fls_info.add(new datFM_File(ff.getName(),url+ff.getName(),ff.size(),"smb","file",data,url,date));
                            }
                        }
                    }
                } catch (Exception e) {
                    //fetch_err=true;
                }
            }

            Collections.sort(dir_info);
            Collections.sort(fls_info);

            dir_info.addAll(fls_info);
        }
    }
    private void fetch_sftp() {
        if(path.lastIndexOf("/")!=path.length()-1){path=path+"/";}

        url = path;
        datFM.url=url;

        hostname = url;
        if(!hostname.equals("sftp://")){
            hostname = hostname.replace("sftp://","");
            if(hostname.contains("/")){hostname = hostname.substring(0, hostname.indexOf("/"));}
        }

        if(datFM.sftp_auth_channel[panel_ID]==null){
            datFM.sftp_auth_channel[panel_ID]=new ChannelSftp();

            if(datFM.sftp_auth_session[panel_ID]==null){
                sftp_session_auth=false;
                //sftp_success_auth=false;
                Log.e("SFTP:","Null session.");
            } else {

                if(datFM.sftp_session[panel_ID].getConfig("StrictHostKeyChecking").equals("yes")){
                    try {
                        ///datFM.sftp_session[panel_ID].setConfig("PasswordAuthentication","yes");
                        datFM.sftp_session[panel_ID].setHostKeyRepository(datFM.sftp_auth_session[panel_ID].getHostKeyRepository());
                        //datFM.sftp_session[panel_ID].setHost(hostname);

                        datFM.sftp_session[panel_ID].connect();

                        datFM.sftp_session_pass[panel_ID] = null;
                        Channel channel = datFM.sftp_session[panel_ID].openChannel( "sftp" );
                        channel.connect();
                        datFM.sftp_auth_channel[panel_ID] = (ChannelSftp) channel;
                    } catch (Exception e) {
                        strict_host_key=false;
                        Log.e("SFTP:", e.getMessage());
                    }
                } else {
                    datFM.sftp_session_pass[panel_ID] = null;
                    try {
                        datFM.sftp_session[panel_ID].connect();
                        Channel channel = datFM.sftp_session[panel_ID].openChannel( "sftp" );
                        channel.connect();
                        datFM.sftp_auth_channel[panel_ID] = (ChannelSftp) channel;
                    } catch (Exception e) {
                        Log.e("SFTP:", e.getMessage());
                    }
                }
            }
            //if(sftpChannel!=null){
            //datFM.sftp_auth_channel[panel_ID]=datFM.sftp_auth_channel[panel_ID];//}
        }

        if(datFM.sftp_auth_channel[panel_ID].isConnected()){
            Log.v("SFTP WORK:", "SFTP connected!");
        } else {
            datFM.sftp_auth_channel[panel_ID]=null;
            sftp_success_auth=false;
            Log.e("SFTP ERROR:", "SFTP connection error!");
        }

        /** getSFTPfile.get("remote-file", "local-file" ); **/
// OR
        /** InputStream in = getSFTPfile.get( "remote-file" );**/

        String realpath=url.replace("sftp://"+hostname,"");
        // process inputstream as needed
        //---------START SMB WORKS-------------------------
        if(sftp_success_auth){
            if (panel_ID ==0){
                datFM.parent_left=new datFM_IO(url,panel_ID).getParent()[0];
                datFM.curentLeftDir=url;
            } else {
                datFM.parent_right=new datFM_IO(url,panel_ID).getParent()[0];
                datFM.curentRightDir=url;
            }

            try {
                datFM.sftp_auth_channel[panel_ID].cd(realpath);
                Vector<ChannelSftp.LsEntry> list = datFM.sftp_auth_channel[panel_ID].ls(realpath);
            /*
            for(ChannelSftp.LsEntry entry : list) {
                getSFTPfile.get(entry.getFilename(), realpath + entry.getFilename());
                jsch.addIdentity(privateKey);
        logger.info("rsa private key loaded: " + privateKey);
            }
            */

                for(ChannelSftp.LsEntry ff : list){
                    if((!datFM.pref_show_hide && ff.getFilename().startsWith(".")) ||
                            (ff.getFilename().equals(".") || ff.getFilename().equals(".."))){} else {

                        if(ff.getAttrs().isDir()){
                            String data = datFM.datFM_context.getResources().getString(R.string.fileslist_directory);
                            String name = ff.getFilename();
                            Long date = Long.parseLong(String.valueOf(ff.getAttrs().getMTime()));
                            dir_info.add(new datFM_File(name,url+ff.getFilename(),0,"sftp","dir",data, url, date));
                        } else {
                            String data = formatSize(ff.getAttrs().getSize());
                            Long date = Long.parseLong(String.valueOf(ff.getAttrs().getMTime()));

                            if(datFM.pref_show_date){
                                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm',' d MMM");
                                data = data+" / "+sdf.format(date);
                            }

                            fls_info.add(new datFM_File(ff.getFilename(),url+ff.getFilename(),ff.getAttrs().getSize(),"sftp","file",data,url,date));
                        }
                    }
                }
            } catch (SftpException e) {Log.e("SFTP:",e.getMessage());}


        }
            Collections.sort(dir_info);
            Collections.sort(fls_info);

            dir_info.addAll(fls_info);

        /***datFM.sftp_auth_channel[panel_ID].exit();
        //datFM.sftp_auth_channel[panel_ID].disconnect(); **/
        //-------END SMB WORKS---------------------
    }
    private void fetch_apps(){
        PackageManager pm = datFM.datFM_context.getPackageManager();

        for (ApplicationInfo app : pm.getInstalledApplications(0)) {
            File apk = new File(app.sourceDir);

            String data = formatSize(apk.length());
            Long date = apk.lastModified();

            if(datFM.pref_show_date){
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm',' d MMM");
                data = data+" / "+sdf.format(date);
            }

            fls_info.add(new datFM_File(app.packageName + ".apk",app.sourceDir,apk.length(),"local","file",data, apk.getParent(), date));
        }

        Collections.sort(fls_info);

        dir_info.addAll(fls_info);
    }
    private void fetch_datFM(){
        String section=path.replace("datFM://", "");

        if (panel_ID ==0){
            datFM.parent_left="datFM://";
            datFM.curentLeftDir=path;
        } else {
            datFM.parent_right="datFM://";
            datFM.curentRightDir=path;}

        if (section.contains("/")){
            section=section.substring(0,section.indexOf("/")-1);
        } else if(section.equals("")){
            section="home";
        }

        if(section.equals("home")){
            dir_info.add(new datFM_File(activity.getResources().getString(R.string.fileslist_memory_card), Environment.getExternalStorageDirectory().getPath(),0,"local","sdcard",
                    getAvailableExternalMemorySize()+" / "+getTotalExternalMemorySize(), "datFM://", 0));

            dir_info.add(new datFM_File(activity.getResources().getString(R.string.fileslist_root),"/",0,"local","root",
                    getAvailableInternalMemorySize()+" / "+getTotalInternalMemorySize(), "datFM://", 0));

            dir_info.add(new datFM_File(activity.getResources().getString(R.string.fileslist_favorites),"datFM://favorite",0,"local","favorite",
                    activity.getResources().getString(R.string.fileslist_favorites), "datFM://", 0));

            dir_info.add(new datFM_File(activity.getResources().getString(R.string.fileslist_samba),"datFM://samba",0,"smb","network",
                    activity.getResources().getString(R.string.fileslist_network), "datFM://", 0));

            dir_info.add(new datFM_File(activity.getResources().getString(R.string.fileslist_sftp),"datFM://sftp",0,"sftp","network",
                    activity.getResources().getString(R.string.fileslist_network), "datFM://", 0));

            dir_info.add(new datFM_File(activity.getResources().getString(R.string.fileslist_ftp),"datFM://ftp",0,"ftp","network",
                    activity.getResources().getString(R.string.fileslist_network), "datFM://", 0));

            dir_info.add(new datFM_File(activity.getResources().getString(R.string.fileslist_apps),"datFM://apps",0,"apps","root",
                    activity.getResources().getString(R.string.fileslist_action), "datFM://", 0));

            dir_info.add(new datFM_File(activity.getResources().getString(R.string.fileslist_exit),"datFM://exit",0,"smb","action_exit",
                    activity.getResources().getString(R.string.fileslist_action), "datFM://", 0));
        } else if(section.equals("favorite")){
            dir_info.add(new datFM_File(activity.getResources().getString(R.string.fileslist_add_favorite),"datFM://favorite/add",0,"local","add",
                    activity.getResources().getString(R.string.fileslist_favorites), "datFM://", 0));

            File dir = activity.getFilesDir();
            for(File ff : dir.listFiles()){
                if(ff.getName().startsWith("fav_data_")){
                    try {
                        FileInputStream fis = activity.openFileInput(ff.getName());
                        StringBuffer fileContent = new StringBuffer("");
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = fis.read(buffer)) != -1) {
                            fileContent.append(new String(buffer));
                        }

                        //String fav_name = fileContent.toString().split("\n")[0];
                        String fav_path = fileContent.toString().split("\n")[1];
                        String fav_type = fileContent.toString().split("\n")[2];
                        String fav_protocol = fileContent.toString().split("\n")[3];
                        String fav_size = fileContent.toString().split("\n")[4];
                        String fav_data = fileContent.toString().split("\n")[5];
                        String fav_bookmark_name = fileContent.toString().split("\n")[6];

                        if(fav_type.equals("dir")){
                            dir_info.add(new datFM_File(fav_bookmark_name,fav_path,Long.parseLong(fav_size),fav_protocol,"fav_bookmark_dir",fav_data, "datFM://favorite", 0));
                        } else if(fav_type.equals("file")){
                            dir_info.add(new datFM_File(fav_bookmark_name,fav_path,Long.parseLong(fav_size),fav_protocol,"fav_bookmark_file",fav_data, "datFM://favorite", 0));
                        }

                        fis.close();
                    } catch (Exception e) {e.printStackTrace();}
                }
            }

        } else if(section.equals("samba")){
            dir_info.add(new datFM_File(activity.getResources().getString(R.string.fileslist_add_samba),"datFM://samba/add",0,"smb","add",
                    activity.getResources().getString(R.string.fileslist_network), "datFM://samba", 0));
            dir_info.add(new datFM_File(activity.getResources().getString(R.string.fileslist_browse_samba),"smb://",0,"smb","network",
                    activity.getResources().getString(R.string.fileslist_network), "datFM://samba", 0));

            File dir = activity.getFilesDir();
            for(File ff : dir.listFiles()){
                if(ff.getName().startsWith("smb_data_")){
                    try {
                        FileInputStream fis = activity.openFileInput(ff.getName());
                        StringBuffer fileContent = new StringBuffer("");
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = fis.read(buffer)) != -1) {
                            fileContent.append(new String(buffer));
                        }

                        String server_name = fileContent.toString().split("\n")[0];
                        String server_ip_hostname = fileContent.toString().split("\n")[1];
                        String server_start_dir = fileContent.toString().split("\n")[2];
                        String server_user = fileContent.toString().split("\n")[3];

                        if(server_user.equals("")){
                            dir_info.add(new datFM_File(server_name,"smb://"+server_ip_hostname+"/"+server_start_dir,0,"smb","smb_store_network",server_ip_hostname, "datFM://samba", 0));
                        } else {
                            dir_info.add(new datFM_File(server_name,"smb://"+server_ip_hostname+"/"+server_start_dir,0,"smb","smb_store_network",server_user+"@"+server_ip_hostname, "datFM://samba", 0));
                        }

                        fis.close();
                    } catch (Exception e) {e.printStackTrace();}
                }
            }
        } else if(section.equals("sftp")){
            dir_info.add(new datFM_File(activity.getResources().getString(R.string.fileslist_add_sftp),"datFM://sftp/add",0,"sftp","add",
                    activity.getResources().getString(R.string.fileslist_network), "datFM://sftp", 0));

            File dir = activity.getFilesDir();
            for(File ff : dir.listFiles()){
                if(ff.getName().startsWith("sftp_data_")){
                    try {
                        FileInputStream fis = activity.openFileInput(ff.getName());
                        StringBuffer fileContent = new StringBuffer("");
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = fis.read(buffer)) != -1) {
                            fileContent.append(new String(buffer));
                        }

                        String server_name = fileContent.toString().split("\n")[0];
                        String server_ip_hostname = fileContent.toString().split("\n")[1];
                        String server_start_dir = fileContent.toString().split("\n")[2];
                        String server_user = fileContent.toString().split("\n")[3];
                        String server_port = fileContent.toString().split("\n")[5];

                        if(server_user.equals("")){
                            dir_info.add(new datFM_File(server_name,"sftp://"+server_user+"@"+server_ip_hostname+":"+server_port+"/"+server_start_dir,0,"sftp","sftp_store_network",server_ip_hostname+":"+server_port, "datFM://sftp", 0));
                        } else {
                            dir_info.add(new datFM_File(server_name,"sftp://"+server_user+"@"+server_ip_hostname+":"+server_port+"/"+server_start_dir,0,"sftp","sftp_store_network",server_user+"@"+server_ip_hostname+":"+server_port, "datFM://sftp", 0));
                        }

                        fis.close();
                    } catch (Exception e) {e.printStackTrace();}
                }
            }
        } else if(section.equals("ftp")){
            dir_info.add(new datFM_File(activity.getResources().getString(R.string.fileslist_add_ftp),"datFM://ftp/add",0,"ftp","add",
                    activity.getResources().getString(R.string.fileslist_network), "datFM://ftp", 0));

            dir_info.add(new datFM_File(datFM.ftpServerTitle,"datFM://ftp/run",0,"ftp","add",
                    activity.getResources().getString(R.string.fileslist_network), "datFM://ftp", 0));

            File dir = activity.getFilesDir();
            for(File ff : dir.listFiles()){
                if(ff.getName().startsWith("ftp_data_")){
                    try {
                        FileInputStream fis = activity.openFileInput(ff.getName());
                        StringBuffer fileContent = new StringBuffer("");
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = fis.read(buffer)) != -1) {
                            fileContent.append(new String(buffer));
                        }

                        String server_name = fileContent.toString().split("\n")[0];
                        String server_ip_hostname = fileContent.toString().split("\n")[1];
                        String server_start_dir = fileContent.toString().split("\n")[2];
                        String server_user = fileContent.toString().split("\n")[3];
                        String server_port = fileContent.toString().split("\n")[5];

                        if(server_user.equals("")){
                            dir_info.add(new datFM_File(server_name,"ftp://"+server_ip_hostname+"/"+server_start_dir,0,"ftp","ftp_store_network",server_ip_hostname+":"+server_port, "datFM://ftp", 0));
                        } else {
                            dir_info.add(new datFM_File(server_name,"ftp://"+server_ip_hostname+"/"+server_start_dir,0,"ftp","ftp_store_network",server_user+"@"+server_ip_hostname+":"+server_port, "datFM://ftp", 0));
                        }

                        fis.close();
                    } catch (Exception e) {e.printStackTrace();}
                }
            }
        } else if (section.equals("exit")){
            if (panel_ID ==0){datFM.parent_left="datFM://";datFM.curentLeftDir="datFM://";
            } else {
                datFM.parent_right="datFM://";datFM.curentRightDir="datFM://";}
                datFM.datFM_state.finish();
        } else if (section.equals("apps")){
                dir_info.add(new datFM_File(activity.getResources().getString(R.string.fileslist_apps_package_name),"apps://",0,"apps","root",
                        activity.getResources().getString(R.string.fileslist_action), "datFM://apps", 0));
        }

        //dir_info.add(new datFM_File(ff.getName(),ff.getPath(),0,"smb","dir",data, ff.getParent()));
    }
    private void fetch_local(){
        File dir = new File (path);
        File[] dir_listing;

        if (panel_ID ==0){
            datFM.parent_left=dir.getParent();
            datFM.curentLeftDir=dir.getPath();
        } else {
            datFM.parent_right=dir.getParent();
            datFM.curentRightDir=dir.getPath();
        }

        dir_listing = dir.listFiles();

        if(datFM.pref_root && dir_listing==null){
            fetch_local_root(dir);
        } else {
            try{
                for(File ff: dir_listing)
                {
                    if(!datFM.pref_show_hide && ff.getName().startsWith(".")){} else {
                        if(ff.isDirectory()){
                            String data = datFM.datFM_context.getResources().getString(R.string.fileslist_directory);
                            Long date = ff.lastModified();

                            dir_info.add(new datFM_File(ff.getName(),ff.getPath(),0,"smb","dir",data, ff.getParent(), date));
                        } else {

                            //BigDecimal size = new BigDecimal(ff.length()/1024.00/1024.00);
                            //size = size.setScale(2, BigDecimal.ROUND_HALF_UP);

                            String data = formatSize(ff.length());
                            Long date = ff.lastModified();

                            if(datFM.pref_show_date){
                                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm',' d MMM");
                                data = data+" / "+sdf.format(date);
                            }

                            fls_info.add(new datFM_File(ff.getName(),ff.getPath(),ff.length(),"smb","file",data,ff.getParent(), date));
                        }
                    }
                }
            }catch(Exception e){}
        }
        Collections.sort(dir_info);
        Collections.sort(fls_info);

        dir_info.addAll(fls_info);
        //Collections.sort(dir_info);
    }
    private void fetch_local_root(File d){
        String command = "ls -a -l \""+path+"\"\n";

        String output="";

        try {
            output=RunAsRoot(command);
        } catch (final Exception e) {
            datFM.datFM_state.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    datFM.notify_toast("RunAsRoot fault code: "+e.getMessage(),true);
                }
            });
        }
        //int length=output.split("\n").length;

        if (!output.equals("")){
        for(String str : output.split("\n")){
            try {
                String[] arr = str.split("\\s+");

                String permission = arr[0];
                String file_type = "";
                if (permission.length() > 0) {
                    file_type = permission.substring(0, 1);
                }
                String user = arr[1];
                String group = arr[2];

                boolean is_dir = file_type.equals("d");
                boolean is_file = file_type.equals("-");
                boolean is_unix_socket = file_type.equals("s");
                boolean is_link = file_type.equals("l");
                boolean is_pipe = file_type.equals("p");
                boolean is_ch_device = file_type.equals("c");
                boolean is_bl_device = file_type.equals("b");

                String name;
                String date;
                String time;
                long size = 0;

                if (is_file) {
                    size = Long.parseLong(arr[3]);
                    date = arr[4];
                    time = arr[5];
                    name = arr[6];
                } else {
                    date = arr[3];
                    time = arr[4];
                    name = arr[5];
                }
                String path;

                if (is_link) {
                    path = arr[7];
                    if (path.startsWith("..")) {
                        path = d.getPath() + "/" + path;
                    }
                    String command2 = "ls -ld \"" + path + "\"\n";
                    String output2 = RunAsRoot(command2);
                    if (output2.startsWith("d")) {
                        is_dir = true;
                    } else {
                        is_file = true;
                    }
                } else {
                    path = d.getPath() + "/" + name;
                }

                if (!datFM.pref_show_hide && name.startsWith(".")) {
                } else {
                    if (is_dir) {
                        String data = datFM.datFM_context.getResources().getString(R.string.fileslist_directory);
                        String compiled_date = date + " " + time;
                        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                        Date date_format = null;
                        long date_long = 0;
                        try {
                            date_format = formatter.parse(compiled_date);
                            date_long = date_format.getTime();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        dir_info.add(new datFM_File(name, path, size, "smb", "dir", data, d.getPath(), date_long));
                    } else {
                        String compiled_date = date + " " + time;
                        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                        Date date_format = null;
                        long date_long = 0;
                        try {
                            date_format = formatter.parse(compiled_date);
                            date_long = date_format.getTime();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        String data = formatSize(size);
                        if (datFM.pref_show_date) {
                            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm',' d MMM");
                            data = data + " / " + sdf.format(date_long);
                        }

                        fls_info.add(new datFM_File(name, path, size, "smb", "file", data, d.getPath(), date_long));
                    }
                }
            } catch (final Exception e) {
                datFM.datFM_state.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        datFM.notify_toast("Listing error code: "+e.getMessage(),true);
                    }
                });
            }
        }
        } else {
            datFM.datFM_state.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    datFM.notify_toast("No root permission.",true);
                }
            });
        }
    }



    private void logonScreenSMB(){
        AlertDialog.Builder action_dialog = new AlertDialog.Builder(datFM.datFM_state);
        action_dialog.setTitle("Samba logon");
        LayoutInflater inflater = datFM.datFM_state.getLayoutInflater();
        View layer = inflater.inflate(R.layout.datfm_smb_logon,null);

        final EditText domains = (EditText) layer.findViewById(R.id.logon_domain);
        final EditText names   = (EditText) layer.findViewById(R.id.logon_name);
        final EditText passs   = (EditText) layer.findViewById(R.id.logon_pass);

        if(datFM.smb_auth_session[panel_ID]!=null){
            domains.setText(datFM.smb_auth_session[panel_ID].getDomain());
            names.setText(datFM.smb_auth_session[panel_ID].getUsername());
            passs.setText(datFM.smb_auth_session[panel_ID].getPassword());
        }
        final CheckBox logon_guest = (CheckBox) layer.findViewById(R.id.logon_guest);
        logon_guest.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    domains.setText("");domains.setEnabled(false);
                    names.setText("");names.setEnabled(false);
                    passs.setText("");passs.setEnabled(false);
                } else {
                    domains.setText(datFM.smb_auth_session[panel_ID].getDomain());domains.setEnabled(true);
                    names.setText(datFM.smb_auth_session[panel_ID].getUsername());names.setEnabled(true);
                    passs.setText(datFM.smb_auth_session[panel_ID].getPassword());passs.setEnabled(true);
                }
            }
        });

        action_dialog.setView(layer);
        action_dialog.setPositiveButton(datFM.datFM_state.getResources().getString(R.string.ui_dialog_btn_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String domain_n,user_n,pass_n;
                        domain_n = domains.getText().toString();
                        user_n = names.getText().toString();
                        pass_n = passs.getText().toString();
                        //datFM.curPanel = Integer.parseInt(datFM.id);
                        datFM.curPanel=panel_ID;
                        if(panel_ID==0){
                            datFM.competPanel=1;
                        }else{
                            datFM.competPanel=0;
                        }

                        if(domain_n.equals("")){
                            domain_n = null;
                        }
                        if(user_n.equals("")){
                            user_n = null;
                        }
                        if(pass_n.equals("")){
                            pass_n = null;
                        }

                        datFM.smb_auth_session[panel_ID] = new NtlmPasswordAuthentication(domain_n,user_n,pass_n);

                        new datFM_IO_Fetch(datFM.datFM_state).execute(path, protocol, String.valueOf(panel_ID));
                    }
                });
        action_dialog.setNegativeButton(datFM.datFM_state.getResources().getString(R.string.ui_dialog_btn_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        AlertDialog AboutDialog = action_dialog.create();
        AboutDialog.show();
    }
    private void logonScreenSFTP(){
        AlertDialog.Builder action_dialog = new AlertDialog.Builder(datFM.datFM_state);
        action_dialog.setTitle("SFTP logon");
        LayoutInflater inflater = datFM.datFM_state.getLayoutInflater();
        View layer = inflater.inflate(R.layout.datfm_sftp_logon,null);


        final EditText hostname_locale = (EditText) layer.findViewById(R.id.logon_domain);
        final EditText names   = (EditText) layer.findViewById(R.id.logon_name);
        final EditText passs   = (EditText) layer.findViewById(R.id.logon_pass);
        final EditText portt   = (EditText) layer.findViewById(R.id.logon_port);
        datFM.pemfile   = (Button) layer.findViewById(R.id.btnSFTP_PEM);
        final EditText keypass = (EditText) layer.findViewById(R.id.txtSFTPkeypass);
        datFM.SFTPkeypass = (LinearLayout) layer.findViewById(R.id.linearSFTPkeypass);
        datFM.SFTPkeypass.setVisibility(View.GONE);
        final CheckBox StrictHostCheck = (CheckBox) layer.findViewById(R.id.checkSFTPstrictHost);

        if(hostname!=null){
            if(hostname.indexOf('@')<0){hostname="root@"+hostname;}

            if(hostname.indexOf(':')>0){
                hostname_locale.setText(hostname.substring(hostname.indexOf('@')+1,hostname.indexOf(':')));
                portt.setText(hostname.substring(hostname.indexOf(':')+1));
            } else {
                hostname_locale.setText(hostname.substring(hostname.indexOf('@')+1));
                portt.setText("22");
            }
            names.setText(hostname.substring(0, hostname.indexOf('@')));
        }


        final CheckBox logon_guest = (CheckBox) layer.findViewById(R.id.logon_guest);
        logon_guest.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    //hostname_locale.setText("");hostname_locale.setEnabled(false);
                    names.setText("");names.setEnabled(false);
                    passs.setText("");passs.setEnabled(false);
                } else {
                    try {
                        //hostname_locale.setText(datFM.sftp_auth_channel[panel_ID].getSession().getHost());
                        //hostname_locale.setEnabled(true);
                        //if(!datFM.sftp_auth_channel[panel_ID].getSession().getUserName().equals(null))
                          //  names.setText(datFM.sftp_auth_channel[panel_ID].getSession().getUserName());
                        //passs.setText("");
                        names.setEnabled(true);
                        passs.setEnabled(true);
                    } catch (Exception e) {e.printStackTrace();}
                }
            }
        });

        datFM.sftp_pem_file[panel_ID] = "";

        datFM.pemfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent fileChooserActivity = new Intent(datFM.datFM_state, datFM_FilePicker.class);
                fileChooserActivity.putExtra("panel_ID",String.valueOf(panel_ID));
                datFM.datFM_state.startActivity(fileChooserActivity);
            }
        });

        action_dialog.setView(layer);
        action_dialog.setPositiveButton(datFM.datFM_state.getResources().getString(R.string.ui_dialog_btn_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String hostname_n,user_n,pass_n,port_n;
                        hostname_n = hostname_locale.getText().toString();
                        user_n = names.getText().toString();
                        pass_n = passs.getText().toString();
                        port_n = portt.getText().toString();
                        //datFM.curPanel = Integer.parseInt(datFM.id);
                        datFM.curPanel=panel_ID;
                        if(panel_ID==0){
                            datFM.competPanel=1;
                        }else{
                            datFM.competPanel=0;
                        }
                        if(user_n.equals("")){
                            user_n = null;
                        }
                        if(pass_n.equals("")){
                            pass_n = null;
                        }

                        datFM.sftp_auth_session[panel_ID] = new JSch();
                        String knownHostsFilename = "/sdcard/.ssh_known_hosts";

                        try {
                            datFM.sftp_auth_session[panel_ID].setKnownHosts( knownHostsFilename );
                        } catch (JSchException e) {e.printStackTrace();}


                        datFM.sftp_session[panel_ID].setPassword( pass_n );

                        if(datFM.sftp_pem_file[panel_ID]!=null && !datFM.sftp_pem_file[panel_ID].equals("")){
                            try {
                                if(keypass.getText().toString()!=null && !keypass.getText().toString().isEmpty() && !keypass.getText().toString().equals("")){
                                    datFM.sftp_auth_session[panel_ID].addIdentity(datFM.sftp_pem_file[panel_ID],keypass.getText().toString());
                                } else {
                                    datFM.sftp_auth_session[panel_ID].addIdentity(datFM.sftp_pem_file[panel_ID]);
                                }
                            } catch (JSchException e) {
                                Log.e("datFM: ", "JSCH key error - "+e.getMessage());
                            }
                        }

                        try {
                            datFM.sftp_session[panel_ID] = datFM.sftp_auth_session[panel_ID].getSession( user_n, hostname_n, Integer.parseInt(port_n)); /** CHECK IF NOT INTEGER **/
                        } catch (JSchException e) {
                            Log.e("SFTP:",e.getMessage());
                        }

                        java.util.Properties config = new java.util.Properties();

                        if(!StrictHostCheck.isChecked()){
                            config.put("StrictHostKeyChecking", "no");
                        } else {
                            config.put("StrictHostKeyChecking", "yes");
                        }

                        datFM.sftp_session[panel_ID].setConfig(config);

                        datFM.sftp_auth_channel[panel_ID]=null;
                        new datFM_IO_Fetch(datFM.datFM_state).execute(path, protocol, String.valueOf(panel_ID));
                    }
                });
        action_dialog.setNegativeButton(datFM.datFM_state.getResources().getString(R.string.ui_dialog_btn_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        AlertDialog AboutDialog = action_dialog.create();
        AboutDialog.show();
    }
    private void logonScreenSFTPStrict(){
        AlertDialog.Builder action_dialog = new AlertDialog.Builder(datFM.datFM_state);
        action_dialog.setTitle(datFM.datFM_state.getResources().getString(R.string.ui_dialog_sftp_strict_check_alert));
        LayoutInflater inflater = datFM.datFM_state.getLayoutInflater();
        View layer = inflater.inflate(R.layout.datfm_strict_host,null);

        final TextView host = (TextView) layer.findViewById(R.id.txtHost);
        final TextView type   = (TextView) layer.findViewById(R.id.txtType);
        final TextView finger   = (TextView) layer.findViewById(R.id.txtFingerprint);

        final HostKey hk=datFM.sftp_session[panel_ID].getHostKey();

        host.setText("Host: "+hk.getHost());
        type.setText("Type: "+hk.getType());
        finger.setText("Fingerprint: "+hk.getFingerPrint(datFM.sftp_auth_session[panel_ID]));

        action_dialog.setView(layer);
        action_dialog.setPositiveButton(datFM.datFM_state.getResources().getString(R.string.ui_dialog_btn_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        String knownHostsFilename = "/sdcard/.ssh_known_hosts";
                        String DATA = hk.getHost()+" "+hk.getType()+" "+hk.getKey()+"\n";

                        try {
                            StringBuffer fileContent = new StringBuffer("");

                            if(new File(knownHostsFilename).exists()){
                                FileInputStream fis = new FileInputStream(knownHostsFilename);

                                byte[] buffer = new byte[1024];
                                int length;
                                while ((length = fis.read(buffer)) != -1) {
                                    fileContent.append(new String(buffer));
                                }
                                fis.close();
                            }

                            FileOutputStream fos;
                            fos = new FileOutputStream(knownHostsFilename);
                            DATA = DATA+fileContent.toString();
                            fos.write(DATA.getBytes());
                            fos.close();
                        } catch (Exception e) {
                            Log.e("datFM err: ", e.getMessage());
                        }

                        datFM.sftp_auth_channel[panel_ID]=null;

                        try {
                            datFM.sftp_auth_session[panel_ID].setKnownHosts( knownHostsFilename );
                        } catch (JSchException e) {e.printStackTrace();}

                        try {
                            String StrictHost = datFM.sftp_session[panel_ID].getConfig("StrictHostKeyChecking");
                            String pass       = datFM.sftp_session_pass[panel_ID];
                            String username   = datFM.sftp_session[panel_ID].getUserName();
                            String hostname   = datFM.sftp_session[panel_ID].getHost();
                            int port          = datFM.sftp_session[panel_ID].getPort();

                            //String pass       = datFM.sftp_session_pass[panel_ID];

                            datFM.sftp_session[panel_ID] = datFM.sftp_auth_session[panel_ID].getSession(username,hostname,port);
                            datFM.sftp_session[panel_ID].setConfig("StrictHostKeyChecking",StrictHost);

                            if(pass!=null){
                            datFM.sftp_session[panel_ID].setPassword(pass);datFM.sftp_session_pass[panel_ID]=null;}

                        } catch (Exception e) {
                            Log.e("ERR", "MSG");
                        }

                        new datFM_IO_Fetch(datFM.datFM_state).execute(path, protocol, String.valueOf(panel_ID));
                    }
                });
        action_dialog.setNegativeButton(datFM.datFM_state.getResources().getString(R.string.ui_dialog_btn_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        AlertDialog AboutDialog = action_dialog.create();
        AboutDialog.show();
    }
    private void logonScreenFTP(){
        AlertDialog.Builder action_dialog = new AlertDialog.Builder(datFM.datFM_state);
        action_dialog.setTitle("FTP logon");
        LayoutInflater inflater = datFM.datFM_state.getLayoutInflater();
        View layer = inflater.inflate(R.layout.datfm_ftp_logon,null);

        final EditText hostname_locale = (EditText) layer.findViewById(R.id.logon_domain);
        final EditText names   = (EditText) layer.findViewById(R.id.logon_name);
        final EditText passs   = (EditText) layer.findViewById(R.id.logon_pass);
        final EditText portt   = (EditText) layer.findViewById(R.id.logon_port);
        portt.setHint("21");


        if(datFM.ftp_auth_transfer[panel_ID].getRemoteHost()!=null){
            hostname_locale.setText(datFM.ftp_auth_transfer[panel_ID].getRemoteHost());
        } else {
            if(hostname!=null){
                hostname = hostname.replace("ftp://","");
                if(hostname.contains("/")){hostname = hostname.substring(0, hostname.indexOf("/"));}
                hostname_locale.setText(hostname);
            }
        }
        if(datFM.ftp_auth_transfer[panel_ID].getUserName()!=null){
            names.setText(datFM.ftp_auth_transfer[panel_ID].getUserName());}
        if(datFM.ftp_auth_transfer[panel_ID].getPassword()!=null){
            passs.setText(datFM.ftp_auth_transfer[panel_ID].getPassword());}
        if(datFM.ftp_auth_transfer[panel_ID].getRemotePort()!=0){
            portt.setText(String.valueOf(datFM.ftp_auth_transfer[panel_ID].getRemotePort()));}


        final CheckBox logon_guest = (CheckBox) layer.findViewById(R.id.logon_guest);
        logon_guest.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    //hostname_locale.setText("");hostname_locale.setEnabled(false);
                    names.setText("anonymous");names.setEnabled(false);
                    passs.setText("anonymous@gmail.com");passs.setEnabled(false);
                } else {
                        //hostname_locale.setText(datFM.sftp_auth_channel[panel_ID].getSession().getHost());
                        //hostname_locale.setEnabled(true);
                        if(datFM.ftp_auth_transfer[panel_ID].getUserName()!=null)names.setText(datFM.ftp_auth_transfer[panel_ID].getUserName());
                        if(datFM.ftp_auth_transfer[panel_ID].getPassword()!=null)passs.setText(datFM.ftp_auth_transfer[panel_ID].getPassword());
                        //if(datFM.ftp_auth_transfer[panel_ID].getRemotePort()!=0)portt.setText(datFM.ftp_auth_transfer[panel_ID].getRemotePort());
                        names.setEnabled(true);
                        passs.setEnabled(true);
                }
            }
        });

        action_dialog.setView(layer);
        action_dialog.setPositiveButton(datFM.datFM_state.getResources().getString(R.string.ui_dialog_btn_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String hostname_n,user_n,pass_n,port_n;
                        hostname_n = hostname_locale.getText().toString();
                        user_n = names.getText().toString();
                        pass_n = passs.getText().toString();
                        port_n = portt.getText().toString();
                        //datFM.curPanel = Integer.parseInt(datFM.id);
                        datFM.curPanel=panel_ID;
                        if(panel_ID==0){
                            datFM.competPanel=1;
                        }else{
                            datFM.competPanel=0;
                        }
                        if(user_n.equals("")){
                            user_n = null;
                        }
                        if(pass_n.equals("")){
                            pass_n = null;
                        }

                        datFM.ftp_auth_transfer[panel_ID] = new FileTransferClient();

                        try {
                            datFM.ftp_auth_transfer[panel_ID].setRemoteHost(hostname_n);
                            datFM.ftp_auth_transfer[panel_ID].setRemotePort(Integer.parseInt(port_n));
                            datFM.ftp_auth_transfer[panel_ID].setUserName(user_n);
                            datFM.ftp_auth_transfer[panel_ID].setPassword(pass_n);
                        } catch (Exception e) {
                            Log.e("FTP:",e.getMessage());}

                        new datFM_IO_Fetch(datFM.datFM_state).execute(path, protocol, String.valueOf(panel_ID));
                    }
                });
        action_dialog.setNegativeButton(datFM.datFM_state.getResources().getString(R.string.ui_dialog_btn_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        AlertDialog AboutDialog = action_dialog.create();
        AboutDialog.show();
    }
    public static boolean externalMemoryAvailable() {
        return android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
    }
    public static String getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return formatSize(availableBlocks * blockSize);
    }
    public static String getTotalInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        return formatSize(totalBlocks * blockSize);
    }
    public static String getAvailableExternalMemorySize() {
        if (externalMemoryAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSize();
            long availableBlocks = stat.getAvailableBlocks();
            return formatSize(availableBlocks * blockSize);
        } else {
            return "ERROR";
        }
    }
    public static String getTotalExternalMemorySize() {
        if (externalMemoryAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSize();
            long totalBlocks = stat.getBlockCount();
            return formatSize(totalBlocks * blockSize);
        } else {
            return "ERROR";
        }
    }

    public static String formatSize(long size) {
        String suffix = " Bytes";
        BigDecimal size_comma=new BigDecimal(size);

        if (size >= 1024) {
            suffix = " KiB";
            size_comma=new BigDecimal(size/1024.00);
            size /= 1024;
            if (size >= 1024) {
                suffix = " MiB";
                size_comma=new BigDecimal(size/1024.00);
                size /= 1024;
                if (size >= 1024) {
                    suffix = " GiB";
                    size_comma=new BigDecimal(size/1024.00);
                    size /= 1024;
                }
            }
        }

        size_comma = size_comma.setScale(1, BigDecimal.ROUND_HALF_UP);
        StringBuilder resultBuffer = new StringBuilder(String.valueOf(size_comma));

        if (suffix != null) resultBuffer.append(suffix);
        return resultBuffer.toString();
    }
    private String RunAsRoot(String command){
        String out = "";
        try {
            final Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "/system/bin/sh"});
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
            }
            stdin.writeBytes("exit\n");
            stdin.flush();
            try {
                p.waitFor();
                if (p.exitValue() != 255) {
                    //("root");
                } else {
                    final int error_code = p.exitValue();
                    datFM.datFM_state.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            datFM.notify_toast("Root stream exit code = "+error_code,true);
                        }
                    });
                }
            } catch (InterruptedException e) {
                final int error_code = p.exitValue();
                datFM.datFM_state.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        datFM.notify_toast("Root operation exit code = "+error_code,true);
                    }
                });
            }
        } catch (IOException e) {
            datFM.datFM_state.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    datFM.notify_toast("Root operation general fault.",true);
                }
            });
        }
        return out;
    }
}