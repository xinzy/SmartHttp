package com.xinzy.http.kotlin

/**
 * Created by Xinzy on 2018/10/12.
 */
interface RequestCallback<T> {
    fun onSuccess(t: T?)
    fun onFailure(e: SmartHttpException)
}

class DefaultRequestCallback<T> : RequestCallback<T> {
    override fun onSuccess(t: T?) {}
    override fun onFailure(e: SmartHttpException) {}
}