package com.coolweather.android.util

import android.text.TextUtils

import com.coolweather.android.db.City
import com.coolweather.android.db.County
import com.coolweather.android.db.Province
import com.coolweather.android.gson.Weather
import com.google.gson.Gson

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object Utility {
    /**
     * 解析和处理服务器返回的省级数据
     */
    fun handleProvinceResponse(response: String): Boolean {
        if (!TextUtils.isEmpty(response)) {
            try {
                val allProvinces = JSONArray(response)
                for (i in 0..allProvinces.length() - 1) {
                    val provinceObject = allProvinces.getJSONObject(i)
                    val province = Province()
                    province.provinceName = provinceObject.getString("name")
                    province.provinceCode = provinceObject.getInt("id")
                    province.save()
                }
                return true
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }
        return false
    }

    /**
     * 解析和处理服务器返回的市级数据
     */
    fun handleCityResponse(response: String, provinceId: Int): Boolean {
        if (!TextUtils.isEmpty(response)) {
            try {
                val allCities = JSONArray(response)
                for (i in 0..allCities.length() - 1) {
                    val cityObject = allCities.getJSONObject(i)
                    val city = City()
                    city.cityName = cityObject.getString("name")
                    city.cityCode = cityObject.getInt("id")
                    city.provinceId = provinceId
                    city.save()
                }
                return true
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }
        return false
    }

    /**
     * 解析和处理服务器返回的县级数据
     */
    fun handleCountyResponse(response: String, cityId: Int): Boolean {
        if (!TextUtils.isEmpty(response)) {
            try {
                val allCounties = JSONArray(response)
                for (i in 0..allCounties.length() - 1) {
                    val countyObject = allCounties.getJSONObject(i)
                    val county = County()
                    county.countyName = countyObject.getString("name")
                    county.weatherId = countyObject.getString("weather_id")
                    county.cityId = cityId
                    county.save()
                }
                return true
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }
        return false
    }

    /**
     * 将返回的JSON数据解析成Weather实体类
     */
    fun handleWeatherResponse(response: String): Weather? {
        try {
            val jsonObject = JSONObject(response)
            val jsonArray = jsonObject.getJSONArray("HeWeather")
            val weatherContent = jsonArray.getJSONObject(0).toString()
            return Gson().fromJson(weatherContent, Weather::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }
}
