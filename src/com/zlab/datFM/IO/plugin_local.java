package com.zlab.datFM.IO;

import android.util.Log;
import com.zlab.datFM.R;
import com.zlab.datFM.datFM;
import com.zlab.datFM.datFM_IO;

import java.io.*;

public class plugin_local {
    private String path;
    private int PanelID;
    private int CompetPanel;

    public plugin_local(String file, int id){
        path=file;
        PanelID=id;
        CompetPanel=1;if(PanelID==1)CompetPanel=0;
    }

    public File getFile(){
        return new File(path);
    }

    public Long getFileSize(){
        long size;
        try{
            size = getFile().length();
        } catch (Exception e){
            size=0;
            Log.e("datFM err: ","Can't get file size - "+path);
        }
        return size;
    }

    /** Stream worker **/
    public BufferedInputStream getInput() {
        BufferedInputStream in;
        try {
            in = new BufferedInputStream(new FileInputStream(path));
        } catch (FileNotFoundException e) {
            in = null;
            Log.e("datFM err: ","File not found - "+path);
        }
        return in;
    }
    public BufferedOutputStream getOutput() {
        BufferedOutputStream out;
        try {
            out = new BufferedOutputStream(new FileOutputStream(path));
        } catch (FileNotFoundException e) {
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
            for (File ff : getFile().listFiles()) {
                new plugin_local(ff.getPath(),PanelID).copy(dest+"/"+ff.getName());}
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
            success=delete_recursively(getFile());
        } catch (Exception e){
            success = false;
            Log.e("datFM err: ","File not found - "+path);
        }
        return success;
    }
    public boolean delete_recursively(File f){
        if (f.isDirectory())
            for (File child : f.listFiles())
                delete_recursively(child);
        try{
            f.delete();
        } catch (Exception e){
            Log.e("datFM err: ", "Error while delete - "+f.getAbsolutePath());
        }
        return !f.exists();
    }

    /** RENAME **/
    public boolean rename(String new_name){
        return getFile().renameTo(new File(new_name));
    }
    /** MKDIR **/
    public boolean mkdir(){
        getFile().mkdir();
        return getFile().isDirectory();
    }
    /** EXISTS **/
    public boolean exists(){
        return getFile().exists();
    }
    /** IS DIR **/
    public boolean is_dir() {
        return getFile().isDirectory();
    }
    /** getDir list **/
    public String[] listFiles(){
        String[] filepathlist;
        File[] filelist;

            if (datFM.pref_root){
                filelist = getFile().listFiles();
                if(filelist==null){
                    filelist=root_get_content(getFile());
                }
            } else {
                filelist = getFile().listFiles();
            }

            if(filelist!=null){
                filepathlist = new String[filelist.length];
                for(int i=0;i<filelist.length;i++){
                    filepathlist[i]=filelist[i].getPath();
                }
            } else {
                return null;
            }

        return filepathlist;
    }

    /** GetName in UI thread **/
    public String getName(){
        return getFile().getName();
    }
    public String[] getParent(){
        String[] parent=new String[3];
            if(path.equals("/")){
                parent[0]="datFM://";
                parent[1]="home";
                parent[2]=datFM.datFM_state.getResources().getString(R.string.fileslist_parent_directory);
            } else {
                parent[0]=getFile().getParent();
                parent[1]="parent_dir";
                parent[2]=datFM.datFM_state.getResources().getString(R.string.fileslist_parent_directory);
            }
        return parent;
    }
    public String getPath(){
        return getFile().getPath();
    }

    /** ROOT **/
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
}
