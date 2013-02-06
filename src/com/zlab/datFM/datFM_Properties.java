package com.zlab.datFM;


import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.*;

import java.io.File;
import java.math.BigDecimal;
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

    /** FLAG */
    boolean PROP_CHANGED;

    /** Data **/
    ArrayList<String> paths;
    int count;
    File file;
    File[] files;
    String ext,name,real_name,parent_dir,date,mimeType;
    BigDecimal file_size;
    Uri uri;
    View.OnClickListener TextClick;
    TextWatcher md5Change;

    //public static datFM_Properties datFM_Properties_state;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.datfm_properties);
        //datFM_Properties_state = this;

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

    void get_paths(){
        paths = getIntent().getStringArrayListExtra("paths");
    }
    void get_file(){
        file=new File (paths.get(0));
        name=file.getName();

        /** Name and extension **/
        if(file.isDirectory()){
            real_name = name;
            ext="directory";
        } else {
            if (file.getName().lastIndexOf(".")!=-1){
                real_name = file.getName().substring(0,file.getName().lastIndexOf("."));
                ext = file.getName().substring(file.getName().lastIndexOf(".")+1);
                uri = Uri.fromFile(file);
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()));
            } else {
                real_name = name;
                ext = "unknown";
            }
        }

        /** Parent dir **/
        parent_dir = file.getParent();

        /** Size **/
        new datFM_Properties_Operation().execute(paths);/*
        BigDecimal size = new BigDecimal(file.length()/1024.00/1024.00);
        file_size = size.setScale(3, BigDecimal.ROUND_HALF_UP);*/

        /** Date **/
        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy',' H:mm");
        date = sdf.format(file.lastModified());

        /** Icon **/
        if(file.isDirectory()){
            prop_icon_file.setImageResource(R.drawable.ext_folder);
        } else {
        get_icon();
        }
    }
    void set_var(){
        count = paths.size();
        if (count==1){
            get_file();
            render_one();
        } else {
            get_file();
            render_many();
        }
    }
    void render_one(){
        prop_name.setText(name);
        prop_path.setText(parent_dir);
        prop_type.setText(ext + " (" + mimeType + ")");
        prop_size.setText("Calculating...");
        prop_date.setText(date);

        if (file.canRead()){prop_perm_other_read.setChecked(true);}
        if (file.canWrite()){prop_perm_other_write.setChecked(true);}
        if (file.canExecute()){prop_perm_other_exec.setChecked(true);}
    }
    void render_many(){
        prop_name.setText(count+" files.");
        prop_path.setText(parent_dir);
        prop_type.setText("Files group.");
        prop_size.setText("Calculating...");
        prop_date.setText("Files group.");
    }

    public int ext_check(String[] ext_list, String ext, String name){
        int resID=0;
        for (String find_ext : ext_list) {
            if (find_ext.equals(ext)) {
                resID = datFM.datf_context.getResources().getIdentifier("ext_" + name.toLowerCase(), "drawable", "com.zlab.datFM");
                break;
            }
        }
        return resID;
    }

    void get_icon(){
        int resID = datFM.datf_context.getResources().getIdentifier("ext_" + ext.toLowerCase(), "drawable", "com.zlab.datFM");
        if (resID == 0) {
            String[] docx = {"doc", "docx", "rtf", "odt"};
            resID = ext_check(docx,ext,"docx");

            if (resID==0){
                String[] xlsx = {"xlsx", "xls", "ods"};
                resID = ext_check(xlsx,ext,"xlsx");
            }
            if (resID==0){
                String[] fb2  = {"fb2", "djvu", "chm", "epub", "ibooks"};
                resID = ext_check(fb2,ext,"fb2");
            }
            if (resID==0){
                String[] zip  = {"zip", "7z", "rar"};
                resID = ext_check(zip,ext,"zip");
            }
            if (resID==0){
                String[] ttf  = {"ttf", "otf"};
                resID = ext_check(ttf,ext,"ttf");
            }
            if (resID==0){
                String[] pptx = {"pptx", "ppt"};
                resID = ext_check(pptx,ext,"pptx");
            }
            if (resID==0){
                resID = R.drawable.ext_unknown;
            }
        }
        prop_icon_file.setImageResource(resID);
    }

    /*
    public void recall_size(Long size_f){
        BigDecimal size_d = new BigDecimal(size_f/1024.00/1024.00);
        BigDecimal file_size_d = size_d.setScale(3, BigDecimal.ROUND_HALF_UP);
        datFM_Properties.datFM_Properties_state.prop_size.setText(String.valueOf(file_size_d));
    } */

    public  void btn_call_properties(View view) {
        switch (view.getId()) {
            case R.id.prop_btn_cancel: {
                finish();
                break;}
            case R.id.prop_btn_apply: {
                break;}
            case R.id.prop_btn_calc_md5: {
                new datFM_Properties_md5sum().execute(file);
                break;}
        }
    }
}
