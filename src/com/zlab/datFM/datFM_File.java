package com.zlab.datFM;

public class datFM_File implements Comparable<datFM_File> {

        private String name;
        private String data;
        private String path;
        private String ext;
        private long size;
        private String protocol;
        private String parent;
        private String type;
        private long date;

        public datFM_File(String Name, String Path, long Size, String Prot, String Type, String Data, String Parent, long Date)
        {
            name = Name;
            path = Path;
            size = Size;
            protocol = Prot;
            type = Type;
            data = Data;
            parent = Parent;
            date = Date;
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
            if(dotPos!=0){
                ext = name.substring(dotPos);
                return ext;
            } else {
                return name;
            }
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

        public long getDate()
        {
            return date;
        }

        public int compareByName(datFM_File o) {
            if(this.name != null)
                return this.name.toLowerCase().compareTo(o.getName().toLowerCase());
            else
                throw new IllegalArgumentException();
        }

        public int compareByExt(datFM_File o) {
            if(this.getExt() != null)
                return this.getExt().toLowerCase().compareTo(o.getExt().toLowerCase());
            else
                throw new IllegalArgumentException();
        }

        public int compareTo(datFM_File o) {
            if(this.name != null)
                return this.name.toLowerCase().compareTo(o.getName().toLowerCase());
            else
                throw new IllegalArgumentException();
        }
}