package com.zlab.datFM;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import com.jcraft.jsch.*;
import com.zlab.datFM.IO.plugin_FTP;
import jcifs.UniAddress;
import jcifs.smb.*;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;

import java.io.*;
import java.math.BigDecimal;
import java.net.InetAddress;
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
                    logonScreenSMB();}
            } else if(protocol.equals("sftp")&& (!sftp_success_auth||!sftp_session_auth)){
                if(!sftp_session_auth){
                    Toast.makeText(datFM.datFM_context,"SFTP host error.", Toast.LENGTH_SHORT).show();
                } else if (!sftp_success_auth){
                    Toast.makeText(datFM.datFM_context,"SFTP logon error.", Toast.LENGTH_SHORT).show();
                }
                try {logonScreenSFTP();} catch (JSchException e) {e.printStackTrace();}
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

        if(datFM.ftp_auth_session[panel_ID]==null){
            datFM.ftp_auth_session[panel_ID] = new FTPClient();
        }

        // ------ CHECK FTP AUTH ------------ //

            hostname = url;
            if(!hostname.equals("ftp://")){
                hostname = hostname.replace("ftp://","");
                if(hostname.contains("/")){hostname = hostname.substring(0, hostname.indexOf("/"));}
            }

        /*
                FTPClientConfig config = new FTPClientConfig();
                datFM.ftp_auth_session[panel_ID].configure(config);
                           */

                InetAddress inetaddr=null;
                try {
                    inetaddr = InetAddress.getByName(hostname);
                } catch (UnknownHostException e) {
                    Log.e("datFM error: ", "FTP unknown hostname - "+hostname);
                }

                try {
                    datFM.ftp_auth_session[panel_ID].connect(inetaddr,21);
                } catch (IOException e) {
                    Log.e("datFM error: ", "Set connect hostname:port error - "+hostname);
                }
                    /** Passive Mode **/
                    datFM.ftp_auth_session[panel_ID].enterLocalPassiveMode();
                try {
                    datFM.ftp_auth_session[panel_ID].login("zorgftp","zorgftpAbc123");
                } catch (IOException e) {
                    ftp_success_auth=false;
                    Log.e("datFM error: ", "FTP login error.");
                }

                /** UTF 8 **/
                datFM.ftp_auth_session[panel_ID].setControlEncoding("UTF-8");
                datFM.ftp_auth_session[panel_ID].setAutodetectUTF8(true);


                try {
                    datFM.ftp_auth_session[panel_ID].setFileType(FTPClient.BINARY_FILE_TYPE);
                } catch (IOException e) {
                    Log.e("datFM error: ", "Can't set binary file transfer.");
                }


        //---------START FTP WORKS-------------------------
            FTPFile dir = new plugin_FTP(url,panel_ID).getFile();
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
                    FTPFile[] list = datFM.ftp_auth_session[panel_ID].listFiles(realpath);
                    for(FTPFile ff: list){
                        if(!datFM.pref_show_hide && ff.getName().startsWith(".")){} else {
                            if(ff.isDirectory()){
                                String data = datFM.datFM_context.getResources().getString(R.string.fileslist_directory);
                                Long date = ff.getTimestamp().getTimeInMillis();
                                dir_info.add(new datFM_File(ff.getName(),url+ff.getName(),0,"smb","dir",data, url, date));
                            } else {
                                String data = formatSize(ff.getSize());
                                Long date = ff.getTimestamp().getTimeInMillis();

                                if(datFM.pref_show_date){
                                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm',' d MMM");
                                    data = data+" / "+sdf.format(date);
                                }

                                fls_info.add(new datFM_File(ff.getName(),url+ff.getName(),ff.getSize(),"smb","file",data,url,date));
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
                try {
                    //session = datFM.sftp_auth_session[panel_ID].getSession("root",hostname);
                    datFM.sftp_session[panel_ID].connect();
                    Channel channel = datFM.sftp_session[panel_ID].openChannel( "sftp" );
                    channel.connect();
                    datFM.sftp_auth_channel[panel_ID] = (ChannelSftp) channel;
                } catch (JSchException e) {Log.e("SFTP:", e.getMessage());}
            }
            //if(sftpChannel!=null){
            //datFM.sftp_auth_channel[panel_ID]=datFM.sftp_auth_channel[panel_ID];//}
        }

        if(datFM.sftp_auth_channel[panel_ID].isConnected()){
            Log.e("SFTP WORK:", "SFTP connected!");
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
        } else if (section.equals("exit")){
            if (panel_ID ==0){datFM.parent_left="datFM://";datFM.curentLeftDir="datFM://";
            } else {
                datFM.parent_right="datFM://";datFM.curentRightDir="datFM://";}
            datFM.datFM_state.finish();
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
            datFM.curentRightDir=dir.getPath();}

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
        String command = "ls -al \""+path+"\"\n";
        String output=RunAsRoot(command);
        //int length=output.split("\n").length;

        for(String str : output.split("\n")){
            String[] arr = str.split("\\s+");
            //length = str.split("\\s+").length;

            String permission = arr[0];
            String file_type="";if(permission.length()>0){file_type=permission.substring(0,1);}
            String user = arr[1];
            String group = arr[2];

            boolean is_dir=file_type.equals("d");
            boolean is_file=file_type.equals("-");
            boolean is_unix_socket=file_type.equals("s");
            boolean is_link=file_type.equals("l");
            boolean is_pipe=file_type.equals("p");
            boolean is_ch_device=file_type.equals("c");
            boolean is_bl_device=file_type.equals("b");

            String name;
            String date;
            String time;
            long size=0;

            if(is_file){
                size = Long.parseLong(arr[3]);
                date = arr[4];
                time = arr[5];
                name = arr[6];
            } else {
                date = arr[3];
                time = arr[4];
                name = arr[5];}
            String path;

            if(is_link){
                path = arr[7];
                if(path.startsWith("..")){
                    path=d.getPath()+"/"+path;
                }
                String command2 = "ls -ld \""+path+"\"\n";
                String output2=RunAsRoot(command2);
                if(output2.startsWith("d")){
                    is_dir=true;
                } else {
                    is_file=true;
                }
            } else {
                path=d.getPath()+"/"+name;
            }

            if(!datFM.pref_show_hide && name.startsWith(".")){} else {
                if(is_dir){
                    String data = datFM.datFM_context.getResources().getString(R.string.fileslist_directory);
                    String compiled_date = date+" "+time;
                    DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    Date date_format = null;
                    long date_long=0;
                    try {
                        date_format = formatter.parse(compiled_date);
                        date_long = date_format.getTime();
                    } catch (ParseException e) {e.printStackTrace();}

                    dir_info.add(new datFM_File(name,path,size,"smb","dir",data, d.getPath(), date_long));
                } else {
                    String compiled_date = date+" "+time;
                    DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    Date date_format = null;
                    long date_long =0;
                    try {
                        date_format = formatter.parse(compiled_date);
                        date_long = date_format.getTime();
                    } catch (ParseException e) {e.printStackTrace();}

                    String data = formatSize(size);
                    if(datFM.pref_show_date){
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm',' d MMM");
                        data = data+" / "+sdf.format(date_long);
                    }

                    fls_info.add(new datFM_File(name,path,size,"smb","file",data,d.getPath(), date_long));
                }
            }
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
    private void logonScreenSFTP() throws JSchException {
        AlertDialog.Builder action_dialog = new AlertDialog.Builder(datFM.datFM_state);
        action_dialog.setTitle("SFTP logon");
        LayoutInflater inflater = datFM.datFM_state.getLayoutInflater();
        View layer = inflater.inflate(R.layout.datfm_sftp_logon,null);

        final EditText hostname_locale = (EditText) layer.findViewById(R.id.logon_domain);
        final EditText names   = (EditText) layer.findViewById(R.id.logon_name);
        final EditText passs   = (EditText) layer.findViewById(R.id.logon_pass);
        final EditText portt   = (EditText) layer.findViewById(R.id.logon_port);

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
                    hostname_locale.setText("");hostname_locale.setEnabled(false);
                    names.setText("");names.setEnabled(false);
                    passs.setText("");//passs.setEnabled(false);
                } else {
                    try {
                        hostname_locale.setText(datFM.sftp_auth_channel[panel_ID].getSession().getHost());
                        hostname_locale.setEnabled(true);
                        names.setText(datFM.sftp_auth_channel[panel_ID].getSession().getUserName());names.setEnabled(true);
                        //passs.setText(datFM.smb_auth_session[panel_ID].getPassword());passs.setEnabled(true);
                    } catch (JSchException e) {e.printStackTrace();}
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

                        datFM.sftp_auth_session[panel_ID] = new JSch();
                        String knownHostsFilename = "/sdcard/.ssh_known_hosts";
                        try {
                            datFM.sftp_auth_session[panel_ID].setKnownHosts( knownHostsFilename );
                        } catch (JSchException e) {e.printStackTrace();}

                        try {
                            datFM.sftp_session[panel_ID] = datFM.sftp_auth_session[panel_ID].getSession( user_n, hostname_n, Integer.parseInt(port_n)); /** CHECK IF NOT INTEGER **/
                        } catch (JSchException e) {
                            Log.e("SFTP:",e.getMessage());}

                        datFM.sftp_session[panel_ID].setPassword( pass_n );

                        java.util.Properties config = new java.util.Properties();
                        config.put("StrictHostKeyChecking", "no");
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
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "/system/bin/sh"});
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
                if (p.exitValue() != 255) {//("root");
                } else {/*("not root");*/}
            } catch (InterruptedException e) {/*("not root");*/}
        } catch (IOException e) {/*("not root");*/}
        return out;
    }
}