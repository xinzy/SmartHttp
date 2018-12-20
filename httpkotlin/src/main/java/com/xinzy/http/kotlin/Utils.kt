package com.xinzy.http.kotlin

import android.content.Context
import android.text.TextUtils
import android.util.Log
import okhttp3.Cookie
import java.io.*
import java.net.URLEncoder
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.*

/**
 * Created by Xinzy on 2018/10/12.
 */

internal fun encode(input: String): String {
    if (TextUtils.isEmpty(input)) return ""
    return try {
        URLEncoder.encode(input, HttpParam.ENCODE)
    } catch (e: UnsupportedEncodingException) {
        input
    }
}

internal fun getCacheDir(context: Context, filename: String): File {
    var cacheFile = File(context.externalCacheDir, filename)
    if (!cacheFile.exists()) {
        if (!cacheFile.mkdirs()) {
            cacheFile = File(context.cacheDir, filename)
            if (!cacheFile.exists()) {
                cacheFile.mkdirs()
            }
        }
    }
    return cacheFile
}

internal fun isCookieExpired(cookie: Cookie): Boolean {
    return cookie.expiresAt() < System.currentTimeMillis()
}

internal fun md5(input: String): String {
    val md = MessageDigest.getInstance("MD5")
    val digested = md.digest(input.toByteArray())
    return digested.joinToString("") {
        String.format("%02x", it)
    }
}

internal fun log(msg: String, e: Exception? = null) {
    if (!SmartHttp.isDebug) return
    if (e == null) Log.d(SmartHttp.TAG, msg)
    else Log.e(SmartHttp.TAG, msg, e)
}

////////////////////////////////////////////////////////////////////
// Https证书
////////////////////////////////////////////////////////////////////

internal fun sslSocketFactory(trustManager: X509TrustManager?, bksFile: InputStream?, password: String?, certificates: Array<InputStream>?): SSLParams {
    val sslParams = SSLParams()
    try {
        val keyManagers = prepareKeyManager(bksFile, password)
        val trustManagers = prepareTrustManager(certificates)
        val manager = when {
            trustManager != null -> //优先使用用户自定义的TrustManager
                trustManager
            trustManagers != null -> //然后使用默认的TrustManager
                chooseTrustManager(trustManagers)
            else -> //否则使用不安全的TrustManager
                TrustAllManager()
        }
        // 创建TLS类型的SSLContext对象， that uses our TrustManager
        val sslContext = SSLContext.getInstance("TLS")
        // 用上面得到的trustManagers初始化SSLContext，这样sslContext就会信任keyStore中的证书
        // 第一个参数是授权的密钥管理器，用来授权验证，比如授权自签名的证书验证。第二个是被授权的证书管理器，用来验证服务器端的证书
        sslContext.init(keyManagers, arrayOf<TrustManager>(manager), null)
        // 通过sslContext获取SSLSocketFactory对象
        sslParams.sSLSocketFactory = sslContext.socketFactory
        sslParams.trustManager = manager
        return sslParams
    } catch (e: Exception) {
        throw AssertionError(e)
    }
}

internal fun prepareKeyManager(bksFile: InputStream?, password: String?): Array<KeyManager>? {
    try {
        if (bksFile == null || password == null) return null
        val clientKeyStore = KeyStore.getInstance("BKS")
        clientKeyStore.load(bksFile, password.toCharArray())
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(clientKeyStore, password.toCharArray())
        return kmf.keyManagers
    } catch (e: Exception) {
    }

    return null
}

internal fun prepareTrustManager(certificates: Array<InputStream>?): Array<TrustManager>? {
    if (certificates == null || certificates.isEmpty()) return null
    try {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        // 创建一个默认类型的KeyStore，存储我们信任的证书
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null)
        for ((index, certStream) in certificates.withIndex()) {
            val certificateAlias = Integer.toString(index)
            // 证书工厂根据证书文件的流生成证书 cert
            val cert = certificateFactory.generateCertificate(certStream)
            // 将 cert 作为可信证书放入到keyStore中
            keyStore.setCertificateEntry(certificateAlias, cert)
            try {
                certStream.close()
            } catch (e: IOException) {
            }
        }
        //我们创建一个默认类型的TrustManagerFactory
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        //用我们之前的keyStore实例初始化TrustManagerFactory，这样tmf就会信任keyStore中的证书
        tmf.init(keyStore)
        //通过tmf获取TrustManager数组，TrustManager也会信任keyStore中的证书
        return tmf.trustManagers
    } catch (e: Exception) {
    }

    return null
}

internal fun chooseTrustManager(trustManagers: Array<TrustManager>): X509TrustManager {
    for (trustManager in trustManagers) {
        if (trustManager is X509TrustManager) {
            return trustManager
        }
    }
    return TrustAllManager()
}

internal class TrustAllManager : X509TrustManager {
    @Throws(CertificateException::class)
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
    }

    @Throws(CertificateException::class)
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return emptyArray()
    }
}

internal class SSLParams {
    var sSLSocketFactory: SSLSocketFactory? = null
    var trustManager: X509TrustManager? = null
}