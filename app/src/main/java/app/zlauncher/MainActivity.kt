package app.zlauncher

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import app.zlauncher.data.Constants
import app.zlauncher.data.Prefs
import app.zlauncher.databinding.ActivityMainBinding
import app.zlauncher.helper.getColorFromAttr
import app.zlauncher.helper.hasBeenHours
import app.zlauncher.helper.isDefaultLauncher
import app.zlauncher.helper.isEinkDisplay
import app.zlauncher.helper.isTablet
import app.zlauncher.helper.requestLauncherRole
import app.zlauncher.helper.resetLauncherViaFakeActivity
import app.zlauncher.ui.AppDrawerFragment
import app.zlauncher.ui.MainPagerAdapter
import app.zlauncher.ui.SettingsFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityMainBinding
    private var timerJob: Job? = null
    private var isResumed = false
    private var profileReceiver: BroadcastReceiver? = null
    private var packageReceiver: BroadcastReceiver? = null
    private var launcherAppsCallback: LauncherApps.Callback? = null
    private lateinit var backCallback: OnBackPressedCallback

    private val launcherSelectorLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) resetLauncherViaFakeActivity()
    }

    private val enableAdminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) prefs.lockModeOn = true
    }

    override fun attachBaseContext(context: Context) {
        val newConfig = Configuration(context.resources.configuration)
        newConfig.fontScale = Prefs(context).textSizeScale
        applyOverrideConfiguration(newConfig)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = Prefs(this)
        if (isEinkDisplay()) prefs.appTheme = AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(prefs.appTheme)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        binding.pager.adapter = MainPagerAdapter(this)
        binding.pager.setCurrentItem(MainPagerAdapter.PAGE_HOME, false)
        binding.pager.offscreenPageLimit = 1
        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position != MainPagerAdapter.PAGE_DRAWER) {
                    resetDrawerPickMode()
                } else {
                    viewModel.getAppList()
                }
            }
        })

        backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    binding.settingsOverlay.visibility == android.view.View.VISIBLE ->
                        closeSettingsOverlay()
                    binding.pager.currentItem != MainPagerAdapter.PAGE_HOME ->
                        binding.pager.setCurrentItem(MainPagerAdapter.PAGE_HOME, true)
                    else -> {
                        // Behave like a launcher: no-op.
                    }
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, backCallback)

        if (prefs.firstOpen) {
            viewModel.firstOpen(true)
            prefs.firstOpen = false
            viewModel.setDefaultClockApp()
            viewModel.setDefaultWeatherApp()
            viewModel.resetLauncherLiveData.call()
        }

        initObservers(viewModel)
        viewModel.getAppList()
        registerLauncherAppsCallback()
        setupOrientation()

        window.addFlags(FLAG_LAYOUT_NO_LIMITS)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            profileReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    viewModel.isPrivateSpaceToggling = false
                    viewModel.getPrivateSpaceAppList()
                }
            }
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PROFILE_AVAILABLE)
                addAction(Intent.ACTION_PROFILE_UNAVAILABLE)
            }
            registerReceiver(profileReceiver, filter)
        }
    }

    override fun onStart() {
        super.onStart()
        restartLauncherOrCheckTheme()
    }

    override fun onResume() {
        super.onResume()
        isResumed = true
        viewModel.isPrivateSpaceToggling = false
        registerPackageReceiver()
        viewModel.getAppList()
    }

    override fun onPause() {
        unregisterPackageReceiver()
        super.onPause()
    }

    override fun onStop() {
        isResumed = false
        backToHomeScreen()
        super.onStop()
    }

    override fun onUserLeaveHint() {
        backToHomeScreen()
        super.onUserLeaveHint()
    }

    override fun onNewIntent(intent: Intent?) {
        val alreadyHome = binding.pager.currentItem == MainPagerAdapter.PAGE_HOME &&
                binding.settingsOverlay.visibility != android.view.View.VISIBLE
        backToHomeScreen()
        if (alreadyHome && isResumed && prefs.homeButtonShowRecents) {
            viewModel.showRecentApps.call()
        }
        super.onNewIntent(intent)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        AppCompatDelegate.setDefaultNightMode(prefs.appTheme)
    }

    private fun initObservers(viewModel: MainViewModel) {
        viewModel.launcherResetFailed.observe(this) { resetFailed ->
            if (resetFailed) {
                startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
            }
        }
        viewModel.resetLauncherLiveData.observe(this) {
            if (isDefaultLauncher() || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                resetLauncherViaFakeActivity()
            } else {
                requestLauncherRole(launcherSelectorLauncher)
            }
        }
        viewModel.openSettingsOverlay.observe(this) { openSettingsOverlay() }
        viewModel.closeSettingsOverlay.observe(this) { closeSettingsOverlay() }
        viewModel.showAppDrawerForFlag.observe(this) { flag ->
            flag?.let { openDrawerWithFlag(it) }
        }
        viewModel.snapToHome.observe(this) {
            if (binding.settingsOverlay.visibility == android.view.View.VISIBLE) closeSettingsOverlay()
            binding.pager.setCurrentItem(MainPagerAdapter.PAGE_HOME, true)
        }
    }

    fun launchAdminEnable(intent: Intent) {
        enableAdminLauncher.launch(intent)
    }

    fun openWeatherFromExternal(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
        }
    }

    private fun openSettingsOverlay() {
        if (binding.settingsOverlay.visibility == android.view.View.VISIBLE) return
        val fragment = SettingsFragment()
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_enter, R.anim.fade_exit, R.anim.fade_enter, R.anim.fade_exit)
            .replace(R.id.settingsOverlay, fragment, SETTINGS_TAG)
            .commit()
        binding.settingsOverlay.visibility = android.view.View.VISIBLE
    }

    private fun closeSettingsOverlay() {
        val existing = supportFragmentManager.findFragmentByTag(SETTINGS_TAG) ?: return
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_enter, R.anim.fade_exit, R.anim.fade_enter, R.anim.fade_exit)
            .remove(existing)
            .commit()
        binding.settingsOverlay.visibility = android.view.View.GONE
    }

    private fun openDrawerWithFlag(flag: Int) {
        val args = Bundle().apply {
            putInt(Constants.Key.FLAG, flag)
            putBoolean(Constants.Key.RENAME, false)
        }
        val drawer = (supportFragmentManager.findFragmentByTag("f${MainPagerAdapter.PAGE_DRAWER}") as? AppDrawerFragment)
        if (drawer != null) {
            drawer.applyFlag(flag, canRename = AppDrawerFragment.pendingRename)
        } else {
            // Defer until the fragment exists; ViewPager2 instantiates lazily.
            binding.pager.post {
                val refreshed = supportFragmentManager.findFragmentByTag("f${MainPagerAdapter.PAGE_DRAWER}")
                        as? AppDrawerFragment
                refreshed?.applyFlag(flag, canRename = AppDrawerFragment.pendingRename) ?: run {
                    AppDrawerFragment.pendingFlag = flag
                }
            }
        }
        if (closeSettingsOverlayIfOpen()) {
            binding.pager.post { binding.pager.setCurrentItem(MainPagerAdapter.PAGE_DRAWER, true) }
        } else {
            binding.pager.setCurrentItem(MainPagerAdapter.PAGE_DRAWER, true)
        }
    }

    private fun closeSettingsOverlayIfOpen(): Boolean {
        val wasOpen = binding.settingsOverlay.visibility == android.view.View.VISIBLE
        if (wasOpen) closeSettingsOverlay()
        return wasOpen
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun setupOrientation() {
        if (isTablet(this) || Build.VERSION.SDK_INT == Build.VERSION_CODES.O) return
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    private fun backToHomeScreen() {
        if (viewModel.isPrivateSpaceToggling) return
        if (binding.settingsOverlay.visibility == android.view.View.VISIBLE) closeSettingsOverlay()
        if (binding.pager.currentItem != MainPagerAdapter.PAGE_HOME) {
            resetDrawerPickMode()
            binding.pager.setCurrentItem(MainPagerAdapter.PAGE_HOME, false)
        }
    }

    private fun resetDrawerPickMode() {
        (supportFragmentManager.findFragmentByTag("f${MainPagerAdapter.PAGE_DRAWER}") as? AppDrawerFragment)
            ?.clearPickMode()
    }

    private fun restartLauncherOrCheckTheme(forceRestart: Boolean = false) {
        if (forceRestart || prefs.launcherRestartTimestamp.hasBeenHours(4)) {
            prefs.launcherRestartTimestamp = System.currentTimeMillis()
            cacheDir.deleteRecursively()
            recreate()
        } else {
            checkTheme()
        }
    }

    private fun checkTheme() {
        timerJob?.cancel()
        timerJob = lifecycleScope.launch {
            delay(200)
            if ((prefs.appTheme == AppCompatDelegate.MODE_NIGHT_YES && getColorFromAttr(R.attr.primaryColor) != getColor(R.color.white))
                || (prefs.appTheme == AppCompatDelegate.MODE_NIGHT_NO && getColorFromAttr(R.attr.primaryColor) != getColor(R.color.black))
            ) {
                restartLauncherOrCheckTheme(true)
            }
        }
    }

    private fun registerPackageReceiver() {
        if (packageReceiver != null) return
        packageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                viewModel.getAppList()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, filter)
    }

    private fun registerLauncherAppsCallback() {
        if (launcherAppsCallback != null) return
        val launcherApps = getSystemService(LauncherApps::class.java)
        launcherAppsCallback = object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String, user: UserHandle) {
                viewModel.getAppList()
            }

            override fun onPackageRemoved(packageName: String, user: UserHandle) {
                viewModel.getAppList()
            }

            override fun onPackageChanged(packageName: String, user: UserHandle) {
                viewModel.getAppList()
            }

            override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
                viewModel.getAppList()
            }

            override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
                viewModel.getAppList()
            }
        }
        launcherApps.registerCallback(launcherAppsCallback!!, Handler(mainLooper))
    }

    private fun unregisterLauncherAppsCallback() {
        launcherAppsCallback?.let { callback ->
            try {
                getSystemService(LauncherApps::class.java).unregisterCallback(callback)
            } catch (_: Exception) {
            }
        }
        launcherAppsCallback = null
    }

    private fun unregisterPackageReceiver() {
        packageReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {
            }
        }
        packageReceiver = null
    }

    override fun onDestroy() {
        unregisterLauncherAppsCallback()
        profileReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {
            }
        }
        super.onDestroy()
    }

    companion object {
        private const val SETTINGS_TAG = "settings"
    }
}
