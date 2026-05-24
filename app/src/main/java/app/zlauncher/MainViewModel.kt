package app.zlauncher

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.zlauncher.data.AppModel
import app.zlauncher.data.Constants
import app.zlauncher.data.Prefs
import app.zlauncher.helper.SingleLiveEvent
import app.zlauncher.helper.getAppsList
import app.zlauncher.helper.getPrivateSpaceApps
import app.zlauncher.helper.getPrivateSpaceUserHandle
import app.zlauncher.helper.isPackageInstalled
import app.zlauncher.helper.isPrivateSpaceLocked
import app.zlauncher.helper.isZLauncherDefault
import app.zlauncher.helper.showToast
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val prefs = Prefs(appContext)

    val firstOpen = MutableLiveData<Boolean>()
    val refreshHome = MutableLiveData<Boolean>()
    val toggleDateTime = MutableLiveData<Unit>()
    val updateSwipeApps = MutableLiveData<Any>()
    val appList = MutableLiveData<List<AppModel>?>()
    val hiddenApps = MutableLiveData<List<AppModel>?>()
    val isZLauncherDefault = MutableLiveData<Boolean>()
    val launcherResetFailed = MutableLiveData<Boolean>()
    val homeAppAlignment = MutableLiveData<Int>()

    val privateSpaceApps = MutableLiveData<List<AppModel>?>()
    val privateSpaceLocked = MutableLiveData<Boolean>()
    val privateSpaceAvailable = MutableLiveData<Boolean>()

    /** Suppress backToHomeScreen during Private Space lock/unlock auth. */
    var isPrivateSpaceToggling = false

    val resetLauncherLiveData = SingleLiveEvent<Unit?>()
    val showRecentApps = SingleLiveEvent<Unit?>()

    /** Activity observes this to switch the pager to the drawer in "pick app" mode. */
    val showAppDrawerForFlag = SingleLiveEvent<Int>()

    /** Activity observes this to show/hide the settings overlay. */
    val openSettingsOverlay = SingleLiveEvent<Unit?>()
    val closeSettingsOverlay = SingleLiveEvent<Unit?>()

    /** Activity observes this to switch to the Home page (e.g. after a drawer pick). */
    val snapToHome = SingleLiveEvent<Unit?>()

    fun selectedApp(appModel: AppModel, flag: Int) {
        if (appModel is AppModel.PrivateSpaceHeader) return
        when (flag) {
            Constants.FLAG_LAUNCH_APP -> when (appModel) {
                is AppModel.PinnedShortcut -> launchShortcut(appModel)
                is AppModel.App -> launchApp(appModel.appPackage, appModel.activityClassName, appModel.user)
                else -> {}
            }

            Constants.FLAG_HIDDEN_APPS -> if (appModel is AppModel.App) {
                launchApp(appModel.appPackage, appModel.activityClassName, appModel.user)
            }

            Constants.FLAG_SET_HOME_APP_1 -> saveHomeApp(appModel, 1)
            Constants.FLAG_SET_HOME_APP_2 -> saveHomeApp(appModel, 2)
            Constants.FLAG_SET_HOME_APP_3 -> saveHomeApp(appModel, 3)
            Constants.FLAG_SET_HOME_APP_4 -> saveHomeApp(appModel, 4)
            Constants.FLAG_SET_HOME_APP_5 -> saveHomeApp(appModel, 5)
            Constants.FLAG_SET_HOME_APP_6 -> saveHomeApp(appModel, 6)
            Constants.FLAG_SET_HOME_APP_7 -> saveHomeApp(appModel, 7)
            Constants.FLAG_SET_HOME_APP_8 -> saveHomeApp(appModel, 8)
            Constants.FLAG_SET_HOME_APP_9 -> saveHomeApp(appModel, 9)
            Constants.FLAG_SET_HOME_APP_10 -> saveHomeApp(appModel, 10)
            Constants.FLAG_SET_HOME_APP_11 -> saveHomeApp(appModel, 11)
            Constants.FLAG_SET_HOME_APP_12 -> saveHomeApp(appModel, 12)

            Constants.FLAG_SET_SWIPE_LEFT_APP -> saveSwipeApp(appModel, isLeft = true)
            Constants.FLAG_SET_SWIPE_RIGHT_APP -> saveSwipeApp(appModel, isLeft = false)
            Constants.FLAG_SET_CLOCK_APP -> saveClockApp(appModel)
            Constants.FLAG_SET_CALENDAR_APP -> saveCalendarApp(appModel)
            Constants.FLAG_SET_WEATHER_APP -> saveWeatherApp(appModel)
        }
    }

    private fun launchShortcut(appModel: AppModel.PinnedShortcut) {
        val launcher = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val query = LauncherApps.ShortcutQuery().apply {
            setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
        }
        launcher.getShortcuts(query, appModel.user)?.find { it.id == appModel.shortcutId }
            ?.let { shortcut -> launcher.startShortcut(shortcut, null, null) }
    }

    private fun saveHomeApp(appModel: AppModel, position: Int) {
        if (appModel is AppModel.PrivateSpaceHeader) return
        val label = appModel.appLabel
        val packageName = appModel.appPackage
        val user = appModel.user.toString()
        val activityClass = (appModel as? AppModel.App)?.activityClassName
        val isShortcut = appModel is AppModel.PinnedShortcut
        val shortcutId = (appModel as? AppModel.PinnedShortcut)?.shortcutId.orEmpty()
        when (position) {
            1 -> { prefs.appName1 = label; prefs.appPackage1 = packageName; prefs.appUser1 = user; prefs.appActivityClassName1 = activityClass; prefs.isShortcut1 = isShortcut; prefs.shortcutId1 = shortcutId }
            2 -> { prefs.appName2 = label; prefs.appPackage2 = packageName; prefs.appUser2 = user; prefs.appActivityClassName2 = activityClass; prefs.isShortcut2 = isShortcut; prefs.shortcutId2 = shortcutId }
            3 -> { prefs.appName3 = label; prefs.appPackage3 = packageName; prefs.appUser3 = user; prefs.appActivityClassName3 = activityClass; prefs.isShortcut3 = isShortcut; prefs.shortcutId3 = shortcutId }
            4 -> { prefs.appName4 = label; prefs.appPackage4 = packageName; prefs.appUser4 = user; prefs.appActivityClassName4 = activityClass; prefs.isShortcut4 = isShortcut; prefs.shortcutId4 = shortcutId }
            5 -> { prefs.appName5 = label; prefs.appPackage5 = packageName; prefs.appUser5 = user; prefs.appActivityClassName5 = activityClass; prefs.isShortcut5 = isShortcut; prefs.shortcutId5 = shortcutId }
            6 -> { prefs.appName6 = label; prefs.appPackage6 = packageName; prefs.appUser6 = user; prefs.appActivityClassName6 = activityClass; prefs.isShortcut6 = isShortcut; prefs.shortcutId6 = shortcutId }
            7 -> { prefs.appName7 = label; prefs.appPackage7 = packageName; prefs.appUser7 = user; prefs.appActivityClassName7 = activityClass; prefs.isShortcut7 = isShortcut; prefs.shortcutId7 = shortcutId }
            8 -> { prefs.appName8 = label; prefs.appPackage8 = packageName; prefs.appUser8 = user; prefs.appActivityClassName8 = activityClass; prefs.isShortcut8 = isShortcut; prefs.shortcutId8 = shortcutId }
            9 -> { prefs.appName9 = label; prefs.appPackage9 = packageName; prefs.appUser9 = user; prefs.appActivityClassName9 = activityClass; prefs.isShortcut9 = isShortcut; prefs.shortcutId9 = shortcutId }
            10 -> { prefs.appName10 = label; prefs.appPackage10 = packageName; prefs.appUser10 = user; prefs.appActivityClassName10 = activityClass; prefs.isShortcut10 = isShortcut; prefs.shortcutId10 = shortcutId }
            11 -> { prefs.appName11 = label; prefs.appPackage11 = packageName; prefs.appUser11 = user; prefs.appActivityClassName11 = activityClass; prefs.isShortcut11 = isShortcut; prefs.shortcutId11 = shortcutId }
            12 -> { prefs.appName12 = label; prefs.appPackage12 = packageName; prefs.appUser12 = user; prefs.appActivityClassName12 = activityClass; prefs.isShortcut12 = isShortcut; prefs.shortcutId12 = shortcutId }
        }
        refreshHome(false)
    }

    private fun saveSwipeApp(appModel: AppModel, isLeft: Boolean) {
        if (appModel is AppModel.PrivateSpaceHeader) return
        val label = appModel.appLabel
        val packageName = appModel.appPackage
        val user = appModel.user.toString()
        val activityClass = (appModel as? AppModel.App)?.activityClassName
        val isShortcut = appModel is AppModel.PinnedShortcut
        val shortcutId = (appModel as? AppModel.PinnedShortcut)?.shortcutId.orEmpty()
        if (isLeft) {
            prefs.appNameSwipeLeft = label
            prefs.appPackageSwipeLeft = packageName
            prefs.appUserSwipeLeft = user
            prefs.appActivityClassNameSwipeLeft = activityClass
            prefs.isShortcutSwipeLeft = isShortcut
            prefs.shortcutIdSwipeLeft = shortcutId
        } else {
            prefs.appNameSwipeRight = label
            prefs.appPackageSwipeRight = packageName
            prefs.appUserSwipeRight = user
            prefs.appActivityClassNameSwipeRight = activityClass
            prefs.isShortcutSwipeRight = isShortcut
            prefs.shortcutIdSwipeRight = shortcutId
        }
        updateSwipeApps()
    }

    private fun saveClockApp(appModel: AppModel) {
        if (appModel is AppModel.App) {
            prefs.clockAppPackage = appModel.appPackage
            prefs.clockAppUser = appModel.user.toString()
            prefs.clockAppClassName = appModel.activityClassName
        }
    }

    private fun saveCalendarApp(appModel: AppModel) {
        if (appModel is AppModel.App) {
            prefs.calendarAppPackage = appModel.appPackage
            prefs.calendarAppUser = appModel.user.toString()
            prefs.calendarAppClassName = appModel.activityClassName
        }
    }

    private fun saveWeatherApp(appModel: AppModel) {
        if (appModel is AppModel.App) {
            prefs.weatherAppPackage = appModel.appPackage
            prefs.weatherAppUser = appModel.user.toString()
            prefs.weatherAppClassName = appModel.activityClassName
        }
    }

    fun firstOpen(value: Boolean) {
        firstOpen.postValue(value)
    }

    fun refreshHome(appCountUpdated: Boolean) {
        refreshHome.value = appCountUpdated
    }

    fun toggleDateTime() {
        toggleDateTime.postValue(Unit)
    }

    private fun updateSwipeApps() {
        updateSwipeApps.postValue(Unit)
    }

    private fun launchApp(packageName: String, activityClassName: String?, userHandle: UserHandle) {
        val launcher = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val activityInfo = launcher.getActivityList(packageName, userHandle)

        val isActivityValid = !activityClassName.isNullOrBlank() &&
                activityInfo.any { it.componentName.className == activityClassName }

        val component = if (isActivityValid) {
            ComponentName(packageName, activityClassName!!)
        } else {
            when (activityInfo.size) {
                0 -> {
                    appContext.showToast(appContext.getString(R.string.app_not_found))
                    return
                }
                1 -> ComponentName(packageName, activityInfo[0].name)
                else -> ComponentName(packageName, activityInfo[activityInfo.size - 1].name)
            }.also { prefs.updateAppActivityClassName(packageName, it.className) }
        }

        try {
            launcher.startMainActivity(component, userHandle, null, null)
        } catch (e: SecurityException) {
            try {
                launcher.startMainActivity(component, android.os.Process.myUserHandle(), null, null)
            } catch (_: Exception) {
                appContext.showToast(appContext.getString(R.string.unable_to_open_app))
            }
        } catch (_: Exception) {
            appContext.showToast(appContext.getString(R.string.unable_to_open_app))
        }
    }

    fun getAppList(includeHiddenApps: Boolean = false) {
        viewModelScope.launch {
            appList.value = getAppsList(appContext, prefs, includeRegularApps = true, includeHiddenApps)
        }
        getPrivateSpaceAppList()
    }

    fun getHiddenApps() {
        viewModelScope.launch {
            hiddenApps.value =
                getAppsList(appContext, prefs, includeRegularApps = false, includeHiddenApps = true)
        }
    }

    fun isZLauncherDefault() {
        isZLauncherDefault.value = isZLauncherDefault(appContext)
    }

    fun updateHomeAlignment(gravity: Int) {
        prefs.homeAlignment = gravity
        homeAppAlignment.value = prefs.homeAlignment
    }

    fun getPrivateSpaceAppList() {
        viewModelScope.launch {
            val handle = getPrivateSpaceUserHandle(appContext)
            privateSpaceAvailable.value = handle != null
            if (handle != null) {
                privateSpaceLocked.value = isPrivateSpaceLocked(appContext, handle)
                privateSpaceApps.value = getPrivateSpaceApps(appContext, prefs)
            } else {
                privateSpaceLocked.value = true
                privateSpaceApps.value = emptyList()
            }
        }
    }

    fun openPrivateSpaceSettings() {
        try {
            val intent = Intent("android.settings.PRIVATE_SPACE_SETTINGS")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                appContext.startActivity(intent)
            } catch (_: Exception) {
                appContext.showToast(appContext.getString(R.string.unable_to_open_app))
            }
        }
    }

    fun togglePrivateSpaceLock() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        val handle = getPrivateSpaceUserHandle(appContext) ?: return
        try {
            isPrivateSpaceToggling = true
            val userManager = appContext.getSystemService(Context.USER_SERVICE) as UserManager
            val currentlyLocked = userManager.isQuietModeEnabled(handle)
            userManager.requestQuietModeEnabled(!currentlyLocked, handle)
        } catch (e: Exception) {
            isPrivateSpaceToggling = false
            e.printStackTrace()
        }
    }

    fun setDefaultClockApp() {
        viewModelScope.launch {
            try {
                Constants.CLOCK_APP_PACKAGES.firstOrNull { appContext.isPackageInstalled(it) }?.let { packageName ->
                    appContext.packageManager.getLaunchIntentForPackage(packageName)?.component?.className?.let {
                        prefs.clockAppPackage = packageName
                        prefs.clockAppClassName = it
                        prefs.clockAppUser = android.os.Process.myUserHandle().toString()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun openAppDrawerForFlag(flag: Int) {
        showAppDrawerForFlag.value = flag
    }

    fun openSettings() {
        openSettingsOverlay.call()
    }

    fun closeSettings() {
        closeSettingsOverlay.call()
    }

    fun snapToHome() {
        snapToHome.call()
    }
}
