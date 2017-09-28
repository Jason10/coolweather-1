package com.coolweather.android.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.Context
import android.content.SharedPreferences
import android.os.IBinder
import android.os.SystemClock
import android.preference.PreferenceManager

import com.coolweather.android.gson.Weather
import com.coolweather.android.util.HttpUtil
import com.coolweather.android.util.Utility

import java.io.IOException

import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response

class AutoUpdateService : Service() {
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        updateWeather()
        updateBingPic()

        val manager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // the number of milliseconds in eight hours
        val interval = 8 * 60 * 60 * 1000
        val triggerAtTime = SystemClock.elapsedRealtime() + interval
        val i = Intent(this, AutoUpdateService::class.java)
        val pi = PendingIntent.getService(this, 0, i, 0)
        manager.cancel(pi)
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi)

        return super.onStartCommand(intent, flags, startId)
    }

    private fun updateWeather() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val weatherString = prefs.getString("weather", null)
        weatherString?.let {
            // get info directly if has cached
            val weather = Utility.handleWeatherResponse(it)
            weather?.let {
                val basic = it.basic
                basic?.let {
                    val weatherId = it.weatherId
                    val weatherUrl = "${HttpUtil.Url}?cityid=$weatherId&${HttpUtil.Key}"
                    HttpUtil.sendOkHttpRequest(weatherUrl, object : Callback {
                        @Throws(IOException::class)
                        override fun onResponse(call: Call, response: Response) {
                            val responseText = response.body().string()
                            val weatherInfo = Utility.handleWeatherResponse(responseText)
                            weatherInfo?.let {
                                if ("ok" == it.status) {
                                    val editor = PreferenceManager.getDefaultSharedPreferences(this@AutoUpdateService).edit()
                                    editor.putString("weather", responseText)
                                    editor.apply()
                                }
                            }
                        }

                        override fun onFailure(call: Call, e: IOException) {
                            e.printStackTrace()
                        }
                    })
                }
            }
        }
    }

    private fun updateBingPic() {
        HttpUtil.sendOkHttpRequest(HttpUtil.Bing, object : Callback {
            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val bingPic = response.body().string()
                val editor = PreferenceManager.getDefaultSharedPreferences(
                        this@AutoUpdateService).edit()
                editor.putString("bing_pic", bingPic)
                editor.apply()
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
        })
    }
}