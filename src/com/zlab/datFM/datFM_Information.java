package com.zlab.datFM;

public class datFM_Information implements Comparable<datFM_Information>{
    private String name;
    private String data;
    private String path;

    public datFM_Information(String n, String d, String p)
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
    public int compareTo(datFM_Information o) {
        if(this.name != null)
            return this.name.toLowerCase().compareTo(o.getName().toLowerCase());
        else
            throw new IllegalArgumentException();
    }
}
