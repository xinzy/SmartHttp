package com.xinzy.http.kotlin

import android.text.TextUtils
import okhttp3.Headers

/**
 * Created by Xinzy on 2018/10/12.
 */
internal class HttpHeader {

    val headers = mutableMapOf<String, String>()

    fun add(key: String, value: String): HttpHeader {
        if (TextUtils.isEmpty(key)) throw IllegalArgumentException("key cannot be null")
        headers.put(key, value)
        return this
    }

    fun merge(header: HttpHeader?): HttpHeader {
        return if (header == null) this else merge(header.headers)
    }

    fun merge(headers: Map<String, String>?): HttpHeader {
        headers?.let {
            for (key in it.keys) {
                val v = it[key]
                v?.let {  this.headers.put(key, v) }
            }
        }

        return this
    }

    fun convert(): Headers {
        return Headers.of(headers)
    }
}