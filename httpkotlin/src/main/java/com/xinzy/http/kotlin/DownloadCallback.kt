package com.xinzy.http.kotlin

/**
 * Created by Xinzy on 2018/10/12.
 */
interface DownloadCallback {
    fun onStart()
    fun onLoading(current: Long, total: Long)
    fun onEnd()
    fun onFailure(e: SmartHttpException)
}

class DefaultDownloadCallback: DownloadCallback {
    override fun onStart() {}

    override fun onLoading(current: Long, total: Long) {}

    override fun onEnd() {}

    override fun onFailure(e: SmartHttpException) {}
}