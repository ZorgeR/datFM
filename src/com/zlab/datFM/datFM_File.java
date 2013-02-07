package com.zlab.datFM;

public class datFM_File {

        private String name;
        private String data;
        private String path;
        private String ext;
        private String size;
        private String protocol;

        public datFM_File(String n, String p, String e, String s, String prot, String d)
        {
            name = n;
            path = p;
            ext  = e;
            size = s;
            protocol = prot;
            data = d;
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
            return ext;
        }

        public String getSize()
        {
            return size;
        }

        public String getProtocol()
        {
            return protocol;
        }

        public String getData()
        {
            return data;
        }

}