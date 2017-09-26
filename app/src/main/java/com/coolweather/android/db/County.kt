package com.coolweather.android.db

import org.litepal.crud.DataSupport

class County : DataSupport() {
    var id: Int = 0

    var countyName: String? = null

    var weatherId: String? = null  //via the id to get weather info

    var cityId: Int = 0
}
