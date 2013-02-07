package com.zlab.datFM;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.*;

import java.io.*;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import jcifs.UniAddress;
import jcifs.smb.*;

import static com.zlab.datFM.datFM_ZA_Interface.*;

public class datFM extends Activity {

    /** VAR GLOBAL **/
    LinearLayout layoutPathPanelLeft, layoutPathPanelRight,layoutButtonPanel;
    static ListView listLeft,listRight;
    TextView textPanelRight,textPanelLeft,textItemsRightSelected,textItemsLeftSelected;
    EditText textCurrentPathLeft, textCurrentPathRight;
    String curentLeftDir,curentRightDir;
    Button btnShare,btnAddFolder,btnAddToArchive,btnCopy,btnCut,btnSelectAll,btnDeselectAll,btnDelete,btnRename;
    boolean[] selectedRight,selectedLeft;
    static int curPanel,competPanel;
    int selLeft=0;
    int selRight=0;
    String protocol;

    /** VARS FOR OPERATION**/
    static int sel;
    boolean[] selected;
    static datFM_Adaptor adapter;
    static String destDir,curDir;
    TextView itemsSelected,textPanel;

    /** ASYNC **/
    public static datFM datFM_state;
    public static Context datf_context;

    /** VARS HOLDER FOR TAB **/
    private File dirLeft,dirRight;
    protected datFM_Adaptor adapterLeft,adapterRight;
    protected datFM_Protocol_Adaptor adapterLeft_Protocol,adapterRight_Protocol;

    /** VARS PREFS **/
    public SharedPreferences prefs;
    boolean pref_show_panel,pref_open_dir,pref_open_arcdir,
            pref_open_arcdir_window,pref_save_path,pref_dir_focus,
            pref_kamikaze,pref_show_text_on_panel,pref_show_navbar,
            pref_show_panel_discr;
    static boolean pref_show_apk,pref_show_video,pref_show_photo,
                   pref_show_folder_discr,pref_show_files_discr,pref_root;
    boolean firstAlert;
    boolean settings_opened = false;
    String pref_icons_cache;

    /** ICON CACHE **/
    static int cache_size;
    static int cache_counter=0;
    static boolean scroll=false;
    public static Drawable[] cache_icons;
    public static String[] cache_paths;

    /** ZArchiver **/
    datFM_ZA_Interface ZA;

    /** UI **/
    //static int x_pos,y_pos;

