package com.xinzy.smarthttp;

import android.util.ArrayMap;

import java.util.List;

/**
 * Created by shaozeng.yang on 2017/6/8.
 */

public class Res {
    public String method;
    public ArrayMap<String, String> get;
    public ArrayMap<String, String> post;
    public ArrayMap<String, String> header;
    public ArrayMap<String, String> put;
    public ArrayMap<String, String> delete;
    public ArrayMap<String, String> cookie;
    public List<Files> files;

    @Override
    public String toString() {
        return "Res{" +
                "method='" + method + '\'' +
                ", get=" + get +
                ", post=" + post +
                ", header=" + header +
                ", put=" + put +
                ", delete=" + delete +
                ", cookie=" + cookie +
                ", files=" + files +
                '}';
    }

    public class Files {
        public String name;
        public String type;
        public String url;
        public int size;

        @Override
        public String toString() {
            return "Files{" + "name='" + name + '\'' +
                    ", type='" + type + '\'' + ", url='" + url + '\'' + ", size=" + size + '}';
        }
    }
}
