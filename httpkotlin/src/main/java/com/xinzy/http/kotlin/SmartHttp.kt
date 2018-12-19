package com.xinzy.http.kotlin

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.support.annotation.IntDef
import android.support.annotation.Nullable
import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class SmartHttp private constructor(private val mUrl: String/** url  */, private val mRequestMethod: Int = METHOD_GET/** 请求方式  */) {

    /** header */
    private val mHeader = HttpHeader()

    /** parameter */
    private val mParam = HttpParam()

    /** json resolver */
    private var mResolver: Gson? = null

    private var mTag: Any? = null
    private var mCacheKey: String? = null

    private var mTargetClazz: Class<*>? = null
    private var mTargetTypeToken: TypeToken<*>? = null

    private val mHandler = Handler(Looper.getMainLooper())

    /**
     * 设置json解析器
     */
    fun resolver(resolver: Gson): SmartHttp {
        mResolver = resolver
        return this
    }

    /**
     * 添加header信息
     * @param key
     * @param val
     * @return
     */
    fun header(key: String, `val`: String): SmartHttp {
        mHeader.add(key, `val`)
        return this
    }

    /**
     * 设置header
     * @param header
     * @return
     */
    fun header(header: Map<String, String>): SmartHttp {
        mHeader.merge(header)
        return this
    }

    /**
     * 添加参数
     * @param key
     * @param val
     * @return
     */
    fun param(key: String, `val`: String): SmartHttp {
        mParam.add(key, `val`)
        return this
    }

    /**
     * 设置参数
     * @param param
     * @return
     */
    fun param(param: Map<String, String>): SmartHttp {
        mParam.add(param)
        return this
    }

    /**
     * 添加上传的文件
     * @param key
     * @param file
     * @param filename
     * @return
     */
    fun param(key: String, file: File, filename: String = file.name): SmartHttp {
        if (mRequestMethod != METHOD_POST && mRequestMethod != METHOD_PUT && mRequestMethod != METHOD_PATCH) {
            throw IllegalStateException("Upload need POST / PUT / PATCH method")
        }
        if (!file.exists()) {
            throw IllegalArgumentException("Upload file is null or not exist")
        }
        mParam.multi(key, file, filename)
        return this
    }

    /**
     * 参数作为json传递
     */
    fun paramAsJson(): SmartHttp {
        if (mRequestMethod != METHOD_POST && mRequestMethod != METHOD_PUT && mRequestMethod != METHOD_PATCH) {
            throw IllegalStateException("Upload need POST / PUT / PATCH method")
        }
        mParam.asJson()
        return this
    }

    /**
     * JSON参数
     */
    fun param(json: String): SmartHttp {
        if (mRequestMethod != METHOD_POST && mRequestMethod != METHOD_PUT && mRequestMethod != METHOD_PATCH) {
            throw IllegalStateException("Upload need POST / PUT / PATCH method")
        }
        mParam.json(json)
        return this
    }

    /**
     * 设置tag 可以通过该tag关闭请求
     * @param tag
     * @return
     */
    fun tag(tag: Any): SmartHttp {
        mTag = tag
        return this
    }

    /**
     * 设置网络缓存的key。默认使用url 的md5
     * @param key
     * @return
     */
    fun cacheKey(key: String): SmartHttp {
        mCacheKey = md5(key)
        return this
    }

    /**
     * 异步请求
     */
    fun <T> enqueue(clazz: Class<T>, callback: RequestCallback<T>?): T? {
        mTargetClazz = clazz
        return realEnqueue(callback)
    }

    /**
     * 异步请求
     */
    fun <T> enqueue(typeToken: TypeToken<T>, callback: RequestCallback<T>?): T? {
        mTargetTypeToken = typeToken
        return realEnqueue(callback)
    }

    /**
     * 异步请求
     */
    fun enqueue(callback: RequestCallback<String>?): String? {
        return realEnqueue(callback)
    }

    private fun <T> realEnqueue(rawCallback: RequestCallback<T>?): T? {
        val callback = rawCallback ?: DefaultRequestCallback()
        val client = getClient()
        val request = request()

        try {
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    log("request failure", e)
                    if (call.isCanceled) return
                    postError(callback, SmartHttpException(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    if (call.isCanceled) return
                    try {
                        if (response.isSuccessful) {
                            val content = response.body()?.string()
                            if (content == null) {
                                log("http response is null")
                                postSuccess(callback, null)
                            } else {
                                saveCache(content)
                                when {
                                    mTargetClazz != null -> postSuccess(callback, mResolver!!.fromJson<T>(content, mTargetClazz))
                                    mTargetTypeToken != null -> postSuccess(callback, mResolver!!.fromJson<T>(content, mTargetTypeToken!!.type))
                                    else -> postSuccess(callback, content as T)
                                }
                            }
                        } else {
                            log("http response status error; code=${response.code()}")
                            postError(callback, SmartHttpException("http response status error ${response.code()}"))
                        }
                    } catch (e: Exception) {
                        log("request error", e)
                        postError(callback, SmartHttpException(e))
                    }
                }
            })
        } catch (e: Exception) {
            postError(callback, SmartHttpException(e))
        }

        if (TextUtils.isEmpty(mCacheKey)) return null
        val cacheString = sCache?.get(mCacheKey!!) ?: return null
        return when {
            mTargetClazz != null -> mResolver!!.fromJson<T>(cacheString, mTargetClazz)
            mTargetTypeToken != null -> mResolver!!.fromJson<T>(cacheString, mTargetTypeToken!!.type)
            else -> cacheString as T
        }
    }

    private fun <T> postError(callback: RequestCallback<T>, e: SmartHttpException) {
        mHandler.post { callback.onFailure(e) }
    }

    private fun <T> postSuccess(callback: RequestCallback<T>, data: T?) {
        mHandler.post { callback.onSuccess(data) }
    }

    /**
     * 同步请求
     * @param typeToken
     * @param <T>
     * @return
     * */
    fun <T> execute(typeToken: TypeToken<T>): T? {
        mTargetTypeToken = typeToken
        return execute<T>()
    }

    /**
     * 同步请求
     * @param clz
     * @param <T>
     * @return
     * @throws SmartHttpException
     */
    fun <T> execute(clz: Class<T>): T? {
        mTargetClazz = clz
        return execute<T>()
    }

    /**
     * 返回String
     * @return
     * @throws SmartHttpException
     */
    fun execute(): String? {
        return execute<String>()
    }

    private fun <T> execute(): T? {
        try {
            val client = getClient()
            val request = request()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                log("response code is " + response.code(), null)
                return null
            }
            val content = response.body()!!.string()
            saveCache(content)
            return when {
                mTargetClazz != null -> mResolver!!.fromJson<T>(content, mTargetClazz)
                mTargetTypeToken != null -> mResolver!!.fromJson<T>(content, mTargetTypeToken!!.type)
                else -> content as T
            }
        } catch (e: Exception) {
            log("request fail ", e)
            return null
        }
    }

    fun download(filename: String, @Nullable callback: DownloadCallback?) {
        download(File(filename), callback)
    }

    fun download(file: File, @Nullable callback: DownloadCallback?) {
        val innerCallback = callback ?: DefaultDownloadCallback()
        innerCallback.onStart()

        val client = getClient()
        val request = request()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                log("response failure", e)
                innerCallback.onFailure(SmartHttpException(e))
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    log("response code is ${response.code()}", null)
                    innerCallback.onFailure(SmartHttpException("response code is ${response.code()}"))
                    return
                }
                try {
                    val body = response.body()

                    val contentLength = body!!.contentLength()
                    val inputStream = body.byteStream()
                    var current = 0L

                    val outputStream = FileOutputStream(file)
                    val buffer = ByteArray(BUFFER_SIZE)

                    var length = inputStream.read(buffer)
                    while (length > 0) {
                        current += length
                        outputStream.write(buffer, 0, length)

                        innerCallback.onLoading(current, contentLength)
                        length = inputStream.read(buffer)
                    }

                    inputStream.read()

                    inputStream.close()
                    outputStream.close()
                    innerCallback.onEnd()
                } catch (e: Exception) {
                    log("save file failure", e)
                    innerCallback.onFailure(SmartHttpException(e))
                }
            }
        })
    }

    private fun request(): Request {
        val config = SmartHttpConfig.getInstance()
        mHeader.merge(config.commonHeader)
        mParam.merge(config.commonParam)
        if (mResolver == null) { mResolver = config.mResolver }
        val builder = Request.Builder().tag(mTag).headers(mHeader.convert())
        when (mRequestMethod) {
            METHOD_GET -> builder.get().url(spliceUrl())
            METHOD_POST -> builder.url(mUrl).post(mParam.body())
            METHOD_PUT -> builder.url(mUrl).put(mParam.body())
            METHOD_DELETE -> builder.url(mUrl).delete(mParam.body())
            METHOD_HEAD -> builder.url(mUrl).head()
            METHOD_PATCH -> builder.url(mUrl).patch(mParam.body())
            else -> throw IllegalStateException("unknown request method")
        }
        return builder.build()
    }

    private fun spliceUrl(): String {
        val sb = StringBuffer(mUrl)
        if (!mUrl.contains("?")) {
            sb.append('?')
        } else if (!mUrl.endsWith("&")) {
            sb.append('&')
        }
        sb.append(mParam.splitParam())

        return sb.toString()
    }

    private fun saveCache(content: String) {
        if (TextUtils.isEmpty(mCacheKey)) return
        sCache?.save(mCacheKey!!, content)
    }

    /**
     * 返回请求url
     * @return
     */
    fun url() = mUrl

    /**
     * 返回请求头
     * @return
     */
    fun header() = mHeader.headers

    /**
     * 返回请求参数
     * @return
     */
    fun parameter() = mParam.parameter()

    companion object {
        internal const val TAG = "SmartHttp"

        private const val MAX_CACHE_SIZE = 50 * 1024 * 1024
        private const val CACHE_DIR_NAME = "http"

        private const val METHOD_GET = 0x0
        private const val METHOD_POST = 0x1
        private const val METHOD_PUT = 0x2
        private const val METHOD_DELETE = 0x4
        private const val METHOD_HEAD = 0x8
        private const val METHOD_PATCH = 0x10

        private const val BUFFER_SIZE = 16 * 1024

        /** Cookie保存在内存，应用重启失效  */
        const val MEMORY_COOKIE = 0x1
        /** Cookie持久化，应用重启依旧生效  */
        const val PERSISTENT_COOKIE = 0x2

        private var sOkHttpClient: OkHttpClient? = null
        private var sCache: Cache? = null

        internal var isDebug = BuildConfig.DEBUG

        fun get(url: String) = SmartHttp(url, METHOD_GET)

        fun post(url: String) = SmartHttp(url, METHOD_POST)

        fun put(url: String) = SmartHttp(url, METHOD_PUT)

        fun delete(url: String) = SmartHttp(url, METHOD_DELETE)

        fun head(url: String) = SmartHttp(url, METHOD_HEAD)

        fun patch(url: String) = SmartHttp(url, METHOD_PATCH)


        /**
         * 关闭所有请求
         */
        fun cancelAll() {
            sOkHttpClient?.dispatcher()?.cancelAll()
        }

        /**
         * 关闭请求
         * @param tag
         */
        fun cancel(tag: Any?) {
            if (tag == null) return
            sOkHttpClient?.let {
                cancel(it.dispatcher().runningCalls(), tag)
                cancel(it.dispatcher().queuedCalls(), tag)
            }
        }

        private fun cancel(calls: List<Call>?, tag: Any) {
            if (calls != null && calls.isNotEmpty()) {
                for (call in calls) {
                    if (!call.isCanceled && tag == call.request().tag()) {
                        call.cancel()
                    }
                }
            }
        }

        private fun getClient(): OkHttpClient {
            if (sOkHttpClient == null) {
                throw java.lang.IllegalArgumentException("U should invoke SmartHttpConfig.init() first")
            }
            return sOkHttpClient!!
        }

        ///////////////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////////

        internal fun init(context: Context, config: SmartHttpConfig) {
            isDebug = config.isDebug
            val cacheDir = getCacheDir(context, CACHE_DIR_NAME)
            sCache = Cache(cacheDir)

            val builder: OkHttpClient.Builder = if (sOkHttpClient != null) {
                sOkHttpClient!!.newBuilder()
            } else {
                OkHttpClient.Builder().cache(okhttp3.Cache(cacheDir, MAX_CACHE_SIZE.toLong()))
            }

            when (config.cookieType) {
                MEMORY_COOKIE -> builder.cookieJar(CookieJarImp())
                PERSISTENT_COOKIE -> builder.cookieJar(CookieJarImp(cacheDir.absolutePath))
            }
            prepareClient(builder, config)
            sOkHttpClient = builder.build()
        }

        private fun prepareClient(builder: OkHttpClient.Builder, config: SmartHttpConfig) {
            builder.writeTimeout(config.writeTimeout.toLong(), TimeUnit.SECONDS)
                    .readTimeout(config.readTimeout.toLong(), TimeUnit.SECONDS)
                    .connectTimeout(config.connectTimeout.toLong(), TimeUnit.SECONDS)

            if (config.interceptors.isNotEmpty()) {
                for (interceptor in config.interceptors) {
                    builder.addInterceptor(interceptor)
                }
            }
            if (config.networkInterceptors.isNotEmpty()) {
                for (interceptor in config.networkInterceptors) {
                    builder.addNetworkInterceptor(interceptor)
                }
            }
            if (config.sslSocketFactory != null && config.trustManager != null) {
                builder.sslSocketFactory(config.sslSocketFactory!!, config.trustManager!!)
            }
            config.hostnameVerifier?.let { builder.hostnameVerifier(it) }
        }
    }

    @IntDef(value = [PERSISTENT_COOKIE, MEMORY_COOKIE])
    internal annotation class CookieType
}