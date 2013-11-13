package com.zlab.datFM;


import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class datFM_Properties extends Activity {

    /** UI **/
    EditText prop_name,prop_type,prop_path,prop_date;
    public static EditText prop_size,prop_md5sum,prop_md5sum_check;
    CheckBox prop_perm_owner_read,prop_perm_owner_write,prop_perm_owner_exec;
    CheckBox prop_perm_group_read,prop_perm_group_write,prop_perm_group_exec;
    CheckBox prop_perm_other_read,prop_perm_other_write,prop_perm_other_exec;
    Button prop_btn_apply;
    public static Button prop_btn_calc_md5;
    ImageView prop_icon_file;
    public static ImageView prop_icon_md5_check;
    static ProgressBar prop_size_progress,prop_md5_progress;
    Context prop_context;

    /** FLAG */

    /** Data **/
    ArrayList<datFM_File> paths;
    int count;
    String file;
    String ext,name,real_name,parent_dir,date,mimeType;
    boolean isDir;
    View.OnClickListener TextClick;
    TextWatcher md5Change;
    CompoundButton.OnCheckedChangeListener CheckedListener;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (!datFM.pref_theme.equals("Dark Fullscreen")){
            if(datFM.currentApiVersion < Build.VERSION_CODES.HONEYCOMB){
                setTheme(android.R.style.Theme_Light);
            } else {
                setTheme(android.R.style.Theme_Holo_Light);
            }
        }
        super.onCreate(savedInstanceState);

        if(datFM.isTablet(datFM.datFM_context)){
            setContentView(R.layout.datfm_properties_tablet);
        } else {
            setContentView(R.layout.datfm_properties_phones);
        }

        prop_context = this;

        /** EditText **/
        prop_name = (EditText) findViewById(R.id.prop_name);
        prop_type = (EditText) findViewById(R.id.prop_type);
        prop_path = (EditText) findViewById(R.id.prop_path);
        prop_size = (EditText) findViewById(R.id.prop_size);
        prop_date = (EditText) findViewById(R.id.prop_date);
        prop_md5sum = (EditText) findViewById(R.id.prop_md5sum);
        prop_md5sum_check = (EditText) findViewById(R.id.prop_md5sum_check);

        /** Not editable **/
        prop_name.setKeyListener(null);
        prop_type.setKeyListener(null);
        prop_path.setKeyListener(null);
        prop_size.setKeyListener(null);
        prop_date.setKeyListener(null);
        prop_md5sum.setKeyListener(null);

        /** **/
        TextClick = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText v = (EditText) view;
                ClipboardManager clipboard = (ClipboardManager)getApplicationContext().getSystemService(CLIPBOARD_SERVICE);
                clipboard.setText(v.getText().toString());
                Toast.makeText(getApplicationContext(),"Copied in clipboard!",Toast.LENGTH_SHORT).show();
            }
        };
        md5Change = new TextWatcher() {
            public void afterTextChanged(Editable arg0) {
                String input = arg0.toString();

                if(!prop_md5sum_check.getText().toString().equals("")){
                    if(prop_md5sum.getText().toString().equals(input)){
                        prop_icon_md5_check.setImageResource(android.R.drawable.checkbox_on_background);
                    } else {
                        prop_icon_md5_check.setImageResource(android.R.drawable.presence_busy);
                    }
                }
            }
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
        };
        prop_md5sum_check.addTextChangedListener(md5Change);

        prop_name.setOnClickListener(TextClick);
        prop_type.setOnClickListener(TextClick);
        prop_path.setOnClickListener(TextClick);
        prop_size.setOnClickListener(TextClick);
        prop_date.setOnClickListener(TextClick);
        prop_md5sum.setOnClickListener(TextClick);

        CheckedListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                prop_btn_apply.setEnabled(true);
            }
        };
        /** Owner permission **/
        prop_perm_owner_read = (CheckBox) findViewById(R.id.prop_perm_owner_read);
        prop_perm_owner_write = (CheckBox) findViewById(R.id.prop_perm_owner_write);
        prop_perm_owner_exec = (CheckBox) findViewById(R.id.prop_perm_owner_exec);
        /** Group permission **/
        prop_perm_group_read = (CheckBox) findViewById(R.id.prop_perm_group_read);
        prop_perm_group_write = (CheckBox) findViewById(R.id.prop_perm_group_write);
        prop_perm_group_exec = (CheckBox) findViewById(R.id.prop_perm_group_exec);
        /** Other permission **/
        prop_perm_other_read = (CheckBox) findViewById(R.id.prop_perm_other_read);
        prop_perm_other_write = (CheckBox) findViewById(R.id.prop_perm_other_write);
        prop_perm_other_exec = (CheckBox) findViewById(R.id.prop_perm_other_exec);

        /** Owner permission **/
        prop_perm_owner_read.setOnCheckedChangeListener(CheckedListener);
        prop_perm_owner_write.setOnCheckedChangeListener(CheckedListener);
        prop_perm_owner_exec.setOnCheckedChangeListener(CheckedListener);
        /** Group permission **/
        prop_perm_group_read.setOnCheckedChangeListener(CheckedListener);
        prop_perm_group_write.setOnCheckedChangeListener(CheckedListener);
        prop_perm_group_exec.setOnCheckedChangeListener(CheckedListener);
        /** Other permission **/
        prop_perm_other_read.setOnCheckedChangeListener(CheckedListener);
        prop_perm_other_write.setOnCheckedChangeListener(CheckedListener);
        prop_perm_other_exec.setOnCheckedChangeListener(CheckedListener);

        /** Buttons **/
        prop_btn_apply = (Button) findViewById(R.id.prop_btn_apply);
        prop_btn_calc_md5 = (Button) findViewById(R.id.prop_btn_calc_md5);

        /** Image **/
        prop_icon_file = (ImageView) findViewById(R.id.prop_icon_file);
        prop_icon_md5_check = (ImageView) findViewById(R.id.prop_icon_md5_check);
        prop_size_progress = (ProgressBar) findViewById(R.id.prop_size_progress);
        prop_md5_progress = (ProgressBar) findViewById(R.id.prop_md5_progress);
        /** Get VAR **/
        get_paths();

        /** Set VAR**/
        set_var();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    void get_paths(){
        paths = datFM.properties_array;
    }
    void get_file(){
        file=paths.get(0).getPath();
        name=paths.get(0).getName();
        if(paths.get(0).getType().equals("dir")){isDir=true;}

        /** Name and extension **/
        if(isDir){
            real_name = name;
            ext="directory";
        } else {
            if (name.lastIndexOf(".")!=-1){
                real_name = name.substring(0,name.lastIndexOf("."));
                ext = paths.get(0).getExt();
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            } else {
                real_name = name;
                ext = "unknown";
            }
        }

        /** Parent dir **/
        parent_dir = paths.get(0).getParent();

        /** Size **/
        new datFM_Properties_SizeCalc().execute(paths);

        /** Date **/
        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy',' H:mm");
        date = sdf.format(paths.get(0).getDate());

        /** Icon **/
        if(isDir){
            prop_icon_file.setImageResource(R.drawable.ext_folder_human_o2);
        } else {
            get_icon();
        }
    }
    void set_var(){
        count = paths.size();
        get_file();
        if (count==1){
            render_one();
        } else {
            render_many();
        }
    }
    void render_one(){
        prop_name.setText(name);
        prop_path.setText(parent_dir);
        prop_type.setText(ext + " (" + mimeType + ")");
        prop_size.setText("Calculating...");
        prop_date.setText(date);

        if(datFM.pref_root && file.startsWith("/")){
            String out = "";
            String command;
            if(isDir){
                command = "ls -ld \""+file+"\"\n";
            } else {
                command = "ls -al \""+file+"\"\n";
            }

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
            } catch (Exception e) {
                Log.e("datFM err: ","can't run root operation."+e.getMessage());
            }

            String permission = out.split(" ")[0];
            //drwxrwxrwx
            /** OWNER */

            if(permission.substring(1,2).equals("r")){
                prop_perm_owner_read.setChecked(true);
            }
            if(permission.substring(2,3).equals("w")){
                prop_perm_owner_write.setChecked(true);
            }
            if(permission.substring(3,4).equals("x")){
                prop_perm_owner_exec.setChecked(true);
            }
            /** GROUP */
            if(permission.substring(4,5).equals("r")){
                prop_perm_group_read.setChecked(true);
            }
            if(permission.substring(5,6).equals("w")){
                prop_perm_group_write.setChecked(true);
            }
            if(permission.substring(6,7).equals("x")){
                prop_perm_group_exec.setChecked(true);
            }
            /** OTHER **/
            if(permission.substring(7,8).equals("r")){
                prop_perm_other_read.setChecked(true);
            }
            if(permission.substring(8,9).equals("w")){
                prop_perm_other_write.setChecked(true);
            }
            if(permission.substring(9,10).equals("x")){
                prop_perm_other_exec.setChecked(true);
            }

        } else if (file.startsWith("/")){
            if (new File(file).canRead()){prop_perm_other_read.setChecked(true);}
            if (new File(file).canWrite()){prop_perm_other_write.setChecked(true);}
            if (new File(file).canExecute()){prop_perm_other_exec.setChecked(true);}
        } else {
            prop_btn_apply.setVisibility(View.GONE);
        }

    }
    void render_many(){
        prop_name.setText(count+" files.");
        prop_path.setText(parent_dir);
        prop_type.setText("Files group.");
        prop_size.setText("Calculating...");
        prop_date.setText("Files group.");
    }

    public int ext_check(String[] ext_list, String name, String ext){
        int resID=0;
        for (String find_ext : ext_list) {
            if (find_ext.equals(ext)) {
                resID = datFM.datFM_context.getResources().getIdentifier("ext_" + name.toLowerCase()+"_"+datFM.pref_theme_icons, "drawable", "com.zlab.datFM");
                break;
            }
        }
        return resID;
    }

    public void get_icon(){
        int resID = datFM.datFM_context.getResources().getIdentifier("ext_" + ext.toLowerCase()+"_"+datFM.pref_theme_icons, "drawable", "com.zlab.datFM");

        if (resID==0){
            String[] audio = {"mp3", "m4a", "aac", "ogg"};
            resID = ext_check(audio,"audio",ext);
        }
        if (resID==0){
            String[] video = {"mp4", "3gp", "mkv", "avi", "mpg", "mpeg", "flv", "vob"};
            resID = ext_check(video,"video",ext);
        }
        if (resID == 0) {
            String[] docx = {"doc", "docx", "rtf", "odt"};
            resID = ext_check(docx,"docx",ext);
        }
        if (resID==0){
            String[] xlsx = {"xlsx", "xls", "ods"};
            resID = ext_check(xlsx,"xlsx",ext);
        }
        if (resID==0){
            String[] fb2  = {"fb2", "djvu", "chm", "epub", "ibooks"};
            resID = ext_check(fb2,"fb2",ext);
        }
        if (resID==0){
            String[] zip  = {"zip", "7z", "rar", "bz", "tar", "gzip"};
            resID = ext_check(zip,"zip",ext);
        }
        if (resID==0){
            String[] ttf  = {"ttf", "otf"};
            resID = ext_check(ttf,"ttf",ext);
        }
        if (resID==0){
            String[] pptx = {"pptx", "ppt"};
            resID = ext_check(pptx,"pptx",ext);
        }
        if (resID==0){
            //resID = R.drawable.ext_unknown_human_o2;
            resID = getResources().getIdentifier("ext_unknown"+"_"+datFM.pref_theme_icons, "drawable", "com.zlab.datFM");
        }
        prop_icon_file.setImageResource(resID);
    }

    public  void btn_call_properties(View view) {
        switch (view.getId()) {
            case R.id.prop_btn_cancel: {
                finish();
                break;}
            case R.id.prop_btn_apply: {
                if(datFM.pref_root){
                    int perm_owner = 0;
                    int perm_group = 0;
                    int perm_other = 0;

                    /** Owner permission **/
                    if(prop_perm_owner_read.isChecked())perm_owner=perm_owner+4;
                    if(prop_perm_owner_write.isChecked())perm_owner=perm_owner+2;
                    if(prop_perm_owner_exec.isChecked())perm_owner=perm_owner+1;

                    /** Group permission **/
                    if(prop_perm_group_read.isChecked())perm_group=perm_group+4;
                    if(prop_perm_group_write.isChecked())perm_group=perm_group+2;
                    if(prop_perm_group_exec.isChecked())perm_group=perm_group+1;
                    /** Other permission **/
                    if(prop_perm_other_read.isChecked())perm_other=perm_other+4;
                    if(prop_perm_other_write.isChecked())perm_other=perm_other+2;
                    if(prop_perm_other_exec.isChecked())perm_other=perm_other+1;

                    String[] commands = {"chmod "+perm_owner+perm_group+perm_other+" \""+file+"\"\n"};
                    RunAsRoot(commands);
                    Toast.makeText(datFM.datFM_context,"Done!",Toast.LENGTH_SHORT).show();
                } else {
                    datFM.notify_toast("Need root permission",true);
                }
                break;}
            case R.id.prop_btn_calc_md5: {
                new datFM_Properties_MD5().execute(file);
                break;}
        }
    }
    private void RunAsRoot(String[] cmds){
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());

            for (String tmpCmd : cmds) {
                byte[] buf = tmpCmd.getBytes("UTF-8");
                os.write(buf,0,buf.length);
                os.writeBytes("echo \n");
            }
            os.writeBytes("exit\n");
            os.flush();
            try {
                p.waitFor();
                if (p.exitValue() != 255) {//("root");
                } else {/*("not root");*/}
            } catch (InterruptedException e) {/*("not root");*/}
        } catch (IOException e) {/*("not root");*/}
    }

}
