package com.zlab.datFM;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
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
import java.util.ArrayList;
import java.util.List;

import static com.zlab.datFM.datFM_ZA_Interface.*;

public class datFM extends Activity {

    /** VAR GLOBAL **/
    LinearLayout layoutPathPanelLeft, layoutPathPanelRight,layoutButtonPanel;
    static ListView listLeft,listRight;
    TextView textPanelRight,textPanelLeft,textItemsRightSelected,textItemsLeftSelected;
    EditText textCurrentPathLeft, textCurrentPathRight;
    static String curentLeftDir,curentRightDir;
    static String parent_left,parent_right;
    LinearLayout btnShare,btnAddFolder,btnAddToArchive,btnCopy,btnCut,btnSelectAll,btnDeselectAll,btnDelete,btnRename;
    TextView btnShareText,btnAddFolderText,btnAddToArchiveText,btnCopyText,btnCutText,btnSelectAllText,btnDeselectAllText,btnDeleteText,btnRenameText;
    boolean[] selectedRight,selectedLeft;
    static int curPanel,competPanel;
    int selLeft=0;
    int selRight=0;
    int posLeft,posRight;
    static String user, pass, url, domain;
    String prevName;
    static String[] protocols=new String[2];
    static int currentApiVersion;

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
    protected datFM_Adaptor adapterLeft, adapterRight;

