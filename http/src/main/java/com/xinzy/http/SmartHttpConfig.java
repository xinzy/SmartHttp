package com.xinzy.http;

import android.content.Context;
import android.support.annotation.IntDef;

import com.google.gson.Gson;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Interceptor;

import static com.xinzy.http.SmartHttp.MEMORY_COOKIE;
import static com.xinzy.http.SmartHttp.NO_COOKIE;
import static com.xinzy.http.SmartHttp.PERSISTENT_COOKIE;

/**
 * 配置类
 * Created by Xinzy on 2017/5/19.
 */

public final class SmartHttpConfig {

    private static final int DEFAULT_TIMEOUT = 10;
    public static final String UA = "User-Agent";

    int connectTimeout = DEFAULT_TIMEOUT;
    int readTimeout = DEFAULT_TIMEOUT;
    int writeTimeout = DEFAULT_TIMEOUT;

    boolean isDebug = false;

    HttpHeader commonHeader = new HttpHeader();
    HttpParam commonParam = new HttpParam();

    Gson mResolver = new Gson();

    Context mContext;

    int cookieType;

    SSLSocketFactory sslSocketFactory;
    X509TrustManager trustManager;
    HostnameVerifier hostnameVerifier;

    List<Interceptor> interceptors = new ArrayList<>();
    List<Interceptor> networkInterceptors = new ArrayList<>();

    private static SmartHttpConfig sHttpConfig;

    private SmartHttpConfig() {
    }

    public static SmartHttpConfig getInstance() {
        if (sHttpConfig == null) {
            synchronized (SmartHttpConfig.class) {
                if (sHttpConfig == null) {
                    sHttpConfig = new SmartHttpConfig();
                }
            }
        }

        return sHttpConfig;
    }

    SmartHttpConfig copy() {
        SmartHttpConfig config = new SmartHttpConfig();
        config.connectTimeout = connectTimeout;
        config.readTimeout = readTimeout;
        config.writeTimeout = readTimeout;
        config.trustManager = trustManager;
        config.hostnameVerifier = hostnameVerifier;
        config.sslSocketFactory = sslSocketFactory;
        return config;
    }

    /**
     * Connection Timeout
     * @param connectTimeout 单位秒
     * @return
     */
    public SmartHttpConfig connectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    /**
     * Read Timeout
     * @param readTimeout 单位秒
     * @return
     */
    public SmartHttpConfig readTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    /**
     * Write Timeout
     * @param writeTimeout 单位秒
     * @return
     */
    public SmartHttpConfig writeTimeout(int writeTimeout) {
        this.writeTimeout = writeTimeout;
        return this;
    }

    /**
     * 设置请求通用头
     * @param key
     * @param value
     * @return
     */
    public SmartHttpConfig addCommonHeader(String key, String value) {
        commonHeader.add(key, value);
        return this;
    }

    /**
     * 设置请求通用头
     * @param headers
     * @return
     */
    public SmartHttpConfig addCommonHeader(Map<String, String> headers) {
        commonHeader.merge(headers);
        return this;
    }

    /**
     * 设置请求通用参数
     * @param key
     * @param val
     * @return
     */
    public SmartHttpConfig addCommonParam(String key, String val) {
        commonParam.add(key, val);
        return this;
    }

    /**
     * 设置浏览器ua
     * @param userAgent
     * @return
     */
    public SmartHttpConfig userAgent(String userAgent) {
        commonHeader.add(UA, userAgent);
        return this;
    }

    /**
     * 设置是否开启debug
     * @param debug
     */
    public SmartHttpConfig debug(boolean debug) {
        isDebug = debug;
        return this;
    }

    public SmartHttpConfig addInterceptor(Interceptor interceptor) {
        interceptors.add(interceptor);
        return this;
    }

    public SmartHttpConfig addNetworkInterceptor(Interceptor interceptor) {
        networkInterceptors.add(interceptor);
        return this;
    }

    public SmartHttpConfig resolver(Gson resolver) {
        mResolver = resolver;
        return this;
    }

    public SmartHttpConfig cookie(@CookieType int cookieType) {
        this.cookieType = cookieType;
        return this;
    }

    public SmartHttpConfig with(Context context) {
        mContext = context.getApplicationContext();
        return this;
    }

    /**
     * https单向认证 用含有服务端公钥的证书校验服务端证书
     * @param certificates
     * @return
     */
    public SmartHttpConfig certificates(InputStream... certificates) {

        return certificates(null, null, certificates);
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
    public SmartHttpConfig certificates(InputStream bksFile, String password, InputStream... certificates) {
        Utils.SSLParams param = Utils.sslSocketFactory(null, bksFile, password, certificates);
        sslSocketFactory = param.sSLSocketFactory;
        trustManager = param.trustManager;
        return this;
    }

    /**
     * https单向认证
     * 可以额外配置信任服务端的证书策略，否则默认是按CA证书去验证的，若不是CA可信任的证书，则无法通过验证
     * @param trustManager
     * @return
     */
    public SmartHttpConfig certificates(X509TrustManager trustManager) {
        return certificates(null, null, trustManager);
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
    public SmartHttpConfig certificates(InputStream bksFile, String password, X509TrustManager trustManager) {
        Utils.SSLParams param = Utils.sslSocketFactory(trustManager, bksFile, password, null);
        sslSocketFactory = param.sSLSocketFactory;
        this.trustManager = param.trustManager;
        return this;
    }

    public SmartHttpConfig hostnameVerifier() {
        return hostnameVerifier(new TrustHostnameVerifier());
    }

    public SmartHttpConfig hostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
        return this;
    }

    public void initHttpClient() {
        if (mContext == null) {
            throw new IllegalStateException("must invoke function with(context) first");
        }
        if (!commonHeader.headers.containsKey(UA)) {
            commonHeader.add(UA, Utils.getUserAgent(mContext));
        }
        SmartHttp.init(this);
    }

    @IntDef (value = {NO_COOKIE, MEMORY_COOKIE, PERSISTENT_COOKIE})
    @interface CookieType {
    }

    class TrustHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }
}
