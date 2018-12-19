package com.xinzy.http.kotlin

import android.text.TextUtils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import java.io.File
import java.lang.reflect.Type

/**
 * Created by Xinzy on 2017/6/8.
 *
 */
internal interface Cache {

    fun save(key: String, data: String?)

    fun save(key: String, header: Map<String, String>?)

    fun get(key: String): String?

    fun <T> get(key: String, token: TypeToken<T>): T?

    fun <T> get(key: String, clazz: Class<T>): T?

    fun header(key: String): Map<String, String>?
}

internal class CacheImpl(private val mCacheDir: File) : Cache {
    private val mResolver = Gson()

    override fun save(key: String, data: String?) {
        data?.let { write(File(mCacheDir, "$key.1"), it) }
    }

    override fun save(key: String, header: Map<String, String>?) {
        header?.let { write(File(mCacheDir, "$key.0"), mResolver.toJson(it)) }
    }

    override fun get(key: String): String? {
        return read(File(mCacheDir, "$key.1"))
    }

    override fun <T> get(key: String, token: TypeToken<T>): T? {
        val content = get(key) ?: return null
        return try {
            mResolver.fromJson<T>(content, token.type)
        } catch (e: Exception) {
            null
        }
    }

    override fun <T> get(key: String, clazz: Class<T>): T? {
        val content = get(key) ?: return null
        return try {
            mResolver.fromJson<T>(content, clazz)
        } catch (e: Exception) {
            null
        }
    }

    override fun header(key: String): Map<String, String>? {
        val cacheFile = File(mCacheDir, "$key.0")
        val content = read(cacheFile)
        if (TextUtils.isEmpty(content)) {
            return null
        }
        return try {
            mResolver.fromJson<Map<String, String>>(content, object : TypeToken<Map<String, String>>() {}.type)
        } catch (e: Exception) {
            null
        }
    }
}