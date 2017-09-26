package com.coolweather.android.db

import org.litepal.crud.DataSupport

class City : DataSupport() {
    var id: Int = 0

    var cityName: String? = null

    var cityCode: Int = 0

    var provinceId: Int = 0
}