    /** VARS PREFS **/
    public SharedPreferences prefs;
    boolean pref_show_panel,pref_open_dir,pref_open_arcdir,
            pref_open_arcdir_window,pref_save_path,pref_dir_focus,
            pref_kamikaze,pref_show_text_on_panel,pref_show_navbar,
            pref_show_panel_discr,pref_small_panel;
    static boolean pref_show_apk,pref_show_video,pref_show_photo,
            pref_show_folder_discr,pref_show_files_discr,pref_root,pref_sambalogin,pref_font_bold_folder,pref_show_hide;
    boolean firstAlert;
    boolean settings_opened = false;
    String pref_icons_cache,pref_icons_size,pref_text_name_size,pref_text_discr_size,pref_font_style,pref_font_typeface;
    static int icons_size,text_name_size,text_discr_size,font_style;
    static Typeface font_typeface;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.datfm);
        datf_context = this;
        datFM_state = ((datFM) datFM.datf_context);

        /** Определение версии API **/
        currentApiVersion = android.os.Build.VERSION.SDK_INT;

        /** Инициализация UI **/
        init_UI();
        init_Listener();

        /** Инициализация настроек **/
        pref_getter();
        pref_setter();

        /** OLD API **/
        old_api_fixes();

        /** Инициализация каталогов **/
        fill_new(curentLeftDir, 0);
        fill_new(curentRightDir, 1);

        /** Интерфейс для ZArchiver **/
        ZA = new datFM_ZA_Interface(datf_context);
    }
    protected void onResume() {
        super.onResume();
        if(settings_opened){
            pref_setter();
            adapterLeft.notifyDataSetChanged();
            adapterRight.notifyDataSetChanged();
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

        /* TODO ui_change_on_action(); Сделать проброс newConfig */
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            int size_in_dp=12;
            if(pref_small_panel){size_in_dp = 9;}
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
                if(currentApiVersion < Build.VERSION_CODES.HONEYCOMB){layer.setBackgroundColor(Color.WHITE);}
                action_dialog.setView(layer);
                AlertDialog AboutDialog = action_dialog.create();
                AboutDialog.show();
                return true;
            }
            case R.id.mainmenu_fav_downloads: {
                if (curPanel ==0){
                    fill_new(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString(), 0);
                } else {
                    fill_new(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString(), 1);
                }
                return true;
            }
            case R.id.mainmenu_fav_dcim: {
                if (curPanel ==0){
                    fill_new(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString(), 0);
                } else {
                    fill_new(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString(), 1);
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
            case R.id.mainmenu_samba:{
                if(curPanel==0){
                    textCurrentPathLeft.setText("smb://");
                    textCurrentPathLeft.requestFocus();
                } else {
                    textCurrentPathRight.setText("smb://");
                    textCurrentPathRight.requestFocus();
                }
                return true;}
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private ArrayList<Uri> root_build_content(File dir){
        ArrayList<Uri> uri_list = new ArrayList<Uri>();
        try{
        if (dir.isDirectory()) {
            for (File child : dir.listFiles()) {
                uri_list.addAll(root_build_content(child));
            }
        } else {
            uri_list.add(Uri.fromFile(dir));
        }
        } catch (Exception e){
            Log.e("ERR:","Listing Error");
        }

        return uri_list;
    }

    protected void fill_new(String path, int Panel_ID){
        boolean smb = path.startsWith("smb://");
        boolean local = path.startsWith("/");
        boolean protocol_accepted=true;

        curPanel=Panel_ID;
        if(Panel_ID==0){competPanel=1;}else{competPanel=0;}

        if(local){
            protocols[Panel_ID]="local";
        } else if (smb){
            protocols[Panel_ID]="smb";
        } else {
            Toast.makeText(datf_context,"Unknown protocol!",Toast.LENGTH_SHORT).show();
            protocol_accepted=false;
        }

        if(protocol_accepted){
            new datFM_Protocol_Fetch(this).execute(path, protocols[Panel_ID], String.valueOf(Panel_ID));
        }
    }
    protected void fill_panel(List<datFM_FileInformation> dir, List<datFM_FileInformation> fls,int panel_ID){
        curPanel = panel_ID;
        if(panel_ID==0){competPanel=1;}else{competPanel=0;}

        if(panel_ID==0){
            if(!curentLeftDir.equals("/") && !curentLeftDir.equals("smb:////")){
                String data = getResources().getString(R.string.fileslist_parent_directory);
                dir.add(0,new datFM_FileInformation("..",parent_left,0,protocols[0],"parent_dir", data, parent_left));
            }
        }else{
            if(!curentRightDir.equals("/") && !curentRightDir.equals("smb:////")){
                String data = getResources().getString(R.string.fileslist_parent_directory);
                dir.add(0,new datFM_FileInformation("..",parent_right,0,protocols[1],"parent_dir", data, parent_right));
            }
        }

        if (panel_ID==0){
            selectedLeft = new boolean[dir.size()];
            adapterLeft = new datFM_Adaptor(datFM.this,R.layout.datfm_list,dir,selectedLeft);
            listLeft.setAdapter(adapterLeft);
            textPanelLeft.setText(
                    getResources().getString(R.string.fileslist_folders_count)+(dir.size()-fls.size()-1)+", "+
                            getResources().getString(R.string.fileslist_files_count)+fls.size());
            textCurrentPathLeft.setText(curentLeftDir);
            if (posLeft<listLeft.getCount()&&posLeft!=0){listLeft.setSelection(posLeft);posLeft=0;}
            if (prevName!=null&&pref_dir_focus){for (int i=0;i<listLeft.getCount();i++){
                if (prevName.equals(adapterLeft.getItem(i).getName())){listLeft.setSelection(i);prevName="";}}}
            update_panel_focus();
        } else {
            selectedRight = new boolean[dir.size()];
            adapterRight = new datFM_Adaptor(datFM.this,R.layout.datfm_list,dir,selectedRight);
            listRight.setAdapter(adapterRight);
            textPanelRight.setText(
                    getResources().getString(R.string.fileslist_folders_count)+(dir.size()-fls.size()-1)+", "+
                            getResources().getString(R.string.fileslist_files_count)+fls.size());
            textCurrentPathRight.setText(curentRightDir);
            if (posRight<listRight.getCount()&&posRight!=0){listRight.setSelection(posRight);posRight=0;}
            if (prevName!=null&&pref_dir_focus){for (int i=0;i<listRight.getCount();i++){
                if (prevName.equals(adapterRight.getItem(i).getName())){listRight.setSelection(i);prevName="";}}}
            update_panel_focus();
        }
    }

    protected void openContextMenu(final int pos, int id){
        if(id==listLeft.getId()){curPanel=0;}else{curPanel=1;}
        update_operation_vars();
        String open,open_with,unpack_archive,properties;

        open = getResources().getString(R.string.contextmenu_open);
        open_with = getResources().getString(R.string.contextmenu_open_with);
        unpack_archive = getResources().getString(R.string.contextmenu_open_unpack_ZA);
        properties = getResources().getString(R.string.contextmenu_properties);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if ( (ZA.isSupport()) &&
             (adapter.getItem(pos).getExt().equals("zip")||
              adapter.getItem(pos).getExt().equals("rar")||
              adapter.getItem(pos).getExt().equals("7z") ||
              adapter.getItem(pos).getExt().equals("tar"))){
            CharSequence[] items = {open, open_with, unpack_archive, properties};
            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    if(item == 0) {
                        if(sel==0)onFileClick(adapter.getItem(pos));
                    } else if(item == 1) {
                        if(sel==0)openFileAs(adapter.getItem(pos));
                    } else if(item == 2) {
                        String path,name;
                        path = adapter.getItem(pos).getPath();
                        name = adapter.getItem(pos).getName().substring(0, adapter.getItem(pos).getName().lastIndexOf("."));
                        ZA_unpack(path, name);
                    } else if(item == 3){
                        action_properties(pos);
                    }
                }
            });
        } else {
            CharSequence[] items = {open, open_with, properties};
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
        }
        AlertDialog dialog = builder.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        WindowManager.LayoutParams WMLP = dialog.getWindow().getAttributes();

        WMLP.gravity = Gravity.CENTER;

        dialog.getWindow().setAttributes(WMLP);
        dialog.show();
    }
    protected void openFile(String path,String name, String ext){

            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.fromFile(new File(path));
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()));

            if(ext.equals("7z")){mimeType="zip";}

            intent.setDataAndType(uri, mimeType);

            try {
                startActivity(intent);}
            catch (RuntimeException i){
                notify_toast(getResources().getString(R.string.notify_file_unknown));
            }
    }
    protected void openFileAs(datFM_FileInformation o){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(new File(o.getPath()));
        intent.setDataAndType(uri, "*/*");

        try {
            startActivity(intent);}
        catch (RuntimeException i){
            notify_toast(getResources().getString(R.string.notify_file_unknown));
        }
    }
    protected void onFileClick(datFM_FileInformation o){
        if(o.getType().equals("dir")){
            fill_new(o.getPath(), curPanel);
        } else if (o.getType().equals("parent_dir")){
            if (curPanel==0){
                prevName = new datFM_IO(curentLeftDir).getName();
                fill_new(o.getPath(), curPanel);
            } else {
                prevName = new datFM_IO(curentRightDir).getName();
                fill_new(o.getPath(), curPanel);
            }
        } else {
            openFile(o.getPath(), o.getName(), o.getExt());
        }
    }
    protected void onFileClickLong(datFM_FileInformation o, int position){
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
                if(pref_show_panel_discr){
                    textPanel.setVisibility(View.VISIBLE);}
            } else {
                itemsSelected.setText("Selected: "+sel);
                itemsSelected.setVisibility(View.VISIBLE);
                if(pref_show_panel_discr){
                    textPanel.setVisibility(View.GONE);}
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

        if (sel==0){
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
            posLeft = listLeft.getFirstVisiblePosition();
            posRight = listRight.getFirstVisiblePosition();
            fill_new(curentLeftDir, 0);
            fill_new(curentRightDir, 1);
            action_deselect_all(2);
        } else if (panel_update_ID==0){
            posLeft = listLeft.getFirstVisiblePosition();
            if (operation.equals("new_folder")){
                if (pref_open_dir){
                    fill_new(dest, panel_update_ID);
                } else {
                    fill_new(curentLeftDir, panel_update_ID);
                }
            } else {
                fill_new(curentLeftDir, panel_update_ID);
            }
            if (posLeft<listLeft.getCount()){listLeft.setSelection(posLeft);}
        } else if (panel_update_ID==1){
            posRight = listRight.getFirstVisiblePosition();
            if (operation.equals("new_folder")){
                if (pref_open_dir){
                    fill_new(dest, panel_update_ID);
                } else {
                    fill_new(curentRightDir, panel_update_ID);
                }
            } else {
                fill_new(curentRightDir, panel_update_ID);
            }
            if (posRight<listRight.getCount()){listRight.setSelection(posRight);}
        }
    }
    protected void update_panel_focus(){
        if (curPanel ==0){
            //noinspection deprecation
            if (currentApiVersion <= Build.VERSION_CODES.HONEYCOMB){
                layoutPathPanelLeft.setBackgroundDrawable(getResources().getDrawable(R.drawable.dialog_full_holo_light));
            } else {
                layoutPathPanelLeft.setBackgroundDrawable(getResources().getDrawable(android.R.drawable.dialog_holo_light_frame));
            }
            layoutPathPanelRight.setBackgroundColor(Color.TRANSPARENT);
        } else {
            /** only API 16 -> layoutPathPanelRight.setBackground(getResources().getDrawable(android.R.drawable.dialog_holo_light_frame)); **/
            //noinspection deprecation
            if (currentApiVersion <= Build.VERSION_CODES.HONEYCOMB){
                layoutPathPanelRight.setBackgroundDrawable(getResources().getDrawable(R.drawable.dialog_full_holo_light));
            } else {
                layoutPathPanelRight.setBackgroundDrawable(getResources().getDrawable(android.R.drawable.dialog_holo_light_frame));
            }
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
            if(currentApiVersion < Build.VERSION_CODES.HONEYCOMB){layer.setBackgroundColor(Color.WHITE);}

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
        if(currentApiVersion < Build.VERSION_CODES.HONEYCOMB){layer.setBackgroundColor(Color.WHITE);}

        final EditText textNewFolderName = (EditText) layer.findViewById(R.id.textNewFolderName);
        textNewFolderName.setHint(getResources().getString(R.string.ui_dialog_hint_newfolder));
        newFolderDialog.setView(layer);

        newFolderDialog.setPositiveButton(getResources().getString(R.string.ui_dialog_btn_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String newdir_name = textNewFolderName.getText().toString();

                        if(protocols[curPanel].equals("smb")){
                            curDir=curDir.substring(0,curDir.lastIndexOf("/"));
                        }

                        if (!newdir_name.equals("")){
                            String dir = curDir+"/"+textNewFolderName.getText().toString();
                            new datFM_FileOperation(datFM_state).execute("new_folder", dir, "","","",String.valueOf(curPanel),String.valueOf(competPanel));
                        } else {
                            String dir = curDir+"/"+getResources().getString(R.string.ui_dialog_title_newfolder);
                            new datFM_FileOperation(datFM_state).execute("new_folder", dir, "","","",String.valueOf(curPanel),String.valueOf(competPanel));
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
    private void action_copy() {
        if (!curDir.equals(destDir)){
            new datFM_FileOperation(this).execute("copy", "unknown", destDir,"","",String.valueOf(curPanel),String.valueOf(competPanel));
        } else {
            notify_toast(getResources().getString(R.string.notify_operation_same_folder));
        }
    }
    private void action_move() {
        if (!curDir.equals(destDir)){
            new datFM_FileOperation(this).execute("move", "unknown", destDir,"","",String.valueOf(curPanel),String.valueOf(competPanel));
        } else {
            notify_toast(getResources().getString(R.string.notify_operation_same_folder));
        }
    }
    private void action_rename(){

        AlertDialog.Builder newFolderDialog = new AlertDialog.Builder(this);
        newFolderDialog.setTitle(getResources().getString(R.string.ui_dialog_title_rename));
        LayoutInflater inflater = getLayoutInflater();
        View layer = inflater.inflate(R.layout.datfm_newfolder,null);
        if(currentApiVersion < Build.VERSION_CODES.HONEYCOMB){layer.setBackgroundColor(Color.WHITE);}

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
                            new datFM_FileOperation(datFM.this).execute("rename", curDir, "unknown", new_name, mask,String.valueOf(curPanel),String.valueOf(curPanel));
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
        new datFM_FileOperation(this).execute("delete", "unknown", curDir,"","",String.valueOf(curPanel),String.valueOf(competPanel));
    }
    private void action_send(){
        update_operation_vars();
        if (sel==1){
            for (int i=1;i<selected.length;i++){
                if (selected[i]){
                    datFM_FileInformation from = adapter.getItem(i);
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
                    datFM_FileInformation from = adapter.getItem(i);
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
                datFM_FileInformation from = adapter.getItem(i);
                paths.add(from.getPath());
                sec = true;
            }
        }
        if(!sec){
            datFM_FileInformation from = adapter.getItem(pos);
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
        if(currentApiVersion < Build.VERSION_CODES.HONEYCOMB){layer.setBackgroundColor(Color.WHITE);}

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
        if(currentApiVersion < Build.VERSION_CODES.HONEYCOMB){layer.setBackgroundColor(Color.WHITE);}

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
                                                    fill_new(newArcPath, curPanel);
                                                } else {
                                                    fill_new(newArcPath, competPanel);
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

        //btnShare = (Button) findViewById(R.id.btnShare);
        btnShare = (LinearLayout) findViewById(R.id.btnShare);
        btnAddFolder = (LinearLayout) findViewById(R.id.btnAddFolder);
        btnAddToArchive = (LinearLayout) findViewById(R.id.btnAddToArchive);
        btnCopy = (LinearLayout) findViewById(R.id.btnCopy);
        btnCut = (LinearLayout) findViewById(R.id.btnCut);
        btnSelectAll = (LinearLayout) findViewById(R.id.btnSelectAll);
        btnDeselectAll = (LinearLayout) findViewById(R.id.btnDeselectAll);
        btnDelete = (LinearLayout) findViewById(R.id.btnDelete);
        btnRename = (LinearLayout) findViewById(R.id.btnRename);

        btnShareText = (TextView) findViewById(R.id.btnShareText);
        btnAddFolderText = (TextView) findViewById(R.id.btnAddFolderText);
        btnAddToArchiveText = (TextView) findViewById(R.id.btnAddToArchiveText);
        btnCopyText = (TextView) findViewById(R.id.btnCopyText);
        btnCutText = (TextView) findViewById(R.id.btnCutText);
        btnSelectAllText = (TextView) findViewById(R.id.btnSelectAllText);
        btnDeselectAllText = (TextView) findViewById(R.id.btnDeselectAllText);
        btnDeleteText = (TextView) findViewById(R.id.btnDeleteText);
        btnRenameText = (TextView) findViewById(R.id.btnRenameText);
    }
    public  void init_UI_Listener(View view) {
        switch (view.getId()) {
            case R.id.btnUPleft:{
                if(selLeft==0){
                    curPanel=0; competPanel=1;
                    prevName = new datFM_IO(curentLeftDir).getName();
                    if(!curentLeftDir.equals("/") && curentLeftDir!=null){
                        fill_new(parent_left, 0);}
                } else {
                    notify_toast("Deselect all item, before change directory.");
                }
                break;}
            case R.id.btnUPright:{
                if (selRight==0){
                    curPanel=1; competPanel=2;
                    prevName = new datFM_IO(curentRightDir).getName();
                    if(!curentRightDir.equals("/") && curentRightDir!=null){
                        fill_new(parent_right, 1);}
                /*
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
                */
                } else {
                    notify_toast("Deselect all item, before change directory.");
                }
                break;}
            case R.id.btnGOleft:{
                if (selLeft==0){
                    curPanel=0; competPanel=1;
                    String path = textCurrentPathLeft.getText().toString();
                    fill_new(path, 0);} else {
                    notify_toast("Deselect all item, before change directory.");
                }
                EditText_unfocused();
                break;}
            case R.id.btnGOright:{
                if (selRight==0){
                curPanel=1; competPanel=0;
                String path = textCurrentPathRight.getText().toString();
                fill_new(path, 1);} else {
                    notify_toast("Deselect all item, before change directory.");
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
                datFM_FileInformation o = adapterLeft.getItem(position);
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
                datFM_FileInformation o = adapterRight.getItem(position);
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
                datFM_FileInformation o = adapterLeft.getItem(position);
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
                datFM_FileInformation o = adapterRight.getItem(position);
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
        pref_sambalogin = prefs.getBoolean("pref_sambalogin",true);
        pref_icons_size = prefs.getString("pref_icons_size","64");
        pref_text_name_size = prefs.getString("pref_text_name_size","14");
        pref_text_discr_size = prefs.getString("pref_text_discr_size","12");
        pref_font_style = prefs.getString("pref_font_style","normal");
        pref_font_typeface = prefs.getString("pref_font_typeface","normal");
        pref_font_bold_folder = prefs.getBoolean("pref_font_bold_folder",false);
        pref_small_panel = prefs.getBoolean("pref_small_panel",false);
        pref_show_hide = prefs.getBoolean("pref_show_hide",false);

        /** Fonts **/
        if(pref_font_style.equalsIgnoreCase("normal")){
            font_style=Typeface.NORMAL;
        } else if(pref_font_style.equalsIgnoreCase("bold")){
            font_style=Typeface.BOLD;
        } else {
            font_style=Typeface.ITALIC;
        }

        if(pref_font_typeface.equalsIgnoreCase("normal")){
            font_typeface=Typeface.defaultFromStyle(Typeface.NORMAL);
        } else if(pref_font_typeface.equalsIgnoreCase("monospace")){
            font_typeface=Typeface.MONOSPACE;
        } else if(pref_font_typeface.equalsIgnoreCase("sans serif")){
            font_typeface=Typeface.SANS_SERIF;
        } else {
            font_typeface=Typeface.SERIF;
        }

        /** Icons Size **/
        icons_size=Integer.parseInt(pref_icons_size);
        text_name_size=Integer.parseInt(pref_text_name_size);
        text_discr_size=Integer.parseInt(pref_text_discr_size);

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
            btnShareText.setVisibility(View.VISIBLE);
            btnAddFolderText.setVisibility(View.VISIBLE);
            btnAddToArchiveText.setVisibility(View.VISIBLE);
            btnCopyText.setVisibility(View.VISIBLE);
            btnCutText.setVisibility(View.VISIBLE);
            btnSelectAllText.setVisibility(View.VISIBLE);
            btnDeselectAllText.setVisibility(View.VISIBLE);
            btnDeleteText.setVisibility(View.VISIBLE);
            btnRenameText.setVisibility(View.VISIBLE);
        } else {
            btnShareText.setVisibility(View.GONE);
            btnAddFolderText.setVisibility(View.GONE);
            btnAddToArchiveText.setVisibility(View.GONE);
            btnCopyText.setVisibility(View.GONE);
            btnCutText.setVisibility(View.GONE);
            btnSelectAllText.setVisibility(View.GONE);
            btnDeselectAllText.setVisibility(View.GONE);
            btnDeleteText.setVisibility(View.GONE);
            btnRenameText.setVisibility(View.GONE);
        }

        /** TextColor Theme **
        btnShareText.setTextColor(Color.WHITE);
        btnAddFolderText.setTextColor(Color.WHITE);
        btnAddToArchiveText.setTextColor(Color.WHITE);
        btnCopyText.setTextColor(Color.WHITE);
        btnCutText.setTextColor(Color.WHITE);
        btnSelectAllText.setTextColor(Color.WHITE);
        btnDeselectAllText.setTextColor(Color.WHITE);
        btnDeleteText.setTextColor(Color.WHITE);
        btnRenameText.setTextColor(Color.WHITE);
        /** **/

        /** Nav Bar Show **/
        if(!pref_show_navbar){
            layoutPathPanelLeft.setVisibility(View.GONE);
            layoutPathPanelRight.setVisibility(View.GONE);
        } else {
            layoutPathPanelLeft.setVisibility(View.VISIBLE);
            layoutPathPanelRight.setVisibility(View.VISIBLE);
        }

        /** Small panel **/
        if(pref_small_panel){
            int in_dp = 48;
            final float scale_px = getResources().getDisplayMetrics().density;
            int in_px = (int) (in_dp * scale_px + 0.5f);
            layoutPathPanelLeft.getLayoutParams().height = in_px;
            layoutPathPanelRight.getLayoutParams().height = in_px;
            layoutButtonPanel.getLayoutParams().height = in_px;
        } else {
            int in_dp = 60;
            final float scale_px = getResources().getDisplayMetrics().density;
            int in_px = (int) (in_dp * scale_px + 0.5f);
            layoutPathPanelLeft.getLayoutParams().height = in_px;
            layoutPathPanelRight.getLayoutParams().height = in_px;
            layoutButtonPanel.getLayoutParams().height = in_px;
        }
        //*/

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

        /** Смена элементов интерфейса **/
        ui_change_on_action();

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

    }

    private void ui_change_on_action(){
        if (getApplicationContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            int size_in_dp = 8;
            final float scale = getResources().getDisplayMetrics().density;
            int size_in_px = (int) (size_in_dp * scale + 0.5f);
            //
            pref_btn_text_size(size_in_px);
        } else {
            int size_in_dp=12;
            if(pref_small_panel){size_in_dp = 9;}
            final float scale = getResources().getDisplayMetrics().density;
            int size_in_px = (int) (size_in_dp * scale + 0.5f);
            //
            pref_btn_text_size(size_in_px);
        }
    }
    private void old_api_fixes(){
        /** FIX OLD API LEVEL UI **/
        if (currentApiVersion < Build.VERSION_CODES.HONEYCOMB){
            //layoutButtonPanel.setBackgroundDrawable(getResources().getDrawable(R.drawable.dialog_full_holo_dark));
            listRight.setCacheColorHint(Color.TRANSPARENT);
            listLeft.setCacheColorHint(Color.TRANSPARENT);

            btnShareText.setTextColor(Color.BLACK);
            btnAddFolderText.setTextColor(Color.BLACK);
            btnAddToArchiveText.setTextColor(Color.BLACK);
            btnCopyText.setTextColor(Color.BLACK);
            btnCutText.setTextColor(Color.BLACK);
            btnSelectAllText.setTextColor(Color.BLACK);
            btnDeselectAllText.setTextColor(Color.BLACK);
            btnDeleteText.setTextColor(Color.BLACK);
            btnRenameText.setTextColor(Color.BLACK);
        }// else{}
        // android:background="@android:drawable/dialog_holo_dark_frame"
    }
    private void pref_btn_text_size(int size){
        btnShareText.setTextSize(size);
        btnAddFolderText.setTextSize(size);
        btnAddToArchiveText.setTextSize(size);
        btnCopyText.setTextSize(size);
        btnCutText.setTextSize(size);
        btnSelectAllText.setTextSize(size);
        btnDeselectAllText.setTextSize(size);
        btnDeleteText.setTextSize(size);
        btnRenameText.setTextSize(size);
        textCurrentPathLeft.setTextSize(size+2);
        textCurrentPathRight.setTextSize(size+2);
    }

    private void firstAlertShow(){
        AlertDialog.Builder action_dialog = new AlertDialog.Builder(this);
        action_dialog.setTitle("Alert");
        LayoutInflater inflater = getLayoutInflater();
        View layer = inflater.inflate(R.layout.datfm_firstalert,null);

        if(currentApiVersion < Build.VERSION_CODES.HONEYCOMB){layer.setBackgroundColor(Color.WHITE);}

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
    public static void notify_toast(String msg){
        Toast toast = Toast.makeText(datFM_state, msg, Toast.LENGTH_LONG);
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
}