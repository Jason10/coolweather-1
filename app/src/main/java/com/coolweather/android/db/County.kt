package com.coolweather.android.db

import org.litepal.crud.DataSupport

data class County(var id: Int = 0,
                  var countyName: String? = null,
                  var weatherId: String? = null,
                  var cityId: Int = 0) : DataSupport() {}
