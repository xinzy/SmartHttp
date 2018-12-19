package com.xinzy.http;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.xinzy.http.SmartHttpConfig.UA;

/**
 * Created by Xinzy on 2017/5/19.
 */

public final class SmartHttp {
    static final String TAG = "SmartHttp";

    private static final int METHOD_GET = 0x0;
    private static final int METHOD_POST = 0x1;
    private static final int METHOD_PUT = 0x2;
    private static final int METHOD_DELETE = 0x4;
    private static final int METHOD_HEAD = 0x8;
    private static final int METHOD_PATCH = 0x10;

    private static final int BUFFER_SIZE = 16 * 1024;

    /** 无Cookie持久化 */
    public static final int NO_COOKIE = 0x0;
    /** Cookie保存在内存，应用重启失效 */
    public static final int MEMORY_COOKIE = 0x1;
    /** Cookie持久化，应用重启依旧生效 */
    public static final int PERSISTENT_COOKIE = 0x2;

    /** 请求方式 */
    private int mRequestMethod = METHOD_GET;

    /** url */
    private String mUrl;

    /** request tag  */
    private Object mTag;

    /** header  */
    private HttpHeader mHeader;

    /** parameter */
    private HttpParam mParam;

    /** 缓存key */
    private String mCacheKey;

    static boolean isDebug;

    private static File mCacheDir;

    private static OkHttpClient sOkHttpClient;

    private Cache mCache;

    private SmartHttpConfig mCopyConfig;

    private Handler mMainHandler = new Handler(Looper.getMainLooper());
    private Class<?> mTargetClazz;
    private TypeToken<?> mTargetTypeReference;

    private Gson mGson;

    private SmartHttp(String url, int method) {
        mRequestMethod = method;
        mUrl = url;

        mParam = new HttpParam();
        mHeader = new HttpHeader();
    }

    static void init(SmartHttpConfig config) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        prepareClient(builder, config);

