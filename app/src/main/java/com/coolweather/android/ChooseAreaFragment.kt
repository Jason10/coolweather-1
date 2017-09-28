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

import kotlinx.android.synthetic.main.choose_area.*

class ChooseAreaFragment : Fragment() {
    private var progressDialog: ProgressDialog? = null

    private var adapter: ArrayAdapter<String>? = null

    private val dataList = ArrayList<String>()

    private var provinceList: List<Province>? = null

    private var cityList: List<City>? = null

    private var countyList: List<County>? = null

    private var selectedProvince: Province? = null

    private var selectedCity: City? = null

    private var currentLevel: Int = 0

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.choose_area, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, dataList)
        list_view.adapter = adapter
        list_view.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            if (currentLevel == LEVEL_PROVINCE) {
                selectedProvince = provinceList!![position]
                queryCities()
            } else if (currentLevel == LEVEL_CITY) {
                selectedCity = cityList!![position]
                queryCounties()
            } else if (currentLevel == LEVEL_COUNTY) {
                val weatherId = countyList!![position].weatherId
                weatherId?.let {
                    when (activity) {
                        is MainActivity -> {
                            val intent = Intent(activity, WeatherActivity::class.java)
                            intent.putExtra("weather_id", weatherId)
                            startActivity(intent)
                            activity.finish()
                        }
                        is WeatherActivity -> {
                            val activity = activity as WeatherActivity
                            activity.drawerLayout.closeDrawers()
                            activity.swipeRefresh.isRefreshing = true
                            activity.requestWeather(weatherId)
                        }
                    }
                }
            }
        }
        back_button.setOnClickListener {
            if (currentLevel == LEVEL_COUNTY) {
                queryCities()
            } else if (currentLevel == LEVEL_CITY) {
                queryProvinces()
            }
        }
        queryProvinces()
    }

    private fun queryProvinces() {
        title_text.setText(R.string.china)
        back_button.visibility = View.GONE
        provinceList = DataSupport.findAll(Province::class.java)
        provinceList?.let {
            if (it.isNotEmpty()) {
                dataList.clear()
                it.map {
                    dataList.add(it.provinceName!!)
                }
                adapter!!.notifyDataSetChanged()
                list_view.setSelection(0)
                currentLevel = LEVEL_PROVINCE
            }
            return
        }
        queryFromServer(HttpUtil.China, "province")
    }

    private fun queryCities() {
        title_text.text = selectedProvince!!.provinceName
        back_button.visibility = View.VISIBLE
        cityList = DataSupport.where("provinceid = ?",
                selectedProvince!!.id.toString()).find(City::class.java)
        cityList?.let {
            if (it.isNotEmpty()) {
                dataList.clear()
                it.map {
                    dataList.add(it.cityName!!)
                }
                adapter!!.notifyDataSetChanged()
                list_view.setSelection(0)
                currentLevel = LEVEL_CITY
            }
            return
        }
        val provinceCode = selectedProvince!!.provinceCode
        val address = "${HttpUtil.China}/$provinceCode"
        queryFromServer(address, "city")
    }

    private fun queryCounties() {
        title_text.text = selectedCity!!.cityName
        back_button.visibility = View.VISIBLE
        countyList = DataSupport.where("cityid = ?",
                selectedCity!!.id.toString()).find(County::class.java)
        countyList?.let {
            if (it.isNotEmpty()) {
                dataList.clear()
                it.map {
                    dataList.add(it.countyName!!)
                }
                adapter!!.notifyDataSetChanged()
                list_view.setSelection(0)
                currentLevel = LEVEL_COUNTY
            }
            return
        }
        val provinceCode = selectedProvince!!.provinceCode
        val cityCode = selectedCity!!.cityCode
        val address = "http://guolin.tech/api/china/$provinceCode/$cityCode"
        queryFromServer(address, "county")
    }

    private fun queryFromServer(address: String, type: String) {
        showProgressDialog()
        HttpUtil.sendOkHttpRequest(address, object : Callback {
            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val responseText = response.body().string()
                var result = false
                when (type) {
                    "province" -> {
                        result = Utility.handleProvinceResponse(responseText)
                    }
                    "city" -> {
                        result = Utility.handleCityResponse(responseText, selectedProvince!!.id)
                    }
                    "county" -> {
                        result = Utility.handleCountyResponse(responseText, selectedCity!!.id)
                    }
                }
                if (result) {
                    activity.runOnUiThread(Runnable {
                        closeProgressDialog()
                        when (type) {
                            "province" -> {
                                queryProvinces()
                            }
                            "city" -> {
                                queryCities()
                            }
                            "county" -> {
                                queryCounties()
                            }
                        }
                    })
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                activity.runOnUiThread(Runnable {
                    closeProgressDialog()
                    Toast.makeText(context, "load failed", Toast.LENGTH_SHORT).show()
                })
            }
        })
    }

    private fun showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(activity)
            progressDialog?.setMessage("loading...")
            progressDialog?.setCanceledOnTouchOutside(false)
        }
        progressDialog?.show()
    }

    private fun closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog?.dismiss()
        }
    }

    companion object {
        private val TAG = "ChooseAreaFragment"

        val LEVEL_PROVINCE = 0

        val LEVEL_CITY = 1

        val LEVEL_COUNTY = 2
    }
}
