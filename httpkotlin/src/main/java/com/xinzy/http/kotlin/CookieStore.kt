package com.xinzy.http.kotlin

import android.support.v4.util.ArrayMap
import android.text.TextUtils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import java.io.File
import java.util.ArrayList
import java.util.Collections

import okhttp3.Cookie
import okhttp3.HttpUrl

/**
 * Created by Xinzy on 2017/6/9.
 */

internal interface CookieStore {
    fun save(url: HttpUrl, cookies: List<Cookie>)
    fun load(url: HttpUrl): List<Cookie>
}

internal class MemoryCookieStore internal constructor() : CookieStore {
    private val mCookies: MutableMap<String, List<Cookie>> = Collections.synchronizedMap(ArrayMap())

    override fun save(url: HttpUrl, cookies: List<Cookie>) {
        mCookies[url.host()] = cookies
    }

    override fun load(url: HttpUrl): List<Cookie> {
        val host = url.host()
        val cookies = mCookies[host]
        if (cookies != null && cookies.isNotEmpty()) {
            val cs = ArrayList<Cookie>()
            for (cookie in cookies) {
                if (!isCookieExpired(cookie)) {
                    cs.add(cookie)
                }
            }
            return cs
        }

        return ArrayList(0)
    }
}

internal class PersistentCookieStore internal constructor(private val mCookieDir: File) : CookieStore {
    private val mGson = Gson()

    init {
        if (!mCookieDir.exists()) {
            mCookieDir.mkdirs()
        }
    }

    override fun save(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isNotEmpty()) {
            val file = File(mCookieDir, md5(url.host()))

            val cs = ArrayList<C>()
            for (cookie in cookies) {
                cs.add(C.convert(cookie))
            }
            val content = mGson.toJson(cs)
            write(file, content)
        }
    }

    override fun load(url: HttpUrl): List<Cookie> {
        val f = File(mCookieDir, md5(url.host()))
        val content = read(f)
        val cookies = ArrayList<Cookie>()
        if (!TextUtils.isEmpty(content)) {
            try {
                val list = mGson.fromJson<List<C>>(content, object : TypeToken<List<C>>() {
                }.type)
                if (list != null && list.isNotEmpty()) {
                    for (c in list) {
                        val cookie = c.convert()
                        if (!isCookieExpired(cookie)) {
                            cookies.add(cookie)
                        }
                    }
                }
            } catch (e: Exception) {
            }
        }
        return cookies
    }
}

internal class C internal constructor(var name: String, var value: String, var expiresAt: Long, var domain: String, var path: String,
                             var secure: Boolean, var httpOnly: Boolean, var persistent: Boolean, var hostOnly: Boolean) {

    internal fun convert(): Cookie {
        val builder = Cookie.Builder().name(name).value(value).expiresAt(expiresAt).domain(domain).path(path)
        if (secure) {
            builder.secure()
        }
        if (hostOnly) {
            builder.httpOnly()
        }
        return builder.build()
    }

    companion object {

        internal fun convert(cookie: Cookie): C {
            return C(cookie.name(), cookie.value(), cookie.expiresAt(), cookie.domain(), cookie.path(),
                    cookie.secure(), cookie.httpOnly(), cookie.persistent(), cookie.hostOnly())
        }
    }
}