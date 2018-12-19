package com.xinzy.http.kotlin

import android.text.TextUtils
import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject
import java.io.File
import java.net.URLConnection

/**
 * Created by Xinzy on 2018/10/12.
 */
internal class HttpParam {

    companion object {
        internal const val ENCODE = "UTF-8"

        private val MEDIA_TYPE_PLAIN = MediaType.parse("text/plain;charset=utf-8")
        private val MEDIA_TYPE_JSON = MediaType.parse("application/json;charset=utf-8")
        private val MEDIA_TYPE_STREAM = MediaType.parse("application/octet-stream")
    }

    val params = mutableListOf<Entry>()
    var isMulti = false
    var isJSON = false
    var mJSON: String? = null

    fun add(key: String, value: String?): HttpParam {
        if (!TextUtils.isEmpty(key)) {
            val v = value ?: ""
            params.add(Entry(key, v))
        }
        return this
    }

    fun add(vals: Map<String, String>?): HttpParam {
        if (vals != null && vals.isNotEmpty()) {
            val keys = vals.keys
            keys.forEach { add(it, vals[it])}
        }
        return this
    }

    fun asJson(): HttpParam {
        isJSON = true
        return this
    }

    fun json(json: String?): HttpParam {
        mJSON = json
        isJSON = true
        return this
    }

    fun multi(key: String, file: File, filename: String): HttpParam {
        if (!TextUtils.isEmpty(key)) {
            isMulti = true
            params.add(Entry(key, filename, file))
        }
        return this
    }

    fun merge(param: HttpParam?): HttpParam {
        if (param?.params != null && param.params.size > 0) {
            params.addAll(param.params)
        }
        return this
    }

    fun body(): RequestBody {
        if (isJSON) {
            return when {
                mJSON != null -> RequestBody.create(MEDIA_TYPE_JSON, mJSON!!.toByteArray())
                params.isEmpty() -> RequestBody.create(MEDIA_TYPE_JSON, "{}")
                else -> RequestBody.create(MEDIA_TYPE_JSON, toJSON())
            }
        }
        if (params.isEmpty()) {
            return RequestBody.create(MEDIA_TYPE_PLAIN, ByteArray(0))
        }

        return when {
            params.isEmpty() -> RequestBody.create(MEDIA_TYPE_PLAIN, ByteArray(0))
            isMulti -> {
                val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
                for (param in params) {
                    if (param.isMulti) {
                        builder.addFormDataPart(param.key, param.value, RequestBody.create(param.mediaType, param.file!!))
                    } else {
                        builder.addFormDataPart(param.key, encode(param.value))
                    }
                }
                builder.build()
            }
            else -> {
                val builder = FormBody.Builder()
                params.filterNot { it.isMulti }.forEach { builder.add(it.key, it.value) }
                builder.build()
            }
        }
    }

    fun parameter(): Map<String, String> {
        return if (params.isEmpty()) {
            mapOf()
        } else {
            val map = mutableMapOf<String, String>()
            params.filterNot { !it.isMulti }.forEach { item -> run {
                map.put(item.key, item.value)
            } }
            map
        }
    }

    private fun toJSON(): String {
        return try {
            JSONObject(parameter()).toString()
        } catch (e: Exception) {
            "{}"
        }
    }

    internal class Entry {
        var key: String
        var value: String
        var file: File? = null
        var isMulti: Boolean = false

        val mediaType: MediaType?
            get() {
                if (file == null) throw IllegalArgumentException("upload file is null")

                val fileNameMap = URLConnection.getFileNameMap()
                val name = file!!.name.replace("#", "")
                val contentType = fileNameMap.getContentTypeFor(name)
                return if (contentType != null) {
                    MediaType.parse(contentType)
                } else MEDIA_TYPE_STREAM
            }

        constructor(key: String, value: String) {
            this.key = key
            this.value = value
        }

        constructor(key: String, value: String, file: File) {
            this.key = key
            this.value = value
            this.file = file
            this.isMulti = true
        }
    }
}