    /** SMB **/


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.datfm);
        datf_context = this;
        datFM_state = ((datFM) datFM.datf_context);

        /** Инициализация UI **/
        init_UI();
        init_Listener();

        /** Инициализация настроек **/
        pref_getter();
        pref_setter();

        /** Инициализация каталогов **/
        dirLeft = new File(curentLeftDir);
        dirRight = new File(curentRightDir);
        fill_local(dirLeft, 0);
        fill_local(dirRight, 1);

        /** Интерфейс для ZArchiver **/
        ZA = new datFM_ZA_Interface(datf_context);
    }
    protected void onResume() {
        super.onResume();
        if(settings_opened){
            pref_setter();
            settings_opened = false;}
    }
    protected void onStop(){
        prefs.edit().putString("lastLeftDir", curentLeftDir).commit();
        prefs.edit().putString("lastRightDir", curentRightDir).commit();
        super.onStop();
    }
    protected void onDestroy(){
        if (pref_root){
            String[] commands = {"mount -o ro,remount /system\n"};
            RunAsRoot(commands);
        }
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // check new orientation, use new layout
        // setContentView(R.layout.activity_main_screen);
        // Checks the orientation of the screen

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            int size_in_dp = 14;
            final float scale = getResources().getDisplayMetrics().density;
            int size_in_px = (int) (size_in_dp * scale + 0.5f);
            //
            pref_btn_text_size(size_in_px);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            int size_in_dp = 8;
            final float scale = getResources().getDisplayMetrics().density;
            int size_in_px = (int) (size_in_dp * scale + 0.5f);
            //
            pref_btn_text_size(size_in_px);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        //if (selLeft==0 && selRight==0){
        getMenuInflater().inflate(R.menu.datfm_mainmenu, menu);
        //} else {
        //    getMenuInflater().inflate(R.menu.datfm_actionmenu, menu);
        //}

        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mainmenu_add: {
                update_operation_vars();
                action_new_folder();
                return true;
            }
            case R.id.mainmenu_about: {
                AlertDialog.Builder action_dialog = new AlertDialog.Builder(this);
                action_dialog.setTitle("About");
                LayoutInflater inflater = getLayoutInflater();
                View layer = inflater.inflate(R.layout.datfm_about,null);
                action_dialog.setView(layer);
                AlertDialog AboutDialog = action_dialog.create();
                AboutDialog.show();
                return true;
            }
            case R.id.mainmenu_cloud: {
                Toast.makeText(getApplicationContext(),"In development.",Toast.LENGTH_SHORT).show();
                return true;
            }
            case R.id.mainmenu_fav_downloads: {
                if (curPanel ==0){
                    fill_local(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), 0);
                } else {
                    fill_local(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), 1);
                }
                return true;
            }
            case R.id.mainmenu_fav_dcim: {
                if (curPanel ==0){
                    fill_local(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), 0);
                } else {
                    fill_local(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), 1);
                }
                return true;
            }
            case R.id.mainmenu_find: {
                Toast.makeText(getApplicationContext(),"In development.",Toast.LENGTH_SHORT).show();
                return true;
            }
            case R.id.mainmenu_help: {
                Toast.makeText(getApplicationContext(),"In development.",Toast.LENGTH_SHORT).show();
                return true;
            }
            case R.id.mainmenu_sort: {
                Toast.makeText(getApplicationContext(),"In development.",Toast.LENGTH_SHORT).show();
                return true;
            }
            case R.id.mainmenu_update: {
                update_tab(0,"null","null",2);
                return true;
            }
            case R.id.mainmenu_settings: {
                settings_opened = true;
                Intent settingsActivity = new Intent(getBaseContext(),
                        com.zlab.datFM.datFM_Preferences.class);
                startActivity(settingsActivity);
                return true;
            }
            case R.id.mainmenu_exit: {
                finish();
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private File[] root_get_content(File d){
        File[] dirs;
        String out = new String();
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
    private ArrayList<Uri> root_build_content(File dir){
        ArrayList<Uri> uri_list = new ArrayList<Uri>();
        if (dir.isDirectory()) {
            for (File child : dir.listFiles()) {
                uri_list.addAll(root_build_content(child));
            }
        } else {
            uri_list.add(Uri.fromFile(dir));
        }
        return uri_list;
    }
    protected void fill_local(File f, int lr){
        File[] dirs;
        if (pref_root){
            dirs = f.listFiles();
            if(dirs==null){
            dirs = root_get_content(f);}
        } else {
            dirs = f.listFiles();
        }

        curPanel = lr;
        if(lr==0){competPanel=1;}else{competPanel=0;}


        //if(!f.getName().equalsIgnoreCase("")){
            this.setTitle(f.getPath());      /** Можно убрать **/
            if (curPanel ==0){
                curentLeftDir=f.getPath();
            } else {
                curentRightDir=f.getPath();}
        //}

        List<datFM_Information> dir = new ArrayList<datFM_Information>();
        List<datFM_Information> fls = new ArrayList<datFM_Information>();
        try{
            for(File ff: dirs)
            {
                if(ff.isDirectory())
                    dir.add(new datFM_Information(ff.getName(),getResources().getString(R.string.fileslist_directory),ff.getAbsolutePath()));
                else
                {
                    BigDecimal size = new BigDecimal(ff.length()/1024.00/1024.00);
                    size = size.setScale(2, BigDecimal.ROUND_HALF_UP);

                    fls.add(new datFM_Information(ff.getName(),"size: "+size+" MiB",ff.getAbsolutePath()));
                }
            }
        }catch(Exception e)
        {

        }
        Collections.sort(dir);
        Collections.sort(fls);

        dir.addAll(fls);
        if(/*!f.getName().equalsIgnoreCase("sdcard") && */!f.getName().equalsIgnoreCase(""))
            dir.add(0,new datFM_Information("..",getResources().getString(R.string.fileslist_parent_directory),f.getParent()));

        if (curPanel ==0){
            selectedLeft = new boolean[dir.size()];
            adapterLeft = new datFM_Adaptor(datFM.this,R.layout.datfm_list,dir,selectedLeft);
            listLeft.setAdapter(adapterLeft);
            textPanelLeft.setText(
                    getResources().getString(R.string.fileslist_folders_count)+(dir.size()-fls.size()-1)+", "+
                    getResources().getString(R.string.fileslist_files_count)+fls.size());
            textCurrentPathLeft.setText(curentLeftDir);
            update_panel_focus();
        } else {
            selectedRight = new boolean[dir.size()];
            adapterRight = new datFM_Adaptor(datFM.this,R.layout.datfm_list,dir,selectedRight);
            listRight.setAdapter(adapterRight);
            textPanelRight.setText(
                            getResources().getString(R.string.fileslist_folders_count)+(dir.size()-fls.size()-1)+", "+
                            getResources().getString(R.string.fileslist_files_count)+fls.size());
            textCurrentPathRight.setText(curentRightDir);
            update_panel_focus();
        }
    }
    protected void fill_smb(List<datFM_File> dir, List<datFM_File> fls,int panel_ID){

        curPanel = panel_ID;
        if(panel_ID==0){competPanel=1;}else{competPanel=0;}

        //if(!smbdir.getName().equalsIgnoreCase(""))
            //dir.add(0,new datFM_Information("..",getResources().getString(R.string.fileslist_parent_directory),f.getParent()));

        if (curPanel==0){
            selectedLeft = new boolean[dir.size()];
            adapterLeft_Protocol = new datFM_Protocol_Adaptor(datFM.this,R.layout.datfm_list,dir,selectedLeft);
            listLeft.setAdapter(adapterLeft);
            textPanelLeft.setText(
                    getResources().getString(R.string.fileslist_folders_count)+(dir.size()-fls.size()-1)+", "+
                            getResources().getString(R.string.fileslist_files_count)+fls.size());
            textCurrentPathLeft.setText(curentLeftDir);
            update_panel_focus();
        } else {
            selectedRight = new boolean[dir.size()];
            adapterRight_Protocol = new datFM_Protocol_Adaptor(datFM.this,R.layout.datfm_list,dir,selectedRight);
            listRight.setAdapter(adapterRight);
            textPanelRight.setText(
                    getResources().getString(R.string.fileslist_folders_count)+(dir.size()-fls.size()-1)+", "+
                            getResources().getString(R.string.fileslist_files_count)+fls.size());
            textCurrentPathRight.setText(curentRightDir);
            update_panel_focus();
        }
    }

    protected void fill_panel(List<datFM_Information> dir, List<datFM_Information> fls,int lr){

    }

    protected void openContextMenu(final int pos, int id){
        if(id==listLeft.getId()){curPanel=0;}else{curPanel=1;}
        update_operation_vars();
        String open,open_with,properties;
        open = getResources().getString(R.string.contextmenu_open);
        open_with = getResources().getString(R.string.contextmenu_open_with);
        properties = getResources().getString(R.string.contextmenu_properties);

        CharSequence[] items = {open, open_with, properties};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if(item == 0) {
                    if(sel==0)onFileClick(adapter.getItem(pos));
                } else if(item == 1) {
                    if(sel==0)openFileAs(adapter.getItem(pos));
                } else if(item == 2) {
                    action_properties(pos);
                }
            }
        });

        AlertDialog dialog = builder.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //dialog.setTitle("Menu:");
        WindowManager.LayoutParams WMLP = dialog.getWindow().getAttributes();

        WMLP.gravity = Gravity.CENTER; //Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL;
        // WMLP.x = x_pos;   //x position
        // WMLP.y = y_pos;   //y position

        dialog.getWindow().setAttributes(WMLP);
        dialog.show();
    }
    protected void openFile(String path,String name, String ext){

        if ( (ZA.isSupport()) &&
               (ext.equals("zip")||
                ext.equals("rar")||
                ext.equals("7z") ||
                ext.equals("tar"))){
            ZA_unpack(path, name.substring(0, name.lastIndexOf(".")));
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.fromFile(new File(path));
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()));
            intent.setDataAndType(uri, mimeType);

            try {
                startActivity(intent);}
            catch (RuntimeException i){
                notify_toast(getResources().getString(R.string.notify_file_unknown));
            }
        }
    }
    protected void openFileAs(datFM_Information o){
        //int dotPos = o.getName().lastIndexOf(".")+1;
        //String ext = o.getName().substring(dotPos);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(new File(o.getPath()));
        //String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()));
        intent.setDataAndType(uri, "*/*");

        try {
            startActivity(intent);}
        catch (RuntimeException i){
            notify_toast(getResources().getString(R.string.notify_file_unknown));
        }
    }
    protected void onFileClick(datFM_Information o){
        int dotPos = o.getName().lastIndexOf(".")+1;
        String ext = o.getName().substring(dotPos);

        if(o.getData().equalsIgnoreCase(getResources().getString(R.string.fileslist_directory))){
            if (curPanel==0){
                dirLeft = new File(o.getPath());
                fill_local(dirLeft, curPanel);
            } else {
                dirRight = new File(o.getPath());
                fill_local(dirRight, curPanel);
            }
        } else if (o.getData().equalsIgnoreCase(getResources().getString(R.string.fileslist_parent_directory))){
            String prevName;
            if (curPanel==0){
                dirLeft = new File(o.getPath());
                prevName = new File(curentLeftDir).getName();
                fill_local(dirLeft, curPanel);
            } else {
                dirRight = new File(o.getPath());
                prevName = new File(curentRightDir).getName();
                fill_local(dirRight, curPanel);
            }

            if (pref_dir_focus){
                if (curPanel==0){
                    for (int i=0;i<listLeft.getCount();i++){
                        if (prevName.equals(adapterLeft.getItem(i).getName())){
                            listLeft.setSelection(i);
                        }
                    }
                } else {
                    for (int i=0;i<listRight.getCount();i++){
                        if (prevName.equals(adapterRight.getItem(i).getName())){
                            listRight.setSelection(i);
                        }
                    }
                }
            }
        } else {
            openFile(o.getPath(), o.getName(),ext);
        }
    }
    protected void onFileClickLong(datFM_Information o, int position){
        update_operation_vars();
        if(!o.getName().equals("..")){
            if (!selected[position]){
                selected[position]=true;
                sel++;
            } else {
                selected[position]=false;
                sel--;
            }

            if (sel==0){
                itemsSelected.setVisibility(View.GONE);
                textPanel.setVisibility(View.VISIBLE);
            } else {
                itemsSelected.setText("Selected: "+sel);
                itemsSelected.setVisibility(View.VISIBLE);
                textPanel.setVisibility(View.GONE);
            }

            if (curPanel ==0){
                selectedLeft=selected;
                selLeft=sel;
            } else {
                selectedRight=selected;
                selRight=sel;
            }
            update_operation_vars();
            adapter.notifyDataSetChanged();
        }
    }

    protected void update_operation_vars(){
        if (curPanel ==0){
            selected = selectedLeft;
            adapter = adapterLeft;
            destDir = curentRightDir;
            curDir = curentLeftDir;
            itemsSelected = textItemsLeftSelected;
            sel=selLeft;
            textPanel = textPanelLeft;
        }else{
            selected = selectedRight;
            adapter = adapterRight;
            destDir = curentLeftDir;
            curDir = curentRightDir;
            itemsSelected = textItemsRightSelected;
            textPanel = textPanelRight;
            sel=selRight;
        }

        if (!pref_show_panel){
            if (selLeft==0 && selRight==0){
                layoutButtonPanel.setVisibility(View.GONE);
            } else {
                layoutButtonPanel.setVisibility(View.VISIBLE);
            }
        }

        if (curPanel ==0 && selLeft==0){
            btnShare.setEnabled(false);
            btnCopy.setEnabled(false);
            btnCut.setEnabled(false);
            btnDelete.setEnabled(false);
            btnDeselectAll.setEnabled(false);
            btnRename.setEnabled(false);
            btnAddFolder.setVisibility(View.VISIBLE);
            btnAddToArchive.setVisibility(View.GONE);
        } else if (curPanel ==1 && selRight==0){
            btnShare.setEnabled(false);
            btnCopy.setEnabled(false);
            btnCut.setEnabled(false);
            btnDelete.setEnabled(false);
            btnDeselectAll.setEnabled(false);
            btnRename.setEnabled(false);
            btnAddFolder.setVisibility(View.VISIBLE);
            btnAddToArchive.setVisibility(View.GONE);
        } else {
            btnShare.setEnabled(true);
            btnCopy.setEnabled(true);
            btnCut.setEnabled(true);
            btnDelete.setEnabled(true);
            btnDeselectAll.setEnabled(true);
            btnRename.setEnabled(true);
            btnAddFolder.setVisibility(View.GONE);
            btnAddToArchive.setVisibility(View.VISIBLE);
        }
    }
    protected void update_tab(int items, String operation,String dest,int panel_ID){
        int panel_update_ID=2;
        int panel_desel_ID=2;

        if (panel_ID==3){
            panel_desel_ID=curPanel;
            panel_update_ID=curPanel;
        } else if (panel_ID==0 || panel_ID==1){
            panel_update_ID=panel_ID;
            panel_desel_ID=panel_ID;
        } else if (panel_ID==4){
            panel_desel_ID=curPanel;
            panel_update_ID=competPanel;
        }
        if(curentLeftDir.equals(curentRightDir)){
            panel_update_ID=2;
        }

        action_deselect_all(panel_desel_ID);

        if (operation.equals("copy")){
            Toast.makeText(getApplicationContext(),items+" "+getResources().getString(R.string.notify_copy)+" "+dest,Toast.LENGTH_SHORT).show();
        } else if (operation.equals("move")){
            Toast.makeText(getApplicationContext(),items+" "+getResources().getString(R.string.notify_move)+" "+dest,Toast.LENGTH_SHORT).show();
        } else if (operation.equals("new_folder")){
            Toast.makeText(getApplicationContext(),getResources().getString(R.string.notify_newfolder)+" "+dest,Toast.LENGTH_SHORT).show();
        } else if (operation.equals("delete")){
            Toast.makeText(getApplicationContext(),items+" "+getResources().getString(R.string.notify_delete), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(),getResources().getString(R.string.notify_refresh_tab),Toast.LENGTH_SHORT).show();
        }

        if (panel_update_ID==2){
            int posLeft = listLeft.getFirstVisiblePosition();
            int posRight = listRight.getFirstVisiblePosition();
            fill_local(new File(curentLeftDir), 0);
            fill_local(new File(curentRightDir), 1);
            if (posLeft<listLeft.getCount()){listLeft.setSelection(posLeft);}
            if (posRight<listRight.getCount()){listRight.setSelection(posRight);}
        } else if (panel_update_ID==0){
            int posLeft = listLeft.getFirstVisiblePosition();
            if (operation.equals("new_folder")){
                if (pref_open_dir){
                    fill_local(new File(dest), panel_update_ID);
                } else {
                    fill_local(new File(curentLeftDir), panel_update_ID);
                }
            } else {
                fill_local(new File(curentLeftDir), panel_update_ID);
            }
            if (posLeft<listLeft.getCount()){listLeft.setSelection(posLeft);}
        } else if (panel_update_ID==1){
            int posRight = listRight.getFirstVisiblePosition();
            if (operation.equals("new_folder")){
                if (pref_open_dir){
                    fill_local(new File(dest), panel_update_ID);
                } else {
                    fill_local(new File(curentRightDir), panel_update_ID);
                }
            } else {
                fill_local(new File(curentRightDir), panel_update_ID);
            }
            if (posRight<listRight.getCount()){listRight.setSelection(posRight);}
        }
    }
    protected void update_panel_focus(){
        if (curPanel ==0){
            //noinspection deprecation
            layoutPathPanelLeft.setBackgroundDrawable(getResources().getDrawable(android.R.drawable.dialog_holo_light_frame));
            layoutPathPanelRight.setBackgroundColor(Color.TRANSPARENT);
        } else {
            /** only API 16 -> layoutPathPanelRight.setBackground(getResources().getDrawable(android.R.drawable.dialog_holo_light_frame)); **/
            //noinspection deprecation
            layoutPathPanelRight.setBackgroundDrawable(getResources().getDrawable(android.R.drawable.dialog_holo_light_frame));
            layoutPathPanelLeft.setBackgroundColor(Color.TRANSPARENT);
        }
        update_operation_vars();
        setTitle(curDir);
    }

    private void action_dialog(String title, String from, String to, int count, final String operation){
        if (!pref_kamikaze){
            AlertDialog.Builder action_dialog = new AlertDialog.Builder(this);
            action_dialog.setTitle(title);
            LayoutInflater inflater = getLayoutInflater();
            View layer = inflater.inflate(R.layout.datfm_actiondialog,null);

            TextView textDialogCount = (TextView) layer.findViewById(R.id.textDialogCount);
            TextView textDialogFrom = (TextView) layer.findViewById(R.id.textDialogFrom);
            TextView textDialogTo = (TextView) layer.findViewById(R.id.textDialogTo);

            if (operation.equals("copy")){
                textDialogCount.setText(count+" "+getResources().getString(R.string.ui_dialog_header_copy));
                textDialogFrom.setText(getResources().getString(R.string.ui_dialog_from)+" "+from);
                textDialogTo.setText(getResources().getString(R.string.ui_dialog_to)+" "+to);
            } else if (operation.equals("move")){
                textDialogCount.setText(count+" "+getResources().getString(R.string.ui_dialog_header_move));
                textDialogFrom.setText(getResources().getString(R.string.ui_dialog_from)+" "+from);
                textDialogTo.setText(getResources().getString(R.string.ui_dialog_to)+" "+to);
            } else if (operation.equals("delete")){
                textDialogCount.setText(count+" "+getResources().getString(R.string.ui_dialog_header_delete));
                textDialogFrom.setText(getResources().getString(R.string.ui_dialog_in)+" "+from);
                textDialogTo.setText("");
            }

            action_dialog.setView(layer);
            action_dialog.setPositiveButton(getResources().getString(R.string.ui_dialog_btn_ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (operation.equals("copy")){
                                action_copy();
                            } else if (operation.equals("move")){
                                action_move();
                            } else if (operation.equals("delete")){
                                action_delete();
                            }
                        }
                    });
            action_dialog.setNegativeButton(getResources().getString(R.string.ui_dialog_btn_cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });

            AlertDialog AprooveDialog = action_dialog.create();
            AprooveDialog.show();

        } else {
            if (operation.equals("copy")){
                action_copy();
            } else if (operation.equals("move")){
                action_move();
            } else if (operation.equals("delete")){
                action_delete();
            }
        }
    }
    private void action_new_folder(){
        AlertDialog.Builder newFolderDialog = new AlertDialog.Builder(this);
        newFolderDialog.setTitle(getResources().getString(R.string.ui_dialog_title_newfolder));
        LayoutInflater inflater = getLayoutInflater();
        View layer = inflater.inflate(R.layout.datfm_newfolder,null);
        final EditText textNewFolderName = (EditText) layer.findViewById(R.id.textNewFolderName);
        textNewFolderName.setHint(getResources().getString(R.string.ui_dialog_hint_newfolder));
        newFolderDialog.setView(layer);

        newFolderDialog.setPositiveButton(getResources().getString(R.string.ui_dialog_btn_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String newdir_name = textNewFolderName.getText().toString();
                        if (!newdir_name.equals("")){
                            File newdir_path = new File(curDir+"/"+textNewFolderName.getText().toString());
                            if(!newdir_path.exists()){
                                if (pref_root){
                                    newdir_path.mkdir();
                                    if(!newdir_path.exists()){
                                        action_new_folder_root(newdir_path.getPath());}
                                } else {
                                    newdir_path.mkdir();
                                }
                                update_tab(0,"new_folder",curDir+"/"+textNewFolderName.getText().toString(),curPanel);
                            }
                        } else {
                            File newdir_path = new File(curDir+"/"+getResources().getString(R.string.ui_dialog_title_newfolder));
                            if (!newdir_path.exists()){
                                if (pref_root){
                                    newdir_path.mkdir();
                                    if(!newdir_path.exists()){
                                        action_new_folder_root(newdir_path.getPath());}
                                } else {
                                    newdir_path.mkdir();
                                }
                                update_tab(0,"new_folder",curDir+"/"+getResources().getString(R.string.ui_dialog_title_newfolder),curPanel);
                            } else {
                                for (int i=1;i<1000;i++){
                                    newdir_path = new File(curDir+"/"+getResources().getString(R.string.ui_dialog_title_newfolder)+" "+"("+i+")");
                                    if (!newdir_path.exists()){
                                        if (pref_root){
                                            newdir_path.mkdir();
                                            if(!newdir_path.exists()){
                                                action_new_folder_root(newdir_path.getPath());}
                                        } else {
                                            newdir_path.mkdir();
                                        }
                                        update_tab(0,"new_folder",curDir+"/"+getResources().getString(R.string.ui_dialog_title_newfolder)+" "+"("+i+")",curPanel);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                });
        newFolderDialog.setNegativeButton(getResources().getString(R.string.ui_dialog_btn_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        AlertDialog NewFolderNameDialog = newFolderDialog.create();
        NewFolderNameDialog.show();
    }
    private void action_new_folder_root(String path){
        String[] commands = {"mkdir \""+path+"\"\n","chmod 775 \""+path+"\"\n"};
        RunAsRoot(commands);}
    private void action_copy() {
        if (!curDir.equals(destDir)){
            new datFM_Operation(this).execute("copy", "unknown", destDir);
        } else {
            notify_toast(getResources().getString(R.string.notify_operation_same_folder));
        }
    }
    private void action_move() {
        if (!curDir.equals(destDir)){
            new datFM_Operation(this).execute("move", "unknown", destDir);
        } else {
            notify_toast(getResources().getString(R.string.notify_operation_same_folder));
        }
    }
    private void action_rename(){

        AlertDialog.Builder newFolderDialog = new AlertDialog.Builder(this);
        newFolderDialog.setTitle(getResources().getString(R.string.ui_dialog_title_rename));
        LayoutInflater inflater = getLayoutInflater();
        View layer = inflater.inflate(R.layout.datfm_newfolder,null);
        final EditText textNewFolderName = (EditText) layer.findViewById(R.id.textNewFolderName);
        textNewFolderName.setHint(getResources().getString(R.string.ui_dialog_hint_rename));
        newFolderDialog.setView(layer);

        newFolderDialog.setPositiveButton(getResources().getString(R.string.ui_dialog_btn_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String new_name = textNewFolderName.getText().toString();
                        String mask = "false";
                        if (sel!=1){
                            mask="true";
                        }
                        if (!new_name.equals("")){
                            new datFM_Operation(datFM.this).execute("rename", curDir, "unknown", new_name, mask);
                        }
                    }
                });
        newFolderDialog.setNegativeButton(getResources().getString(R.string.ui_dialog_btn_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        AlertDialog BankSelectDialog = newFolderDialog.create();
        BankSelectDialog.show();
    }
    private void action_delete() {
        new datFM_Operation(this).execute("delete", "unknown", curDir);
    }
    private void action_send(){
        update_operation_vars();
        if (sel==1){
            for (int i=1;i<selected.length;i++){
                if (selected[i]){
                    datFM_Information from = adapter.getItem(i);
                    File sending_file = new File(from.getPath());

                    if (sending_file.isFile()){
                        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
                        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        Uri uri = Uri.fromFile(sending_file);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                        shareIntent.putExtra(Intent.EXTRA_TEXT, uri.toString());
                        shareIntent.setType("*/*");
                        startActivity(Intent.createChooser(shareIntent,"Send file"));
                    } else {
                        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
                        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        ArrayList<Uri> uri_list = new ArrayList<Uri>();
                        uri_list.addAll(root_build_content(sending_file));
                        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uri_list);
                        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_TEXT, uri_list);
                        shareIntent.setType("*/*");
                        startActivity(Intent.createChooser(shareIntent,"Send file"));
                    }
                    //String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()));
                    //shareIntent.setType(mimeType);
                }
            }
        } else {
            Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            ArrayList<Uri> uri_list = new ArrayList<Uri>();

            for (int i=1;i<selected.length;i++){
                if (selected[i]){
                    datFM_Information from = adapter.getItem(i);
                    File sending_file = new File (from.getPath());

                    if (sending_file.isFile()){
                        uri_list.add(Uri.fromFile(sending_file));
                    } else {
                        uri_list.addAll(root_build_content(sending_file));
                    }
                }
            }

            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uri_list);
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_TEXT, uri_list);
            shareIntent.setType("*/*");
            startActivity(Intent.createChooser(shareIntent,"Send file"));

            /**
             Toast toast = Toast.makeText(this, "Select only one element!", Toast.LENGTH_LONG);
             View view2 = toast.getView();
             view2.setBackgroundResource(R.drawable.toast_err);
             TextView text = (TextView) view2.findViewById(android.R.id.message);
             text.setTextColor(Color.BLACK);
             text.setShadowLayer(0,0,0,0);
             toast.show();
             **/
        }
    }
    private void action_select_all() {
        if (curPanel ==0){
            for (int i=1;i<selectedLeft.length;i++){
                selectedLeft[i]=true;
            }
            selLeft=selectedLeft.length-1;
            textItemsLeftSelected.setText("Selected: "+selLeft);
            textItemsLeftSelected.setVisibility(View.VISIBLE);
            textPanelLeft.setVisibility(View.GONE);
            adapterLeft.notifyDataSetChanged();
        } else {
            for (int i=1;i<selectedRight.length;i++){
                selectedRight[i]=true;
            }
            selRight=selectedRight.length-1;
            textItemsRightSelected.setText("Selected: "+selRight);
            textItemsRightSelected.setVisibility(View.VISIBLE);
            textPanelRight.setVisibility(View.GONE);
            adapterRight.notifyDataSetChanged();
        }
        update_operation_vars();
    }
    private void action_deselect_all(int panelID) {
        if (panelID==2){
            for (int i=1;i<selectedLeft.length;i++){
                selectedLeft[i]=false;
            }
            selLeft=0;
            textItemsLeftSelected.setVisibility(View.GONE);
            if(pref_show_panel_discr){
                textPanelLeft.setVisibility(View.VISIBLE);
            }
            adapterLeft.notifyDataSetChanged();
            for (int i=1;i<selectedRight.length;i++){
                selectedRight[i]=false;
            }
            selRight=0;
            textItemsRightSelected.setVisibility(View.GONE);
            if(pref_show_panel_discr){
                textPanelRight.setVisibility(View.VISIBLE);
            }
            adapterRight.notifyDataSetChanged();
        } else if (panelID==0){
            for (int i=1;i<selectedLeft.length;i++){
                selectedLeft[i]=false;
            }
            selLeft=0;
            textItemsLeftSelected.setVisibility(View.GONE);
            if(pref_show_panel_discr){
                textPanelLeft.setVisibility(View.VISIBLE);
            }
            adapterLeft.notifyDataSetChanged();
        } else if (panelID==1){
            for (int i=1;i<selectedRight.length;i++){
                selectedRight[i]=false;
            }
            selRight=0;
            textItemsRightSelected.setVisibility(View.GONE);
            if(pref_show_panel_discr){
                textPanelRight.setVisibility(View.VISIBLE);
            }
            adapterRight.notifyDataSetChanged();
        }
        update_operation_vars();
    }  /** Использовать Current Panel, вместо panelID **/
    private void action_properties(int pos){
        Intent properties = new Intent(datFM.datf_context, com.zlab.datFM.datFM_Properties.class);
        ArrayList<String> paths = new ArrayList<String>();

        boolean sec = false;
        for (int i=1;i<selected.length;i++){
            if (selected[i]){
                datFM_Information from = adapter.getItem(i);
                paths.add(from.getPath());
                sec = true;
            }
        }
        if(!sec){
            datFM_Information from = adapter.getItem(pos);
            paths.add(from.getPath());
        }

        properties.putStringArrayListExtra("paths", paths);
        startActivity(properties);
    }

    private void ZA_pack(){
        AlertDialog.Builder NewArchiveDialog = new AlertDialog.Builder(this);
        NewArchiveDialog.setTitle(getResources().getString(R.string.ui_dialog_title_archive));
        LayoutInflater inflater = getLayoutInflater();
        View layer = inflater.inflate(R.layout.datfm_newarchive,null);
        final EditText textNewArchiveName = (EditText) layer.findViewById(R.id.textNewArchiveName);
        final Spinner spinArchType = (Spinner) layer.findViewById(R.id.archiveType);
        final Spinner spinArchLevel = (Spinner) layer.findViewById(R.id.archiveLevel);
        final CheckBox checkArchFileToDelete = (CheckBox) layer.findViewById(R.id.archiveCheckDeleteAfterDone);
        NewArchiveDialog.setView(layer);

        textNewArchiveName.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable arg0) {
                String name = arg0.toString();
                String type = spinArchType.getSelectedItem().toString();

                if (name.lastIndexOf(".")!=-1){
                    if (!name.substring(name.lastIndexOf(".") + 1).equals(type)) {
                        textNewArchiveName.setText(name.substring(0,name.lastIndexOf(".")) + "." + type);
                    }
                } else {
                    String real_name = "";
                    String fake_ext;

                    fake_ext = name.substring(name.length()-3);
                    if(fake_ext.equals("zip") ||
                            fake_ext.equals("tar")){
                        real_name = name.substring(0,name.length()-3);}

                    fake_ext=name.substring(name.length()-2);
                    if(fake_ext.equals("7z")){
                        real_name = name.substring(0,name.length()-2);}

                    textNewArchiveName.setText(real_name + "." + type);
                }
            }

            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
        });
        spinArchType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> a, View view, int i, long l) {
                String name = textNewArchiveName.getText().toString();
                String type = a.getItemAtPosition(i).toString();

                int position = textNewArchiveName.getSelectionStart();

                if(name.lastIndexOf(".")!=0){
                    if(name.substring(name.lastIndexOf(".") + 1).equals("7z") ||
                            name.substring(name.lastIndexOf(".") + 1).equals("zip")||
                            name.substring(name.lastIndexOf(".") + 1).equals("tar")){
                        textNewArchiveName.setText(name.substring(0,name.lastIndexOf(".")) + "." + type);
                    } else {
                        textNewArchiveName.setText(name + "." + type);
                    }
                    textNewArchiveName.setSelection(position);
                } else {
                    textNewArchiveName.setText("." + type);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        NewArchiveDialog.setPositiveButton(getResources().getString(R.string.ui_dialog_btn_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        String newArcName = textNewArchiveName.getText().toString();
                        if (!newArcName.equals("")){
                            String newArcType="";
                            int newArcLevel=0;
                            String[] FileList=new String[sel];

                            if (spinArchType.getSelectedItem().toString().equals("7z")){
                                newArcType= datFM_ZA_Interface.ARCHIVE_TYPE_7Z;
                            } else if (spinArchType.getSelectedItem().toString().equals("zip")){
                                newArcType= datFM_ZA_Interface.ARCHIVE_TYPE_ZIP;
                            } else if (spinArchType.getSelectedItem().toString().equals("tar")){
                                newArcType= datFM_ZA_Interface.ARCHIVE_TYPE_TAR;
                            }

                            if(spinArchLevel.getSelectedItem().toString().equals("min")){
                                newArcLevel= datFM_ZA_Interface.COMPRESSION_LAVEL_FAST;
                            } else if (spinArchLevel.getSelectedItem().toString().equals("mid")){
                                newArcLevel= datFM_ZA_Interface.COMPRESSION_LAVEL_NORMAL;
                            } else if (spinArchLevel.getSelectedItem().toString().equals("max")){
                                newArcLevel= datFM_ZA_Interface.COMPRESSION_LAVEL_MAX;
                            }

                            int count=0;
                            for (int i=0;i<selected.length;i++){
                                if(selected[i]){
                                    FileList[count]=adapter.getItem(i).getName();
                                    count++;
                                }
                            }

                            if ( !ZA.isSupport() )
                            {
                                Toast.makeText(getApplicationContext(),"ZArchiver 0.6.0 or later required!",Toast.LENGTH_SHORT).show();
                                return;
                            } else {
                                if(checkArchFileToDelete.isChecked()){
                                    ZA.setOnActionComplete(new OnActionComplete() {
                                        @Override
                                        public void onActionComplete(int iTaskID, int iAction, boolean bSucessful) {
                                            if(bSucessful){
                                                action_delete();
                                            } else {
                                                Toast.makeText(datf_context,"Pack error!",Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                } else {
                                    ZA.setOnActionComplete(new OnActionComplete() {
                                        @Override
                                        public void onActionComplete(int iTaskID, int iAction, boolean bSucessful) {
                                            if(bSucessful){
                                                update_tab(0,"","",3);
                                            } else {
                                                Toast.makeText(datf_context,"Pack error!",Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                }
                                ZA.CreateArchive(curDir+"/"+newArcName,curDir,FileList, newArcType, newArcLevel);
                            }
                        }
                    }
                });
        NewArchiveDialog.setNegativeButton(getResources().getString(R.string.ui_dialog_btn_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        AlertDialog NewFolderNameDialog = NewArchiveDialog.create();
        NewFolderNameDialog.show();
    }
    private void ZA_unpack(final String path, final String name){

        AlertDialog.Builder NewArchiveDialog = new AlertDialog.Builder(this);
        NewArchiveDialog.setTitle(getResources().getString(R.string.ui_dialog_title_archive_extract));
        LayoutInflater inflater = getLayoutInflater();
        View layer = inflater.inflate(R.layout.datfm_newfolder,null);

        final EditText arcExtractPath = (EditText) layer.findViewById(R.id.textNewFolderName);

        ViewGroup v = (ViewGroup) layer.findViewById(R.id.textNewFolderName).getParent();
        final CheckBox check_in_arcname = new CheckBox(this);
        final CheckBox check_in_current = new CheckBox(this);
        final CheckBox check_in_userpath = new CheckBox(this);

        check_in_arcname.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        check_in_current.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        check_in_userpath.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        check_in_arcname.setChecked(true);
        check_in_current.setChecked(false);
        check_in_userpath.setChecked(false);

        check_in_arcname.setText(getResources().getString(R.string.ui_dialog_archive_unpack_to_name));
        check_in_current.setText(getResources().getString(R.string.ui_dialog_archive_unpack_to_dir));
        check_in_userpath.setText(getResources().getString(R.string.ui_dialog_archive_unpack_to_user));

        check_in_arcname.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){check_in_current.setChecked(false);check_in_userpath.setChecked(false);
                      arcExtractPath.setText(curDir+"/"+name+"/");
                      arcExtractPath.setEnabled(false);}
            }
        });
        check_in_current.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    check_in_arcname.setChecked(false);
                    check_in_userpath.setChecked(false);
                    arcExtractPath.setText(curDir + "/");
                    arcExtractPath.setEnabled(false);
                }
            }
        });
        check_in_userpath.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    check_in_arcname.setChecked(false);
                    check_in_current.setChecked(false);
                    arcExtractPath.setEnabled(true);
                }
            }
        });

        arcExtractPath.setEnabled(false);
        v.addView(check_in_arcname);
        v.addView(check_in_current);
        v.addView(check_in_userpath);

        arcExtractPath.setText(curDir+"/"+name+"/");
        NewArchiveDialog.setView(layer);

        NewArchiveDialog.setPositiveButton(getResources().getString(R.string.ui_dialog_btn_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        final String newArcPath = arcExtractPath.getText().toString();
                        if (!newArcPath.equals("")){

                            if ( !ZA.isSupport() )
                            {
                                Toast.makeText(datf_context,"ZArchiver 0.6.0 or later required!",Toast.LENGTH_SHORT).show();
                                return;
                            } else {
                                ZA.setOnActionComplete(new OnActionComplete() {
                                    @Override
                                    public void onActionComplete(int iTaskID, int iAction, boolean bSucessful) {
                                        if(bSucessful){
                                            if(pref_open_arcdir){
                                                if (pref_open_arcdir_window){
                                                    fill_local(new File(newArcPath), curPanel);
                                                } else {
                                                    fill_local(new File(newArcPath), competPanel);
                                                }
                                            } else {
                                            }
                                        } else {
                                            Toast.makeText(datf_context,"Unpack error!",Toast.LENGTH_SHORT).show();
                                        }
                                        update_tab(0,"","",3);
                                    }
                                });

                                ZA.ExtractArchive(path, newArcPath);
                            }
                        }
                    }
                });
        NewArchiveDialog.setNegativeButton(getResources().getString(R.string.ui_dialog_btn_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        AlertDialog NewFolderNameDialog = NewArchiveDialog.create();
        NewFolderNameDialog.show();
    }

    private void init_UI() {
        listLeft = (ListView) findViewById(R.id.listLeft);
        listRight = (ListView) findViewById(R.id.listRight);
        textPanelRight = (TextView) findViewById(R.id.textItemsRight);
        textPanelLeft = (TextView) findViewById(R.id.textItemsLeft);
        textItemsRightSelected = (TextView) findViewById(R.id.textItemsRightSelected);
        textItemsLeftSelected = (TextView) findViewById(R.id.textItemsLeftSelected);
        textCurrentPathLeft = (EditText) findViewById(R.id.textCurrentPathLeft);
        textCurrentPathRight = (EditText) findViewById(R.id.textCurrentPathRight);
        layoutPathPanelLeft = (LinearLayout) findViewById(R.id.layoutPathPannelLeft);
        layoutPathPanelRight = (LinearLayout) findViewById(R.id.layoutPathPannelRight);
        layoutButtonPanel = (LinearLayout) findViewById(R.id.layoutButtonPanel);

        btnShare = (Button) findViewById(R.id.btnShare);
        btnAddFolder = (Button) findViewById(R.id.btnAddFolder);
        btnAddToArchive = (Button) findViewById(R.id.btnAddToArchive);
        btnCopy = (Button) findViewById(R.id.btnCopy);
        btnCut = (Button) findViewById(R.id.btnCut);
        btnSelectAll = (Button) findViewById(R.id.btnSelectAll);
        btnDeselectAll = (Button) findViewById(R.id.btnDeselectAll);
        btnDelete = (Button) findViewById(R.id.btnDelete);
        btnRename = (Button) findViewById(R.id.btnRename);
    }
    public  void init_UI_Listener(View view) {
        switch (view.getId()) {
            case R.id.btnUPleft:{
                if(selLeft==0){
                    String prevName = new File(curentLeftDir).getName();
                    if (!prevName.equals("")){
                        fill_local(new File(curentLeftDir).getParentFile(), 0);
                        if (pref_dir_focus){
                            for (int i=0;i<listLeft.getCount();i++){
                                if (prevName.equals(adapterLeft.getItem(i).getName())){
                                    listLeft.setSelection(i);
                                }
                            }
                        }
                    }
                }
                break;}
            case R.id.btnUPright:{
                if (selRight==0){
                    String prevName = new File(curentRightDir).getName();
                    if (!prevName.equals("")){
                        fill_local(new File(curentRightDir).getParentFile(), 1);
                        if (pref_dir_focus){
                            for (int i=0;i<listRight.getCount();i++){
                                if (prevName.equals(adapterRight.getItem(i).getName())){
                                    listRight.setSelection(i);
                                }
                            }
                        }
                    }
                }
                break;}
            case R.id.btnGOleft:{
                boolean smb = textCurrentPathLeft.getText().toString().startsWith("smb://");
                if(smb){
                    protocol="smb";
                    new datFM_Protocol_SMB(this).execute(textCurrentPathLeft.getText().toString());
                } else {
                    protocol="local";
                    File new_dir = new File (textCurrentPathLeft.getText().toString());
                    if (new_dir.isDirectory()){
                        if (selLeft==0){
                            fill_local(new_dir, 0);
                        }
                    } else {
                        Toast.makeText(getApplicationContext(),"Not a directory!",Toast.LENGTH_SHORT).show();
                    }
                    EditText_unfocused();
                }
                break;}
            case R.id.btnGOright:{
                File new_dir = new File (textCurrentPathRight.getText().toString());
                if (new_dir.isDirectory()){
                    if (selRight==0){
                        fill_local(new_dir, 1);
                    }
                } else {
                    Toast.makeText(getApplicationContext(),"Not a directory!",Toast.LENGTH_SHORT).show();
                }
                EditText_unfocused();
                break;}
            case R.id.btnSelectAll: {
                action_select_all();
                break;}
            case R.id.btnDeselectAll: {
                update_operation_vars();
                action_deselect_all(curPanel);
                break;}
            case R.id.btnAddFolder: {
                update_operation_vars();
                action_new_folder();
                break;}
            case R.id.btnAddToArchive: {
                update_operation_vars();
                ZA_pack();
                break;}
            case R.id.btnCopy: {
                update_operation_vars();
                action_dialog(getResources().getString(R.string.ui_dialog_title_copy), curDir, destDir, sel, "copy");
                break;}
            case R.id.btnCut: {
                update_operation_vars();
                action_dialog(getResources().getString(R.string.ui_dialog_title_move),curDir,destDir,sel,"move");
                break;}
            case R.id.btnDelete: {
                update_operation_vars();
                action_dialog(getResources().getString(R.string.ui_dialog_title_delete),curDir,destDir,sel,"delete");
                break;}
            case R.id.btnRename: {
                update_operation_vars();
                action_rename();
                break;}
            case R.id.btnShare: {
                action_send();
                break;}
        }
    }
    private void init_Listener(){
        listLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                curPanel =0;
                competPanel = 1;
                update_panel_focus();
                return false;
            }
        });
        listRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                curPanel = 1;
                competPanel = 0;
                update_panel_focus();
                return false;
            }
        });

        listLeft.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState==0){
                    adapterLeft.notifyDataSetChanged();
                    scroll=false;
                } else {
                    scroll=true;
                }
            }
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });
        listRight.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState==0){
                    adapterRight.notifyDataSetChanged();
                    scroll=false;
                } else {
                    scroll=true;
                }
            }
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });


        listLeft.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
                datFM_Information o = adapterLeft.getItem(position);
                curPanel=0;
                if (selLeft!=0){
                    onFileClickLong(o, position); // folder */
                } else {
                    onFileClick(o);
                }
                EditText_unfocused();
            }
        });
        listRight.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
                datFM_Information o = adapterRight.getItem(position);
                curPanel = 1;
                if (selRight!=0){
                    onFileClickLong(o, position); // folder */
                } else {
                    onFileClick(o);
                }
                EditText_unfocused();
            }
        });
        listLeft.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                datFM_Information o = adapterLeft.getItem(position);
                curPanel = 0;
                if(o.getData().equalsIgnoreCase(getResources().getString(R.string.fileslist_directory))||o.getData().equalsIgnoreCase(getResources().getString(R.string.fileslist_parent_directory))){
                    onFileClickLong(o, position); // folder
                } else {
                    onFileClickLong(o, position); // file
                }
                EditText_unfocused();
                return true;
            }
        });

        listRight.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                datFM_Information o = adapterRight.getItem(position);
                curPanel =1;
                if(o.getData().equalsIgnoreCase(getResources().getString(R.string.fileslist_directory))||o.getData().equalsIgnoreCase(getResources().getString(R.string.fileslist_parent_directory))){
                    onFileClickLong(o, position); // folder
                } else {
                    onFileClickLong(o, position); // file
                }
                EditText_unfocused();
                return true;
            }
        });

    }

    private void EditText_unfocused(){
        if (textCurrentPathRight.isFocused()){
            textCurrentPathRight.clearFocus();
            EditText_kb_close(textCurrentPathRight);
        }
        if (textCurrentPathLeft.isFocused()){
            textCurrentPathLeft.clearFocus();
            EditText_kb_close(textCurrentPathLeft);
        }
    }
    private void EditText_kb_close(EditText et){
        InputMethodManager imm = (InputMethodManager)getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
    }

    private void pref_getter(){
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }
    private void pref_setter(){
        pref_show_panel = prefs.getBoolean("pref_show_panel",true);
        pref_open_dir = prefs.getBoolean("pref_open_dir",true);
        pref_open_arcdir = prefs.getBoolean("pref_open_arcdir",false);
        pref_open_arcdir_window = prefs.getBoolean("pref_open_arcdir_window",true);
        pref_save_path = prefs.getBoolean("pref_save_path",true);
        pref_dir_focus = prefs.getBoolean("pref_dir_focus",true);
        pref_kamikaze = prefs.getBoolean("pref_kamikaze",false);
        pref_root = prefs.getBoolean("pref_root",false);
        pref_show_apk = prefs.getBoolean("pref_show_apk",true);
        pref_show_video = prefs.getBoolean("pref_show_video",true);
        pref_show_photo = prefs.getBoolean("pref_show_photo",true);
        pref_icons_cache = prefs.getString("pref_icons_cache","50");
        pref_show_text_on_panel = prefs.getBoolean("pref_show_text_on_panel",true);
        pref_show_navbar = prefs.getBoolean("pref_show_navbar",true);
        pref_show_folder_discr = prefs.getBoolean("pref_show_folder_discr",true);
        pref_show_files_discr = prefs.getBoolean("pref_show_files_discr",true);
        pref_show_panel_discr = prefs.getBoolean("pref_show_panel_discr",true);

        /** Cache size **/
        cache_size=Integer.parseInt(pref_icons_cache);
        cache_icons=new Drawable[cache_size];
        cache_paths=new String[cache_size];
        /** Cache size **/

        if (pref_save_path){
            curentLeftDir = prefs.getString("lastLeftDir", "/sdcard");
            curentRightDir = prefs.getString("lastRightDir", "/sdcard");
        } else {
            curentLeftDir="/sdcard";
            curentRightDir="/sdcard";
        }

        /** First Time ALERT **/
        firstAlert = prefs.getBoolean("firstAlert",true);
        if (firstAlert){
            firstAlertShow();
        }

        /** Text On Panel **/
        if(pref_show_text_on_panel){
            btnShare.setText(getResources().getString(R.string.btn_share));
            btnAddFolder.setText(getResources().getString(R.string.btn_newfolder));
            btnAddToArchive.setText(getResources().getString(R.string.btn_addtoarchive));
            btnCopy.setText(getResources().getString(R.string.btn_copy));
            btnCut.setText(getResources().getString(R.string.btn_move));
            btnSelectAll.setText(getResources().getString(R.string.btn_selectall));
            btnDeselectAll.setText(getResources().getString(R.string.btn_deselectall));
            btnDelete.setText(getResources().getString(R.string.btn_delete));
            btnRename.setText(getResources().getString(R.string.btn_rename));
        } else {
            btnShare.setText("");
            btnAddFolder.setText("");
            btnAddToArchive.setText("");
            btnCopy.setText("");
            btnCut.setText("");
            btnSelectAll.setText("");
            btnDeselectAll.setText("");
            btnDelete.setText("");
            btnRename.setText("");
        }

        /** Nav Bar Show **/
        if(!pref_show_navbar){
            layoutPathPanelLeft.setVisibility(View.GONE);
            layoutPathPanelRight.setVisibility(View.GONE);
        } else {
            layoutPathPanelLeft.setVisibility(View.VISIBLE);
            layoutPathPanelRight.setVisibility(View.VISIBLE);
        }

        /** Panel Descriptions **/
        if(!pref_show_panel_discr){
            textPanelLeft.setVisibility(View.GONE);
            textPanelRight.setVisibility(View.GONE);
        } else {
            textPanelLeft.setVisibility(View.VISIBLE);
            textPanelRight.setVisibility(View.VISIBLE);
        }

        /** Button panel **/
        if (!pref_show_panel){
            if (selLeft==0 && selRight==0){
                layoutButtonPanel.setVisibility(View.GONE);
            } else {
                layoutButtonPanel.setVisibility(View.VISIBLE);
            }
        } else {
            layoutButtonPanel.setVisibility(View.VISIBLE);
        }

        /** Root **/
        if (pref_root){
            String[] commands = {"mount -o rw,remount /system\n"};
            RunAsRoot(commands);
        }

        /** Animation **/

        /** Panel Text on Rotate **/
        /**if (getApplicationContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
         int size_in_dp = 14;
         final float scale = getResources().getDisplayMetrics().density;
         int size_in_px = (int) (size_in_dp * scale + 0.5f);
         //
         pref_btn_text_size(size_in_px);
         } else*/
        if (getApplicationContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            int size_in_dp = 8;
            final float scale = getResources().getDisplayMetrics().density;
            int size_in_px = (int) (size_in_dp * scale + 0.5f);
            //
            pref_btn_text_size(size_in_px);
        }
    }
    private void pref_btn_text_size(int size){
        btnShare.setTextSize(size);
        btnAddFolder.setTextSize(size);
        btnAddToArchive.setTextSize(size);
        btnCopy.setTextSize(size);
        btnCut.setTextSize(size);
        btnSelectAll.setTextSize(size);
        btnDeselectAll.setTextSize(size);
        btnDelete.setTextSize(size);
        btnRename.setTextSize(size);
    }

    private void firstAlertShow(){
        AlertDialog.Builder action_dialog = new AlertDialog.Builder(this);
        action_dialog.setTitle("Alert");
        LayoutInflater inflater = getLayoutInflater();
        View layer = inflater.inflate(R.layout.datfm_firstalert,null);

        action_dialog.setView(layer);
        action_dialog.setPositiveButton(getResources().getString(R.string.ui_dialog_btn_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        prefs.edit().putBoolean("firstAlert", false).commit();
                    }
                });
        action_dialog.setNegativeButton(getResources().getString(R.string.ui_dialog_btn_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });

        AlertDialog AprooveDialog = action_dialog.create();
        AprooveDialog.show();
    }
    private void notify_toast(String msg){
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        View view2 = toast.getView();
        view2.setBackgroundResource(R.drawable.toast_err);
        TextView text = (TextView) view2.findViewById(android.R.id.message);
        text.setTextColor(Color.BLACK);
        text.setShadowLayer(0,0,0,0);
        toast.show();
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

    /** SMB **/

}
