package com.xinzy.http;

import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import java.util.Map;
import java.util.Set;

import okhttp3.Headers;

/**
 * Created by Xinzy on 2017/5/19.
 */

class HttpHeader {

    Map<String, String> headers;

    HttpHeader() {
        headers = new ArrayMap<>(8);
    }

    HttpHeader add(String key, String value) {
        if (TextUtils.isEmpty(key)) throw new IllegalArgumentException("key cannot be null");
        headers.put(key, value);
        return this;
    }

    HttpHeader merge(HttpHeader header) {
        if (header == null) return this;
        return merge(header.headers);
    }

    HttpHeader merge(Map<String, String> headers) {
        if (headers != null && headers.size() > 0) {
            Set<String> keys = headers.keySet();
            for (String key : keys) {
                if (!this.headers.containsKey(key)) {
                    this.headers.put(key, headers.get(key));
                }
            }
        }

        return this;
    }

    Headers convert() {
        return Headers.of(headers);
    }
}
