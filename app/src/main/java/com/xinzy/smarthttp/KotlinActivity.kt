package com.xinzy.smarthttp

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.gson.reflect.TypeToken
import com.xinzy.http.kotlin.*

class KotlinActivity : AppCompatActivity() {


    companion object {
        private const val TAG = "KotlinActivity"
        private const val URL = "http://10.107.77.132/api/api.php?xxx=xx"
        private const val URL_DOWNLOAD = "http://192.168.3.23/api/download.zip"

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


}
