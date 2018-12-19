package com.xinzy.http.kotlin

import android.content.Context
import com.google.gson.Gson
import com.xinzy.http.kotlin.BuildConfig
import okhttp3.Interceptor
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager
import java.io.InputStream
import javax.net.ssl.SSLSession


/**
 * Created by Xinzy on 2018/10/12.
 */
class SmartHttpConfig private constructor() {

    internal var connectTimeout = DEFAULT_TIMEOUT
    internal var readTimeout = DEFAULT_TIMEOUT
    internal var writeTimeout = DEFAULT_TIMEOUT

    internal var isDebug = BuildConfig.DEBUG

    internal var cookieType: Int = 0

    internal var mResolver = Gson()

    internal var commonHeader = HttpHeader()
    internal var commonParam = HttpParam()

    internal var sslSocketFactory: SSLSocketFactory? = null
    internal var trustManager: X509TrustManager? = null
    internal var hostnameVerifier: HostnameVerifier? = null

    internal var interceptors = mutableListOf<Interceptor>()
    internal var networkInterceptors = mutableListOf<Interceptor>()


    /**
     * Connection Timeout
     * @param connectTimeout 单位秒
     * @return
     */
    fun connectTimeout(connectTimeout: Int): SmartHttpConfig {
        this.connectTimeout = connectTimeout
        return this
    }

    /**
     * Read Timeout
     * @param readTimeout 单位秒
     * @return
     */
    fun readTimeout(readTimeout: Int): SmartHttpConfig {
        this.readTimeout = readTimeout
        return this
    }

    /**
     * Write Timeout
     * @param writeTimeout 单位秒
     * @return
     */
    fun writeTimeout(writeTimeout: Int): SmartHttpConfig {
        this.writeTimeout = writeTimeout
        return this
    }

    /**
     * 设置请求通用头
     * @param key
     * @param value
     * @return
     */
    fun addCommonHeader(key: String, value: String): SmartHttpConfig {
        commonHeader.add(key, value)
        return this
    }

    /**
     * 设置请求通用头
     * @param headers
     * @return
     */
    fun addCommonHeader(headers: Map<String, String>): SmartHttpConfig {
        commonHeader.merge(headers)
        return this
    }

    /**
     * 设置请求通用参数
     * @param key
     * @param val
     * @return
     */
    fun addCommonParam(key: String, `val`: String): SmartHttpConfig {
        commonParam.add(key, `val`)
        return this
    }

    /**
     * 设置请求通用头
     * @param headers
     * @return
     */
    fun addCommonParam(params: Map<String, String>): SmartHttpConfig {
        if (params.isNotEmpty()) {
            val keys = params.keys
            keys.forEach { commonParam.add(it, params[it]) }
        }
        return this
    }

    /**
     * 设置是否开启debug
     * @param debug
     */
    fun debug(debug: Boolean): SmartHttpConfig {
        isDebug = debug
        return this
    }

    fun addInterceptor(interceptor: Interceptor): SmartHttpConfig {
        interceptors.add(interceptor)
        return this
    }

    fun addNetworkInterceptor(interceptor: Interceptor): SmartHttpConfig {
        networkInterceptors.add(interceptor)
        return this
    }

    /**
     * https双向认证
     * bksFile 和 password -> 客户端使用bks证书校验服务端证书
     * certificates -> 用含有服务端公钥的证书校验服务端证书
     * @param bksFile
     * @param password
     * @param certificates
     * @return
     */
    fun certificates(bksFile: InputStream? = null, password: String? = null, certificates: Array<InputStream>? = null): SmartHttpConfig {
        val param = sslSocketFactory(null, bksFile, password, certificates)
        sslSocketFactory = param.sSLSocketFactory
        trustManager = param.trustManager
        return this
    }

    /**
     * https双向认证
     * bksFile 和 password -> 客户端使用bks证书校验服务端证书
     * X509TrustManager -> 如果需要自己校验，那么可以自己实现相关校验，如果不需要自己校验，那么传null即可
     * @param bksFile
     * @param password
     * @param trustManager
     * @return
     */
    fun certificates(trustManager: X509TrustManager, bksFile: InputStream? = null, password: String? = null): SmartHttpConfig {
        val param = sslSocketFactory(trustManager, bksFile, password, null)
        sslSocketFactory = param.sSLSocketFactory
        this.trustManager = param.trustManager
        return this
    }

    fun hostnameVerifier(hostnameVerifier: HostnameVerifier = TrustHostnameVerifier()): SmartHttpConfig {
        this.hostnameVerifier = hostnameVerifier
        return this
    }

    fun resolver(resolver: Gson): SmartHttpConfig {
        mResolver = resolver
        return this
    }

    fun cookieType(@SmartHttp.CookieType type: Int): SmartHttpConfig {
        cookieType = type
        return this
    }

    fun init(context: Context) {
        SmartHttp.init(context, this)
    }

    internal inner class TrustHostnameVerifier : HostnameVerifier {
        override fun verify(hostname: String, session: SSLSession): Boolean {
            return true
        }
    }

    companion object {
        var DEFAULT_TIMEOUT = 10
        private var sInstance: SmartHttpConfig? = null

        fun getInstance(): SmartHttpConfig {
            if (sInstance == null) {
                synchronized(SmartHttpConfig::class) {
                    if (sInstance == null) {
                        sInstance = SmartHttpConfig()
                    }
                }
            }
            return sInstance!!
        }
    }
}