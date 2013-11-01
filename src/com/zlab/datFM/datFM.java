package com.zlab.datFM;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.*;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FileTransferClient;
import com.jcraft.jsch.*;
import com.zlab.datFM.FilePicker.datFM_FilePicker;
import com.zlab.datFM.ZA.ZArchiver_IO;
import com.zlab.datFM.crypt.AES_256;
import com.zlab.datFM.hooks.HR_ScrollView;
import com.zlab.datFM.hooks.HR_ScrollViewListener;
import com.zlab.datFM.player.datFM_audio;
import com.zlab.datFM.player.datFM_video;
import com.zlab.datFM.stream.Streamer;
import com.zlab.datFM.swiftp.FtpServerService;
import jcifs.smb.NtlmPasswordAuthentication;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static com.zlab.datFM.ZA.ZArchiver_IO.*;

public class datFM extends Activity {

    /** VAR GLOBAL **/
    LinearLayout layoutPathPanelLeft, layoutPathPanelRight,layoutButtonPanel,layoutActiveLeft,layoutActiveRight,
            layoutParentPathPanelLeft,layoutParentPathPanelRight,pathBarSpacer;
    LinearLayout leftside,rightside,sideholder;
    HR_ScrollView sideholderscroll;
    static ListView listLeft,listRight;
    TextView textPanelRight,textPanelLeft,textItemsRightSelected,textItemsLeftSelected;
    EditText textCurrentPathLeft, textCurrentPathRight;
    public static String curentLeftDir,curentRightDir;
    static String parent_left,parent_right;
    LinearLayout btnShare,btnAddFolder,btnAddToArchive,btnCopy,btnCut,btnSelectAll,btnDeselectAll,btnDelete,btnRename;
    public static LinearLayout SFTPkeypass;
    public static Button pemfile;
    TextView btnShareText,btnAddFolderText,btnAddToArchiveText,btnCopyText,btnCutText,btnSelectAllText,btnDeselectAllText,btnDeleteText,btnRenameText;
    boolean[] selectedRight,selectedLeft;
    boolean FLAG_back_pressed_short = false;
    boolean FLAG_back_pressed_long = false;
    static public int curPanel,competPanel;
    int screen_width;
    int selLeft=0;
    int selRight=0;
    int posLeft,posRight,pathPanelBgr,pathPanelBgrOther;
    int pathPanelBgrFill=0;
    static String url;
    String prevName;
    static public String ftpServerTitle;
    static String[] protocols=new String[2];
    public static int currentApiVersion;
    static int color_item_selected;
    LinearLayout btnUPleft, btnUPright;
    static ArrayList<datFM_File> properties_array;

    /** SMB Client **/
    public static NtlmPasswordAuthentication[] smb_auth_session = new NtlmPasswordAuthentication[2];

    /** SFTP Client**/
    static JSch sftp_auth_session[] = new JSch[2];
    public static ChannelSftp[] sftp_auth_channel = new ChannelSftp[2];
    static Session sftp_session[] = new Session[2];
    public static KeyPair[] sftp_pem_key = new KeyPair[2];
    public static String[] sftp_pem_file = new String[2];

    /** FTP Client **/
    public static FileTransferClient[] ftp_auth_transfer = new FileTransferClient[2];

    /** VARS FOR OPERATION**/
    static int sel;
    boolean[] selected;
    static datFM_File_ListAdaptor adapter;
    static String destDir,curDir;
    TextView itemsSelected,textPanel;

    /** ASYNC **/
    public static datFM datFM_state;
    public static Context datFM_context;

    /** VARS HOLDER FOR TAB **/
    public static datFM_File_ListAdaptor adapterLeft, adapterRight;

    /** VARS PREFS **/
    public SharedPreferences prefs;
    boolean pref_show_panel,pref_open_dir,pref_open_arcdir,
            pref_open_arcdir_window,pref_save_path,pref_dir_focus,
            pref_kamikaze,pref_show_text_on_panel,pref_show_navbar,
            pref_show_panel_discr,pref_clear_filecache,pref_show_single_navbar,pref_show_single_panel,
            pref_force_dual_panel_in_landscape,pref_backbtn_override,pref_fastscroll;
    public static boolean pref_show_apk,pref_show_video,pref_show_photo,
            pref_show_folder_discr,pref_show_files_discr,pref_root,pref_sambalogin,pref_font_bold_folder,pref_show_hide,
            pref_show_date;
    boolean firstAlert;
    boolean settings_opened = false;
    public static boolean pref_build_in_audio_player,pref_build_in_video_player,pref_build_in_photo_player;
    String pref_icons_cache,pref_icons_size,pref_text_name_size,pref_text_discr_size,pref_font_style,pref_font_typeface;
    int pref_actionbar_size,pref_path_bar_size,pref_bartext_size;
    public static String pref_theme;
    public static String pref_theme_icons;
    static int icons_size;
    static int text_name_size;
    static int text_discr_size;
    public static int font_style;
    public static Typeface font_typeface;

    /** ICON CACHE **/
    static int cache_size;
    static int cache_counter=0;
    static boolean scroll=false;
    public static Drawable[] cache_icons;
    public static String[] cache_paths;

    /** ZArchiver **/
    ZArchiver_IO ZA;

