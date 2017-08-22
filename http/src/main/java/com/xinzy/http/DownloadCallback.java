package com.xinzy.http;

/**
 * Created by Xinzy on 2017/6/12.
 *
 */
public interface DownloadCallback {
    void onStart();
    void onLoading(long current, long total);
    void onEnd();
    void onFailure(SmartHttpException e);
}
