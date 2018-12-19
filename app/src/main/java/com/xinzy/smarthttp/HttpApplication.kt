package com.xinzy.smarthttp

import android.app.Application

import com.xinzy.http.SmartHttp
import com.xinzy.http.SmartHttpConfig
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Created by on 2017/6/8.
 */

class HttpApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val config = SmartHttpConfig.getInstance().debug(true).connectTimeout(30).writeTimeout(30)
                .readTimeout(30).addCommonHeader("key", "val").addCommonParam("key", "val").cookie(SmartHttp.MEMORY_COOKIE)
                .addNetworkInterceptor(InitInterceptor())
        config.initHttpClient(this)


        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        com.xinzy.http.kotlin.SmartHttpConfig.getInstance().debug(true).connectTimeout(10)
                .writeTimeout(10).readTimeout(10).addCommonHeader("header", "headerValue")
                .addCommonParam("param", "paramValue").cookieType(com.xinzy.http.kotlin.SmartHttp.PERSISTENT_COOKIE)
                .hostnameVerifier().certificates()//.addInterceptor(interceptor)
                .init(this)
    }
}