    /** UI **/
    int horizontal_scroll_percentage;
    DisplayMetrics displaymetrics;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        /** Определение версии API **/
        currentApiVersion = android.os.Build.VERSION.SDK_INT;
        /** Инициализация настроек **/
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        pref_getter();
        /** Инициализация темы **/
        setTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.datfm);
        datFM_context = this;
        datFM_state = ((datFM) datFM.datFM_context);

        /** Инициализация UI **/
        init_UI();
        init_Listener_Static();
        ftpServerTitle=getApplicationContext().getResources().getString(R.string.fileslist_run_ftp);

        /** Применение настроек **/
        if(firstAlert){
            pref_device_preferred();}
        pref_setter();

        /** OLD API **/
        old_api_fixes();

        /** Инициализация каталогов **/
        fill_new(curentLeftDir, 0);
        fill_new(curentRightDir, 1);

        if (!pref_theme.contains("Classic")) {
            if(currentApiVersion >= Build.VERSION_CODES.HONEYCOMB){
                listLeft.setSelector(R.drawable.datfm_listselector);
                listRight.setSelector(R.drawable.datfm_listselector);
            }
            textItemsLeftSelected.setBackgroundDrawable(getResources().getDrawable(R.drawable.dialog_full_holo_dark));
            textItemsRightSelected.setBackgroundDrawable(getResources().getDrawable(R.drawable.dialog_full_holo_dark));
            textPanelLeft.setBackgroundDrawable(getResources().getDrawable(R.drawable.dialog_full_holo_dark));
            textPanelRight.setBackgroundDrawable(getResources().getDrawable(R.drawable.dialog_full_holo_dark));
        }

        /** Интерфейс для ZArchiver **/
        ZA = new ZArchiver_IO(datFM_context);

        /** Размер экрана **/
        displaymetrics = new DisplayMetrics();
    }
    protected void setTheme(){
        pathPanelBgr=Color.parseColor("#ff46b2ff");
        pathPanelBgrOther=Color.parseColor("#5046b2ff");

        if (pref_theme.equals("Dark Fullscreen")){
            color_item_selected=Color.parseColor("#88650000");
            pathPanelBgrFill=Color.parseColor("#ff131514");
        } else {
            if(currentApiVersion < Build.VERSION_CODES.HONEYCOMB){
                setTheme(android.R.style.Theme_Light_NoTitleBar);
            } else {
                setTheme(android.R.style.Theme_Holo_Light_NoActionBar);
            }
            color_item_selected=Color.parseColor("#ff46b2ff");
        }
    }
    protected void onResume() {
        super.onResume();
        if(settings_opened){
            pref_getter();
            pref_setter();
            update_panel_focus();
            if(adapterLeft!=null)adapterLeft.notifyDataSetChanged();
            if(adapterRight!=null)adapterRight.notifyDataSetChanged();
            settings_opened = false;
        }
    }
    protected void onStop(){
        try{unregisterReceiver(ZA);} catch (Exception e){Log.e("ZArchiver : ","No active reciever.");}
        prefs.edit().putString("lastLeftDir", curentLeftDir).commit();
        prefs.edit().putString("lastRightDir", curentRightDir).commit();
        super.onStop();
    }
    protected void onDestroy(){
        if (pref_root){
            String[] commands = {"mount -o ro,remount /system\n"};
            RunAsRoot(commands);
        }
        if(pref_clear_filecache){
            action_clear_file_cache();
        }
        disconnectSFTP();
        disconnectFTP();
        //datFM_Destroyed=true;
        super.onDestroy();
    }

    //protected void disconnectSMB(){}
    protected void disconnectSFTP(){
        if(datFM.sftp_auth_channel[0]!=null){datFM.sftp_auth_channel[0].exit();datFM.sftp_auth_channel[0].disconnect();}
        if(datFM.sftp_auth_channel[1]!=null){datFM.sftp_auth_channel[1].exit();datFM.sftp_auth_channel[1].disconnect();}
    }
    protected void disconnectFTP(){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if(datFM.ftp_auth_transfer[0]!=null){
                        try {
                            datFM.ftp_auth_transfer[0].disconnect();
                        } catch (FTPException e) {
                            Log.e("datFM: ", e.getMessage());
                        } catch (IOException e) {
                            Log.e("datFM: ", e.getMessage());
                        }
                    }
                    if(datFM.ftp_auth_transfer[1]!=null){
                        try {
                            datFM.ftp_auth_transfer[1].disconnect();
                        } catch (FTPException e) {
                            Log.e("datFM: ", e.getMessage());
                        } catch (IOException e) {
                            Log.e("datFM: ", e.getMessage());
                        }
                    }
                }
            });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        /* TODO ui_change_on_action(); Сделать проброс newConfig */

        new Thread() { /** запуск с задержкой **/
            public void run() {
                try {Thread.sleep(150);
                } catch (InterruptedException e) {e.printStackTrace();}
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        update_panel_focus();
                    }
                });
            }
        }.start();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.datfm_mainmenu, menu);
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
            case R.id.mainmenu_home:{
                fill_new("datFM://",curPanel);
                return true;}
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        /** Для перехода наверх по кнопке назад **/
        if(pref_backbtn_override){
            if ((keyCode == KeyEvent.KEYCODE_BACK)) {
                event.startTracking();
                if (FLAG_back_pressed_long == true) {
                    FLAG_back_pressed_short = false;
                } else {
                    FLAG_back_pressed_short = true;
                    FLAG_back_pressed_long = false;
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        /** Для перехода наверх по кнопке назад **/
        if(pref_backbtn_override){
            if (keyCode == KeyEvent.KEYCODE_BACK) {
            event.startTracking();
            if (FLAG_back_pressed_short) {
                //Log.d("Test", "Short");
                if(curPanel==0){
                    if(selLeft==0){
                        prevName = new datFM_IO(curentLeftDir,curPanel).getName();
                        if(curentLeftDir!=null){
                            fill_new(parent_left, 0);}
                    } else {
                        notify_toast(getResources().getString(R.string.notify_deselect_before_change_dir),true);
                    }
                } else {
                    if (selRight==0){
                        prevName = new datFM_IO(curentRightDir,curPanel).getName();
                        if(curentRightDir!=null){
                            fill_new(parent_right, 1);}
                    } else {
                        notify_toast(getResources().getString(R.string.notify_deselect_before_change_dir),true);
                    }
                }
            }
            FLAG_back_pressed_short = true;
            FLAG_back_pressed_long = false;
            return true;
            }
        }

        return super.onKeyUp(keyCode, event);
    }
    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        /** Для выхода по удержанию кнопки назад **/
        if(pref_backbtn_override){
            if ((keyCode == KeyEvent.KEYCODE_BACK)) {
                FLAG_back_pressed_short = false;
                FLAG_back_pressed_long = true;
                finish();
                return true;
            }
        }
        return super.onKeyLongPress(keyCode, event);
    }

    protected void fill_new(String path, int Panel_ID){
        /* TODO убрать костыль */
        if(path!=null){
            boolean smb = path.startsWith("smb://");
            boolean sftp = path.startsWith("sftp://");
            boolean ftp = path.startsWith("ftp://");
            boolean local = path.startsWith("/");
            boolean home = path.startsWith("datFM://");
            boolean protocol_accepted=true;

            curPanel=Panel_ID;
            if(Panel_ID==0){competPanel=1;}else{competPanel=0;}

            if(local){
                protocols[Panel_ID]="local";
            } else if (smb){
                protocols[Panel_ID]="smb";
            } else if (sftp){
                protocols[Panel_ID]="sftp";
            } else if (home){
                protocols[Panel_ID]="datfm";
            } else if (ftp){
                protocols[Panel_ID]="ftp";
            } else {
                Toast.makeText(datFM_context,getResources().getString(R.string.notify_unknown_protocol),Toast.LENGTH_SHORT).show();
                protocol_accepted=false;
            }

            if(protocol_accepted){
                new datFM_IO_Fetch(this).execute(path, protocols[Panel_ID], String.valueOf(Panel_ID));
            }
        } else {
            fill_new("datFM://",Panel_ID);
        }
    }
    protected void fill_panel(List<datFM_File> dir, List<datFM_File> fls,int panel_ID){
        curPanel = panel_ID;
        if(panel_ID==0){competPanel=1;}else{competPanel=0;}

            if(panel_ID==0){
                    if(!curentLeftDir.equals("datFM://")){
                    String[] parent_data=new datFM_IO(curentLeftDir,curPanel).getParent();
                    parent_left = parent_data[0];
                    String data = parent_data[2];
                        dir.add(0,new datFM_File("..",parent_left,0,protocols[0],parent_data[1], data, parent_left, 0));
                    }
            } else {
                if(!curentRightDir.equals("datFM://")){
                    String[] parent_data=new datFM_IO(curentRightDir,curPanel).getParent();
                    parent_right = parent_data[0];
                    String data = parent_data[2];
                    dir.add(0,new datFM_File("..",parent_right,0,protocols[1],parent_data[1], data, parent_right, 0));
                }
            }

            if (panel_ID==0){
                selectedLeft = new boolean[dir.size()];
                adapterLeft = new datFM_File_ListAdaptor(datFM.this,R.layout.datfm_list,dir,selectedLeft,curPanel);
                listLeft.setAdapter(adapterLeft);
                textPanelLeft.setText(
                        getResources().getString(R.string.fileslist_folders_count)+(dir.size()-fls.size()-1)+", "+
                                getResources().getString(R.string.fileslist_files_count)+fls.size());
                textCurrentPathLeft.setText(curentLeftDir);
                if (posLeft<listLeft.getCount()&&posLeft!=0){listLeft.setSelection(posLeft);posLeft=0;}
                if (prevName!=null&&pref_dir_focus){for (int i=0;i<listLeft.getCount();i++){
                    if (prevName.equals(adapterLeft.getItem(i).getName())){listLeft.setSelection(i);prevName="";}}}
            } else {
                selectedRight = new boolean[dir.size()];
                adapterRight = new datFM_File_ListAdaptor(datFM.this,R.layout.datfm_list,dir,selectedRight,curPanel);
                listRight.setAdapter(adapterRight);
                textPanelRight.setText(
                        getResources().getString(R.string.fileslist_folders_count)+(dir.size()-fls.size()-1)+", "+
                                getResources().getString(R.string.fileslist_files_count)+fls.size());
                textCurrentPathRight.setText(curentRightDir);
                if (posRight<listRight.getCount()&&posRight!=0){listRight.setSelection(posRight);posRight=0;}
                if (prevName!=null&&pref_dir_focus){for (int i=0;i<listRight.getCount();i++){
                    if (prevName.equals(adapterRight.getItem(i).getName())){listRight.setSelection(i);prevName="";}}}
            }

        update_panel_focus();
    }

    protected void openContextMenu(final int pos, int id){
        if(id==listLeft.getId()){curPanel=0;}else{curPanel=1;}
        update_operation_vars();
        String open,open_with,unpack_archive,properties,add_to_favorite,edit,delete;
        boolean show=true;
        open = getResources().getString(R.string.contextmenu_open);
        open_with = getResources().getString(R.string.contextmenu_open_with);
        unpack_archive = getResources().getString(R.string.contextmenu_open_unpack_ZA);
        properties = getResources().getString(R.string.contextmenu_properties);
        add_to_favorite=getResources().getString(R.string.fileslist_add_favorite);
        edit=getResources().getString(R.string.contextmenu_edit);
        delete=getResources().getString(R.string.btn_delete);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);


        if (adapter.getItem(pos).getType().equals("smb_store_network")){
            CharSequence[] items = {edit,delete};
            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    if(item == 0) {
                        action_smb_editserver(adapter.getItem(pos).getName());
                    } else if(item == 1) {
                        selected[pos]=true;action_delete();
                    }
                }
            });
        } else if (adapter.getItem(pos).getType().equals("sftp_store_network")){
            CharSequence[] items = {edit,delete};
            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    if(item == 0) {
                        action_sftp_editserver(adapter.getItem(pos).getName());
                    } else if(item == 1) {
                        selected[pos]=true;action_delete();
                    }
                }
            });
        } else if (adapter.getItem(pos).getType().equals("ftp_store_network")){
            CharSequence[] items = {edit,delete};
            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    if(item == 0) {
                        action_ftp_editserver(adapter.getItem(pos).getName());
                    } else if(item == 1) {
                        selected[pos]=true;action_delete();
                    }
                }
            });
        } else if (adapter.getItem(pos).getType().startsWith("fav_bookmarkk")){
            CharSequence[] items = {delete};
            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    if(item == 0) {
                        selected[pos]=true;action_delete();
                    }
                }
            });
        } else if (adapter.getItem(pos).getType().equals("dir") || adapter.getItem(pos).getType().equals("file")){
            if ( (ZA.isSupport()) &&
                    (adapter.getItem(pos).getExt().equals("zip")||
                            adapter.getItem(pos).getExt().equals("rar")||
                            adapter.getItem(pos).getExt().equals("7z") ||
                            adapter.getItem(pos).getExt().equals("tar"))){
                CharSequence[] items = {open, open_with, unpack_archive,add_to_favorite, properties};
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        if(item == 0) {
                            if(sel==0)onFileClick(adapter.getItem(pos));
                        } else if(item == 1) {
                            if(sel==0)openFileAs(adapter.getItem(pos).getPath(),adapter.getItem(pos).getName(),adapter.getItem(pos).getExt());
                        } else if(item == 2) {
                            String path,name;
                            path = adapter.getItem(pos).getPath();
                            name = adapter.getItem(pos).getName().substring(0, adapter.getItem(pos).getName().lastIndexOf("."));
                            ZA_unpack(path, name);
                        } else if(item == 3) {
                            action_fav_new(adapter.getItem(pos));
                        } else if(item == 4){
                            action_properties(pos);
                        }
                    }
                });
            } else {
                CharSequence[] items = {open, open_with, add_to_favorite, properties};
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        if(item == 0) {
                            if(sel==0)onFileClick(adapter.getItem(pos));
                        } else if(item == 1) {
                            if(sel==0)openFileAs(adapter.getItem(pos).getPath(),adapter.getItem(pos).getName(),adapter.getItem(pos).getExt());
                        } else if(item == 2) {
                            action_fav_new(adapter.getItem(pos));
                        } else if(item == 3) {
                            action_properties(pos);
                        }
                    }
                });
            }
        } else {
            show=false;
        }

        AlertDialog dialog = builder.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        WindowManager.LayoutParams WMLP = dialog.getWindow().getAttributes();

        WMLP.gravity = Gravity.CENTER;

        dialog.getWindow().setAttributes(WMLP);
        if(show)dialog.show();
    }
    protected void openRemoteFile(String path,String name, String ext){
        File tmp_dir = new File(Environment.getExternalStorageDirectory().getPath()+"/Android/data/datFM/cache");
        if(!tmp_dir.exists()){
            tmp_dir.mkdir();
        }
        new datFM_File_Operations(this).execute("open_remote", path, tmp_dir.getPath()+"/"+name,ext,"",String.valueOf(curPanel),String.valueOf(competPanel));
    }
    protected void openRemoteFileAs(String path,String name, String ext){
        File tmp_dir = new File(Environment.getExternalStorageDirectory().getPath()+"/Android/data/datFM/cache");
        if(!tmp_dir.exists()){
            tmp_dir.mkdir();
        }
        new datFM_File_Operations(this).execute("open_as_remote", path, tmp_dir.getPath()+"/"+name,ext,"",String.valueOf(curPanel),String.valueOf(competPanel));
    }
    protected void openFile(final String path,final String name, String ext){
        boolean local = path.startsWith("/");
        if(!local){
            if(Streamer.isStreamMediaByExt(name)){
                new Thread(){
                    public void run(){
                            Streamer s;
                            s = Streamer.getInstance();

                            //SmbFile smbfile = new datFM_IO(path,curPanel).getFileSmb();
                            //SmbFile smbfile = new plugin_SMB(path,curPanel).getFile();
                            s.setStreamSrc(path, null);//the second argument can be a list of subtitle files
                            runOnUiThread(new Runnable(){
                                public void run(){
                                    try{
                                        Uri uri = Uri.parse(Streamer.URL + Uri.fromFile(new File(Uri.parse(path).getPath())).getEncodedPath());

                                        if(Streamer.mediaType(name).equals("audio/*") && pref_build_in_audio_player){
                                            Intent detail = new Intent(datFM_state, datFM_audio.class);
                                            detail.putExtra("FileName",		new datFM_IO(path,curPanel).getName());
                                            detail.putExtra("MediaURL",		uri.toString());
                                            startActivity(detail);
                                        } else if (Streamer.mediaType(name).equals("video/*") && pref_build_in_video_player){
                                            Intent detail = new Intent(datFM_state, datFM_video.class);
                                            detail.putExtra("MediaURL", uri.toString());
                                            startActivity(detail);
                                        } else {
                                            Intent i = new Intent(Intent.ACTION_VIEW);
                                            i.setDataAndType(uri, Streamer.mediaType(name));
                                            startActivity(i);
                                        }
                                    }catch (ActivityNotFoundException e){
                                        e.printStackTrace();
                                    }
                                }
                            });
                    }
                }.start();
            } else {
                openRemoteFile(path, name, ext);
            }
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW);

            Uri uri = Uri.fromFile(new File(path));
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase());

            /* TODO Перейти к библиотеке типов */
            if(ext.equals("7z")){mimeType="zip";}

            if(Streamer.mediaType(name).equals("audio/*") && pref_build_in_audio_player){
                Intent detail = new Intent(datFM_state, datFM_audio.class);
                detail.putExtra("FileName",		new datFM_IO(path,curPanel).getName());
                detail.putExtra("MediaURL",		uri.toString());
                startActivity(detail);
            } else if (Streamer.mediaType(name).equals("video/*") && pref_build_in_video_player){
                Intent detail = new Intent(datFM_state, datFM_video.class);
                detail.putExtra("MediaURL", uri.toString());
                startActivity(detail);
            } else {
                try {
                    intent.setDataAndType(uri, mimeType);
                    startActivity(intent);
                } catch (RuntimeException i){
                    notify_toast(getResources().getString(R.string.notify_file_unknown),true);
                }
            }
        }
    }
    protected void openFileAs(String path,String name, String ext){
        boolean local = path.startsWith("/");
        if(!local){
            openRemoteFileAs(path, name, ext);
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.fromFile(new File(path));
            intent.setDataAndType(uri, "*/*");

            try {
                startActivity(intent);}
            catch (RuntimeException i){
                notify_toast(getResources().getString(R.string.notify_file_unknown),true);
            }
        }
    }
    protected void onFileClick(datFM_File o){
        if(o.getType().equals("file")){
            openFile(o.getPath(), o.getName(), o.getExt());
        } else if (o.getType().equals("parent_dir")){
            if (curPanel==0){
                prevName = new datFM_IO(curentLeftDir,curPanel).getName();
                fill_new(o.getPath(), curPanel);
            } else {
                prevName = new datFM_IO(curentRightDir,curPanel).getName();
                fill_new(o.getPath(), curPanel);
            }
        } else if (o.getType().equals("smb_store_network")){
            action_smb_openserver(o);
        } else if (o.getType().equals("sftp_store_network")){
            action_sftp_openserver(o);
        } else if (o.getType().equals("ftp_store_network")){
            action_ftp_openserver(o);
        } else {
            if(o.getPath().equals("datFM://samba/add")){
                action_smb_newserver(null);
            } else if(o.getPath().equals("datFM://sftp/add")){
                    action_sftp_newserver(null);
            } else if(o.getPath().equals("datFM://ftp/add")){
                action_ftp_newserver(null);
            } else if(o.getPath().equals("datFM://ftp/run")){
                action_ftp_server_start();
            } else if(o.getPath().equals("datFM://favorite/add")){
                    action_fav_newalert();
            } else if(o.getType().equals("fav_bookmark_file")){
                openFile(o.getPath(), o.getName(), o.getExt());
            } else {
                fill_new(o.getPath(), curPanel);
            }
        }
    }
    protected void onFileClickLong(datFM_File o, int position){
        update_operation_vars();
        if(     o.getType().equals("file") ||
                o.getType().equals("dir")  ||
                o.getType().equals("smb_store_network") ||
                o.getType().equals("sftp_store_network") ||
                o.getType().equals("ftp_store_network") ||
                o.getType().startsWith("fav_bookmark")){
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
                itemsSelected.setText(getResources().getString(R.string.fileslist_file_selected)+sel);
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
        } else if (operation.equals("rename")){
            Toast.makeText(getApplicationContext(),items+" "+getResources().getString(R.string.notify_rename), Toast.LENGTH_SHORT).show();
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

        /** Обновление параметров дисплея **/
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        screen_width = displaymetrics.widthPixels;

        /** Исправляет ошибку при использовании GMD Auto Hide Soft Keys **/
        if (sideholderscroll.getWidth() > screen_width){screen_width=sideholderscroll.getWidth();}

        if(pref_show_single_panel && !(pref_force_dual_panel_in_landscape && getApplicationContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)){
            leftside.setLayoutParams(new LinearLayout.LayoutParams(screen_width - 1, ViewGroup.LayoutParams.MATCH_PARENT));
            rightside.setLayoutParams(new LinearLayout.LayoutParams(screen_width - 1, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            leftside.setLayoutParams(new LinearLayout.LayoutParams(screen_width / 2, ViewGroup.LayoutParams.MATCH_PARENT));
            rightside.setLayoutParams(new LinearLayout.LayoutParams((screen_width / 2) - 1, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        if (curPanel == 0){
            //noinspection deprecation
            layoutActiveLeft.setBackgroundColor(pathPanelBgr);
            layoutActiveRight.setBackgroundColor(pathPanelBgrOther);
            if(pref_show_single_navbar){layoutParentPathPanelRight.setVisibility(View.GONE);layoutParentPathPanelLeft.setVisibility(View.VISIBLE);}
        } else {
            /** only API 16 -> layoutPathPanelRight.setBackground(getResources().getDrawable(android.R.drawable.dialog_holo_light_frame)); **/
            //noinspection deprecation
            layoutActiveRight.setBackgroundColor(pathPanelBgr);
            layoutActiveLeft.setBackgroundColor(pathPanelBgrOther);
            if(pref_show_single_navbar){layoutParentPathPanelLeft.setVisibility(View.GONE);layoutParentPathPanelRight.setVisibility(View.VISIBLE);}
        }

        if(pathPanelBgrFill!=0){layoutPathPanelRight.setBackgroundColor(pathPanelBgrFill);layoutPathPanelLeft.setBackgroundColor(pathPanelBgrFill);}
        update_operation_vars();
        //scroll_init_delay(); ///*************************************************/
        setTitle(curDir);
    }

    private void action_fav_newalert(){
        AlertDialog.Builder action_dialog = new AlertDialog.Builder(this);
        action_dialog.setTitle(getResources().getString(R.string.fileslist_add_favorite));
        LayoutInflater inflater = getLayoutInflater();
        View layer = inflater.inflate(R.layout.datfm_actiondialog,null);
        if(currentApiVersion < Build.VERSION_CODES.HONEYCOMB){layer.setBackgroundColor(Color.WHITE);}

        TextView textDialogCount = (TextView) layer.findViewById(R.id.textDialogCount);
        TextView textDialogFrom = (TextView) layer.findViewById(R.id.textDialogFrom);
        TextView textDialogTo = (TextView) layer.findViewById(R.id.textDialogTo);
        textDialogFrom.setVisibility(View.GONE);
        textDialogTo.setVisibility(View.GONE);
        textDialogCount.setText(getResources().getString(R.string.notify_fav_add_instruction));

        action_dialog.setView(layer);
        action_dialog.setNegativeButton(getResources().getString(R.string.ui_dialog_btn_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

        AlertDialog AprooveDialog = action_dialog.create();
        AprooveDialog.show();
    }
    private void action_fav_new(final datFM_File o){
        AlertDialog.Builder action_dialog = new AlertDialog.Builder(this);
        action_dialog.setTitle(getResources().getString(R.string.fileslist_favorites));
        LayoutInflater inflater = getLayoutInflater();
        View layer = inflater.inflate(R.layout.datfm_smb_keychainpass,null);
        if(currentApiVersion < Build.VERSION_CODES.HONEYCOMB){layer.setBackgroundColor(Color.WHITE);}

        final EditText fav_name_text = (EditText) layer.findViewById(R.id.smb_auth_keychain);
        fav_name_text.setInputType(InputType.TYPE_CLASS_TEXT);
        fav_name_text.setHint(getResources().getString(R.string.ui_dialog_smd_addserver_bookmark_name));
        fav_name_text.setText(o.getName());

        action_dialog.setView(layer);
        action_dialog.setPositiveButton(getResources().getString(R.string.ui_dialog_btn_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String name = fav_name_text.getText().toString();
                        if (!name.equals("")){
                            try {
                                String FILENAME = "fav_data_"+fav_name_text.getText().toString();
                                String DATA = o.getName()+"\n"+o.getPath()    +"\n"+
                                              o.getType()+"\n"+o.getProtocol()+"\n"+
                                              o.getSize()+"\n"+o.getData()+"\n"+
                                              fav_name_text.getText().toString()+"\n"+"END";

                                FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
                                fos.write(DATA.getBytes());
                                fos.close();
                                notify_toast("Done!",false);
                            } catch (Exception e) {e.printStackTrace();}
                        } else {
                            notify_toast("Please enter name!",true);
                            action_fav_new(o);
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
    }
    /** SMB **/
    private void action_smb_newserver(String[] formdata){
        AlertDialog.Builder action_dialog = new AlertDialog.Builder(this);
        action_dialog.setTitle("SMB Server");
        LayoutInflater inflater = getLayoutInflater();
        View layer = inflater.inflate(R.layout.datfm_smb_add_server,null);
        if(currentApiVersion < Build.VERSION_CODES.HONEYCOMB){layer.setBackgroundColor(Color.WHITE);}

        final EditText smb_add_server_name = (EditText) layer.findViewById(R.id.smb_add_server_name);
        final EditText smb_add_server_ip_hostname = (EditText) layer.findViewById(R.id.smb_add_server_ip);
        final EditText smb_add_server_start_dir = (EditText) layer.findViewById(R.id.smb_add_server_startdir);
        final EditText smb_add_server_user = (EditText) layer.findViewById(R.id.smb_add_server_user);
        final EditText smb_add_server_pass = (EditText) layer.findViewById(R.id.smb_add_server_pass);
        final EditText smb_add_server_domain = (EditText) layer.findViewById(R.id.smb_add_server_domain);
        final EditText smb_add_server_encrypt_pass = (EditText) layer.findViewById(R.id.smb_add_server_encrypt_pass);

        if(formdata!=null){
            smb_add_server_name.setText(formdata[0]);
            smb_add_server_ip_hostname.setText(formdata[1]);
            smb_add_server_start_dir.setText(formdata[2]);
            smb_add_server_user.setText(formdata[3]);
            smb_add_server_pass.setText(formdata[4]);
            smb_add_server_domain.setText(formdata[5]);

            if(formdata[6].equals("1")){
                smb_add_server_encrypt_pass.setText(formdata[7]);
                formdata[7]=null;
            }
        }

        action_dialog.setView(layer);
        action_dialog.setPositiveButton(getResources().getString(R.string.ui_dialog_btn_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String server_name = smb_add_server_name.getText().toString();
                        String server_ip_hostname = smb_add_server_ip_hostname.getText().toString();
                        String server_start_dir = smb_add_server_start_dir.getText().toString();
                        String server_user = smb_add_server_user.getText().toString();
                        String server_pass = smb_add_server_pass.getText().toString();
                        String server_domain = smb_add_server_domain.getText().toString();
                        String server_encrypt_pass = smb_add_server_encrypt_pass.getText().toString();

                        try {
                            String iscrypted="0";

                            if(!server_pass.equals("") && !server_encrypt_pass.equals("")){
                                    server_pass = AES_256.encrypt(server_encrypt_pass, server_pass);
                                    iscrypted="1";
                            }

                            String FILENAME = "smb_data_"+server_name;
                            String DATA = server_name+"\n"      +server_ip_hostname+"\n"+
                                          server_start_dir+"\n" +server_user+"\n"+
                                          server_pass+"\n"      +server_domain+"\n"+
                                          iscrypted+"\n"+"END";

                            FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
                            fos.write(DATA.getBytes());
                            fos.close();

                            update_tab(0,"","",3);

                        } catch (Exception e) {e.printStackTrace();}
                    }
                });
        action_dialog.setNegativeButton(getResources().getString(R.string.ui_dialog_btn_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

        AlertDialog AprooveDialog = action_dialog.create();
        AprooveDialog.show();
    }
    private void action_smb_openserver(final datFM_File o){
        File dirs = getFilesDir();
        for(File ff : dirs.listFiles()){
            String name = ff.getName().replace("smb_data_","");
            if(name.equals(o.getName())){
                try {
                    FileInputStream fis = openFileInput(ff.getName());
                    StringBuffer fileContent = new StringBuffer("");
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) != -1) {
                        fileContent.append(new String(buffer));
                    }

                    //String server_name = fileContent.toString().split("\n")[0];
                    //String server_ip_hostname = fileContent.toString().split("\n")[1];
                    //String server_start_dir = fileContent.toString().split("\n")[2];
                    String server_user = fileContent.toString().split("\n")[3];
                    String server_pass = fileContent.toString().split("\n")[4];
                    String server_domain = fileContent.toString().split("\n")[5];
                    String iscrypted = fileContent.toString().split("\n")[6];

                    if(!server_user.equals("")){
                        //user=server_user;
                        //domain=server_domain;
                        smb_auth_session[curPanel]= new NtlmPasswordAuthentication(server_domain,server_user,null);

                        if(iscrypted.equals("0")){
                            //pass=server_pass;
                            smb_auth_session[curPanel]= new NtlmPasswordAuthentication(server_domain,server_user,server_pass);
                            fill_new(o.getPath(), curPanel);
                        } else {
                            AlertDialog.Builder action_dialog = new AlertDialog.Builder(this);
                            action_dialog.setTitle("Keychain");
                            LayoutInflater inflater = getLayoutInflater();
                            View layer = inflater.inflate(R.layout.datfm_smb_keychainpass,null);
                            if(currentApiVersion < Build.VERSION_CODES.HONEYCOMB){layer.setBackgroundColor(Color.WHITE);}

                            final EditText smb_keychain = (EditText) layer.findViewById(R.id.smb_auth_keychain);
                            final String server_pass_encrypted = server_pass;

                            action_dialog.setView(layer);
                            action_dialog.setPositiveButton(getResources().getString(R.string.ui_dialog_btn_ok),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            String smb_keychainpass = smb_keychain.getText().toString();
                                            try {
                                                String pass = AES_256.decrypt(smb_keychainpass, server_pass_encrypted);
                                                smb_auth_session[curPanel] = new NtlmPasswordAuthentication(smb_auth_session[curPanel].getDomain(), smb_auth_session[curPanel].getUsername(),pass);
                                                fill_new(o.getPath(), curPanel);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                action_smb_openserver(o);
                                                notify_toast(getResources().getString(R.string.notify_access_denied),true);
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
                        }
                    } else {
                        smb_auth_session[curPanel]= new NtlmPasswordAuthentication(null,null,null);
                        fill_new(o.getPath(), curPanel);
                    }
                    fis.close();
                } catch (Exception e) {e.printStackTrace();}
            }
        }
    }
    private void action_smb_editserver(final String bookmarkname){
        File dirs = getFilesDir();
        for(File ff : dirs.listFiles()){
            String name = ff.getName().replace("smb_data_","");
            if(name.equals(bookmarkname)){
                try {
                    FileInputStream fis = openFileInput(ff.getName());
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
                    String server_pass = fileContent.toString().split("\n")[4];
                    String server_domain = fileContent.toString().split("\n")[5];
                    String iscrypted = fileContent.toString().split("\n")[6];


                        if(iscrypted.equals("0")){
                            String[] formdata = {server_name,server_ip_hostname,server_start_dir,server_user,server_pass,server_domain,iscrypted};
                            action_smb_newserver(formdata);
                        } else {
                            AlertDialog.Builder action_dialog = new AlertDialog.Builder(this);
                            action_dialog.setTitle("Keychain");
                            LayoutInflater inflater = getLayoutInflater();
                            View layer = inflater.inflate(R.layout.datfm_smb_keychainpass,null);
                            if(currentApiVersion < Build.VERSION_CODES.HONEYCOMB){layer.setBackgroundColor(Color.WHITE);}

                            final EditText smb_keychain = (EditText) layer.findViewById(R.id.smb_auth_keychain);
                            final String server_pass_encrypted = server_pass;
                            final String[] formdata = {server_name,server_ip_hostname,server_start_dir,server_user,server_pass,server_domain,iscrypted,null};

                            action_dialog.setView(layer);
                            action_dialog.setPositiveButton(getResources().getString(R.string.ui_dialog_btn_ok),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            String smb_keychainpass = smb_keychain.getText().toString();
                                            try {
                                                formdata[4]= AES_256.decrypt(smb_keychainpass, server_pass_encrypted);
                                                formdata[6]="1";
                                                formdata[7]=smb_keychainpass;
                                                action_smb_newserver(formdata);
                                            } catch (Exception e) {
                                                //e.printStackTrace();
                                                action_smb_editserver(bookmarkname);
                                                notify_toast(getResources().getString(R.string.notify_access_denied),true);
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
                        }

                    fis.close();
                } catch (Exception e) {e.printStackTrace();}
            }
        }
    }
    /** SFTP **/
    private void action_sftp_newserver(String[] formdata){
        AlertDialog.Builder action_dialog = new AlertDialog.Builder(this);
        action_dialog.setTitle("SFTP Server");
        LayoutInflater inflater = getLayoutInflater();
        View layer = inflater.inflate(R.layout.datfm_sftp_add_server,null);
        if(currentApiVersion < Build.VERSION_CODES.HONEYCOMB){layer.setBackgroundColor(Color.WHITE);}

        final EditText sftp_add_server_name = (EditText) layer.findViewById(R.id.sftp_add_server_name);
        final EditText sftp_add_server_ip_hostname = (EditText) layer.findViewById(R.id.sftp_add_server_ip);
        final EditText sftp_add_server_start_dir = (EditText) layer.findViewById(R.id.sftp_add_server_startdir);
        final EditText sftp_add_server_user = (EditText) layer.findViewById(R.id.sftp_add_server_user);
        final EditText sftp_add_server_pass = (EditText) layer.findViewById(R.id.sftp_add_server_pass);
        final EditText sftp_add_server_port = (EditText) layer.findViewById(R.id.sftp_add_server_port);
        final EditText sftp_add_server_encrypt_pass = (EditText) layer.findViewById(R.id.sftp_add_server_encrypt_pass);

        pemfile   = (Button) layer.findViewById(R.id.btnSFTP_PEM);
        final EditText key_password = (EditText) layer.findViewById(R.id.txtSFTPkeypass);
        SFTPkeypass = (LinearLayout) layer.findViewById(R.id.linearSFTPkeypass);
        SFTPkeypass.setVisibility(View.GONE);
        final CheckBox StrictHostCheck = (CheckBox) layer.findViewById(R.id.checkSFTPstrictHost);

        datFM.sftp_pem_file[curPanel] = "";
        pemfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent fileChooserActivity = new Intent(datFM.datFM_state, datFM_FilePicker.class);
                fileChooserActivity.putExtra("panel_ID",String.valueOf(curPanel));
                datFM.datFM_state.startActivity(fileChooserActivity);
            }
        });

        if(formdata!=null){
            sftp_add_server_name.setText(formdata[0]);
            sftp_add_server_ip_hostname.setText(formdata[1]);
            sftp_add_server_start_dir.setText(formdata[2]);
            sftp_add_server_user.setText(formdata[3]);
            sftp_add_server_pass.setText(formdata[4]);
            sftp_add_server_port.setText(formdata[5]);
            //iscrypted.setText(formdata[6]);
            sftp_add_server_encrypt_pass.setText(formdata[6]);
            if(new datFM_IO(formdata[7],curPanel).getName()!=null && !new datFM_IO(formdata[7],curPanel).getName().equals("")){
                pemfile.setText(new datFM_IO(formdata[7],curPanel).getName());
            } else {
                pemfile.setText("*.PEM");
            }
            key_password.setText(formdata[8]);
            if(formdata[9].equals("yes")){
                StrictHostCheck.setChecked(true);
            }
            if(formdata[7]!=null && !formdata[7].equals("")){
                SFTPkeypass.setVisibility(View.VISIBLE);
            }
            /*
            if(formdata[6].equals("1")){
                sftp_add_server_encrypt_pass.setText(formdata[7]);
                formdata[7]=null;
            }        */
        }
        if (sftp_add_server_port.getText().toString().equals("")){sftp_add_server_port.setText("22");}
        if (sftp_add_server_user.getText().toString().equals("")){sftp_add_server_user.setText("root");}

        action_dialog.setView(layer);
        action_dialog.setPositiveButton(getResources().getString(R.string.ui_dialog_btn_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String server_name = sftp_add_server_name.getText().toString();
                        String server_ip_hostname = sftp_add_server_ip_hostname.getText().toString();
                        String server_start_dir = sftp_add_server_start_dir.getText().toString();
                        String server_user = sftp_add_server_user.getText().toString();
                        String server_pass = sftp_add_server_pass.getText().toString();
                        String server_port = sftp_add_server_port.getText().toString();
                        String server_encrypt_pass = sftp_add_server_encrypt_pass.getText().toString();

                        String server_pemfile="";
                        String server_keypass=key_password.getText().toString();
                        String StrictHostCheckStr="";

                        if(sftp_pem_file[curPanel]!=null && !sftp_pem_file[curPanel].equals("")){
                                if(key_password.getText().toString()!=null && !key_password.getText().toString().isEmpty() && !key_password.getText().toString().equals("")){
                                    server_pemfile=sftp_pem_file[curPanel];
                                }
                        }
                        if(!StrictHostCheck.isChecked()){
                            StrictHostCheckStr="no";
                        } else {
                            StrictHostCheckStr="yes";
                        }

                        try {
                            String iscrypted="0";

                            if(!server_encrypt_pass.equals("")){
                                server_pass = AES_256.encrypt(server_encrypt_pass, server_pass);
                                if(!server_keypass.equals("")){server_keypass=AES_256.encrypt(server_encrypt_pass, server_keypass);}
                                iscrypted="1";
                            }

                            String FILENAME = "sftp_data_"+server_name;
                            String DATA = server_name+"\n"+server_ip_hostname+"\n"+
                                          server_start_dir+"\n"+server_user+"\n"+
                                          server_pass+"\n"+server_port+"\n"+
                                          iscrypted+"\n"+server_pemfile+"\n"+
                                          server_keypass+"\n"+StrictHostCheckStr+"\n"+"END";

                            FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
                            fos.write(DATA.getBytes());
                            fos.close();

                            update_tab(0,"","",3);

                        } catch (Exception e) {e.printStackTrace();}
                    }
                });
        action_dialog.setNegativeButton(getResources().getString(R.string.ui_dialog_btn_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

        AlertDialog AprooveDialog = action_dialog.create();
        AprooveDialog.show();
    }
    private void action_sftp_openserver(final datFM_File o){
        File dirs = getFilesDir();
        for(File ff : dirs.listFiles()){
            String name = ff.getName().replace("sftp_data_","");
            if(name.equals(o.getName())){
                try {
                    FileInputStream fis = openFileInput(ff.getName());
                    StringBuffer fileContent = new StringBuffer("");
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) != -1) {
                        fileContent.append(new String(buffer));
                    }

                    //String server_name = fileContent.toString().split("\n")[0];
                    String server_ip_hostname = fileContent.toString().split("\n")[1];
                    //String server_start_dir = fileContent.toString().split("\n")[2];
                    String server_user = fileContent.toString().split("\n")[3];
                    String server_pass = fileContent.toString().split("\n")[4];
                    String server_port = fileContent.toString().split("\n")[5];
                    String iscrypted = fileContent.toString().split("\n")[6];
                    String pemfile = fileContent.toString().split("\n")[7];
                    String keypass = fileContent.toString().split("\n")[8];
                    String StrictHost = fileContent.toString().split("\n")[9];
                    sftp_pem_file[curPanel] = "";

                    if(!server_user.equals("")){
                        //user=server_user;
                        //domain=server_domain;

                        datFM.sftp_auth_session[curPanel] = new JSch();
                        String knownHostsFilename = "/sdcard/.ssh_known_hosts";

                        try {datFM.sftp_auth_session[curPanel].setKnownHosts( knownHostsFilename );
                        } catch (JSchException e) {e.printStackTrace();}


                        try {
                            datFM.sftp_session[curPanel] = datFM.sftp_auth_session[curPanel].getSession( server_user, server_ip_hostname, Integer.parseInt(server_port)); /** CHECK IF NOT INTEGER **/
                        } catch (JSchException e) {
                            Log.e("SFTP:",e.getMessage());}


                        if(iscrypted.equals("0")){
                            datFM.sftp_session[curPanel].setPassword( server_pass );

                            sftp_pem_file[curPanel] = pemfile;

                            if(sftp_pem_file[curPanel]!=null && !sftp_pem_file[curPanel].equals("")){
                                try {
                                    if(keypass!=null && !keypass.isEmpty() && !keypass.equals("")){
                                        sftp_auth_session[curPanel].addIdentity(sftp_pem_file[curPanel],keypass);
                                    } else {
                                        sftp_auth_session[curPanel].addIdentity(sftp_pem_file[curPanel]);
                                    }
                                } catch (JSchException e) {
                                    Log.e("datFM: ", "JSCH key error - "+e.getMessage());
                                }
                            }

                            java.util.Properties config = new java.util.Properties();
                            if(StrictHost.equals("no")){
                                config.put("StrictHostKeyChecking", "no");
                            } else {
                                config.put("StrictHostKeyChecking", "yes");
                            }

                            datFM.sftp_session[curPanel].setConfig(config);
                            datFM.sftp_auth_channel[curPanel]=null;

                            //new datFM_IO_Fetch(datFM.datFM_state).execute(path, protocol, String.valueOf(panel_ID));
                            fill_new(o.getPath(), curPanel);
                        } else {
                            AlertDialog.Builder action_dialog = new AlertDialog.Builder(this);
                            action_dialog.setTitle("Keychain");
                            LayoutInflater inflater = getLayoutInflater();
                            View layer = inflater.inflate(R.layout.datfm_smb_keychainpass,null);
                            if(currentApiVersion < Build.VERSION_CODES.HONEYCOMB){layer.setBackgroundColor(Color.WHITE);}

                            final EditText sftp_keychain = (EditText) layer.findViewById(R.id.smb_auth_keychain);
                            final String server_pass_encrypted = server_pass;
                            final String server_keypass_encrypted = keypass;
                            final String StrictHostKey = StrictHost;
                            final String pemfile_path = pemfile;

                            action_dialog.setView(layer);
                            action_dialog.setPositiveButton(getResources().getString(R.string.ui_dialog_btn_ok),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            String smb_keychainpass = sftp_keychain.getText().toString();
                                            try {
                                                String pass = AES_256.decrypt(smb_keychainpass, server_pass_encrypted);
                                                String keypass = AES_256.decrypt(smb_keychainpass, server_keypass_encrypted);
                                                datFM.sftp_session[curPanel].setPassword( pass );

                                                sftp_pem_file[curPanel] = pemfile_path;

                                                if(sftp_pem_file[curPanel]!=null && !sftp_pem_file[curPanel].equals("")){
                                                    try {
                                                        if(keypass!=null && !keypass.isEmpty() && !keypass.equals("")){
                                                            sftp_auth_session[curPanel].addIdentity(sftp_pem_file[curPanel],keypass);
                                                        } else {
                                                            sftp_auth_session[curPanel].addIdentity(sftp_pem_file[curPanel]);
                                                        }
                                                    } catch (JSchException e) {
                                                        Log.e("datFM: ", "JSCH key error - "+e.getMessage());
                                                    }
                                                }

                                                java.util.Properties config = new java.util.Properties();
                                                if(StrictHostKey.equals("no")){
                                                    config.put("StrictHostKeyChecking", "no");
                                                } else {
                                                    config.put("StrictHostKeyChecking", "yes");
                                                }

                                                datFM.sftp_session[curPanel].setConfig(config);
                                                datFM.sftp_auth_channel[curPanel]=null;
                                                //new datFM_IO_Fetch(datFM.datFM_state).execute(path, protocol, String.valueOf(panel_ID));
                                                fill_new(o.getPath(), curPanel);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                action_sftp_openserver(o);
                                                notify_toast(getResources().getString(R.string.notify_access_denied),true);
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
                        }
                    }
                    fis.close();
                } catch (Exception e) {e.printStackTrace();}
            }
        }
    }
    private void action_sftp_editserver(final String bookmarkname){
        File dirs = getFilesDir();
        for(File ff : dirs.listFiles()){
            String name = ff.getName().replace("sftp_data_","");
            if(name.equals(bookmarkname)){
                try {
                    FileInputStream fis = openFileInput(ff.getName());
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
                    String server_pass = fileContent.toString().split("\n")[4];
                    String server_port = fileContent.toString().split("\n")[5];
                    String iscrypted = fileContent.toString().split("\n")[6];
                    String pemfile = fileContent.toString().split("\n")[7];
                    String keypass = fileContent.toString().split("\n")[8];
                    String StrictHost = fileContent.toString().split("\n")[9];

                    if(iscrypted.equals("0")){
                        String[] formdata = {server_name,server_ip_hostname,server_start_dir,server_user,server_pass,server_port,"",pemfile,keypass,StrictHost};
                        action_sftp_newserver(formdata);
                    } else {
                        AlertDialog.Builder action_dialog = new AlertDialog.Builder(this);
                        action_dialog.setTitle("Keychain");
                        LayoutInflater inflater = getLayoutInflater();
                        View layer = inflater.inflate(R.layout.datfm_smb_keychainpass,null);
                        if(currentApiVersion < Build.VERSION_CODES.HONEYCOMB){layer.setBackgroundColor(Color.WHITE);}

                        final EditText sftp_keychain = (EditText) layer.findViewById(R.id.smb_auth_keychain);
                        final String server_pass_encrypted = server_pass;
                        final String[] formdata = {server_name,server_ip_hostname,server_start_dir,server_user,server_pass,server_port,iscrypted,null,null,null};
                        final String server_keypass_encrypted = keypass;
                        final String StrictHostKey = StrictHost;
                        final String pemfile_path = pemfile;

                        action_dialog.setView(layer);
                        action_dialog.setPositiveButton(getResources().getString(R.string.ui_dialog_btn_ok),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        String sftp_keychainpass = sftp_keychain.getText().toString();
                                        try {
                                            formdata[4]= AES_256.decrypt(sftp_keychainpass, server_pass_encrypted);
                                            formdata[6]=sftp_keychainpass;
                                            //formdata[7]=sftp_keychainpass;
                                            formdata[7]=pemfile_path;
                                            formdata[8]=AES_256.decrypt(sftp_keychainpass, server_keypass_encrypted);
                                            formdata[9]=StrictHostKey;

                                            action_sftp_newserver(formdata);
                                        } catch (Exception e) {
                                            Log.e("Decrypt fail: ", e.getMessage());
                                            action_sftp_editserver(bookmarkname);
                                            notify_toast(getResources().getString(R.string.notify_access_denied),true);
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
                    }

                    fis.close();
                } catch (Exception e) {e.printStackTrace();}
            }
        }
    }
    /** FTP **/
    private void action_ftp_newserver(String[] formdata){
        AlertDialog.Builder action_dialog = new AlertDialog.Builder(this);
        action_dialog.setTitle("FTP Server");
        LayoutInflater inflater = getLayoutInflater();
        View layer = inflater.inflate(R.layout.datfm_ftp_add_server,null);
        if(currentApiVersion < Build.VERSION_CODES.HONEYCOMB){layer.setBackgroundColor(Color.WHITE);}

        final EditText ftp_add_server_name = (EditText) layer.findViewById(R.id.sftp_add_server_name);
        final EditText ftp_add_server_ip_hostname = (EditText) layer.findViewById(R.id.sftp_add_server_ip);
        final EditText ftp_add_server_start_dir = (EditText) layer.findViewById(R.id.sftp_add_server_startdir);
        final EditText ftp_add_server_user = (EditText) layer.findViewById(R.id.sftp_add_server_user);
        final EditText ftp_add_server_pass = (EditText) layer.findViewById(R.id.sftp_add_server_pass);
        final EditText ftp_add_server_port = (EditText) layer.findViewById(R.id.sftp_add_server_port);
        final EditText ftp_add_server_encrypt_pass = (EditText) layer.findViewById(R.id.sftp_add_server_encrypt_pass);
        ftp_add_server_port.setHint("21");

        if(formdata!=null){
            ftp_add_server_name.setText(formdata[0]);
            ftp_add_server_ip_hostname.setText(formdata[1]);
            ftp_add_server_start_dir.setText(formdata[2]);
            ftp_add_server_user.setText(formdata[3]);
            ftp_add_server_pass.setText(formdata[4]);
            ftp_add_server_port.setText(formdata[5]);
            ftp_add_server_encrypt_pass.setText(formdata[6]);
            //ftp_add_server_encrypt_pass.setText(formdata[6]);
                             /*
            if(formdata[6].equals("1")){
                sftp_add_server_encrypt_pass.setText(formdata[7]);
                formdata[7]=null;
            }        */
        }
        if (ftp_add_server_port.getText().toString().equals("")){ftp_add_server_port.setText("21");}
        if (ftp_add_server_user.getText().toString().equals("")){ftp_add_server_user.setText("root");}

        action_dialog.setView(layer);
        action_dialog.setPositiveButton(getResources().getString(R.string.ui_dialog_btn_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String server_name = ftp_add_server_name.getText().toString();
                        String server_ip_hostname = ftp_add_server_ip_hostname.getText().toString();
                        String server_start_dir = ftp_add_server_start_dir.getText().toString();
                        String server_user = ftp_add_server_user.getText().toString();
                        String server_pass = ftp_add_server_pass.getText().toString();
                        String server_port = ftp_add_server_port.getText().toString();
                        String server_encrypt_pass = ftp_add_server_encrypt_pass.getText().toString();

                        try {
                            String iscrypted="0";

                            if(!server_pass.equals("") && !server_encrypt_pass.equals("")){
                                server_pass = AES_256.encrypt(server_encrypt_pass, server_pass);
                                iscrypted="1";
                            }

                            String FILENAME = "ftp_data_"+server_name;
                            String DATA = server_name+"\n"+server_ip_hostname+"\n"+
                                    server_start_dir+"\n"+server_user+"\n"+
                                    server_pass+"\n"+server_port+"\n"+
                                    iscrypted+"\n"+"END";

                            FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
                            fos.write(DATA.getBytes());
                            fos.close();

                            update_tab(0,"","",3);

                        } catch (Exception e) {e.printStackTrace();}
                    }
                });
        action_dialog.setNegativeButton(getResources().getString(R.string.ui_dialog_btn_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

        AlertDialog AprooveDialog = action_dialog.create();
        AprooveDialog.show();
    }
    private void action_ftp_openserver(final datFM_File o){
        File dirs = getFilesDir();
        for(File ff : dirs.listFiles()){
            String name = ff.getName().replace("ftp_data_","");
            if(name.equals(o.getName())){
                try {
                    FileInputStream fis = openFileInput(ff.getName());
                    StringBuffer fileContent = new StringBuffer("");
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) != -1) {
                        fileContent.append(new String(buffer));
                    }

                    //String server_name = fileContent.toString().split("\n")[0];
                    String server_ip_hostname = fileContent.toString().split("\n")[1];
                    //String server_start_dir = fileContent.toString().split("\n")[2];
                    String server_user = fileContent.toString().split("\n")[3];
                    String server_pass = fileContent.toString().split("\n")[4];
                    String server_port = fileContent.toString().split("\n")[5];
                    String iscrypted = fileContent.toString().split("\n")[6];

                    if(!server_user.equals("")){
                        //user=server_user;
                        //domain=server_domain;

                        datFM.ftp_auth_transfer[curPanel] = new FileTransferClient();
                        try {
                            datFM.ftp_auth_transfer[curPanel].setRemoteHost(server_ip_hostname);
                            datFM.ftp_auth_transfer[curPanel].setRemotePort(Integer.parseInt(server_port));
                            datFM.ftp_auth_transfer[curPanel].setUserName(server_user);
                        } catch (Exception e) {
                            Log.e("FTP:",e.getMessage());}


                        if(iscrypted.equals("0")){
                            datFM.ftp_auth_transfer[curPanel].setPassword(server_pass);
                            //new datFM_IO_Fetch(datFM.datFM_state).execute(path, protocol, String.valueOf(panel_ID));
                            fill_new(o.getPath(), curPanel);
                        } else {
                            AlertDialog.Builder action_dialog = new AlertDialog.Builder(this);
                            action_dialog.setTitle("Keychain");
                            LayoutInflater inflater = getLayoutInflater();
                            View layer = inflater.inflate(R.layout.datfm_smb_keychainpass,null);
                            if(currentApiVersion < Build.VERSION_CODES.HONEYCOMB){layer.setBackgroundColor(Color.WHITE);}

                            final EditText sftp_keychain = (EditText) layer.findViewById(R.id.smb_auth_keychain);
                            final String server_pass_encrypted = server_pass;

                            action_dialog.setView(layer);
                            action_dialog.setPositiveButton(getResources().getString(R.string.ui_dialog_btn_ok),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            String smb_keychainpass = sftp_keychain.getText().toString();
                                            try {
                                                String pass = AES_256.decrypt(smb_keychainpass, server_pass_encrypted);
                                                datFM.ftp_auth_transfer[curPanel].setPassword(pass);
                                                //new datFM_IO_Fetch(datFM.datFM_state).execute(path, protocol, String.valueOf(panel_ID));
                                                fill_new(o.getPath(), curPanel);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                action_ftp_openserver(o);
                                                notify_toast(getResources().getString(R.string.notify_access_denied),true);
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
                        }
                    }
                    fis.close();
                } catch (Exception e) {e.printStackTrace();}
            }
        }
    }
    private void action_ftp_editserver(final String bookmarkname){
        File dirs = getFilesDir();
        for(File ff : dirs.listFiles()){
            String name = ff.getName().replace("ftp_data_","");
            if(name.equals(bookmarkname)){
                try {
                    FileInputStream fis = openFileInput(ff.getName());
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
                    String server_pass = fileContent.toString().split("\n")[4];
                    String server_port = fileContent.toString().split("\n")[5];
                    String iscrypted = fileContent.toString().split("\n")[6];


                    if(iscrypted.equals("0")){
                        String[] formdata = {server_name,server_ip_hostname,server_start_dir,server_user,server_pass,server_port,""};
                        action_ftp_newserver(formdata);
                    } else {
                        AlertDialog.Builder action_dialog = new AlertDialog.Builder(this);
                        action_dialog.setTitle("Keychain");
                        LayoutInflater inflater = getLayoutInflater();
                        View layer = inflater.inflate(R.layout.datfm_smb_keychainpass,null);
                        if(currentApiVersion < Build.VERSION_CODES.HONEYCOMB){layer.setBackgroundColor(Color.WHITE);}

                        final EditText ftp_keychain = (EditText) layer.findViewById(R.id.smb_auth_keychain);
                        final String server_pass_encrypted = server_pass;
                        final String[] formdata = {server_name,server_ip_hostname,server_start_dir,server_user,server_pass,server_port,iscrypted,null};

                        action_dialog.setView(layer);
                        action_dialog.setPositiveButton(getResources().getString(R.string.ui_dialog_btn_ok),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        String ftp_keychainpass = ftp_keychain.getText().toString();
                                        try {
                                            formdata[4]= AES_256.decrypt(ftp_keychainpass, server_pass_encrypted);
                                            formdata[6]=ftp_keychainpass;
                                            //formdata[7]=ftp_keychainpass;
                                            action_ftp_newserver(formdata);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            action_ftp_editserver(bookmarkname);
                                            notify_toast(getResources().getString(R.string.notify_access_denied),true);
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
                    }

                    fis.close();
                } catch (Exception e) {e.printStackTrace();}
            }
        }
    }
    private void action_ftp_server_start(){
        if(!FtpServerService.isRunning()){
            sendBroadcast(new Intent(FtpServerService.ACTION_START_FTPSERVER));
        } else {
            sendBroadcast(new Intent(FtpServerService.ACTION_STOP_FTPSERVER));
        }
    }
    public static void action_ftp_server_status(){
        if(datFM_state.curentLeftDir.equals("datFM://ftp")){datFM_state.fill_new(datFM_state.curentLeftDir, 0);datFM_state.action_deselect_all(0);}
        if(datFM_state.curentRightDir.equals("datFM://ftp")){datFM_state.fill_new(datFM_state.curentRightDir, 1);datFM_state.action_deselect_all(1);}
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
        if(protocols[curPanel].equals("local")  ||
           protocols[curPanel].equals("smb")    ||
           protocols[curPanel].equals("sftp")   ||
           protocols[curPanel].equals("ftp")    ){
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
                            if (!newdir_name.equals("")){
                                String dir = curDir+"/"+textNewFolderName.getText().toString();
                                new datFM_File_Operations(datFM_state).execute("new_folder", dir, "","","",String.valueOf(curPanel),String.valueOf(competPanel));
                            } else {
                                String dir = curDir+"/"+getResources().getString(R.string.ui_dialog_title_newfolder);
                                new datFM_File_Operations(datFM_state).execute("new_folder", dir, "","","",String.valueOf(curPanel),String.valueOf(competPanel));
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
    }
    private void action_copy() {
        if (!curDir.equals(destDir)){
            new datFM_File_Operations(this).execute("copy", "unknown", destDir,"","",String.valueOf(curPanel),String.valueOf(competPanel));
        } else {
            notify_toast(getResources().getString(R.string.notify_operation_same_folder),true);
        }
    }
    private void action_move() {
        if (!curDir.equals(destDir)){
            new datFM_File_Operations(this).execute("move", "unknown", destDir,"","",String.valueOf(curPanel),String.valueOf(competPanel));
        } else {
            notify_toast(getResources().getString(R.string.notify_operation_same_folder),true);
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

        if (sel==1){
            for(int i=0;i<selected.length;i++){
            if(selected[i])textNewFolderName.setText(adapter.getItem(i).getName());
            }
        }

        newFolderDialog.setPositiveButton(getResources().getString(R.string.ui_dialog_btn_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String new_name = textNewFolderName.getText().toString();
                        String mask = "false";
                        if (sel!=1){
                            mask="true";
                        }
                        if (!new_name.equals("")){
                            new datFM_File_Operations(datFM.this).execute("rename", curDir, "unknown", new_name, mask,String.valueOf(curPanel),String.valueOf(curPanel));
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
        new datFM_File_Operations(this).execute("delete", "unknown", curDir,"","",String.valueOf(curPanel),String.valueOf(competPanel));
    }
    private void action_select_all() {
        if (curPanel ==0){
            for (int i=1;i<selectedLeft.length;i++){
                if(!selectedLeft[i]){
                    if(adapterLeft.getItem(i).getType().equals("file")              ||
                       adapterLeft.getItem(i).getType().equals("dir")               ||
                       adapterLeft.getItem(i).getType().equals("smb_store_network") ||
                       adapterLeft.getItem(i).getType().equals("sftp_store_network") ||
                       adapterLeft.getItem(i).getType().equals("ftp_store_network") ||
                       adapterLeft.getItem(i).getType().startsWith("fav_bookmark")  ){
                        selLeft++;
                        selectedLeft[i]=true;
                    }
                }
            }
            if(selLeft>0){
                textItemsLeftSelected.setText(getResources().getString(R.string.fileslist_file_selected)+selLeft);
                textItemsLeftSelected.setVisibility(View.VISIBLE);
                textPanelLeft.setVisibility(View.GONE);
                adapterLeft.notifyDataSetChanged();
            }
        } else {
            for (int i=1;i<selectedRight.length;i++){
                if(!selectedRight[i]){
                    if(adapterRight.getItem(i).getType().equals("file")              ||
                       adapterRight.getItem(i).getType().equals("dir")               ||
                       adapterRight.getItem(i).getType().equals("smb_store_network") ||
                       adapterRight.getItem(i).getType().equals("sftp_store_network") ||
                       adapterRight.getItem(i).getType().equals("ftp_store_network") ||
                       adapterRight.getItem(i).getType().startsWith("fav_bookmark")  ){
                        selRight++;
                        selectedRight[i]=true;
                    }
                }
            }
            if(selRight>0){
                textItemsRightSelected.setText(getResources().getString(R.string.fileslist_file_selected)+selRight);
                textItemsRightSelected.setVisibility(View.VISIBLE);
                textPanelRight.setVisibility(View.GONE);
                adapterRight.notifyDataSetChanged();
            }
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
        Intent properties = new Intent(datFM.datFM_context, datFM_Properties.class);
        properties_array = new ArrayList<datFM_File>();

        boolean sec = false;
        for (int i=1;i<selected.length;i++){
            if (selected[i]){
                datFM_File from = adapter.getItem(i);
                properties_array.add(from);
                sec = true;
            }
        }
        if(!sec){
            datFM_File from = adapter.getItem(pos);
            properties_array.add(from);
        }

        startActivity(properties);
    }
    private void action_clear_file_cache(){
            new datFM_IO(Environment.getExternalStorageDirectory().getPath()+"/Android/data/datFM/cache",curPanel).delete();
    }
    private void action_send(){
        update_operation_vars();
        if (sel==1){
            for (int i=1;i<selected.length;i++){
                if (selected[i]){
                    datFM_File from = adapter.getItem(i);
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
                        uri_list.addAll(action_send_list_builder(sending_file));
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
                    datFM_File from = adapter.getItem(i);
                    File sending_file = new File (from.getPath());

                    if (sending_file.isFile()){
                        uri_list.add(Uri.fromFile(sending_file));
                    } else {
                        uri_list.addAll(action_send_list_builder(sending_file));
                    }
                }
            }

            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uri_list);
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_TEXT, uri_list);
            shareIntent.setType("*/*");
            startActivity(Intent.createChooser(shareIntent,"Send file"));
        }
    }
    private ArrayList<Uri> action_send_list_builder(File dir){
        ArrayList<Uri> uri_list = new ArrayList<Uri>();
        try{
            if (dir.isDirectory()) {
                for (File child : dir.listFiles()) {
                    uri_list.addAll(action_send_list_builder(child));
                }
            } else {
                uri_list.add(Uri.fromFile(dir));
            }
        } catch (Exception e){
            Log.e("ERR:","Listing Error");
        }

        return uri_list;
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
        layoutParentPathPanelLeft = (LinearLayout) findViewById(R.id.leftPath);
        layoutParentPathPanelRight = (LinearLayout) findViewById(R.id.rightPath);
        pathBarSpacer = (LinearLayout) findViewById(R.id.pathBarSpacer);
        layoutPathPanelLeft = (LinearLayout) findViewById(R.id.layoutPathPannelLeft);
        layoutPathPanelRight = (LinearLayout) findViewById(R.id.layoutPathPannelRight);
        layoutButtonPanel = (LinearLayout) findViewById(R.id.layoutButtonPanel);
        layoutActiveLeft = (LinearLayout) findViewById(R.id.layoutActiveLeft);
        layoutActiveRight = (LinearLayout) findViewById(R.id.layoutActiveRight);

        /** SCROLL **/
            leftside = (LinearLayout) findViewById(R.id.leftside);
            rightside = (LinearLayout) findViewById(R.id.rightside);
            sideholder = (LinearLayout) findViewById(R.id.sideholder);
            sideholderscroll = (HR_ScrollView) findViewById(R.id.sideholderscroll);
            sideholderscroll.setHorizontalFadingEdgeEnabled(false);

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

        btnUPleft = (LinearLayout) findViewById(R.id.btnUPleft);
        btnUPright = (LinearLayout) findViewById(R.id.btnUPright);
    }
    public  void init_UI_Listener(View view) {
        switch (view.getId()) {
            case R.id.btnUPleft:{
                if(selLeft==0){
                    curPanel=0; competPanel=1;
                    prevName = new datFM_IO(curentLeftDir,curPanel).getName();
                    if(curentLeftDir!=null){
                        fill_new(parent_left, 0);}
                } else {
                    notify_toast(getResources().getString(R.string.notify_deselect_before_change_dir),true);
                }
                break;}
            case R.id.btnUPright:{
                if (selRight==0){
                    curPanel=1; competPanel=0;
                    prevName = new datFM_IO(curentRightDir,curPanel).getName();
                    if(curentRightDir!=null){
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
                    notify_toast(getResources().getString(R.string.notify_deselect_before_change_dir),true);
                }
                break;}
            case R.id.btnGOleft:{
                if (selLeft==0){
                    curPanel=0; competPanel=1;
                    String path = textCurrentPathLeft.getText().toString();
                    fill_new(path, 0);
                } else {
                    notify_toast(getResources().getString(R.string.notify_deselect_before_change_dir),true);
                }
                path_unfocused();
                break;}
            case R.id.btnGOright:{
                if (selRight==0){
                    curPanel=1; competPanel=0;
                    String path = textCurrentPathRight.getText().toString();
                    fill_new(path, 1);
                } else {
                    notify_toast(getResources().getString(R.string.notify_deselect_before_change_dir),true);
                }
                path_unfocused();
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
    private void init_Listener_Static(){

        btnUPleft.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(selLeft==0){
                fill_new("datFM://",0);
                } else {
                    notify_toast(getResources().getString(R.string.notify_deselect_before_change_dir),true);
                }
                return false;
            }
        });
        btnUPright.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(selRight==0){
                fill_new("datFM://",1);
                } else {
                    notify_toast(getResources().getString(R.string.notify_deselect_before_change_dir),true);
                }
                return false;
            }
        });

        listLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                    {
                        curPanel = 0;
                        competPanel = 1;
                        update_panel_focus();
                        break;
                    }
                }
                return false;
            }
        });
        listRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                    {
                        curPanel = 1;
                        competPanel = 0;
                        update_panel_focus();
                        break;
                    }
                }
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
                datFM_File o = adapterLeft.getItem(position);
                curPanel=0;
                if (selLeft!=0){
                    onFileClickLong(o, position); // folder */
                } else {
                    onFileClick(o);
                }
                path_unfocused();
            }
        });
        listRight.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
                datFM_File o = adapterRight.getItem(position);
                curPanel = 1;
                if (selRight!=0){
                    onFileClickLong(o, position); // folder */
                } else {
                    onFileClick(o);
                }
                path_unfocused();
            }
        });
        listLeft.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                datFM_File o = adapterLeft.getItem(position);
                curPanel = 0;
                if(o.getData().equalsIgnoreCase(getResources().getString(R.string.fileslist_directory))||o.getData().equalsIgnoreCase(getResources().getString(R.string.fileslist_parent_directory))){
                    onFileClickLong(o, position); // folder
                } else {
                    onFileClickLong(o, position); // file
                }
                path_unfocused();
                return true;
            }
        });

        listRight.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                datFM_File o = adapterRight.getItem(position);
                curPanel =1;
                if(o.getData().equalsIgnoreCase(getResources().getString(R.string.fileslist_directory))||o.getData().equalsIgnoreCase(getResources().getString(R.string.fileslist_parent_directory))){
                    onFileClickLong(o, position); // folder
                } else {
                    onFileClickLong(o, position); // file
                }
                path_unfocused();
                return true;
            }
        });
    }
    private void init_Listener_Scroll(){
            sideholderscroll.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction())
                    {
                        case MotionEvent.ACTION_UP:
                        {
                            scroll_init_nodelay(curPanel);
                            break;
                        }
                    }
                    return false;
                }
            });

            sideholderscroll.setScrollViewListener(new HR_ScrollViewListener() {
                @Override
                public void onScrollChanged(HR_ScrollView scrollView, int x, int y, int oldx, int oldy) {
                    horizontal_scroll_percentage = x*100/screen_width;
                    if(curPanel==0){
                        if(sideholderscroll.isScrolled() && horizontal_scroll_percentage > 25 && horizontal_scroll_percentage < 50){
                            sideholderscroll.setIsScrolled(false);
                            scroll_init_delay(1);
                        }
                    } else {
                        if(sideholderscroll.isScrolled() && horizontal_scroll_percentage < 75 && horizontal_scroll_percentage > 50){
                            sideholderscroll.setIsScrolled(false);
                            scroll_init_delay(0);
                        }
                    }

                    if(!sideholderscroll.isScrolled() && (horizontal_scroll_percentage<2 || horizontal_scroll_percentage>98)){
                        final Handler handler = new Handler();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {Thread.sleep(200);} catch (InterruptedException e) {}
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        sideholderscroll.setIsScrolled(true);
                                    }
                                });
                            }
                        }).start();
                    }
                }
            });
    }
    private void scroll_init_delay(final int pan_id){
        if(pref_show_single_panel && !(pref_force_dual_panel_in_landscape && getApplicationContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)){
            final Handler handler = new Handler();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {Thread.sleep(100);} catch (InterruptedException e) {}
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(pan_id==0){
                                sideholderscroll.fullScroll(HorizontalScrollView.FOCUS_LEFT);
                            } else {
                                sideholderscroll.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
                            }
                            curPanel=pan_id;
                            update_panel_focus();
                        }
                    });
                }
            }).start();
        }
    }
    private void scroll_init_nodelay(final int pan_id){
        if(pref_show_single_panel && !(pref_force_dual_panel_in_landscape && getApplicationContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)){
            sideholderscroll.post(new Runnable() {
                @Override
                public void run() {
                    if(pan_id==0){
                        sideholderscroll.fullScroll(HorizontalScrollView.FOCUS_LEFT);
                    } else {
                        sideholderscroll.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
                    }
                }
            });
        }
    }

    private void pref_getter(){
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
        pref_show_panel_discr = prefs.getBoolean("pref_show_panel_discr",false);
        pref_sambalogin = prefs.getBoolean("pref_sambalogin",true);
        pref_icons_size = prefs.getString("pref_icons_size","64");
        pref_text_name_size = prefs.getString("pref_text_name_size","14");
        pref_text_discr_size = prefs.getString("pref_text_discr_size","12");
        pref_font_style = prefs.getString("pref_font_style","bold");

        if(currentApiVersion>15){
            pref_font_typeface = prefs.getString("pref_font_typeface","condensed");
        } else {
            pref_font_typeface = prefs.getString("pref_font_typeface", "normal");
        }

        pref_font_bold_folder = prefs.getBoolean("pref_font_bold_folder",false);
        pref_actionbar_size = Integer.parseInt(prefs.getString("pref_actionbar_size","38"));
        pref_path_bar_size = Integer.parseInt(prefs.getString("pref_path_bar_size","34"));
        pref_bartext_size = Integer.parseInt(prefs.getString("pref_bartext_size","10"));
        pref_show_hide = prefs.getBoolean("pref_show_hide",false);
        pref_clear_filecache = prefs.getBoolean("pref_clear_filecache",true);
        pref_theme = prefs.getString("pref_theme","Dark Fullscreen");
        pref_theme_icons = prefs.getString("pref_theme_icons","human_o2");
        pref_show_single_navbar = prefs.getBoolean("pref_show_single_navbar",false);
        pref_show_single_panel = prefs.getBoolean("pref_show_single_panel",false);
        pref_force_dual_panel_in_landscape = prefs.getBoolean("pref_force_dual_panel_in_landscape",false);
        pref_show_date = prefs.getBoolean("pref_show_date",false);
        pref_backbtn_override = prefs.getBoolean("pref_backbtn_override",false);
        pref_fastscroll = prefs.getBoolean("pref_fastscroll",false);
        pref_build_in_audio_player = prefs.getBoolean("pref_build_in_audio_player",true);
        pref_build_in_video_player = prefs.getBoolean("pref_build_in_video_player",true);
        pref_build_in_photo_player = prefs.getBoolean("pref_build_in_photo_player",true);
        firstAlert = prefs.getBoolean("firstAlert",true);
    }
    private void pref_setter(){

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
        } else if(pref_font_typeface.equalsIgnoreCase("condensed")){
            if(currentApiVersion>15){ /** Condensed for 4.1 and UP **/
            font_typeface=Typeface.create("sans-serif-condensed", font_style);}
        } else {
            font_typeface=Typeface.defaultFromStyle(Typeface.NORMAL);
        }

        /** Icons Size **/
        icons_size=Integer.parseInt(pref_icons_size);
        text_name_size=Integer.parseInt(pref_text_name_size);
        text_discr_size=Integer.parseInt(pref_text_discr_size);

        /** Cache size **/
        cache_size=Integer.parseInt(pref_icons_cache);
        cache_icons=new Drawable[cache_size];
        cache_paths=new String[cache_size];
        cache_counter=0;
        /** Cache size **/

        if (pref_save_path){
            curentLeftDir = prefs.getString("lastLeftDir", "datFM://");
            curentRightDir = prefs.getString("lastRightDir", "datFM://");
        } else {
            curentLeftDir="datFM://";
            curentRightDir="datFM://";
        }

        /** First Time ALERT **/
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

        /** Nav Bar Show **/
        if(pref_show_single_navbar){
            pathBarSpacer.setVisibility(View.GONE);
        } else {
            pathBarSpacer.setVisibility(View.VISIBLE);
            layoutParentPathPanelRight.setVisibility(View.VISIBLE);
            layoutParentPathPanelLeft.setVisibility(View.VISIBLE);
        }
        if(!pref_show_navbar){
            layoutPathPanelLeft.setVisibility(View.GONE);
            layoutPathPanelRight.setVisibility(View.GONE);
        } else {
            layoutPathPanelLeft.setVisibility(View.VISIBLE);
            layoutPathPanelRight.setVisibility(View.VISIBLE);
        }

        /** Высота панелей **/
        int in_dp = pref_actionbar_size;
        final float scale_px = getResources().getDisplayMetrics().density;
        int in_px = (int) (in_dp * scale_px + 0.5f);
        layoutButtonPanel.getLayoutParams().height = in_px;
        in_dp = pref_path_bar_size;
        in_px = (int) (in_dp * scale_px + 0.5f);
        layoutPathPanelLeft.getLayoutParams().height = in_px;
        layoutPathPanelRight.getLayoutParams().height = in_px;
        //*/

        /** Panel Descriptions **/
        if(!pref_show_panel_discr){
            textPanelLeft.setVisibility(View.GONE);
            textPanelRight.setVisibility(View.GONE);
        } else {
            textPanelLeft.setVisibility(View.VISIBLE);
            textPanelRight.setVisibility(View.VISIBLE);
        }

        /** Скрытие панели действий **/
        if (!pref_show_panel){
            if (selLeft==0 && selRight==0){
                layoutButtonPanel.setVisibility(View.GONE);
            } else {
                layoutButtonPanel.setVisibility(View.VISIBLE);
            }
        } else {
            layoutButtonPanel.setVisibility(View.VISIBLE);
        }

        /** Однопанельный режим **/
        if(pref_show_single_panel){
            init_Listener_Scroll();
        }

        /** Выставить размер текста адресной строки и панели действия **/
        pref_btn_text_size(pref_bartext_size);

        /* TODO - fast scroll settings */
        if(pref_fastscroll){
            listRight.setFastScrollEnabled(true);
            listLeft.setFastScrollEnabled(true);
        }

        /** Root **/
        if (pref_root){
            String[] commands = {"mount -o rw,remount /system\n"};
            RunAsRoot(commands);
        }

    }
    private void pref_device_preferred(){
        if(isTablet(datFM_context)){
            /** настройки для планшетов */
            DisplayMetrics metrics = getResources().getDisplayMetrics();

            if(metrics.densityDpi>400){
                pref_icons_size = "128";
                prefs.edit().putString("pref_icons_size", "128").commit();
            } else if (metrics.densityDpi>300){
                pref_icons_size = "96";
                prefs.edit().putString("pref_icons_size", "96").commit();
            } else if (metrics.densityDpi>250){
                pref_icons_size = "72";
                prefs.edit().putString("pref_icons_size", "72").commit();
            }

            DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            double x = Math.pow(dm.widthPixels/dm.xdpi,2);
            double y = Math.pow(dm.heightPixels/dm.ydpi,2);
            double screenInches = Math.sqrt(x+y);

            if(screenInches<7.5){
                pref_show_single_navbar=true;
                pref_show_text_on_panel=false;

                prefs.edit().putBoolean("pref_show_single_navbar", true).commit();
                prefs.edit().putBoolean("pref_show_text_on_panel", false).commit();
            }

        } else {
            /** настройки для телефонов */
            //pref_show_panel=false;

            pref_show_single_navbar=true;
            pref_show_single_panel=true;
            pref_force_dual_panel_in_landscape=true;
            pref_show_text_on_panel=false;

            DisplayMetrics metrics = getResources().getDisplayMetrics();

            if(metrics.densityDpi>400){
                pref_icons_size = "128";
                prefs.edit().putString("pref_icons_size", "128").commit();
            } else if (metrics.densityDpi>300){
                pref_icons_size = "96";
                prefs.edit().putString("pref_icons_size", "96").commit();
            } else if (metrics.densityDpi>250){
                pref_icons_size = "72";
                prefs.edit().putString("pref_icons_size", "72").commit();
            }

            //pref_icons_size

            prefs.edit().putBoolean("pref_show_single_navbar", true).commit();
            prefs.edit().putBoolean("pref_show_single_panel", true).commit();
            prefs.edit().putBoolean("pref_force_dual_panel_in_landscape", true).commit();
            prefs.edit().putBoolean("pref_show_text_on_panel", false).commit();
        }

    }

    public static void notify_toast(String msg,boolean err){
        Toast toast = Toast.makeText(datFM_state, msg, Toast.LENGTH_LONG);
        View view2 = toast.getView();

        if(err){
        view2.setBackgroundResource(R.drawable.toast_err);
        } else {
        view2.setBackgroundResource(R.drawable.toast_sec);
        }

        TextView text = (TextView) view2.findViewById(android.R.id.message);
        text.setTextColor(Color.BLACK);
        text.setShadowLayer(0,0,0,0);
        toast.show();
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

    private void path_unfocused(){
        if (textCurrentPathRight.isFocused()){
            textCurrentPathRight.clearFocus();
            path_kb_close(textCurrentPathRight);
        }
        if (textCurrentPathLeft.isFocused()){
            textCurrentPathLeft.clearFocus();
            path_kb_close(textCurrentPathLeft);
        }
    }
    private void path_kb_close(EditText et){
        InputMethodManager imm = (InputMethodManager)getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
    }

    private void old_api_fixes(){
        /** FIX OLD API LEVEL UI **/
        if (currentApiVersion < Build.VERSION_CODES.HONEYCOMB){
            btnShareText.setTextColor(Color.BLACK);
            btnAddFolderText.setTextColor(Color.BLACK);
            btnAddToArchiveText.setTextColor(Color.BLACK);
            btnCopyText.setTextColor(Color.BLACK);
            btnCutText.setTextColor(Color.BLACK);
            btnSelectAllText.setTextColor(Color.BLACK);
            btnDeselectAllText.setTextColor(Color.BLACK);
            btnDeleteText.setTextColor(Color.BLACK);
            btnRenameText.setTextColor(Color.BLACK);
        }
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
                                newArcType= ZArchiver_IO.ARCHIVE_TYPE_7Z;
                            } else if (spinArchType.getSelectedItem().toString().equals("zip")){
                                newArcType= ZArchiver_IO.ARCHIVE_TYPE_ZIP;
                            } else if (spinArchType.getSelectedItem().toString().equals("tar")){
                                newArcType= ZArchiver_IO.ARCHIVE_TYPE_TAR;
                            }

                            if(spinArchLevel.getSelectedItem().toString().equals("min")){
                                newArcLevel= ZArchiver_IO.COMPRESSION_LAVEL_FAST;
                            } else if (spinArchLevel.getSelectedItem().toString().equals("mid")){
                                newArcLevel= ZArchiver_IO.COMPRESSION_LAVEL_NORMAL;
                            } else if (spinArchLevel.getSelectedItem().toString().equals("max")){
                                newArcLevel= ZArchiver_IO.COMPRESSION_LAVEL_MAX;
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
                                Toast.makeText(getApplicationContext(),getResources().getString(R.string.notify_cant_find_ZA),Toast.LENGTH_SHORT).show();
                                //return;
                            } else {
                                if(checkArchFileToDelete.isChecked()){
                                    ZA.setOnActionComplete(new OnActionComplete() {
                                        @Override
                                        public void onActionComplete(int iTaskID, int iAction, boolean bSucessful) {
                                            if(bSucessful){
                                                action_delete();
                                            } else {
                                                Toast.makeText(datFM_context,"Pack error!",Toast.LENGTH_SHORT).show();
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
                                                Toast.makeText(datFM_context,"Pack error!",Toast.LENGTH_SHORT).show();
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
                                Toast.makeText(datFM_context,getResources().getString(R.string.notify_cant_find_ZA),Toast.LENGTH_SHORT).show();
                                //return;
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
                                                update_tab(0,"null","null",curPanel);
                                            }
                                        } else {
                                            Toast.makeText(datFM_context,"Unpack error!",Toast.LENGTH_SHORT).show();
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

    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

}