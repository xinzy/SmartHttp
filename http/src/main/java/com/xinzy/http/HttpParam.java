package com.xinzy.http;

import android.text.TextUtils;
import android.util.ArrayMap;

import com.google.gson.Gson;

import java.io.File;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * Created by Xinzy on 2017/5/19.
 */

class HttpParam {

    static final String ENCODE = "UTF-8";

    public static final MediaType MEDIA_TYPE_PLAIN = MediaType.parse("text/plain;charset=utf-8");
    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json;charset=utf-8");
    public static final MediaType MEDIA_TYPE_STREAM = MediaType.parse("application/octet-stream");

    private static final int TYPE_DEFAULT = 0;
    private static final int TYPE_MULTI = 1;
    private static final int TYPE_JSON = 2;

    List<Entry> params = new ArrayList<>();;
    private int paramType = TYPE_DEFAULT;

    HttpParam add(String key, String val) {
        if (!TextUtils.isEmpty(key)) {
            val = val == null ? "" : val;
            params.add(new Entry(key, val));
        }
        return this;
    }

    HttpParam add(Map<String, String> vals) {
        if (vals != null && vals.size() > 0) {
            Set<String> keys = vals.keySet();
            for (String key : keys) {
                add(key, vals.get(key));
            }
        }
        return this;
    }

    HttpParam multi(String key, File file, String filename) {
        if (!TextUtils.isEmpty(key)) {
            paramType = TYPE_MULTI;
            params.add(new Entry(key, filename, file));
        }
        return this;
    }

    HttpParam merge(HttpParam param) {
        if (param != null && param.params != null && param.params.size() > 0) {
            params.addAll(param.params);
        }
        return this;
    }

    HttpParam asJson() {
        paramType = TYPE_JSON;
        return this;
    }

    RequestBody body() {
        if (params.isEmpty()) {
            return RequestBody.create(MEDIA_TYPE_PLAIN, new byte[0]);
        }
        if (paramType == TYPE_MULTI) {
            MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
            for (Entry param : params) {
                if (param.isMulti) {
                    builder.addFormDataPart(param.key, param.value, RequestBody.create(param.getMediaType(), param.file));
                } else {
                    builder.addFormDataPart(param.key, Utils.encode(param.value));
                }
            }
            return builder.build();
        } else if (paramType == TYPE_JSON) {
            return RequestBody.create(MEDIA_TYPE_JSON, toJson());
        } else {
            FormBody.Builder builder = new FormBody.Builder();
            for (Entry param : params) {
                if (!param.isMulti) {
                    builder.add(param.key, param.value);
                }
            }
            return builder.build();
        }
    }

    Map<String, String> parameters() {
        Map<String, String> map = new ArrayMap<>();
        for (Entry entry : params) {
            if (entry != null && !entry.isMulti) {
                map.put(entry.key, entry.value);
            }
        }

        return map;
    }

    private String toJson() {
        if (params.size() == 0) {
            return "{}";
        } else {
            Map<String, String> map = new ArrayMap<>(params.size());
            for (Entry entry : params) {
                if (!entry.isMulti) {
                    map.put(entry.key, entry.value);
                }
            }
            return new Gson().toJson(map);
        }
    }

    static class Entry {
        String key;
        String value;
        File file;
        boolean isMulti;

        Entry(String key, String value) {
            this.key = key;
            this.value = value;
        }

        Entry(String key, String value, File file) {
            this.key = key;
            this.value = value;
            this.file = file;
            this.isMulti = true;
        }

        MediaType getMediaType() {
            FileNameMap fileNameMap = URLConnection.getFileNameMap();
            String name = file.getName().replace("#", "");
            String contentType = fileNameMap.getContentTypeFor(name);
            if (contentType != null) {
                return MediaType.parse(contentType);
            }
            return MEDIA_TYPE_STREAM;
        }
    }
}
