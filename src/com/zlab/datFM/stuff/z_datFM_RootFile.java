package com.zlab.datFM.stuff;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

public class z_datFM_RootFile extends File  {

    private String path;
    private String name;
    private String parent;
    private String permission;
    private Long lenght;

    public z_datFM_RootFile(String dirPath, String name) {
        super(dirPath, name);
        path=dirPath+"/"+name;

        getInfo();
    }

    public void getInfo(){
        String command = "ls -ld \""+path+"\"\n";

        String[] outs=RunAsRoot(command).split(" ");
        String[] out_fixed=new String[outs.length];
        int cout=0;

        for(int i=0;i<outs.length;i++){
            if(!outs[i].equals("")){
                out_fixed[cout]=outs[i];
                cout++;
            }
        }

        parent=getParent();
        permission = out_fixed[0];
        if(permission.startsWith("-")){
            lenght = Long.parseLong(out_fixed[3]);
        }
    }

    @Override
    public long length() {
        return lenght;
    }

    @Override
    public boolean isDirectory() {
        return permission.startsWith("d");
    }

    @Override
    public boolean isAbsolute() {
        return path.length() > 0 && path.charAt(0) == separatorChar;
    }

    @Override
    public String getParent() {
        //String parent = new datFM_IO("r:/"+path,datFM.curPanel).getParent()[0];
        return parent.replace("r:/","");
    }

    private String RunAsRoot(String command){
        String out = new String();
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
