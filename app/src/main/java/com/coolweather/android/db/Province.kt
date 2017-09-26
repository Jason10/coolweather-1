package com.coolweather.android.db

import org.litepal.crud.DataSupport

class Province : DataSupport() {
    var id: Int = 0

    var provinceName: String? = null

    var provinceCode: Int = 0
}
