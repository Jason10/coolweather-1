package com.coolweather.android

import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this@MainActivity)
        val weather = prefs.getString("weather", null)
        weather?.let {
            val intent = Intent(this, WeatherActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}