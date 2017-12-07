package com.xinzy.http;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.X509TrustManager;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static com.xinzy.http.SmartHttpConfig.UA;

/**
 * Created by Xinzy on 2017/5/19.
 */

public final class SmartHttp {
    static final String TAG = "SmartHttp";

    private static final int MAX_CACHE_SIZE = 50 * 1024 * 1024;
    private static final int DEFAULT_CACHE_AGE = 60;
    private static final String CACHE_DIR_NAME = "http";

    private static final int METHOD_GET = 0x0;
    private static final int METHOD_POST = 0x1;
    private static final int METHOD_PUT = 0x2;
    private static final int METHOD_DELETE = 0x4;
    private static final int METHOD_HEAD = 0x8;
    private static final int METHOD_PATCH = 0x10;

    private static final int BUFFER_SIZE = 16 * 1024;

    /** 默认缓存 */
    public static final int DEFAULT_CACHE = 0x0;
    /** 不使用缓存 */
    public static final int NO_CACHE = 0x1;
    /** 先读缓存再访问网络 */
    public static final int FIRST_CACHE_THEN_REQUEST = 0x2;


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

    /** json resolver */
    private Gson mResolver;

    /** Cache有效期 */
    private long mCacheTime = 3600;
    /** Cache key */
    private String mCacheKey;
    /** Cache model */
    private int mCacheModel = DEFAULT_CACHE;

    private static String cacheDir;
    private static Cache mCache;

    static boolean isDebug;

    private static OkHttpClient sOkHttpClient;

    private SmartHttpConfig mCopyConfig;

    private SmartHttp(String url, int method) {
        mRequestMethod = method;
        mUrl = url;

        mParam = new HttpParam();
        mHeader = new HttpHeader();
        mResolver = SmartHttpConfig.getInstance().mResolver;
    }

    static void init(SmartHttpConfig config) {

        File cacheFile = new File(config.mContext.getExternalCacheDir(), CACHE_DIR_NAME);
        if (!cacheFile.exists()) {
            if (!cacheFile.mkdirs()) {
                cacheFile = new File(config.mContext.getCacheDir(), CACHE_DIR_NAME);
                if (!cacheFile.exists()) {
                    cacheFile.mkdirs();
                }
            }
        }
        cacheDir = cacheFile.getAbsolutePath();
        mCache = new Cache.CacheImpl(cacheDir);
        isDebug = config.isDebug;

        OkHttpClient.Builder builder;
        if (sOkHttpClient == null) {
            builder = new OkHttpClient.Builder().cache(new okhttp3.Cache(cacheFile, MAX_CACHE_SIZE));
        } else {
            builder = sOkHttpClient.newBuilder();
        }
        switch (config.cookieType) {
            case MEMORY_COOKIE:
                builder.cookieJar(new CookieJarImp());
                break;
            case PERSISTENT_COOKIE:
                builder.cookieJar(new CookieJarImp(cacheDir));
        }
        client(builder, config);

        sOkHttpClient = builder.build();
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
            mCopyConfig = SmartHttpConfig.getInstance().copy();
        }
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
     * 设置Gson 解析器
     * @param resolver
     * @return
     */
    public SmartHttp resolver(Gson resolver) {
        mResolver = resolver;
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
            throw new IllegalArgumentException("Upload file is null or not exist");
        }
        mParam.multi(key, file, filename);
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
     * 设置网络缓存的key。默认使用url 的md5
     * @param key
     * @return
     */
    public SmartHttp cacheKey(String key) {
        mCacheKey = key;
        return this;
    }

    /**
     * 设置缓存有效期
     * @param mCacheTime
     * @return
     */
    public SmartHttp cacheTime(long mCacheTime) {
        return this;
    }

    /**
     * 设置缓存类型
     * @param model
     * @return
     */
    public SmartHttp cacheModel(@CacheModel int model) {
        mCacheModel = model;
        return this;
    }

    /**
     * 异步请求
     * @param callback
     * @return
     */
    public <T> Call enqueue(@NonNull TypeToken<T> typeToken, @Nullable RequestCallback<T> callback) {
        return enqueue(typeToken.getType(), callback);
    }

    /**
     * 异步请求
     * @param clz
     * @param callback
     * @param <T>
     * @return
     */
    public <T> Call enqueue(@NonNull Class<T> clz, @Nullable RequestCallback<T> callback) {
        return enqueue((Type) clz, callback);
    }

    /**
     * 以String数据返回
     * @param callback
     * @return
     */
    public Call enqueue(@Nullable RequestCallback<String> callback) {
        return enqueue((Type) null, callback);
    }

    private <T> Call enqueue(final Type type, RequestCallback<T> rawCallback) {
        checkInit();
        final RequestCallback callback = rawCallback == null ? DefaultCallback.<T>callback() : rawCallback;
        final OkHttpClient client = client();
        final Request request = request();
        final Call call = client.newCall(request);
        checkCache(type, callback);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (call.isCanceled()) return;
                callback.onFailure(new SmartHttpException(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (call.isCanceled()) return;
                if (!response.isSuccessful()) {
                    callback.onFailure(new SmartHttpException("http request error and error code is " + response.code()));
                    return;
                }

                final String content = response.body().string();
                Map<String, String> header = Utils.parseHeader(response.headers());
                saveCache(content, header);

                try {
                    T val = null;
                    if (type == null) {
                        val = (T) content;
                    } else {
                        val = mResolver.fromJson(content, type);
                    }
                    callback.onSuccess(val, header, false);
                } catch (JsonSyntaxException e) {
                    Utils.e("parse fail json string: " + content, e);
                    callback.onFailure(new SmartHttpException("Gson parse json fail", e));
                } catch (Exception e) {
                    Utils.e("", e);
                    callback.onFailure(new SmartHttpException(e));
                }
            }
        });

