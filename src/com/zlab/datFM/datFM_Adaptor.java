package com.zlab.datFM;

import android.content.Context;
import android.graphics.Color;
import android.view.*;
import android.widget.*;

import java.util.List;

public class datFM_Adaptor extends ArrayAdapter<datFM_Information> {

    private Context c;
    private int id;
    private List<datFM_Information> items;
    private boolean[] sel;

    TextView text_FileName;
    TextView text_FileDescription;
    ImageView imgFileIcon;
    LinearLayout ll;
    int resID;

    public datFM_Adaptor(Context context, int LayoutID, List<datFM_Information> objects, boolean[] selected) {
        super(context, LayoutID, objects);
        c = context;
        id = LayoutID;
        items = objects;
        sel = selected;
    }
    public datFM_Information getItem(int i)
    {
        return items.get(i);
    }

    @Override
    public View getView(final int position, View view,final ViewGroup parent) {
        View v = view;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(id, null);
        }

        final datFM_Information o = items.get(position);

        if (o != null) {
            text_FileName = (TextView) v.findViewById(R.id.textFileName);
            text_FileDescription = (TextView) v.findViewById(R.id.textFileDiscription);
            imgFileIcon = (ImageView) v.findViewById(R.id.imgFileIcon);
            ll = (LinearLayout) v.findViewById(R.id.itemOfRow);

            if (sel[position]){
                ll.setBackgroundColor(Color.parseColor("#ff46b2ff"));
            } else {
                ll.setBackgroundColor(Color.TRANSPARENT);
            }

            if(text_FileName!=null){
                text_FileName.setText(o.getName());}
            if(text_FileDescription!=null){
                text_FileDescription.setText(o.getData());}

            if (o.getData().equals(datFM.datf_context.getResources().getString(R.string.fileslist_directory))){
                imgFileIcon.setImageResource(R.drawable.ext_folder);
                if (datFM.pref_show_folder_discr){
                    text_FileDescription.setVisibility(View.VISIBLE);
                } else {
                    text_FileDescription.setVisibility(View.GONE);
                }
            } else if (o.getData().equals(datFM.datf_context.getResources().getString(R.string.fileslist_parent_directory))){
                imgFileIcon.setImageResource(R.drawable.ext_folder_up);
                if (datFM.pref_show_folder_discr){
                    text_FileDescription.setVisibility(View.VISIBLE);
                } else {
                    text_FileDescription.setVisibility(View.GONE);
                }
            } else {
                    if (datFM.pref_show_files_discr){
                        text_FileDescription.setVisibility(View.VISIBLE);
                    } else {
                        text_FileDescription.setVisibility(View.GONE);
                    }
                    /* Get ext */
                    int dotPos = o.getName().lastIndexOf(".")+1;
                    String ext = o.getName().substring(dotPos);

                    resID = getContext().getResources().getIdentifier("ext_" + ext.toLowerCase(), "drawable", "com.zlab.datFM");
                    if (resID == 0) {
                        String[] docx = {"doc", "docx", "rtf", "odt"};
                        resID = ext_check(docx,"docx",ext);

                        if (resID==0){
                            String[] xlsx = {"xlsx", "xls", "ods"};
                            resID = ext_check(xlsx,"xlsx",ext);
                        }
                        if (resID==0){
                            String[] fb2  = {"fb2", "djvu", "chm", "epub", "ibooks"};
                            resID = ext_check(fb2,"fb2",ext);
                        }
                        if (resID==0){
                            String[] zip  = {"zip", "7z", "rar"};
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
                            resID = R.drawable.ext_unknown;
                        }
                    }

                    imgFileIcon.setImageResource(resID);
                    icon_setter(o.getPath(), ext);
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
                    //parent.showContextMenuForChild(view);
                    //parent.showContextMenuForChild(view);
                    //datFM.datFM_state.update_panel_focus();
                    return true;
                }
            });
        }
        return v;
    }

    public void icon_setter(String path, String ext){
        boolean ic_finded=false;

        for(int i=0;i<datFM.cache_size;i++){
            if (datFM.cache_paths[i]!=null){
                if (datFM.cache_paths[i].equals(path)){
                    imgFileIcon.setImageDrawable(datFM.cache_icons[i]);
                    ic_finded=true;
                    break;
                }
            }
        }

        if(!ic_finded){
          //if(!datFM.scroll){
            if (ext.equalsIgnoreCase("apk") && datFM.pref_show_apk){
                    icon_getter_apk(path);
            }
            if ((ext.equalsIgnoreCase("jpg")  ||
                 ext.equalsIgnoreCase("png")  ||
                 ext.equalsIgnoreCase("jpeg") ||
                 ext.equalsIgnoreCase("gif"))&&
                datFM.pref_show_photo){
                    icon_getter_photo(path);
            }
            if ((ext.equalsIgnoreCase("mkv") ||
                 ext.equalsIgnoreCase("mp4") ||
                 ext.equalsIgnoreCase("3gp"))&&
                datFM.pref_show_video){
                    icon_getter_video(path);
            }
          //}
        }
    }
    public void icon_getter_video(String path){
        if (datFM.cache_counter<datFM.cache_size){
            try {
                new datFM_IconGenerator_VIDEO(this).execute(path,String.valueOf(datFM.cache_counter));
                datFM.cache_counter++;
            } catch (Exception e){/*e.printStackTrace();*/}
        } else {
            datFM.cache_counter=0;
            new datFM_IconGenerator_VIDEO(this).execute(path,String.valueOf(datFM.cache_counter));
        }
    }
    public void icon_getter_photo(String path){
        if (datFM.cache_counter<datFM.cache_size){
            try {
                new datFM_IconGenerator_PHOTO(this).execute(path,String.valueOf(datFM.cache_counter));
                datFM.cache_counter++;
            } catch (Exception e){/*e.printStackTrace();*/}
        } else {
            datFM.cache_counter=0;
            new datFM_IconGenerator_PHOTO(this).execute(path,String.valueOf(datFM.cache_counter));
        }
    }
    public void icon_getter_apk(String path){
        if (datFM.cache_counter<datFM.cache_size){
            try {
                new datFM_IconGenerator_APK(this).execute(path,String.valueOf(datFM.cache_counter));
                datFM.cache_counter++;
            } catch (Exception e){/*e.printStackTrace();*/}
        } else {
            datFM.cache_counter=0;
            new datFM_IconGenerator_APK(this).execute(path,String.valueOf(datFM.cache_counter));
        }
    }
    public int ext_check(String[] ext_list, String name, String ext){
        int resID=0;
        for (String find_ext : ext_list) {
            if (find_ext.equals(ext)) {
                resID = getContext().getResources().getIdentifier("ext_" + name.toLowerCase(), "drawable", "com.zlab.datFM");
                break;
            }
        }
        return resID;
    }

}