        sOkHttpClient = builder.build();
        isDebug = config.isDebug;
        mCacheDir = config.cacheDir;
    }

    /**
     * 关闭所有请求
     */
    public static void cancelAll() {
        if (sOkHttpClient != null) {
            sOkHttpClient.dispatcher().cancelAll();
        }
    }

    /**
     * 关闭请求
     * @param tag
     */
    public static void cancel(Object tag) {
        if (sOkHttpClient != null) {
            cancel(sOkHttpClient.dispatcher().runningCalls(), tag);
            cancel(sOkHttpClient.dispatcher().queuedCalls(), tag);
        }
    }

    private static void cancel(List<Call> calls, Object tag) {
        if (tag != null && calls != null && calls.size() > 0) {
            for (Call call : calls) {
                if (!call.isCanceled() && tag.equals(call.request().tag())) {
                    call.cancel();
                }
            }
        }
    }

    private static void checkInit() {
        if (sOkHttpClient == null) {
            init(SmartHttpConfig.getInstance());
        }
    }

    /**
     * https单向认证 用含有服务端公钥的证书校验服务端证书
     * @param certificates
     * @return
     */
    public SmartHttp certificates(InputStream... certificates) {
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
    public SmartHttp certificates(InputStream bksFile, String password, InputStream... certificates) {
        checkConfig();
        mCopyConfig.certificates(bksFile, password, certificates);
        return this;
    }

    /**
     * https单向认证
     * 可以额外配置信任服务端的证书策略，否则默认是按CA证书去验证的，若不是CA可信任的证书，则无法通过验证
     * @param trustManager
     * @return
     */
    public SmartHttp certificates(X509TrustManager trustManager) {
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
    public SmartHttp certificates(InputStream bksFile, String password, X509TrustManager trustManager) {
        checkConfig();
        mCopyConfig.certificates(bksFile, password, trustManager);
        return this;
    }

    public SmartHttp addInterceptor(Interceptor interceptor) {
        checkConfig();
        mCopyConfig.addInterceptor(interceptor);
        return this;
    }

    public SmartHttp addNetworkInterceptor(Interceptor interceptor) {
        checkConfig();
        mCopyConfig.addNetworkInterceptor(interceptor);
        return this;
    }

    /**
     * Connection Timeout
     * @param connectTimeout 单位秒
     * @return
     */
    public SmartHttp connectTimeout(int connectTimeout) {
        checkConfig();
        mCopyConfig.connectTimeout(connectTimeout);
        return this;
    }

    /**
     * Read Timeout
     * @param readTimeout 单位秒
     * @return
     */
    public SmartHttp readTimeout(int readTimeout) {
        checkConfig();
        mCopyConfig.readTimeout(readTimeout);
        return this;
    }

    /**
     * Write Timeout
     * @param writeTimeout 单位秒
     * @return
     */
    public SmartHttp writeTimeout(int writeTimeout) {
        checkConfig();
        mCopyConfig.writeTimeout(writeTimeout);
        return this;
    }

    private void checkConfig() {
        if (mCopyConfig == null) {
            mCopyConfig = SmartHttpConfig.getInstance().duplicate();
        }
    }

    public SmartHttp resolver(@NonNull Gson resolver) {
        mGson = resolver;
        return this;
    }

    /**
     * 设置ua
     * @param userAgent
     * @return
     */
    public SmartHttp userAgent(String userAgent) {
        mHeader.add(UA, userAgent);
        return this;
    }

    /**
     * 添加header信息
     * @param key
     * @param val
     * @return
     */
    public SmartHttp header(@NonNull String key, String val) {
        mHeader.add(key, val);
        return this;
    }

    /**
     * 设置header
     * @param header
     * @return
     */
    public SmartHttp header(Map<String, String> header) {
        mHeader.merge(header);
        return this;
    }

    /**
     * 添加参数
     * @param key
     * @param val
     * @return
     */
    public SmartHttp param(@NonNull String key, String val) {
        mParam.add(key, val);
        return this;
    }

    /**
     * 设置参数
     * @param param
     * @return
     */
    public SmartHttp param(@NonNull Map<String, String> param) {
        mParam.add(param);
        return this;
    }

    /**
     * 添加上传的文件
     * @param key
     * @param file
     * @return
     */
    public SmartHttp param(@NonNull String key, @NonNull File file) {
        return param(key, file, file.getName());
    }

    /**
     * 参数以json格式上传
     * @return
     */
    public SmartHttp paramAsJson() {
        mParam.asJson();
        return this;
    }

    /**
     * 添加上传的文件
     * @param key
     * @param file
     * @param filename
     * @return
     */
    public SmartHttp param(@NonNull String key, File file, String filename) {
        if (mRequestMethod != METHOD_POST && mRequestMethod != METHOD_PUT) {
            throw new IllegalStateException("Upload need POST or PUT method");
        }
        if (file == null || !file.exists()) {
            if (isDebug) {  //Debug模式抛出exception
                throw new IllegalArgumentException("Upload file is null or not exist");
            }
        } else {
            mParam.multi(key, file, filename);
        }
        return this;
    }

    /**
     * 设置tag 可以通过该tag关闭请求
     * @param tag
     * @return
     */
    public SmartHttp tag(Object tag) {
        mTag = tag;
        return this;
    }

    /**
     * 设置缓存key
     * @param key
     * @return
     */
    public SmartHttp cacheKey(String key) {
        mCacheKey = key;
        return this;
    }

    /**
     * 异步请求
     * @param callback
     * @return <T> 上次请求接口时缓存的数据
     */
    public <T> T enqueue(@NonNull TypeToken<T> reference, @Nullable RequestCallback<T> callback) {
        mTargetTypeReference = reference;
        realEnqueue(callback);
        return mCache == null ? null : mCache.read(reference);
    }

    /**
     * 异步请求
     * @param clz
     * @param callback
     * @param <T>
     * @return <T> 上次请求接口时缓存的数据
     */
    public <T> T enqueue(@NonNull Class<T> clz, @Nullable RequestCallback<T> callback) {
        mTargetClazz = clz;
        realEnqueue(callback);
        return mCache == null ? null : mCache.read(clz);
    }

    /**
     * 以String数据返回
     * @param callback
     * @return <T> 上次请求接口时缓存的数据
     */
    public String enqueue(@Nullable RequestCallback<String> callback) {
        realEnqueue(callback);
        return mCache == null ? null : mCache.read();
    }

    private <T> void realEnqueue(RequestCallback<T> rawCallback) {
        checkInit();
        if (mRequestMethod == METHOD_GET) {
            if (TextUtils.isEmpty(mCacheKey)) mCacheKey = mUrl;
            mCache = new Cache(mCacheDir, mCacheKey);
        }

        final RequestCallback callback = rawCallback == null ? new RequestCallback.DefaultRequestCallback<T>() : rawCallback;
        final OkHttpClient client = client();
        final Request request = request();
        final Call call = client.newCall(request);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (call.isCanceled()) {
                    Utils.debug("call is canceled");
                    return;
                }
                postError(callback, new SmartHttpException(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (call.isCanceled()) {
                    Utils.debug("call is canceled");
                    return;
                }
                if (!response.isSuccessful()) {
                    postError(callback, new SmartHttpException("http request error and error code is " + response.code()));
                    return;
                }

                final String content = response.body().string();
//                final Map<String, String> header = Utils.parseHeader(response.headers());
                Utils.debug("http content: " + content);
                try {
                    final T val;
                    if (mTargetClazz != null) {
                        val = (T) mGson.fromJson(content, mTargetClazz);
                    } else if (mTargetTypeReference != null) {
                        val = (T) mGson.fromJson(content, mTargetTypeReference.getType());
                    } else {
                        val = (T) content;
                    }
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess(val);
                        }
                    });
                    if (mCache != null) mCache.save(content);
                } catch (Exception e) {
                    Utils.e("parse json string fail: " + content, e);
                    postError(callback, new SmartHttpException(e));
                }
            }
        });
    }

    private <T> void postError(final RequestCallback<T> callback, final SmartHttpException e) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onFailure(e);
            }
        });
    }

    /**
     * 同步请求
     * @param reference
     * @param <T>
     * @return
     */
    public <T> T execute(@NonNull TypeToken<T> reference) {
        mTargetTypeReference = reference;
        return realExecute();
    }

    /**
     * 同步请求
     * @param clz
     * @param <T>
     * @return
     * @throws SmartHttpException
     */
    public <T> T execute(@NonNull Class<T> clz) {
        mTargetClazz = clz;
        return realExecute();
    }

    /**
     * 返回String
     * @return
     * @throws SmartHttpException
     */
    public String execute() {
        return realExecute();
    }

    private <T> T realExecute() {
        checkInit();
        String content = "";
        try {
            final OkHttpClient client = client();
            Request request = request();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                Utils.e("response code is " + response.code(), null);
                return null;
            }
            content = response.body().string();
            if (mTargetClazz != null) {
                return  (T) mGson.fromJson(content, mTargetClazz);
            } else if (mTargetTypeReference != null) {
                return (T) mGson.fromJson(content, mTargetTypeReference.getType());
            } else {
                return (T) content;
            }
        } catch (Exception e) {
            Utils.e("execute error", e);
            return null;
        }
    }

    /**
     * 下载
     * @param filename
     * @param callback
     * @return
     */
    public Call download(@NonNull String filename, @Nullable DownloadCallback callback) {
        return download(new File(filename), callback);
    }

    /**
     *  下载
     * @param file
     * @param rawCallback
     * @return
     */
    public Call download(@NonNull final File file, @Nullable DownloadCallback rawCallback) {
        checkInit();
        final DownloadCallback callback = rawCallback == null ? new DownloadCallback.DefaultDownloadCallback() : rawCallback;
        callback.onStart();

        final OkHttpClient client = client();
        Call call = client.newCall(request());

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(new SmartHttpException(e));
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (!response.isSuccessful()) {
                    callback.onFailure(new SmartHttpException("Http error, error code is " + response.code()));
                    return;
                }
                try {
                    final ResponseBody body = response.body();
                    final long total = body.contentLength();
                    final InputStream is = body.byteStream();

                    long current = 0;
                    FileOutputStream fos = new FileOutputStream(file);
                    byte[] buff = new byte[BUFFER_SIZE];
                    int length;
                    while ((length = is.read(buff, 0, BUFFER_SIZE)) > 0) {
                        fos.write(buff, 0, length);
                        current += length;
                        callback.onLoading(current, total);
                    }
                    fos.close();
                    callback.onEnd();
                } catch (Exception e) {
                    Utils.e("download fail", e);
                    callback.onFailure(new SmartHttpException(e));
                }
            }
        });
        return call;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 返回请求参数
     * @return
     */
    public Map<String, String> param() {
        return mParam.parameters();
    }

    /**
     * 返回请求头
     * @return
     */
    public Map<String, String> header() {
        return mHeader.headers;
    }

    /**
     * 返回请求url
     * @return
     */
    public String url() {
        return mUrl;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////


    private Request request() {
        SmartHttpConfig config = SmartHttpConfig.getInstance();
        mHeader.merge(config.commonHeader);
        mParam.merge(config.commonParam);

        if (mGson == null) mGson = config.mResolver;

        Request.Builder builder = new Request.Builder().tag(mTag).headers(mHeader.convert());
        switch (mRequestMethod) {
            case METHOD_GET:
                builder.get().url(spliceUrl());
                break;
            case METHOD_POST:
                builder.url(mUrl).post(mParam.body());
                break;
            case METHOD_PUT:
                builder.url(mUrl).put(mParam.body());
                break;
            case METHOD_DELETE:
                builder.url(mUrl).delete(mParam.body());
                break;
            case METHOD_HEAD:
                builder.url(mUrl).head();
                break;
            case METHOD_PATCH:
                builder.url(mUrl).patch(mParam.body());
                break;
            default:
                throw new IllegalStateException("unknown request method");
        }
        return builder.build();
    }

    private String spliceUrl() {
        StringBuffer sb = new StringBuffer(mUrl);
        if (!mUrl.contains("?")) {
            sb.append('?');
        } else if (!mUrl.endsWith("&")) {
            sb.append('&');
        }
        sb.append(Utils.splitParam(mParam));
        return sb.toString();
    }

    private OkHttpClient client() {
        if (mCopyConfig == null) {
            return sOkHttpClient;
        }
        OkHttpClient.Builder builder = sOkHttpClient.newBuilder();
        prepareClient(builder, mCopyConfig);

        return builder.build();
    }

    private static void prepareClient(OkHttpClient.Builder builder, SmartHttpConfig config) {
        builder.writeTimeout(config.writeTimeout, TimeUnit.SECONDS)
                .readTimeout(config.readTimeout, TimeUnit.SECONDS)
                .connectTimeout(config.connectTimeout, TimeUnit.SECONDS);

        if (config.isDebug) {
            builder.addNetworkInterceptor(new LoggingInterceptor());
        }

        switch (config.cookieType) {
            case MEMORY_COOKIE:
                builder.cookieJar(new CookieJarImp());
                break;
            case PERSISTENT_COOKIE:
                builder.cookieJar(new CookieJarImp(config.cacheDir.getAbsolutePath()));
        }

        if (!config.interceptors.isEmpty()) {
            for (Interceptor interceptor : config.interceptors) {
                builder.addInterceptor(interceptor);
            }
        }
        if (!config.networkInterceptors.isEmpty()) {
            for (Interceptor interceptor : config.networkInterceptors) {
                builder.addNetworkInterceptor(interceptor);
            }
        }
        if (config.sslSocketFactory != null && config.trustManager != null) {
            builder.sslSocketFactory(config.sslSocketFactory, config.trustManager);
        }
        if (config.hostnameVerifier != null) {
            builder.hostnameVerifier(config.hostnameVerifier);
        }
    }

    public static SmartHttp get(@NonNull String url) {
        return new SmartHttp(url, METHOD_GET);
    }

    public static SmartHttp post(@NonNull String url) {
        return new SmartHttp(url, METHOD_POST);
    }

    public static SmartHttp put(@NonNull String url) {
        return new SmartHttp(url, METHOD_PUT);
    }

    public static SmartHttp delete(@NonNull String url) {
        return new SmartHttp(url, METHOD_DELETE);
    }

    public static SmartHttp head(@NonNull String url) {
        return new SmartHttp(url, METHOD_HEAD);
    }

    public static SmartHttp patch(@NonNull String url) {
        return new SmartHttp(url, METHOD_PATCH);
    }

    @IntDef (value = {NO_COOKIE, PERSISTENT_COOKIE, MEMORY_COOKIE})
    @interface CookieType{}
}
