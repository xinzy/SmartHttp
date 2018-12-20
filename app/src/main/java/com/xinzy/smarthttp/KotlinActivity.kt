package com.xinzy.smarthttp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.google.gson.reflect.TypeToken
import com.xinzy.http.kotlin.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class KotlinActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "KotlinActivity"
        private const val URL = "http://10.107.77.132/api/api.php?xxx=xx"
        private const val URL_DOWNLOAD = "http://10.107.77.132/api/apks/weidu.apk"

        private const val URL_HTTPS = "https://www.baidu.com"
        private const val URL_HTTPS_UNSAFED = "https://kyfw.12306.cn/otn/leftTicket/query?leftTicketDTO.train_date=2017-06-30&leftTicketDTO.from_station=SHH&leftTicketDTO.to_station=ZZF&purpose_codes=ADULT"


        fun start(context: Context) {
            context.startActivity(Intent(context, KotlinActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kotlin)
    }

    fun onGet(v: View) {
        val cache = SmartHttp.get(URL).param("aa", "11").header("hh", "22").cacheKey("123123")
                .enqueue(object : TypeToken<Res>(){}, object : RequestCallback<Res> {
                    override fun onSuccess(t: Res?) {
                        Log.d(TAG, "onSuccess: $t")
                    }

                    override fun onFailure(e: SmartHttpException) {
                        e.printStackTrace()
                    }
                })
        Log.d(TAG, "cached value=$cache")
    }

    fun onGet2(v: View) {
        val cache = URL.httpGet().param("aa", "11").header("hh", "22").cacheKey("123123123123")
                .enqueue(object : TypeToken<Res>(){},
                        { data: Res? -> run { Log.d(TAG, "onSuccess: $data")} },
                        { Log.d(TAG, "failure") })
        Log.d(TAG, "cached value=$cache")
    }

    fun onUpload(v: View) {
        val file1 = File(Environment.getExternalStorageDirectory(), "0.jpg")
        val file2 = File(Environment.getExternalStorageDirectory(), "1.jpg")
        "http://10.107.77.132/api/weidu.php?action=upload".httpPost()
                .param("aaa", "0000")
                .param("file1", file1).param("file2", file2)
                .enqueue(
                        { data: String? -> run { Log.d(TAG, "onSuccess: $data")} },
                        { Log.d(TAG, "failure") }
                )
    }

    fun onDownload(v: View) {
        val file = File(Environment.getExternalStorageDirectory(), "00.apk")
        URL_DOWNLOAD.httpGet().download(file, object : DownloadCallback {
            override fun onStart() {
                Log.d(TAG, "download start")
            }

            override fun onLoading(current: Long, total: Long) {
                Log.d(TAG, "download start: $current / $total")
            }

            override fun onEnd() {
                Log.d(TAG, "download end")
            }

            override fun onFailure(e: SmartHttpException) {
                Log.e(TAG, "download failure", e)
            }
        })
    }

    fun onCoroutine(v: View) {
        Log.d(TAG, "start thread=${Thread.currentThread().name}")
        GlobalScope.launch(Dispatchers.IO) {
            Log.d(TAG, "thread=${Thread.currentThread().name}")
            val content = URL.httpGet().param("aa", "11").header("hh", "22").cacheKey("123123123123").execute()
            launch(Dispatchers.Main) {
                Log.d(TAG, "${Thread.currentThread().name} content=$content")
            }
        }
    }
}
