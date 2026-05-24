package app.zlauncher.data

import android.content.Context
import android.content.SharedPreferences
import android.view.Gravity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit

class Prefs(context: Context) {
    private val PREFS_FILENAME = "app.zlauncher"

    private val FIRST_OPEN = "FIRST_OPEN"
    private val FIRST_HIDE = "FIRST_HIDE"
    private val LOCK_MODE = "LOCK_MODE"
    private val HOME_APPS_NUM = "HOME_APPS_NUM"
    private val HOME_ALIGNMENT = "HOME_ALIGNMENT"
    private val HOME_BOTTOM_ALIGNMENT = "HOME_BOTTOM_ALIGNMENT"
    private val APP_LABEL_ALIGNMENT = "APP_LABEL_ALIGNMENT"
    private val STATUS_BAR = "STATUS_BAR"
    private val DATE_TIME_VISIBILITY = "DATE_TIME_VISIBILITY"
    private val HIDDEN_APPS = "HIDDEN_APPS"
    private val HIDDEN_APPS_UPDATED = "HIDDEN_APPS_UPDATED"
    private val APP_THEME = "APP_THEME"
    private val SWIPE_DOWN_ACTION = "SWIPE_DOWN_ACTION"
    private val TEXT_SIZE_SCALE = "TEXT_SIZE_SCALE"
    private val HIDE_SET_DEFAULT_LAUNCHER = "HIDE_SET_DEFAULT_LAUNCHER"
    private val LAUNCHER_RESTART_TIMESTAMP = "LAUNCHER_RECREATE_TIMESTAMP"
    private val HOME_BUTTON_SHOW_RECENTS = "HOME_BUTTON_SHOW_RECENTS"

    private val APP_NAME_1 = "APP_NAME_1"
    private val APP_NAME_2 = "APP_NAME_2"
    private val APP_NAME_3 = "APP_NAME_3"
    private val APP_NAME_4 = "APP_NAME_4"
    private val APP_NAME_5 = "APP_NAME_5"
    private val APP_NAME_6 = "APP_NAME_6"
    private val APP_NAME_7 = "APP_NAME_7"
    private val APP_NAME_8 = "APP_NAME_8"
    private val APP_PACKAGE_1 = "APP_PACKAGE_1"
    private val APP_PACKAGE_2 = "APP_PACKAGE_2"
    private val APP_PACKAGE_3 = "APP_PACKAGE_3"
    private val APP_PACKAGE_4 = "APP_PACKAGE_4"
    private val APP_PACKAGE_5 = "APP_PACKAGE_5"
    private val APP_PACKAGE_6 = "APP_PACKAGE_6"
    private val APP_PACKAGE_7 = "APP_PACKAGE_7"
    private val APP_PACKAGE_8 = "APP_PACKAGE_8"
    private val APP_ACTIVITY_CLASS_NAME_1 = "APP_ACTIVITY_CLASS_NAME_1"
    private val APP_ACTIVITY_CLASS_NAME_2 = "APP_ACTIVITY_CLASS_NAME_2"
    private val APP_ACTIVITY_CLASS_NAME_3 = "APP_ACTIVITY_CLASS_NAME_3"
    private val APP_ACTIVITY_CLASS_NAME_4 = "APP_ACTIVITY_CLASS_NAME_4"
    private val APP_ACTIVITY_CLASS_NAME_5 = "APP_ACTIVITY_CLASS_NAME_5"
    private val APP_ACTIVITY_CLASS_NAME_6 = "APP_ACTIVITY_CLASS_NAME_6"
    private val APP_ACTIVITY_CLASS_NAME_7 = "APP_ACTIVITY_CLASS_NAME_7"
    private val APP_ACTIVITY_CLASS_NAME_8 = "APP_ACTIVITY_CLASS_NAME_8"
    private val APP_USER_1 = "APP_USER_1"
    private val APP_USER_2 = "APP_USER_2"
    private val APP_USER_3 = "APP_USER_3"
    private val APP_USER_4 = "APP_USER_4"
    private val APP_USER_5 = "APP_USER_5"
    private val APP_USER_6 = "APP_USER_6"
    private val APP_USER_7 = "APP_USER_7"
    private val APP_USER_8 = "APP_USER_8"

