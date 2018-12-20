package com.xinzy.http.kotlin

import android.text.TextUtils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import java.io.File

/**
 * Created by Xinzy on 2017/6/8.
 *
 */
internal class Cache(private val mCacheDir: File) {
    private val mResolver = Gson()

    fun save(key: String, data: String?) {
        data?.let { File(mCacheDir, "$key.1").writeText(it) }
    }

    fun save(key: String, header: Map<String, String>?) {
        header?.let { File(mCacheDir, "$key.0").writeText(mResolver.toJson(it)) }
    }

    fun get(key: String): String? {
        return File(mCacheDir, "$key.1").readText()
    }

    fun <T> get(key: String, token: TypeToken<*>): T? {
        val content = get(key) ?: return null
        return try {
            mResolver.fromJson<T>(content, token.type)
        } catch (e: Exception) {
            null
        }
    }

    fun <T> get(key: String, clazz: Class<*>): T? {
        val content = get(key) ?: return null
        return try {
            mResolver.fromJson<T>(content, clazz)
        } catch (e: Exception) {
            null
        }
    }

    fun header(key: String): Map<String, String>? {
        val content = File(mCacheDir, "$key.0").readText()
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