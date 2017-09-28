package com.coolweather.android

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

import com.bumptech.glide.Glide
import com.coolweather.android.gson.Forecast
import com.coolweather.android.gson.Weather
import com.coolweather.android.service.AutoUpdateService
import com.coolweather.android.util.HttpUtil
import com.coolweather.android.util.Utility

import java.io.IOException

import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_weather.*
import kotlinx.android.synthetic.main.suggestion.*
import kotlinx.android.synthetic.main.title.*
import kotlinx.android.synthetic.main.now.*
import kotlinx.android.synthetic.main.forecast.*
import kotlinx.android.synthetic.main.aqi.*


class WeatherActivity : AppCompatActivity() {
    private var mWeatherId: String? = null

    val drawerLayout by lazy {
        findViewById(R.id.drawer_layout) as DrawerLayout
    }

    val swipeRefresh by lazy {
        findViewById(R.id.swipe_refresh) as SwipeRefreshLayout
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 21) {
            val decorView = getWindow().getDecorView()
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
            getWindow().setStatusBarColor(Color.TRANSPARENT)
        }
        setContentView(R.layout.activity_weather)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val weatherString = prefs.getString("weather", null)
        if (weatherString != null) {
            // via cache
            val weather = Utility.handleWeatherResponse(weatherString)
            weather?.let {
                mWeatherId = it.basic!!.weatherId
                showWeatherInfo(it)
            }
        } else {
            // via network
            mWeatherId = getIntent().getStringExtra("weather_id")
            mWeatherId?.let {
                weather_layout.visibility = View.INVISIBLE
                requestWeather(it)
            }
        }
        swipeRefresh.setOnRefreshListener {
            mWeatherId?.let {
                requestWeather(it)
            }
        }
        nav_button.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        val bingPic = prefs.getString("bing_pic", null)
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(bing_pic_img)
        } else {
            loadBingPic()
        }
    }

    fun requestWeather(weatherId: String) {
        val weatherUrl = "${HttpUtil.Url}?cityid=$weatherId&${HttpUtil.Key}"
        HttpUtil.sendOkHttpRequest(weatherUrl, object : Callback {
            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val responseText = response.body().string()
                val weather = Utility.handleWeatherResponse(responseText)
                runOnUiThread(Runnable {
                    if (weather != null && "ok" == weather.status) {
                        val editor = PreferenceManager.getDefaultSharedPreferences(this@WeatherActivity).edit()
                        editor.putString("weather", responseText)
                        editor.apply()
                        mWeatherId = weather.basic!!.weatherId
                        showWeatherInfo(weather)
                    } else {
                        Toast.makeText(this@WeatherActivity, "request failed", Toast.LENGTH_SHORT).show()
                    }
                    swipeRefresh.isRefreshing = false
                })
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread(Runnable {
                    Toast.makeText(this@WeatherActivity, "request failed", Toast.LENGTH_SHORT).show()
                    swipeRefresh.isRefreshing = false
                })
            }
        })
        loadBingPic()
    }

    private fun loadBingPic() {
        HttpUtil.sendOkHttpRequest(HttpUtil.Bing, object : Callback {
            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val bingPic = response.body().string()
                val editor = PreferenceManager.getDefaultSharedPreferences(
                        this@WeatherActivity).edit()
                editor.putString("bing_pic", bingPic)
                editor.apply()
                runOnUiThread(Runnable {
                    Glide.with(this@WeatherActivity).load(bingPic).into(bing_pic_img)
                })
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
        })
    }

    private fun showWeatherInfo(weather: Weather) {
        val cityName = weather.basic!!.cityName
        val updateTime = weather.basic!!.update!!.updateTime!!.split(" ".toRegex()).
                dropLastWhile { it.isEmpty() }.toTypedArray()[1]
        val degree = weather.now!!.temperature!! + "℃"
        val weatherInfo = weather.now!!.more!!.info
        title_city.text = cityName
        title_update_time.text = updateTime
        degree_text.text = degree
        weather_info_text.text = weatherInfo
        forecast_layout.removeAllViews()
        weather.forecastList!!.map {
            val view = LayoutInflater.from(this).inflate(R.layout.forecast_item,
                    forecast_layout, false)
            val dateText = view.findViewById(R.id.date_text) as TextView
            val infoText = view.findViewById(R.id.info_text) as TextView
            val maxText = view.findViewById(R.id.max_text) as TextView
            val minText = view.findViewById(R.id.min_text) as TextView
            dateText.text = it.date
            infoText.text = it.more!!.info
            maxText.text = it.temperature!!.max
            minText.text = it.temperature!!.min
            forecast_layout.addView(view)
        }
        weather.aqi?.let {
            aqi_text.text = it.city!!.aqi
            pm25_text.text = it.city!!.pm25
        }
        val comfort = resources.getString(R.string.comfort) + weather.suggestion!!.comfort!!.info!!
        val carWash = resources.getString(R.string.car) + weather.suggestion!!.carWash!!.info!!
        val sport = resources.getString(R.string.sport) + weather.suggestion!!.sport!!.info!!
        comfort_text.text = comfort
        car_wash_text.text = carWash
        sport_text.text = sport
        weather_layout.visibility = View.VISIBLE
        val intent = Intent(this, AutoUpdateService::class.java)
        startService(intent)
    }
}