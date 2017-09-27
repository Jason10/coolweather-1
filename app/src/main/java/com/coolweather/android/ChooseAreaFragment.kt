package com.coolweather.android

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

import com.coolweather.android.db.City
import com.coolweather.android.db.County
import com.coolweather.android.db.Province
import com.coolweather.android.util.HttpUtil
import com.coolweather.android.util.Utility

import org.litepal.crud.DataSupport

import java.io.IOException
import java.util.ArrayList

import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response

class ChooseAreaFragment : Fragment() {

    private var progressDialog: ProgressDialog? = null

    private var titleText: TextView? = null

    private var backButton: Button? = null

    private var listView: ListView? = null

    private var adapter: ArrayAdapter<String>? = null

    private val dataList = ArrayList<String>()

    /**
     * province list
     */
    private var provinceList: List<Province>? = null

    /**
     * city list
     */
    private var cityList: List<City>? = null

    /**
     * country list
     */
    private var countyList: List<County>? = null

    /**
     * selected province
     */
    private var selectedProvince: Province? = null

    /**
     * selected city
     */
    private var selectedCity: City? = null

    /**
     * selected level
     */
    private var currentLevel: Int = 0


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.choose_area, container, false)
        titleText = view.findViewById(R.id.title_text) as TextView
        backButton = view.findViewById(R.id.back_button) as Button
        listView = view.findViewById(R.id.list_view) as ListView
        adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, dataList)
        listView!!.adapter = adapter
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        listView!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            if (currentLevel == LEVEL_PROVINCE) {
                selectedProvince = provinceList!![position]
                queryCities()
            } else if (currentLevel == LEVEL_CITY) {
                selectedCity = cityList!![position]
                queryCounties()
            } else if (currentLevel == LEVEL_COUNTY) {
                val weatherId = countyList!![position].weatherId
                if (activity is MainActivity) {
                    val intent = Intent(activity, WeatherActivity::class.java)
                    intent.putExtra("weather_id", weatherId)
                    startActivity(intent)
                    activity.finish()
                } else if (activity is WeatherActivity) {
                    val activity = activity as WeatherActivity
                    activity.drawerLayout.closeDrawers()
                    activity.swipeRefresh.isRefreshing = true
                    activity.requestWeather(weatherId)
                }
            }
        }
        backButton!!.setOnClickListener {
            if (currentLevel == LEVEL_COUNTY) {
                queryCities()
            } else if (currentLevel == LEVEL_CITY) {
                queryProvinces()
            }
        }
        queryProvinces()
    }

    /**
     * query province, first database, then network
     */
    private fun queryProvinces() {
        titleText!!.setText(R.string.china)
        backButton!!.visibility = View.GONE
        provinceList = DataSupport.findAll(Province::class.java)
        if (provinceList!!.size > 0) {
            dataList.clear()
            for (province in provinceList!!) {
                dataList.add(province.provinceName!!)
            }
            adapter!!.notifyDataSetChanged()
            listView!!.setSelection(0)
            currentLevel = LEVEL_PROVINCE
        } else {
            val address = "http://guolin.tech/api/china"
            queryFromServer(address, "province")
        }
    }

    /**
     * query city, etc.
     */
    private fun queryCities() {
        titleText!!.text = selectedProvince!!.provinceName
        backButton!!.visibility = View.VISIBLE
        cityList = DataSupport.where("provinceid = ?", selectedProvince!!.id.toString()).find(City::class.java)
        if (cityList!!.size > 0) {
            dataList.clear()
            for (city in cityList!!) {
                dataList.add(city.cityName!!)
            }
            adapter!!.notifyDataSetChanged()
            listView!!.setSelection(0)
            currentLevel = LEVEL_CITY
        } else {
            val provinceCode = selectedProvince!!.provinceCode
            val address = "http://guolin.tech/api/china/" + provinceCode
            queryFromServer(address, "city")
        }
    }

    /**
     * query country, etc.
     */
    private fun queryCounties() {
        titleText!!.text = selectedCity!!.cityName
        backButton!!.visibility = View.VISIBLE
        countyList = DataSupport.where("cityid = ?", selectedCity!!.id.toString()).find(County::class.java)
        if (countyList!!.size > 0) {
            dataList.clear()
            for (county in countyList!!) {
                dataList.add(county.countyName!!)
            }
            adapter!!.notifyDataSetChanged()
            listView!!.setSelection(0)
            currentLevel = LEVEL_COUNTY
        } else {
            val provinceCode = selectedProvince!!.provinceCode
            val cityCode = selectedCity!!.cityCode
            val address = "http://guolin.tech/api/china/$provinceCode/$cityCode"
            queryFromServer(address, "county")
        }
    }

    /**
     * query weather info via address and type
     */
    private fun queryFromServer(address: String, type: String) {
        showProgressDialog()
        HttpUtil.sendOkHttpRequest(address, object : Callback {
            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val responseText = response.body().string()
                var result = false
                if ("province" == type) {
                    result = Utility.handleProvinceResponse(responseText)
                } else if ("city" == type) {
                    result = Utility.handleCityResponse(responseText, selectedProvince!!.id)
                } else if ("county" == type) {
                    result = Utility.handleCountyResponse(responseText, selectedCity!!.id)
                }
                if (result) {
                    activity.runOnUiThread(Runnable {
                        closeProgressDialog()
                        if ("province" == type) {
                            queryProvinces()
                        } else if ("city" == type) {
                            queryCities()
                        } else if ("county" == type) {
                            queryCounties()
                        }
                    })
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                // run on main thread
                activity.runOnUiThread(Runnable {
                    closeProgressDialog()
                    Toast.makeText(context, "load failed", Toast.LENGTH_SHORT).show()
                })
            }
        })
    }

    /**
     * show progress
     */
    private fun showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(activity)
            progressDialog!!.setMessage("loading...")
            progressDialog!!.setCanceledOnTouchOutside(false)
        }
        progressDialog!!.show()
    }

    /**
     * hide progress
     */
    private fun closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog!!.dismiss()
        }
    }

    companion object {
        private val TAG = "ChooseAreaFragment"

        val LEVEL_PROVINCE = 0

        val LEVEL_CITY = 1

        val LEVEL_COUNTY = 2
    }
}
