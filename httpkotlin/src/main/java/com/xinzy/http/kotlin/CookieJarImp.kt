package com.xinzy.http.kotlin

import java.io.File

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Created by Xinzy on 2017/6/9.
 */

internal class CookieJarImp : CookieJar {
    private val mCookieStore: CookieStore

    constructor() {
        mCookieStore = MemoryCookieStore()
    }

    constructor(dir: File) {
        val cookieDir = File(dir, "cookie")
        mCookieStore = PersistentCookieStore(cookieDir)
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        mCookieStore.save(url, cookies)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return mCookieStore.load(url)
    }
}
