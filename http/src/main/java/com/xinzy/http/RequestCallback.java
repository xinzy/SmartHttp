package com.xinzy.http;

/**
 * Created by Xinzy on 2017/6/12.
 *
 */
public interface RequestCallback<T> {

    void onSuccess(T t);
    void onFailure(SmartHttpException e);

    class DefaultRequestCallback<T> implements RequestCallback<T> {

        @Override
        public void onSuccess(T t) { }

        @Override
        public void onFailure(SmartHttpException e) { }
    }
}