        return call;
    }

    private <T> void checkCache(Type type, RequestCallback<T> callback) {
        if (mCacheModel != FIRST_CACHE_THEN_REQUEST) return;
        Map<String, String> header = mCache.header(mCacheKey);
        if (type == null) {
            String content = mCache.get(mCacheKey);
            if (!TextUtils.isEmpty(content)) {
                callback.onSuccess((T) content, header, true);
            }
        } else {
            T data = mCache.get(mCacheKey, type);
            if (data != null) {
                callback.onSuccess(data, header, true);
            }
        }
    }

    /**
     * 同步请求
     * @param typeToken
     * @param <T>
     * @return
     * @throws SmartHttpException
     */
    public <T> T execute(@NonNull TypeToken<T> typeToken) throws SmartHttpException {
        return execute(typeToken.getType());
    }

    /**
     * 同步请求
     * @param clz
     * @param <T>
     * @return
     * @throws SmartHttpException
     */
    public <T> T execute(@NonNull Class<T> clz) {
        return execute((Type) clz);
    }

    /**
     * 返回String
     * @return
     * @throws SmartHttpException
     */
    public String execute() {
        return execute((Type) null);
    }

    private <T> T execute(Type type) {
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
            saveCache(content, null);
            if (type == null) {
                return (T) content;
            }
            return mResolver.fromJson(content, type);
        } catch (IOException | JsonSyntaxException e) {
            Utils.e("execute error", e);
            return null;
        }
    }

    /**
     * 将请求转换成RxJava Observable。默认在io线程请求网络并且回调到主线程
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> Observable<T> observable(@NonNull Class<T> clazz) {
        return observable((Type) clazz);
    }

    /**
     * 将请求转换成RxJava Observable。默认在io线程请求网络并且回调到主线程
     * @param typeToken
     * @param <T>
     * @return
     */
    public <T> Observable<T> observable(@NonNull TypeToken<T> typeToken) {
        return observable(typeToken.getType());
    }

    /**
     * 将请求转换成RxJava Observable。默认在io线程请求网络并且回调到主线程
     * @return String
     */
    public Observable<String> observable() {
        return observable((Type) null);
    }

    private <T> Observable<T> observable(final Type type) {
        return Observable.just((Void) null).lift(new Observable.Operator<T, Void>() {
            @Override
            public Subscriber<? super Void> call(final Subscriber<? super T> subscriber) {
                return new Subscriber<Void>() {
                    @Override
                    public void onCompleted() {
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        subscriber.onError(e);
                    }

                    @Override
                    public void onNext(Void param) {
                        T t = execute(type);
                        subscriber.onNext(t);
                    }
                };
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 下载
     * @param file
     * @param callback
     * @return
     */
    public Call download(@NonNull String file, @Nullable DownloadCallback callback) {
        return download(new File(file), callback);
    }

    /**
     *  下载
     * @param file
     * @param rawCallback
     * @return
     */
    public Call download(@NonNull final File file, @Nullable DownloadCallback rawCallback) {
        checkInit();
        final DownloadCallback callback = rawCallback == null ? DefaultDownloadCallback.callback() : rawCallback;
        final OkHttpClient client = client();
        Call call = client.newCall(request());

        callback.onStart();

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(new SmartHttpException(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
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
                } catch (IOException e) {
                    callback.onFailure(new SmartHttpException(e));
                }
            }
        });
        return call;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private void saveCache(String content, Map<String, String> header) {
        if (mRequestMethod != METHOD_GET || TextUtils.isEmpty(mCacheKey)) return;
        mCache.save(mCacheKey, content);
        mCache.save(mCacheKey, header);
    }

    private Request request() {
        SmartHttpConfig config = SmartHttpConfig.getInstance();
        mHeader.merge(config.commonHeader);
        mParam.merge(config.commonParam);
        Request.Builder builder = new Request.Builder().tag(mTag).headers(mHeader.convert());
        if (mCacheModel == DEFAULT_CACHE) {
            builder.cacheControl(new CacheControl.Builder().maxAge(DEFAULT_CACHE_AGE, TimeUnit.SECONDS).build());
        }
        switch (mRequestMethod) {
            case METHOD_GET:
                final String url = spliceUrl();
                builder.get().url(url);
                if (TextUtils.isEmpty(mCacheKey)) {
                    mCacheKey = Utils.md5(url);
                }
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
        client(builder, mCopyConfig);

        return builder.build();
    }

    private static void client(OkHttpClient.Builder builder, SmartHttpConfig config) {
        builder.writeTimeout(config.writeTimeout, TimeUnit.SECONDS)
                .readTimeout(config.readTimeout, TimeUnit.SECONDS)
                .connectTimeout(config.connectTimeout, TimeUnit.SECONDS);

        if (isDebug) {
            builder.addNetworkInterceptor(new LoggingInterceptor());
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

    @IntDef (value = {NO_CACHE, DEFAULT_CACHE, FIRST_CACHE_THEN_REQUEST})
    private @interface CacheModel {}

    private static class DefaultCallback<T> implements RequestCallback<T> {
        static RequestCallback callback() {
            return new DefaultCallback();
        }
        @Override
        public void onSuccess(T t, Map<String, String> headers, boolean isFromCache) {}
        @Override
        public void onFailure(SmartHttpException e) {}
    }

    private static class DefaultDownloadCallback implements DownloadCallback {
        static DownloadCallback callback() {
            return new DefaultDownloadCallback();
        }
        @Override
        public void onStart() {}
        @Override
        public void onLoading(long current, long total) {}
        @Override
        public void onEnd() {}
        @Override
        public void onFailure(SmartHttpException e) {}
    }
}
