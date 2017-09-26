package com.coolweather.android.gson

import com.google.gson.annotations.SerializedName

class Suggestion {
    @SerializedName("comf")
    var comfort: Comfort? = null

    @SerializedName("cw")
    var carWash: CarWash? = null

    var sport: Sport? = null

    inner class Comfort {
        @SerializedName("txt")
        var info: String? = null
    }

    inner class CarWash {
        @SerializedName("txt")
        var info: String? = null
    }

    inner class Sport {
        @SerializedName("txt")
        var info: String? = null
    }
}
