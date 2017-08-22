package com.xinzy.smarthttp;

import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by on 17/6/10.
 */

public class InitInterceptor implements Interceptor{
    private static final String TAG = "InitInterceptor";

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Log.w(TAG, "intercept: Test init interceptor");

        try {
            return chain.proceed(request);
        } catch (IOException e) {
            e.printStackTrace();

            throw e;
        }
    }
}
