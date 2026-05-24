package app.zlauncher.data

object Constants {

    object Key {
        const val FLAG = "flag"
        const val RENAME = "rename"
    }

    object DateTime {
        const val OFF = 0
        const val ON = 1
        const val DATE_ONLY = 2

        fun isTimeVisible(dateTimeVisibility: Int): Boolean = dateTimeVisibility == ON

        fun isDateVisible(dateTimeVisibility: Int): Boolean =
            dateTimeVisibility == ON || dateTimeVisibility == DATE_ONLY
    }

    object SwipeDownAction {
        const val SEARCH = 1
        const val NOTIFICATIONS = 2
    }

    val CLOCK_APP_PACKAGES = arrayOf(
        "com.google.android.deskclock",
        "com.sec.android.app.clockpackage",
        "com.oneplus.deskclock",
        "com.miui.clock",
    )

    /** Fallback packages tried in order when the user has not picked a weather app. */
    val WEATHER_APP_PACKAGES = arrayOf(
        "com.google.android.apps.weather",
        "com.google.android.googlequicksearchbox",
        "com.samsung.android.weather",
        "com.miui.weather2",
        "com.oneplus.weather",
        "com.accuweather.android",
        "com.weather.Weather",
        "com.wunderground.android.weather",
    )

    const val FLAG_LAUNCH_APP = 100
    const val FLAG_HIDDEN_APPS = 101

    const val FLAG_SET_HOME_APP_1 = 1
    const val FLAG_SET_HOME_APP_2 = 2
    const val FLAG_SET_HOME_APP_3 = 3
    const val FLAG_SET_HOME_APP_4 = 4
    const val FLAG_SET_HOME_APP_5 = 5
    const val FLAG_SET_HOME_APP_6 = 6
    const val FLAG_SET_HOME_APP_7 = 7
    const val FLAG_SET_HOME_APP_8 = 8
    const val FLAG_SET_HOME_APP_9 = 9
    const val FLAG_SET_HOME_APP_10 = 10
    const val FLAG_SET_HOME_APP_11 = 11
    const val FLAG_SET_HOME_APP_12 = 12

    const val FLAG_SET_SWIPE_LEFT_APP = 17
    const val FLAG_SET_SWIPE_RIGHT_APP = 18
    const val FLAG_SET_CLOCK_APP = 13
    const val FLAG_SET_CALENDAR_APP = 14
    const val FLAG_SET_WEATHER_APP = 16

    const val LONG_PRESS_DELAY_MS = 500L
    const val ONE_HOUR_IN_MILLIS = 3600000L
    const val ONE_MINUTE_IN_MILLIS = 60000L

    const val MIN_ANIM_REFRESH_RATE = 30f

    /** DuckDuckGo bang search prefix; triggered when the drawer query starts with "!". */
    const val URL_DUCK_SEARCH = "https://duck.co/?q="

    /** Open-Meteo free, no-key weather endpoint. */
    const val URL_OPEN_METEO_BASE = "https://api.open-meteo.com/v1/forecast"
    const val WEATHER_CACHE_TTL_MS = 30 * ONE_MINUTE_IN_MILLIS
    const val WEATHER_CACHE_INVALIDATE_DISTANCE_M = 5000.0

    const val MAX_HOME_APPS = 12

    fun isPickAppFlag(flag: Int): Boolean = when (flag) {
        in FLAG_SET_HOME_APP_1..FLAG_SET_HOME_APP_12 -> true
        FLAG_SET_SWIPE_LEFT_APP, FLAG_SET_SWIPE_RIGHT_APP,
        FLAG_SET_CLOCK_APP, FLAG_SET_CALENDAR_APP, FLAG_SET_WEATHER_APP -> true
        FLAG_HIDDEN_APPS -> true
        else -> false
    }
}
