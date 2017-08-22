package com.xinzy.smarthttp;

import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by  on 17/6/10.
 */

public class ModifyInterceptor implements Interceptor {
    private static final String TAG = "ModifyInterceptor";

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Log.w(TAG, "intercept: Modify interceptor");
        return chain.proceed(request);
    }
}
