package com.xinzy.smarthttp;

import android.app.Application;

import com.xinzy.http.SmartHttp;
import com.xinzy.http.SmartHttpConfig;

/**
 * Created by on 2017/6/8.
 */

public class HttpApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SmartHttpConfig config = SmartHttpConfig.getInstance().with(this).debug(true).connectTimeout(30).writeTimeout(30)
                .readTimeout(30).addCommonHeader("key", "val").addCommonParam("key", "val").cookie(SmartHttp.MEMORY_COOKIE)
                .addNetworkInterceptor(new InitInterceptor());
        config.initHttpClient();
    }
}
