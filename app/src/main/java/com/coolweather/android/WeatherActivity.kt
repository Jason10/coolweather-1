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

class WeatherActivity : AppCompatActivity() {
    val drawerLayout by lazy {
        findViewById(R.id.drawer_layout) as DrawerLayout
    }

    val swipeRefresh by lazy {
        findViewById(R.id.swipe_refresh) as SwipeRefreshLayout
    }

    private var weatherLayout: ScrollView? = null

    private var navButton: Button? = null

    private var titleCity: TextView? = null

    private var titleUpdateTime: TextView? = null

    private var degreeText: TextView? = null

    private var weatherInfoText: TextView? = null

    private var forecastLayout: LinearLayout? = null

    private var aqiText: TextView? = null

    private var pm25Text: TextView? = null

    private var comfortText: TextView? = null

    private var carWashText: TextView? = null

    private var sportText: TextView? = null

    private var bingPicImg: ImageView? = null

    private var mWeatherId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 21) {
            val decorView = getWindow().getDecorView()
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
            getWindow().setStatusBarColor(Color.TRANSPARENT)
        }
        setContentView(R.layout.activity_weather)
        // init views
        bingPicImg = findViewById(R.id.bing_pic_img) as ImageView
        weatherLayout = findViewById(R.id.weather_layout) as ScrollView
        titleCity = findViewById(R.id.title_city) as TextView
        titleUpdateTime = findViewById(R.id.title_update_time) as TextView
        degreeText = findViewById(R.id.degree_text) as TextView
        weatherInfoText = findViewById(R.id.weather_info_text) as TextView
        forecastLayout = findViewById(R.id.forecast_layout) as LinearLayout
        aqiText = findViewById(R.id.aqi_text) as TextView
        pm25Text = findViewById(R.id.pm25_text) as TextView
        comfortText = findViewById(R.id.comfort_text) as TextView
        carWashText = findViewById(R.id.car_wash_text) as TextView
        sportText = findViewById(R.id.sport_text) as TextView
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary)
        navButton = findViewById(R.id.nav_button) as Button
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val weatherString = prefs.getString("weather", null)
        if (weatherString != null) {
            // via cache
            val weather = Utility.handleWeatherResponse(weatherString)
            mWeatherId = weather!!.basic!!.weatherId
            showWeatherInfo(weather)
        } else {
            // via network
            mWeatherId = getIntent().getStringExtra("weather_id")
            weatherLayout!!.visibility = View.INVISIBLE
            requestWeather(mWeatherId)
        }
        swipeRefresh.setOnRefreshListener { requestWeather(mWeatherId) }
        navButton!!.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
        val bingPic = prefs.getString("bing_pic", null)
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(bingPicImg!!)
        } else {
            loadBingPic()
        }
    }

    /**
     * request weather info via id
     */
    fun requestWeather(weatherId: String?) {
        val weatherUrl = "http://guolin.tech/api/weather?cityid=$weatherId&key=bc0418b57b2d4918819d3974ac1285d9"
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

    /**
     * load image from bing
     */
    private fun loadBingPic() {
        val requestBingPic = "http://guolin.tech/api/bing_pic"
        HttpUtil.sendOkHttpRequest(requestBingPic, object : Callback {
            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val bingPic = response.body().string()
                val editor = PreferenceManager.getDefaultSharedPreferences(this@WeatherActivity).edit()
                editor.putString("bing_pic", bingPic)
                editor.apply()
                runOnUiThread(Runnable { Glide.with(this@WeatherActivity).load(bingPic).into(bingPicImg!!) })
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
        })
    }

    /**
     * process and show info
     */
    private fun showWeatherInfo(weather: Weather?) {
        val cityName = weather!!.basic!!.cityName
        val updateTime = weather.basic!!.update!!.updateTime!!.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
        val degree = weather.now!!.temperature!! + "℃"
        val weatherInfo = weather.now!!.more!!.info
        titleCity!!.text = cityName
        titleUpdateTime!!.text = updateTime
        degreeText!!.text = degree
        weatherInfoText!!.text = weatherInfo
        forecastLayout!!.removeAllViews()
        for (forecast in weather.forecastList!!) {
            val view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false)
            val dateText = view.findViewById(R.id.date_text) as TextView
            val infoText = view.findViewById(R.id.info_text) as TextView
            val maxText = view.findViewById(R.id.max_text) as TextView
            val minText = view.findViewById(R.id.min_text) as TextView
            dateText.text = forecast.date
            infoText.text = forecast.more!!.info
            maxText.text = forecast.temperature!!.max
            minText.text = forecast.temperature!!.min
            forecastLayout!!.addView(view)
        }
        if (weather.aqi != null) {
            aqiText!!.text = weather.aqi!!.city!!.aqi
            pm25Text!!.text = weather.aqi!!.city!!.pm25
        }
        val comfort = resources.getString(R.string.comfort) + weather.suggestion!!.comfort!!.info!!
        val carWash = resources.getString(R.string.car) + weather.suggestion!!.carWash!!.info!!
        val sport = resources.getString(R.string.sport) + weather.suggestion!!.sport!!.info!!
        comfortText!!.text = comfort
        carWashText!!.text = carWash
        sportText!!.text = sport
        weatherLayout!!.visibility = View.VISIBLE
        val intent = Intent(this, AutoUpdateService::class.java)
        startService(intent)
    }
}
