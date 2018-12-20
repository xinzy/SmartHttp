package com.xinzy.http.kotlin

import android.text.TextUtils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import java.io.File

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
    private val mCookies = mutableMapOf<String, List<Cookie>>()

    override fun save(url: HttpUrl, cookies: List<Cookie>) {
        mCookies[url.host()] = cookies
    }

    override fun load(url: HttpUrl): List<Cookie> {
        val host = url.host()
        val cookies = mCookies[host]
        if (cookies != null && cookies.isNotEmpty()) {
            val cs = mutableListOf<Cookie>()
            cookies.filterNot { isCookieExpired(it) }.forEach { cs.add(it) }
            return cs
        }

        return listOf()
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
            val cs = mutableListOf<C>()
            cookies.forEach { cs.add(C.convert(it)) }
            File(mCookieDir, md5(url.host())).writeText(mGson.toJson(cs))
        }
    }

    override fun load(url: HttpUrl): List<Cookie> {
        val content = File(mCookieDir, md5(url.host())).readText()
        val cookies = mutableListOf<Cookie>()
        if (!TextUtils.isEmpty(content)) {
            try {
                val list = mGson.fromJson<List<C>>(content, object : TypeToken<List<C>>() {}.type)
                list?.let { cs -> run {
                    cs.filter { it.isExpired() }.forEach { cookies.add(it.convert()) }
                } }
            } catch (e: Exception) {
            }
        }
        return cookies
    }
}

internal class C internal constructor(private var name: String, private var value: String, private var expiresAt: Long,
                                      private var domain: String, private var path: String, private var secure: Boolean,
                                      private var httpOnly: Boolean, private var persistent: Boolean,
                                      private var hostOnly: Boolean) {

    internal fun convert(): Cookie {
        val builder = Cookie.Builder().name(name).value(value).expiresAt(expiresAt).domain(domain).path(path)
        if (secure) {
            builder.secure()
        }
        if (httpOnly) {
            builder.httpOnly()
        }
        return builder.build()
    }

    fun isExpired() = isCookieExpired(convert())

    companion object {
        internal fun convert(cookie: Cookie): C {
            return C(cookie.name(), cookie.value(), cookie.expiresAt(), cookie.domain(), cookie.path(),
                    cookie.secure(), cookie.httpOnly(), cookie.persistent(), cookie.hostOnly())
        }
    }
}