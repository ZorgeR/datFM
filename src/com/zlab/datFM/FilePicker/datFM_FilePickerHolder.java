package com.zlab.datFM.FilePicker;

public class datFM_FilePickerHolder implements Comparable<datFM_FilePickerHolder>{
    private String name;
    private String data;
    private String path;

    public datFM_FilePickerHolder(String n,String d,String p)
    {
        name = n;
        data = d;
        path = p;
    }
    public String getName()
    {
        return name;
    }
    public String getData()
    {
        return data;
    }
    public String getPath()
    {
        return path;
    }
    public int compareTo(datFM_FilePickerHolder o) {
        if(this.name != null)
            return this.name.toLowerCase().compareTo(o.getName().toLowerCase());
        else
            throw new IllegalArgumentException();
    }
}
