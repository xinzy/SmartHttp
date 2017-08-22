package com.xinzy.http;

/**
 * Created by Xinzy on 2017/6/8.
 */

public class SmartHttpException extends Exception {

    public SmartHttpException() {
        super();
    }

    public SmartHttpException(String message) {
        super(message);
    }

    public SmartHttpException(String message, Throwable cause) {
        super(message, cause);
    }

    public SmartHttpException(Throwable cause) {
        super(cause);
    }
}