package com.xinzy.http.kotlin

import com.google.gson.reflect.TypeToken
import java.lang.IllegalArgumentException

fun String.httpGet(): SmartHttp {
    return if (startsWith("http://") || startsWith("https://")) SmartHttp.get(this)
    else throw IllegalArgumentException("url must defer to http or https schema")
}

fun String.httpPost(): SmartHttp {
    return if (startsWith("http://") || startsWith("https://")) SmartHttp.post(this)
    else throw IllegalArgumentException("url must defer to http or https schema")
}

fun String.httpPut(): SmartHttp {
    return if (startsWith("http://") || startsWith("https://")) SmartHttp.put(this)
    else throw IllegalArgumentException("url must defer to http or https schema")
}

fun String.httpDelete(): SmartHttp {
    return if (startsWith("http://") || startsWith("https://")) SmartHttp.delete(this)
    else throw IllegalArgumentException("url must defer to http or https schema")
}

fun String.httphead(): SmartHttp {
    return if (startsWith("http://") || startsWith("https://")) SmartHttp.head(this)
    else throw IllegalArgumentException("url must defer to http or https schema")
}

fun String.httpPatch(): SmartHttp {
    return if (startsWith("http://") || startsWith("https://")) SmartHttp.patch(this)
    else throw IllegalArgumentException("url must defer to http or https schema")
}

fun <T> SmartHttp.enquene(clazz: Class<T>, success: (data: T?) -> Unit, failure: () -> Unit): T? {
    return enqueue(clazz, object : RequestCallback<T> {
        override fun onSuccess(t: T?) { success(t) }
        override fun onFailure(e: SmartHttpException) { failure() }
    })
}

fun <T> SmartHttp.enqueue(token: TypeToken<T>, success: (data: T?) -> Unit, failure: () -> Unit): T? {
    return enqueue(token, object : RequestCallback<T> {
        override fun onSuccess(t: T?) { success(t) }
        override fun onFailure(e: SmartHttpException) { failure() }
    })
}

fun SmartHttp.enquene(success: (data: String?) -> Unit, failure: () -> Unit): String? {
    return enqueue(object : RequestCallback<String> {
        override fun onSuccess(t: String?) { success(t) }
        override fun onFailure(e: SmartHttpException) { failure() }
    })
}