    private val APP_NAME_SWIPE_LEFT = "APP_NAME_SWIPE_LEFT"
    private val APP_NAME_SWIPE_RIGHT = "APP_NAME_SWIPE_RIGHT"
    private val APP_PACKAGE_SWIPE_LEFT = "APP_PACKAGE_SWIPE_LEFT"
    private val APP_PACKAGE_SWIPE_RIGHT = "APP_PACKAGE_SWIPE_RIGHT"
    private val APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT = "APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT"
    private val APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT = "APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT"
    private val APP_USER_SWIPE_LEFT = "APP_USER_SWIPE_LEFT"
    private val APP_USER_SWIPE_RIGHT = "APP_USER_SWIPE_RIGHT"
    private val CLOCK_APP_PACKAGE = "CLOCK_APP_PACKAGE"
    private val CLOCK_APP_USER = "CLOCK_APP_USER"
    private val CLOCK_APP_CLASS_NAME = "CLOCK_APP_CLASS_NAME"
    private val CALENDAR_APP_PACKAGE = "CALENDAR_APP_PACKAGE"
    private val CALENDAR_APP_USER = "CALENDAR_APP_USER"
    private val CALENDAR_APP_CLASS_NAME = "CALENDAR_APP_CLASS_NAME"
    private val WEATHER_APP_PACKAGE = "WEATHER_APP_PACKAGE"
    private val WEATHER_APP_USER = "WEATHER_APP_USER"
    private val WEATHER_APP_CLASS_NAME = "WEATHER_APP_CLASS_NAME"

    private val IS_SHORTCUT_1 = "IS_SHORTCUT_1"
    private val SHORTCUT_ID_1 = "SHORTCUT_ID_1"
    private val IS_SHORTCUT_2 = "IS_SHORTCUT_2"
    private val SHORTCUT_ID_2 = "SHORTCUT_ID_2"
    private val IS_SHORTCUT_3 = "IS_SHORTCUT_3"
    private val SHORTCUT_ID_3 = "SHORTCUT_ID_3"
    private val IS_SHORTCUT_4 = "IS_SHORTCUT_4"
    private val SHORTCUT_ID_4 = "SHORTCUT_ID_4"
    private val IS_SHORTCUT_5 = "IS_SHORTCUT_5"
    private val SHORTCUT_ID_5 = "SHORTCUT_ID_5"
    private val IS_SHORTCUT_6 = "IS_SHORTCUT_6"
    private val SHORTCUT_ID_6 = "SHORTCUT_ID_6"
    private val IS_SHORTCUT_7 = "IS_SHORTCUT_7"
    private val SHORTCUT_ID_7 = "SHORTCUT_ID_7"
    private val IS_SHORTCUT_8 = "IS_SHORTCUT_8"
    private val SHORTCUT_ID_8 = "SHORTCUT_ID_8"

    private val SHORTCUT_ID_SWIPE_LEFT = "SHORTCUT_ID_SWIPE_LEFT"
    private val IS_SHORTCUT_SWIPE_LEFT = "IS_SHORTCUT_SWIPE_LEFT"
    private val SHORTCUT_ID_SWIPE_RIGHT = "SHORTCUT_ID_SWIPE_RIGHT"
    private val IS_SHORTCUT_SWIPE_RIGHT = "IS_SHORTCUT_SWIPE_RIGHT"

