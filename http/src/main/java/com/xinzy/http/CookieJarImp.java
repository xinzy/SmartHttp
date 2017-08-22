package com.xinzy.http;

import android.support.annotation.NonNull;

import java.io.File;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/**
 * Created by Xinzy on 2017/6/9.
 */

class CookieJarImp implements CookieJar {
    private CookieStore mCookieStore;

    public CookieJarImp() {
        mCookieStore = new CookieStore.MemoryCookieStore();
    }

    CookieJarImp(@NonNull String dir) {
        File cookieDir = new File(dir, "cookie");
        mCookieStore = new CookieStore.PersistentCookieStore(cookieDir);
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        mCookieStore.save(url, cookies);
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        return mCookieStore.load(url);
    }
}
