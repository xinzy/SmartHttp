package com.xinzy.smarthttp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.gson.reflect.TypeToken;
import com.xinzy.http.DownloadCallback;
import com.xinzy.http.RequestCallback;
import com.xinzy.http.SmartHttp;
import com.xinzy.http.SmartHttpException;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static final String URL = "http://10.107.77.132/api/api.php?xxx=xx";
    public static final String URL_DOWNLOAD = "http://192.168.3.23/api/download.zip";

    public static final String URL_HTTPS = "https://www.baidu.com";
    public static final String URL_HTTPS_UNSAFED = "https://kyfw.12306.cn/otn/leftTicket/query?leftTicketDTO.train_date=2017-06-30&leftTicketDTO.from_station=SHH&leftTicketDTO.to_station=ZZF&purpose_codes=ADULT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    public void onKotlin(View view) {
        startActivity(new Intent(this, KotlinActivity.class));
    }

    public void onGet(View view) {
        SmartHttp.get(URL).header("key", "123").header("token", "SmartHttp.SmartHttpException").cacheKey("test")
            .param("id", "1").param("name", "xin").param("val", "unknown").enqueue(new TypeToken<Res>(){}, new RequestCallback<Res>() {
            @Override
            public void onSuccess(Res res) {
                Log.d(TAG, "onSuccess: " + res);
            }

            @Override
            public void onFailure(SmartHttpException e) {
                e.printStackTrace();
            }
        });
    }

    public void onDelete(View view) {
        SmartHttp.delete(URL).header("key", "123").header("token", "SmartHttp.SmartHttpException")
            .param("id", "1").param("name", "xin").param("val", "unknown")
            .enqueue(new TypeToken<Res>(){}, new RequestCallback<Res>() {
            @Override
            public void onSuccess(Res res) {
                Log.d(TAG, "onSuccess: " + res);
            }

            @Override
            public void onFailure(SmartHttpException e) {
                e.printStackTrace();
            }
        });
    }

    public void onPut(View view) {
        SmartHttp.put(URL).header("key", "123").header("token", "SmartHttp.SmartHttpException")
                .param("id", "1").param("name", "xin").param("val", "unknown")
                .enqueue(new TypeToken<Res>(){}, new RequestCallback<Res>() {
                    @Override
                    public void onSuccess(Res res) {
                        Log.d(TAG, "onSuccess: " + res);
                    }

                    @Override
                    public void onFailure(SmartHttpException e) {
                        e.printStackTrace();
                    }
                });
    }

    public void onPost(View view) {
        SmartHttp.post(URL).header("key", "123").header("token", "SmartHttp.SmartHttpException")
            .param("id", "1").param("name", "xin").param("val", "unknown")
            .enqueue(Res.class, new RequestCallback<Res>() {
            @Override
            public void onSuccess(Res res) {
                Log.d(TAG, "onSuccess: " + res);
            }

            @Override
            public void onFailure(SmartHttpException e) {
                e.printStackTrace();
            }
        });
    }

    public void onUpload(View v) {
        File file = new File(Environment.getExternalStorageDirectory(), "00.jpg");
        File file1 = new File(Environment.getExternalStorageDirectory(), "01.jpg");
        SmartHttp.post(URL).header("key", "123").header("token", "SmartHttp.SmartHttpException")
            .param("id", "1").param("name", "xin").param("val", "unknown").param("img", file, "img.jpg")
            .param("flower", file1, "flower.jpg").enqueue(new TypeToken<Res>(){}, new RequestCallback<Res>() {
            @Override
            public void onSuccess(Res res) {
                Log.d(TAG, "onSuccess: " + res);
            }

            @Override
            public void onFailure(SmartHttpException e) {
                e.printStackTrace();
            }
        });
    }

    public void onDownload(View view) {
        File file = new File(Environment.getExternalStorageDirectory(), "download.zip");
        SmartHttp.get(URL_DOWNLOAD).download(file, new DownloadCallback() {
            @Override
            public void onStart() {
                Log.i(TAG, "onStart: ");
            }

            @Override
            public void onLoading(long current, long total) {
                Log.i(TAG, "onLoading: current=" + current + "; total=" + total);
            }

            @Override
            public void onEnd() {
                Log.i(TAG, "onEnd: ");
            }

            @Override
            public void onFailure(SmartHttpException e) {
                Log.e(TAG, "onFailure: ", e);
            }
        });
    }

    public void onSync(View v) {
        new Thread(() -> {
                Res res = SmartHttp.get(URL).header("key", "123").header("token", "SmartHttp.SmartHttpException")
                        .param("id", "1").param("name", "xin").param("val", "unknown").execute(Res.class);
                Log.i(TAG, "run: " + res);
        }).start();
    }

    public void onString(View view) {
        SmartHttp.get(URL).header("key", "123").header("token", "SmartHttp.SmartHttpException")
                .param("id", "1").param("name", "xin").param("val", "unknown").enqueue(new RequestCallback<String>() {
            @Override
            public void onSuccess(String s) {
                Log.i(TAG, "onSuccess: " + s);
            }

            @Override
            public void onFailure(SmartHttpException e) {
                e.printStackTrace();
            }
        });
    }

    public void onHttps0(View view) {
        SmartHttp.get(URL_HTTPS).enqueue(new RequestCallback<String>() {
            @Override
            public void onSuccess(String s) {
                Log.i(TAG, "onSuccess: " + s);
            }

            @Override
            public void onFailure(SmartHttpException e) {
                e.printStackTrace();
            }
        });
    }

    public void onHttps1(View view) throws IOException {
        SmartHttp.get(URL_HTTPS_UNSAFED).certificates(getAssets().open("srca.cer")).addNetworkInterceptor(new ModifyInterceptor())
            .enqueue(new RequestCallback<String>() {
            @Override
            public void onSuccess(String s) {
                Log.i(TAG, "onSuccess: " + s);
            }

            @Override
            public void onFailure(SmartHttpException e) {
                e.printStackTrace();
            }
        });
    }

    public void onRx(View v) {
//        SmartHttp.get(URL).header("key", "123").header("token", "SmartHttp.SmartHttpException")
//                .param("id", "1").param("name", "xin").param("val", "unknown").observable(Res.class)
//                .subscribe(res -> Log.d(TAG, "onRx: " + res));
    }

    public void onTest(View view) {
//        Observable.just((Void) null).lift((Observable.Operator<Res, Void>) subscriber -> new Subscriber<Void>() {
//            @Override
//            public void onCompleted() {
//                subscriber.onCompleted();
//            }
//
//            @Override
//            public void onError(Throwable e) {
//                subscriber.onError(e);
//            }
//
//            @Override
//            public void onNext(Void param) {
//                Res res = SmartHttp.get(URL).header("key", "123").header("token", "SmartHttp.SmartHttpException")
//                        .param("id", "1").param("name", "xin").param("val", "unknown").execute(Res.class);
//                subscriber.onNext(res);
//            }
//        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(res -> Log.d(TAG, "onTest: " + res));

//        Observable.just((Void) null).map(param -> {
//                Log.d(TAG, "onTest: " + Thread.currentThread());
//                return SmartHttp.get(URL).header("key", "123").header("token", "SmartHttp.SmartHttpException")
//                        .param("id", "1").param("name", "xin").param("val", "unknown").execute(Res.class);
//                })
//                .observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
//                .subscribe(res -> Log.d(TAG, "onTest: " + res + "; thread = " + Thread.currentThread()));
    }

    public void onCache(View view) {
        SmartHttp.get(URL).header("key", "123").header("token", "SmartHttp.SmartHttpException").cacheKey("test")
                .param("id", "1").param("name", "xin").param("val", "unknown").enqueue(new TypeToken<Res>(){}, new RequestCallback<Res>() {
            @Override
            public void onSuccess(Res res) {
                Log.d(TAG, "onSuccess: res=" + res);
            }

            @Override
            public void onFailure(SmartHttpException e) {
                e.printStackTrace();
            }
        });
    }

    public void onCancel(View view) {
        SmartHttp.get(URL).header("key", "123").header("token", "SmartHttp.SmartHttpException")
            .tag("cancel").param("id", "1").param("name", "xin").param("val", "unknown")
            .enqueue(new TypeToken<Res>(){}, new RequestCallback<Res>() {
            @Override
            public void onSuccess(Res res) {
                Log.d(TAG, "onSuccess: " + res);
            }

            @Override
            public void onFailure(SmartHttpException e) {
                e.printStackTrace();
            }
        });
        SmartHttp.cancel("cancel");
    }

    public void onDelayCancel(View view) {
        SmartHttp.get(URL).header("key", "123").header("token", "SmartHttp.SmartHttpException")
            .tag("cancel").param("id", "1").param("name", "xin").param("val", "unknown")
            .enqueue(new TypeToken<Res>(){}, new RequestCallback<Res>() {
            @Override
            public void onSuccess(Res res) {
                Log.d(TAG, "onSuccess: " + res);
            }

            @Override
            public void onFailure(SmartHttpException e) {
                e.printStackTrace();
            }
        });
        new Handler().postDelayed(() -> SmartHttp.cancel("cancel"), 1000);
    }
}
