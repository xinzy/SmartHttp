package com.xinzy.http;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Created by Xinzy on 2017/6/8.
 *
 */

class Cache {

    private File mCacheFile;
    private Gson mGson = new Gson();

    Cache(File dir, String key) {
        if (dir != null && !TextUtils.isEmpty(key)) {
            mCacheFile = new File(dir, Utils.md5(key));
        }
    }

    boolean save(String content) {
        if (!TextUtils.isEmpty(content)) {
            return Utils.write(mCacheFile, content);
        }
        return false;
    }

    @Nullable
    String read() {
        return Utils.read(mCacheFile);
    }

    @Nullable
    <T> T read(Class<T> cls) {
        String text = read();
        if (TextUtils.isEmpty(text)) return null;
        try {
            return mGson.fromJson(text, cls);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    <T> T read(TypeToken<T> reference) {
        String text = read();
        if (TextUtils.isEmpty(text)) return null;
        try {
            return mGson.fromJson(text, reference.getType());
        } catch (Exception e) {
            return null;
        }
    }


    static void clearCache(File dir, long maxSize, long maxExpire) {
        new ClearCacheTask(dir, maxSize, maxExpire).start();
    }

    static class ClearCacheTask extends Thread {
        private static final float THRESHOLD = .75f;
        private File dir;
        private long maxSize;
        private long maxExpire;

        public ClearCacheTask(File dir, long maxSize, long maxExpire) {
            super("ClearCacheTask");
            this.dir = dir;
            this.maxSize = maxSize;
            this.maxExpire = maxExpire;
        }

        @Override
        public void run() {
            if (dir == null || !dir.exists()) return;
            final long currentTime = System.currentTimeMillis();
            final long expire = currentTime - maxExpire;
            final long threshold = (long) (maxSize * THRESHOLD);
            long size = 0;
            List<File> files = new ArrayList<>();

            File[] children = dir.listFiles();
            if (children == null) return;
            for (File f : children) {
                if (".".equals(f.getName()) || "..".equals(f.getName())) continue;
                if (f.lastModified() > expire || !f.delete()) {
                    size += f.length();
                    files.add(f);
                }
            }

            if (size <= threshold) return;
            Collections.sort(files, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    if (o1.lastModified() == o2.lastModified()) return 0;
                    return o1.lastModified() < o2.lastModified() ? -1 : 1;
                }
            });
            long deleteSize = size - threshold;
            size = 0;
            for (File file : files) {
                long length = file.length();
                if (file.delete()) {
                    size += length;
                }
                if (size > deleteSize) break;
            }
        }
    }
}
