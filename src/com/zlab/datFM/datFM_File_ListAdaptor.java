package com.zlab.datFM;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

public class datFM_File_ListAdaptor extends ArrayAdapter<datFM_File> {

    private Context c;
    private int id;
    private List<datFM_File> items;
    private boolean[] sel;

    TextView text_FileName;
    TextView text_FileDescription;
    ImageView imgFileIcon;
    LinearLayout ll;
    int resID;
    int PanelID;

    public datFM_File_ListAdaptor(Context context, int LayoutID, List<datFM_File> objects, boolean[] selected, int panelID) {
        super(context, LayoutID, objects);
        c = context;
        id = LayoutID;
        items = objects;
        sel = selected;
        PanelID = panelID;
    }

    public datFM_File getItem(int i)
    {
        return items.get(i);
    }

    public List getAllItems()
    {
        return items;
    }


    @Override
    public View getView(final int position, View view,final ViewGroup parent) {
        View v = view;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(id, null);
        }

        datFM_File o = items.get(position);

        if (o != null) {
            text_FileName = (TextView) v.findViewById(R.id.textFileName);
            text_FileDescription = (TextView) v.findViewById(R.id.textFileDiscription);
            imgFileIcon = (ImageView) v.findViewById(R.id.imgFileIcon);

            /****/
            imgFileIcon.getLayoutParams().height = datFM.icons_size;
            imgFileIcon.getLayoutParams().width = datFM.icons_size;

            text_FileName.setTextSize(datFM.text_name_size);
            text_FileDescription.setTextSize(datFM.text_discr_size);

            text_FileName.setTypeface(datFM.font_typeface, datFM.font_style);
            text_FileDescription.setTypeface(datFM.font_typeface, datFM.font_style);
            /****/

            ll = (LinearLayout) v.findViewById(R.id.itemOfRow);

            if (sel[position]){
                ll.setBackgroundColor(datFM.color_item_selected);
            } else {
                if(/*datFM.pref_theme.equals("Dark") || */datFM.pref_theme.equals("Dark Fullscreen")){
                    //ll.setBackgroundDrawable(datFM.datFM_state.getResources().getDrawable(R.drawable.datfm_theme_stripe));
                    ll.setBackgroundColor(Color.parseColor("#88222222"));
                } else {
                ll.setBackgroundColor(Color.TRANSPARENT);
                }
            }

            if(text_FileName!=null){
                text_FileName.setText(o.getName());}
            if(text_FileDescription!=null){
                text_FileDescription.setText(o.getData());}

            if (o.getType().equals("parent_dir")){
                //imgFileIcon.setImageResource(R.drawable.ext_folder_up);
                imgFileIcon.setImageResource(getContext().getResources().getIdentifier("ext_folder_up"+"_"+datFM.pref_theme_icons, "drawable", "com.zlab.datFM"));

                if(datFM.pref_font_bold_folder){text_FileName.setTypeface(datFM.font_typeface, Typeface.BOLD);}
                if (datFM.pref_show_folder_discr){
                    text_FileDescription.setVisibility(View.VISIBLE);
                } else {
                    text_FileDescription.setVisibility(View.GONE);
                }
            } else if (o.getType().equals("dir")){
                //imgFileIcon.setImageResource(R.drawable.ext_folder);
                imgFileIcon.setImageResource(getContext().getResources().getIdentifier("ext_folder"+"_"+datFM.pref_theme_icons, "drawable", "com.zlab.datFM"));
                if(datFM.pref_font_bold_folder){text_FileName.setTypeface(datFM.font_typeface, Typeface.BOLD);}
                if (datFM.pref_show_folder_discr){
                    text_FileDescription.setVisibility(View.VISIBLE);
                } else {
                    text_FileDescription.setVisibility(View.GONE);
                }
            } else if (o.getType().equals("file")){
                if (datFM.pref_show_files_discr){
                    text_FileDescription.setVisibility(View.VISIBLE);
                } else {
                    text_FileDescription.setVisibility(View.GONE);
                }
                    /* Get ext */
                String ext = o.getExt();

                resID = getContext().getResources().getIdentifier("ext_" + ext.toLowerCase() + datFM.pref_theme_icons, "drawable", "com.zlab.datFM");

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
                    String[] photo = {"jpg", "png", "jpeg", "gif"};
                    resID = ext_check(photo,"photo",ext);
                }
                if (resID==0){
                    resID = getContext().getResources().getIdentifier("ext_unknown"+"_"+datFM.pref_theme_icons, "drawable", "com.zlab.datFM");
                }

                imgFileIcon.setImageResource(resID);
                icon_setter(o.getPath(), ext);

            } else if (o.getType().equals("favorite") || o.getType().startsWith("fav_bookmark") ){
                    //imgFileIcon.setImageResource(R.drawable.ext_favorite);
                    imgFileIcon.setImageResource(getContext().getResources().getIdentifier("ext_favorite"+"_"+datFM.pref_theme_icons, "drawable", "com.zlab.datFM"));
            } else if (o.getType().equals("network") || o.getType().equals("smb_store_network") || o.getType().equals("sftp_store_network") || o.getType().equals("ftp_store_network")){
                    //imgFileIcon.setImageResource(R.drawable.ext_network);
                    imgFileIcon.setImageResource(getContext().getResources().getIdentifier("ext_network"+"_"+datFM.pref_theme_icons, "drawable", "com.zlab.datFM"));
            } else if(o.getType().equals("sdcard")){
                    //imgFileIcon.setImageResource(R.drawable.ext_drive);
                    imgFileIcon.setImageResource(getContext().getResources().getIdentifier("ext_drive"+"_"+datFM.pref_theme_icons, "drawable", "com.zlab.datFM"));
            } else if(o.getType().equals("root")){
                    //imgFileIcon.setImageResource(R.drawable.ext_folder_root);
                    imgFileIcon.setImageResource(getContext().getResources().getIdentifier("ext_folder_root"+"_"+datFM.pref_theme_icons, "drawable", "com.zlab.datFM"));
            } else if(o.getType().equals("home")){
                    //imgFileIcon.setImageResource(R.drawable.ext_folder_home);
                    imgFileIcon.setImageResource(getContext().getResources().getIdentifier("ext_folder_home"+"_"+datFM.pref_theme_icons, "drawable", "com.zlab.datFM"));
            } else if(o.getType().equals("add")){
                //resID = getContext().getResources().getIdentifier("ext_" + ext.toLowerCase(), "drawable", "com.zlab.datFM");
                //imgFileIcon.setImageResource(R.drawable.ext_folder_add);
                imgFileIcon.setImageResource(getContext().getResources().getIdentifier("ext_folder_add"+"_"+datFM.pref_theme_icons, "drawable", "com.zlab.datFM"));
            } else if(o.getType().equals("action_exit")){
                imgFileIcon.setImageResource(getContext().getResources().getIdentifier("ext_action_exit"+"_"+datFM.pref_theme_icons, "drawable", "com.zlab.datFM"));
            }

            imgFileIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    datFM.datFM_state.openContextMenu(position, parent.getId());
                    datFM.datFM_state.update_panel_focus();
                }
            });

            imgFileIcon.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    return true;
                }
            });
        }
        return v;
    }

    public void icon_setter(String path, String ext){
        boolean apk=false;
        boolean photo=false;
        boolean video=false;
        boolean unknown=false;

        if (ext.equalsIgnoreCase("apk")){
            apk=true;
        } else if (ext.equalsIgnoreCase("jpg")  ||
                   ext.equalsIgnoreCase("png")  ||
                   ext.equalsIgnoreCase("jpeg") ||
                   ext.equalsIgnoreCase("gif")){
            photo=true;
        } else if (ext.equalsIgnoreCase("mkv")  ||
                   ext.equalsIgnoreCase("mp4")  ||
                   ext.equalsIgnoreCase("3gp")  ||
                   ext.equalsIgnoreCase("avi")  ||
                   ext.equalsIgnoreCase("mpg")  ||
                   ext.equalsIgnoreCase("mpeg")) {
            video=true;
        } else {
            unknown=true;
        }

        if(!unknown){
            boolean ic_finded=false;

            for(int i=0;i<datFM.cache_size;i++){
                if (datFM.cache_paths[i]!=null){
                    if (datFM.cache_paths[i].equals(path)){
                        if(datFM.cache_icons[i]!=null){
                            imgFileIcon.setImageDrawable(datFM.cache_icons[i]);}
                        ic_finded=true;
                        break;
                    }
                } else {
                    break;
                }
            }

            if(!ic_finded){
                    if(!datFM.scroll/* && datFM.protocols[datFM.curPanel].equals("local")*/){
                        if (apk && datFM.pref_show_apk){
                            icon_getter_apk(path);
                        }

                        if (photo && datFM.pref_show_photo){
                            icon_getter_photo(path);
                        }

                        if (video && datFM.pref_show_video){
                            icon_getter_video(path);
                        }
                    }
            }

        }
    }
    public void icon_getter_video(String path){
        if (datFM.cache_counter<datFM.cache_size){
            datFM.cache_icons[datFM.cache_counter]=null;
            datFM.cache_paths[datFM.cache_counter]=path;
            new datFM_IcoGen_Video(this).execute(path,String.valueOf(datFM.cache_counter));
            datFM.cache_counter++;
        } else {
            datFM.cache_counter=0;icon_getter_video(path);
        }
    }
    public void icon_getter_photo(String path){
        if (datFM.cache_counter<datFM.cache_size){
            datFM.cache_icons[datFM.cache_counter]=null;
            datFM.cache_paths[datFM.cache_counter]=path;
            new datFM_IcoGen_Photo(this).execute(path,String.valueOf(datFM.cache_counter));
            datFM.cache_counter++;
        } else {
            datFM.cache_counter=0;icon_getter_photo(path);
        }
    }
    public void icon_getter_apk(String path){
        if (datFM.cache_counter<datFM.cache_size){
            datFM.cache_icons[datFM.cache_counter]=null;
            datFM.cache_paths[datFM.cache_counter]=path;
            new datFM_IcoGen_APK(this).execute(path,String.valueOf(datFM.cache_counter));
            datFM.cache_counter++;
        } else {
            datFM.cache_counter=0;icon_getter_apk(path);
        }
    }

    public int ext_check(String[] ext_list, String name, String ext){
        int resID=0;
        for (String find_ext : ext_list) {
            if (find_ext.equals(ext)) {
                resID = getContext().getResources().getIdentifier("ext_" + name.toLowerCase()+"_"+datFM.pref_theme_icons, "drawable", "com.zlab.datFM");
                break;
            }
        }
        return resID;
    }

}