    private val TODO_TASKS = "TODO_TASKS"
    private val WEATHER_CACHE_JSON = "WEATHER_CACHE_JSON"
    private val WEATHER_CACHE_TIMESTAMP = "WEATHER_CACHE_TIMESTAMP"
    private val WEATHER_CACHE_LAT = "WEATHER_CACHE_LAT"
    private val WEATHER_CACHE_LON = "WEATHER_CACHE_LON"

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0)

    var firstOpen: Boolean
        get() = prefs.getBoolean(FIRST_OPEN, true)
        set(value) = prefs.edit { putBoolean(FIRST_OPEN, value) }

    var firstHide: Boolean
        get() = prefs.getBoolean(FIRST_HIDE, true)
        set(value) = prefs.edit { putBoolean(FIRST_HIDE, value) }

    var lockModeOn: Boolean
        get() = prefs.getBoolean(LOCK_MODE, false)
        set(value) = prefs.edit { putBoolean(LOCK_MODE, value) }

    var homeAppsNum: Int
        get() = prefs.getInt(HOME_APPS_NUM, 4)
        set(value) = prefs.edit { putInt(HOME_APPS_NUM, value) }

    var homeAlignment: Int
        get() = prefs.getInt(HOME_ALIGNMENT, Gravity.START)
        set(value) = prefs.edit { putInt(HOME_ALIGNMENT, value) }

    var homeBottomAlignment: Boolean
        get() = prefs.getBoolean(HOME_BOTTOM_ALIGNMENT, false)
        set(value) = prefs.edit { putBoolean(HOME_BOTTOM_ALIGNMENT, value) }

    var appLabelAlignment: Int
        get() = prefs.getInt(APP_LABEL_ALIGNMENT, Gravity.START)
        set(value) = prefs.edit { putInt(APP_LABEL_ALIGNMENT, value) }

    var showStatusBar: Boolean
        get() = prefs.getBoolean(STATUS_BAR, false)
        set(value) = prefs.edit { putBoolean(STATUS_BAR, value) }

    var dateTimeVisibility: Int
        get() = prefs.getInt(DATE_TIME_VISIBILITY, Constants.DateTime.ON)
        set(value) = prefs.edit { putInt(DATE_TIME_VISIBILITY, value) }

    var appTheme: Int
        get() = prefs.getInt(APP_THEME, AppCompatDelegate.MODE_NIGHT_YES)
        set(value) = prefs.edit { putInt(APP_THEME, value) }

    var textSizeScale: Float
        get() = prefs.getFloat(TEXT_SIZE_SCALE, 1.0f)
        set(value) = prefs.edit { putFloat(TEXT_SIZE_SCALE, value) }

    var hideSetDefaultLauncher: Boolean
        get() = prefs.getBoolean(HIDE_SET_DEFAULT_LAUNCHER, false)
        set(value) = prefs.edit { putBoolean(HIDE_SET_DEFAULT_LAUNCHER, value) }

    var launcherRestartTimestamp: Long
        get() = prefs.getLong(LAUNCHER_RESTART_TIMESTAMP, 0L)
        set(value) = prefs.edit { putLong(LAUNCHER_RESTART_TIMESTAMP, value) }

    var homeButtonShowRecents: Boolean
        get() = prefs.getBoolean(HOME_BUTTON_SHOW_RECENTS, false)
        set(value) = prefs.edit { putBoolean(HOME_BUTTON_SHOW_RECENTS, value) }

    var hiddenApps: MutableSet<String>
        get() = prefs.getStringSet(HIDDEN_APPS, mutableSetOf()) as MutableSet<String>
        set(value) = prefs.edit { putStringSet(HIDDEN_APPS, value) }

    var hiddenAppsUpdated: Boolean
        get() = prefs.getBoolean(HIDDEN_APPS_UPDATED, false)
        set(value) = prefs.edit { putBoolean(HIDDEN_APPS_UPDATED, value) }

    var swipeDownAction: Int
        get() = prefs.getInt(SWIPE_DOWN_ACTION, Constants.SwipeDownAction.NOTIFICATIONS)
        set(value) = prefs.edit { putInt(SWIPE_DOWN_ACTION, value) }

    var appName1: String
        get() = prefs.getString(APP_NAME_1, "").toString()
        set(value) = prefs.edit { putString(APP_NAME_1, value) }

    var appName2: String
        get() = prefs.getString(APP_NAME_2, "").toString()
        set(value) = prefs.edit { putString(APP_NAME_2, value) }

    var appName3: String
        get() = prefs.getString(APP_NAME_3, "").toString()
        set(value) = prefs.edit { putString(APP_NAME_3, value) }

    var appName4: String
        get() = prefs.getString(APP_NAME_4, "").toString()
        set(value) = prefs.edit { putString(APP_NAME_4, value) }

    var appName5: String
        get() = prefs.getString(APP_NAME_5, "").toString()
        set(value) = prefs.edit { putString(APP_NAME_5, value) }

    var appName6: String
        get() = prefs.getString(APP_NAME_6, "").toString()
        set(value) = prefs.edit { putString(APP_NAME_6, value) }

    var appName7: String
        get() = prefs.getString(APP_NAME_7, "").toString()
        set(value) = prefs.edit { putString(APP_NAME_7, value) }

    var appName8: String
        get() = prefs.getString(APP_NAME_8, "").toString()
        set(value) = prefs.edit { putString(APP_NAME_8, value) }

    var appPackage1: String
        get() = prefs.getString(APP_PACKAGE_1, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_1, value) }

    var appPackage2: String
        get() = prefs.getString(APP_PACKAGE_2, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_2, value) }

    var appPackage3: String
        get() = prefs.getString(APP_PACKAGE_3, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_3, value) }

    var appPackage4: String
        get() = prefs.getString(APP_PACKAGE_4, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_4, value) }

    var appPackage5: String
        get() = prefs.getString(APP_PACKAGE_5, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_5, value) }

    var appPackage6: String
        get() = prefs.getString(APP_PACKAGE_6, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_6, value) }

    var appPackage7: String
        get() = prefs.getString(APP_PACKAGE_7, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_7, value) }

    var appPackage8: String
        get() = prefs.getString(APP_PACKAGE_8, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_8, value) }

    var appActivityClassName1: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_1, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_1, value) }

    var appActivityClassName2: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_2, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_2, value) }

    var appActivityClassName3: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_3, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_3, value) }

    var appActivityClassName4: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_4, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_4, value) }

    var appActivityClassName5: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_5, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_5, value) }

    var appActivityClassName6: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_6, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_6, value) }

    var appActivityClassName7: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_7, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_7, value) }

    var appActivityClassName8: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_8, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_8, value) }

    var appUser1: String
        get() = prefs.getString(APP_USER_1, "").toString()
        set(value) = prefs.edit { putString(APP_USER_1, value) }

    var appUser2: String
        get() = prefs.getString(APP_USER_2, "").toString()
        set(value) = prefs.edit { putString(APP_USER_2, value) }

    var appUser3: String
        get() = prefs.getString(APP_USER_3, "").toString()
        set(value) = prefs.edit { putString(APP_USER_3, value) }

    var appUser4: String
        get() = prefs.getString(APP_USER_4, "").toString()
        set(value) = prefs.edit { putString(APP_USER_4, value) }

    var appUser5: String
        get() = prefs.getString(APP_USER_5, "").toString()
        set(value) = prefs.edit { putString(APP_USER_5, value) }

    var appUser6: String
        get() = prefs.getString(APP_USER_6, "").toString()
        set(value) = prefs.edit { putString(APP_USER_6, value) }

    var appUser7: String
        get() = prefs.getString(APP_USER_7, "").toString()
        set(value) = prefs.edit { putString(APP_USER_7, value) }

    var appUser8: String
        get() = prefs.getString(APP_USER_8, "").toString()
        set(value) = prefs.edit { putString(APP_USER_8, value) }

    var appNameSwipeLeft: String
        get() = prefs.getString(APP_NAME_SWIPE_LEFT, "").toString()
        set(value) = prefs.edit { putString(APP_NAME_SWIPE_LEFT, value) }

    var appNameSwipeRight: String
        get() = prefs.getString(APP_NAME_SWIPE_RIGHT, "").toString()
        set(value) = prefs.edit { putString(APP_NAME_SWIPE_RIGHT, value) }

    var appPackageSwipeLeft: String
        get() = prefs.getString(APP_PACKAGE_SWIPE_LEFT, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_SWIPE_LEFT, value) }

    var appActivityClassNameSwipeLeft: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT, value) }

    var appPackageSwipeRight: String
        get() = prefs.getString(APP_PACKAGE_SWIPE_RIGHT, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_SWIPE_RIGHT, value) }

    var appActivityClassNameSwipeRight: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT, value) }

    var appUserSwipeLeft: String
        get() = prefs.getString(APP_USER_SWIPE_LEFT, "").toString()
        set(value) = prefs.edit { putString(APP_USER_SWIPE_LEFT, value) }

    var appUserSwipeRight: String
        get() = prefs.getString(APP_USER_SWIPE_RIGHT, "").toString()
        set(value) = prefs.edit { putString(APP_USER_SWIPE_RIGHT, value) }

    var clockAppPackage: String
        get() = prefs.getString(CLOCK_APP_PACKAGE, "").toString()
        set(value) = prefs.edit { putString(CLOCK_APP_PACKAGE, value) }

    var clockAppUser: String
        get() = prefs.getString(CLOCK_APP_USER, "").toString()
        set(value) = prefs.edit { putString(CLOCK_APP_USER, value) }

    var clockAppClassName: String?
        get() = prefs.getString(CLOCK_APP_CLASS_NAME, "").toString()
        set(value) = prefs.edit { putString(CLOCK_APP_CLASS_NAME, value) }

    var calendarAppPackage: String
        get() = prefs.getString(CALENDAR_APP_PACKAGE, "").toString()
        set(value) = prefs.edit { putString(CALENDAR_APP_PACKAGE, value) }

    var calendarAppUser: String
        get() = prefs.getString(CALENDAR_APP_USER, "").toString()
        set(value) = prefs.edit { putString(CALENDAR_APP_USER, value) }

    var calendarAppClassName: String?
        get() = prefs.getString(CALENDAR_APP_CLASS_NAME, "").toString()
        set(value) = prefs.edit { putString(CALENDAR_APP_CLASS_NAME, value) }

    var weatherAppPackage: String
        get() = prefs.getString(WEATHER_APP_PACKAGE, "").toString()
        set(value) = prefs.edit { putString(WEATHER_APP_PACKAGE, value) }

    var weatherAppUser: String
        get() = prefs.getString(WEATHER_APP_USER, "").toString()
        set(value) = prefs.edit { putString(WEATHER_APP_USER, value) }

    var weatherAppClassName: String?
        get() = prefs.getString(WEATHER_APP_CLASS_NAME, "").toString()
        set(value) = prefs.edit { putString(WEATHER_APP_CLASS_NAME, value) }

    var isShortcut1: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_1, false)
        set(value) = prefs.edit { putBoolean(IS_SHORTCUT_1, value) }

    var shortcutId1: String
        get() = prefs.getString(SHORTCUT_ID_1, "").toString()
        set(value) = prefs.edit { putString(SHORTCUT_ID_1, value) }

    var isShortcut2: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_2, false)
        set(value) = prefs.edit { putBoolean(IS_SHORTCUT_2, value) }

    var shortcutId2: String
        get() = prefs.getString(SHORTCUT_ID_2, "").toString()
        set(value) = prefs.edit { putString(SHORTCUT_ID_2, value) }

    var isShortcut3: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_3, false)
        set(value) = prefs.edit { putBoolean(IS_SHORTCUT_3, value) }

    var shortcutId3: String
        get() = prefs.getString(SHORTCUT_ID_3, "").toString()
        set(value) = prefs.edit { putString(SHORTCUT_ID_3, value) }

    var isShortcut4: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_4, false)
        set(value) = prefs.edit { putBoolean(IS_SHORTCUT_4, value) }

    var shortcutId4: String
        get() = prefs.getString(SHORTCUT_ID_4, "").toString()
        set(value) = prefs.edit { putString(SHORTCUT_ID_4, value) }

    var isShortcut5: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_5, false)
        set(value) = prefs.edit { putBoolean(IS_SHORTCUT_5, value) }

    var shortcutId5: String
        get() = prefs.getString(SHORTCUT_ID_5, "").toString()
        set(value) = prefs.edit { putString(SHORTCUT_ID_5, value) }

    var isShortcut6: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_6, false)
        set(value) = prefs.edit { putBoolean(IS_SHORTCUT_6, value) }

    var shortcutId6: String
        get() = prefs.getString(SHORTCUT_ID_6, "").toString()
        set(value) = prefs.edit { putString(SHORTCUT_ID_6, value) }

    var isShortcut7: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_7, false)
        set(value) = prefs.edit { putBoolean(IS_SHORTCUT_7, value) }

    var shortcutId7: String
        get() = prefs.getString(SHORTCUT_ID_7, "").toString()
        set(value) = prefs.edit { putString(SHORTCUT_ID_7, value) }

    var isShortcut8: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_8, false)
        set(value) = prefs.edit { putBoolean(IS_SHORTCUT_8, value) }

    var shortcutId8: String
        get() = prefs.getString(SHORTCUT_ID_8, "").toString()
        set(value) = prefs.edit { putString(SHORTCUT_ID_8, value) }

    var shortcutIdSwipeLeft: String
        get() = prefs.getString(SHORTCUT_ID_SWIPE_LEFT, "").toString()
        set(value) = prefs.edit { putString(SHORTCUT_ID_SWIPE_LEFT, value) }

    var isShortcutSwipeLeft: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_SWIPE_LEFT, false)
        set(value) = prefs.edit { putBoolean(IS_SHORTCUT_SWIPE_LEFT, value) }

    var shortcutIdSwipeRight: String
        get() = prefs.getString(SHORTCUT_ID_SWIPE_RIGHT, "").toString()
        set(value) = prefs.edit { putString(SHORTCUT_ID_SWIPE_RIGHT, value) }

    var isShortcutSwipeRight: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_SWIPE_RIGHT, false)
        set(value) = prefs.edit { putBoolean(IS_SHORTCUT_SWIPE_RIGHT, value) }

    var todoTasksJson: String
        get() = prefs.getString(TODO_TASKS, "[]").toString()
        set(value) = prefs.edit { putString(TODO_TASKS, value) }

    var weatherCacheJson: String
        get() = prefs.getString(WEATHER_CACHE_JSON, "").toString()
        set(value) = prefs.edit { putString(WEATHER_CACHE_JSON, value) }

    var weatherCacheTimestamp: Long
        get() = prefs.getLong(WEATHER_CACHE_TIMESTAMP, 0L)
        set(value) = prefs.edit { putLong(WEATHER_CACHE_TIMESTAMP, value) }

    var weatherCacheLat: Float
        get() = prefs.getFloat(WEATHER_CACHE_LAT, Float.NaN)
        set(value) = prefs.edit { putFloat(WEATHER_CACHE_LAT, value) }

    var weatherCacheLon: Float
        get() = prefs.getFloat(WEATHER_CACHE_LON, Float.NaN)
        set(value) = prefs.edit { putFloat(WEATHER_CACHE_LON, value) }

    fun getAppName(location: Int): String = when (location) {
        1 -> appName1
        2 -> appName2
        3 -> appName3
        4 -> appName4
        5 -> appName5
        6 -> appName6
        7 -> appName7
        8 -> appName8
        else -> ""
    }

    fun getAppPackage(location: Int): String = when (location) {
        1 -> appPackage1
        2 -> appPackage2
        3 -> appPackage3
        4 -> appPackage4
        5 -> appPackage5
        6 -> appPackage6
        7 -> appPackage7
        8 -> appPackage8
        else -> ""
    }

    fun getAppActivityClassName(location: Int): String = when (location) {
        1 -> appActivityClassName1.orEmpty()
        2 -> appActivityClassName2.orEmpty()
        3 -> appActivityClassName3.orEmpty()
        4 -> appActivityClassName4.orEmpty()
        5 -> appActivityClassName5.orEmpty()
        6 -> appActivityClassName6.orEmpty()
        7 -> appActivityClassName7.orEmpty()
        8 -> appActivityClassName8.orEmpty()
        else -> ""
    }

    fun getAppUser(location: Int): String = when (location) {
        1 -> appUser1
        2 -> appUser2
        3 -> appUser3
        4 -> appUser4
        5 -> appUser5
        6 -> appUser6
        7 -> appUser7
        8 -> appUser8
        else -> ""
    }

    fun getShortcutId(location: Int): String = when (location) {
        1 -> shortcutId1
        2 -> shortcutId2
        3 -> shortcutId3
        4 -> shortcutId4
        5 -> shortcutId5
        6 -> shortcutId6
        7 -> shortcutId7
        8 -> shortcutId8
        else -> ""
    }

    fun getIsShortcut(location: Int): Boolean = when (location) {
        1 -> isShortcut1
        2 -> isShortcut2
        3 -> isShortcut3
        4 -> isShortcut4
        5 -> isShortcut5
        6 -> isShortcut6
        7 -> isShortcut7
        8 -> isShortcut8
        else -> false
    }

    fun setAppActivityClassName(location: Int, activityClassName: String) {
        when (location) {
            1 -> appActivityClassName1 = activityClassName
            2 -> appActivityClassName2 = activityClassName
            3 -> appActivityClassName3 = activityClassName
            4 -> appActivityClassName4 = activityClassName
            5 -> appActivityClassName5 = activityClassName
            6 -> appActivityClassName6 = activityClassName
            7 -> appActivityClassName7 = activityClassName
            8 -> appActivityClassName8 = activityClassName
        }
    }

    fun updateAppActivityClassName(packageName: String, activityClassName: String) {
        for (i in 1..8) {
            if (getAppPackage(i) == packageName) setAppActivityClassName(i, activityClassName)
        }
        if (clockAppPackage == packageName) clockAppClassName = activityClassName
        if (calendarAppPackage == packageName) calendarAppClassName = activityClassName
        if (weatherAppPackage == packageName) weatherAppClassName = activityClassName
        if (appPackageSwipeLeft == packageName) appActivityClassNameSwipeLeft = activityClassName
        if (appPackageSwipeRight == packageName) appActivityClassNameSwipeRight = activityClassName
    }

    fun getAppRenameLabel(appPackage: String): String = prefs.getString(appPackage, "").toString()

    fun setAppRenameLabel(appPackage: String, renameLabel: String) =
        prefs.edit { putString(appPackage, renameLabel) }
}
