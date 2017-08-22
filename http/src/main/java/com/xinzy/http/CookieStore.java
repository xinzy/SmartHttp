package com.xinzy.http;

import android.text.TextUtils;
import android.util.ArrayMap;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

/**
 * Created by Xinzy on 2017/6/9.
 */

interface CookieStore {
    void save(HttpUrl url, List<Cookie> cookies);

    List<Cookie> load(HttpUrl url);


    class MemoryCookieStore implements CookieStore {
        private Map<String, List<Cookie>> mCookies;

        MemoryCookieStore() {
            mCookies = Collections.synchronizedMap(new ArrayMap<String, List<Cookie>>());
        }

        @Override
        public void save(HttpUrl url, List<Cookie> cookies) {
            Utils.debug("MemoryCookieStore: cookie = " + cookies);
            mCookies.put(url.host(), cookies);
        }

        @Override
        public List<Cookie> load(HttpUrl url) {
            String host = url.host();
            List<Cookie> cookies = mCookies.get(host);
            if (cookies != null && cookies.size() > 0) {
                List<Cookie> cs = new ArrayList<>();
                for (Cookie cookie : cookies) {
                    if (!Utils.isCookieExpired(cookie)) {
                        cs.add(cookie);
                    }
                }
                return cs;
            }

            return new ArrayList<>(0);
        }
    }

    class PersistentCookieStore implements CookieStore {
        private File mCookieDir;
        private Gson mGson;

        PersistentCookieStore(File dir) {
            this.mCookieDir = dir;
            if (!mCookieDir.exists()) {
                mCookieDir.mkdirs();
            }
            mGson = new Gson();
        }

        @Override
        public void save(HttpUrl url, List<Cookie> cookies) {
            if (cookies != null && cookies.size() > 0) {
                File file = new File(mCookieDir, Utils.md5(url.host()));

                List<C> cs = new ArrayList<>();
                for (Cookie cookie : cookies) {
                    cs.add(C.convert(cookie));
                }
                String content = mGson.toJson(cs);
                Utils.debug("PersistentCookieStore: cookie = " + content);
                Utils.write(file, content);
            }
        }

        @Override
        public List<Cookie> load(HttpUrl url) {
            File f = new File(mCookieDir, Utils.md5(url.host()));
            String content = Utils.read(f);
            List<Cookie> cookies = new ArrayList<>();
            if (!TextUtils.isEmpty(content)) {
                try {
                    List<C> list = mGson.fromJson(content, new TypeToken<List<C>>(){}.getType());
                    if (list != null && list.size() > 0) {
                        for (C c : list) {
                            Cookie cookie = c.convert();
                            if (!Utils.isCookieExpired(cookie)) {
                                cookies.add(cookie);
                            }
                        }
                    }
                } catch (Exception e) {
                }
            }
            Utils.debug("PersistentCookieStore.load cookie: " + cookies);
            return cookies;
        }
    }

    class C {
        public String name;
        public String value;
        public long expiresAt;
        public String domain;
        public String path;
        public boolean secure;
        public boolean httpOnly;

        public boolean persistent;
        public boolean hostOnly;

        C(String name, String value, long expiresAt, String domain, String path,
                 boolean secure, boolean httpOnly, boolean persistent, boolean hostOnly) {
            this.name = name;
            this.value = value;
            this.expiresAt = expiresAt;
            this.domain = domain;
            this.path = path;
            this.secure = secure;
            this.httpOnly = httpOnly;
            this.persistent = persistent;
            this.hostOnly = hostOnly;
        }

        static C convert(Cookie cookie) {
            return new C(cookie.name(), cookie.value(), cookie.expiresAt(), cookie.domain(), cookie.path(),
                    cookie.secure(), cookie.httpOnly(), cookie.persistent(), cookie.hostOnly());
        }

        Cookie convert() {
            Cookie.Builder builder = new Cookie.Builder().name(name).value(value).expiresAt(expiresAt).domain(domain).path(path);
            if (secure) {
                builder.secure();
            }
            if (hostOnly) {
                builder.httpOnly();
            }
            return builder.build();
        }
    }
}
