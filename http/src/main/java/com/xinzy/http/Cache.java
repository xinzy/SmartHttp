package com.xinzy.http;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Created by Xinzy on 2017/6/8.
 *
 */
interface Cache {

    void save(String key, String data);

    void save(String key, Map<String, String> header);

    String get(String key);

    <T> T get(String key, Type type);

    Map<String, String> header(String key);



    class CacheImpl implements Cache {
        private Gson mResolver;
        private File mCacheDir;

        CacheImpl(String path) {
            mResolver = new Gson();
            mCacheDir = new File(path);
        }

        @Override
        public void save(String key, String data) {
            File cacheFile = new File(mCacheDir, key + ".1");
            Utils.write(cacheFile, data);
        }

        @Override
        public void save(String key, Map<String, String> header) {
            if (header != null) {
                String h = mResolver.toJson(header);
                File cacheFile = new File(mCacheDir, key + ".0");
                Utils.write(cacheFile, h);
            }
        }

        @Override
        public String get(String key) {
            File cacheFile = new File(mCacheDir, key + ".1");
            return Utils.read(cacheFile);
        }

        @Override
        public <T> T get(String key, Type type) {
            String content = get(key);
            T data;
            try {
                data = mResolver.fromJson(content, type);
            } catch (Exception e) {
                data = null;
            }
            return data;
        }

        @Override
        public Map<String, String> header(String key) {
            File cacheFile = new File(mCacheDir, key + ".0");
            String content = Utils.read(cacheFile);
            if (TextUtils.isEmpty(content)) {
                return null;
            }
            try {
                return mResolver.fromJson(content, new TypeToken<Map<String, String>>(){}.getType());
            } catch (Exception e) {
                return null;
            }
        }
    }
}
