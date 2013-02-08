package com.zlab.datFM;

public class datFM_FileInformation implements Comparable<datFM_FileInformation> {

        private String name;
        private String data;
        private String path;
        private String ext;
        private long size;
        private String protocol;
        private String parent;
        private String type;

        public datFM_FileInformation(String Name, String Path, long Size, String Prot, String Type, String Data, String Parent)
        {
            name = Name;
            path = Path;
            size = Size;
            protocol = Prot;
            type = Type;
            data = Data;
            parent = Parent;
        }

        public String getName()
        {
            return name;
        }

        public String getPath()
        {
            return path;
        }

        public String getExt()
        {
            int dotPos = name.lastIndexOf(".")+1;
            String ext = name.substring(dotPos);
            return ext;
        }

        public long getSize()
        {
            return size;
        }

        public String getProtocol()
        {
            return protocol;
        }

        public String getType()
        {
            return type;
        }
        public String getParent(){
            return parent;
        }

        public String getData()
        {
            return data;
        }

        public int compareTo(datFM_FileInformation o) {
        if(this.name != null)
            return this.name.toLowerCase().compareTo(o.getName().toLowerCase());
        else
            throw new IllegalArgumentException();
        }
}