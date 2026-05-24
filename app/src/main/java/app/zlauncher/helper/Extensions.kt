package app.zlauncher.helper

import android.app.Activity
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.os.UserHandle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import app.zlauncher.BuildConfig
import app.zlauncher.data.Constants

fun View.hideKeyboard() {
    this.clearFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun View.showKeyboard() {
    if (this.requestFocus()) {
        postDelayed({
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
fun Activity.requestLauncherRole(launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
    if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
        launcher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
    } else {
        resetDefaultLauncher()
    }
}

fun Context.resetDefaultLauncher() {
    try {
        val componentName = ComponentName(this, FakeHomeActivity::class.java)
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        val selector = Intent(Intent.ACTION_MAIN)
        selector.addCategory(Intent.CATEGORY_HOME)
        startActivity(selector)
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun Context.isDefaultLauncher(): Boolean = BuildConfig.APPLICATION_ID == getDefaultLauncherPackage(this)

fun Context.resetLauncherViaFakeActivity() {
    resetDefaultLauncher()
    if (getDefaultLauncherPackage(this).contains(".")) {
        startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
    }
}

fun Context.openSearch(query: String? = null) {
    val intent = Intent(Intent.ACTION_WEB_SEARCH)
    intent.putExtra(android.app.SearchManager.QUERY, query ?: "")
    startActivity(intent)
}

fun Context.isEinkDisplay(): Boolean {
    return try {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.refreshRate <= Constants.MIN_ANIM_REFRESH_RATE
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun Context.isPackageInstalled(
    packageName: String,
    userHandle: UserHandle = android.os.Process.myUserHandle(),
): Boolean {
    val launcher = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    return launcher.getActivityList(packageName, userHandle).isNotEmpty()
}

fun Long.hasBeenHours(hours: Int): Boolean =
    ((System.currentTimeMillis() - this) / Constants.ONE_HOUR_IN_MILLIS) >= hours

fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

fun getChangedAppTheme(context: Context, currentAppTheme: Int): Int = when (currentAppTheme) {
    AppCompatDelegate.MODE_NIGHT_YES -> AppCompatDelegate.MODE_NIGHT_NO
    AppCompatDelegate.MODE_NIGHT_NO -> AppCompatDelegate.MODE_NIGHT_YES
    else -> if (context.isDarkThemeOn()) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
}
