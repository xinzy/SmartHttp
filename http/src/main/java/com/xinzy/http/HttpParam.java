package com.xinzy.http;

import android.text.TextUtils;
import android.util.ArrayMap;

import java.io.File;
import java.net.FileNameMap;
import java.net.URLConnection;
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

    public static final MediaType MEDIA_TYPE_PLAIN = MediaType.parse("text/plain;charset=utf-8");
    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json;charset=utf-8");
    public static final MediaType MEDIA_TYPE_STREAM = MediaType.parse("application/octet-stream");

    Map<String, Entry> params;
    boolean isMulti = false;

    HttpParam() {
        params = new ArrayMap<>();
    }

    HttpParam add(String key, String val) {
        if (!TextUtils.isEmpty(key)) {
            val = val == null ? "" : val;
            params.put(key, new Entry(key, val));
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
            isMulti = true;
            params.put(key, new Entry(key, filename, file));
        }
        return this;
    }

    HttpParam merge(HttpParam param) {
        if (param != null && param.params != null && param.params.size() > 0) {
            Set<String> keys = param.params.keySet();
            for (String key : keys) {
                if (!params.containsKey(key)) {
                    params.put(key, param.params.get(key));
                }
            }
        }
        return this;
    }

    RequestBody body() {
        if (params.isEmpty()) {
            return RequestBody.create(null, new byte[0]);
        }
        if (isMulti) {
            MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
            Set<String> keys = params.keySet();
            for (String key : keys) {
                Entry param = params.get(key);
                if (param.isMulti) {
                    builder.addFormDataPart(param.key, param.value, RequestBody.create(param.getMediaType(), param.file));
                } else {
                    builder.addFormDataPart(param.key, param.value);
                }
            }
            return builder.build();
        } else {
            FormBody.Builder builder = new FormBody.Builder();
            Set<String> keys = params.keySet();
            for (String key : keys) {
                Entry param = params.get(key);
                if (!param.isMulti) {
                    builder.add(param.key, param.value);
                }
            }
            return builder.build();
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
