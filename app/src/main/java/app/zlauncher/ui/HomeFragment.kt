package app.zlauncher.ui

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import app.zlauncher.MainViewModel
import app.zlauncher.R
import app.zlauncher.data.AppModel
import app.zlauncher.data.Constants
import app.zlauncher.data.Prefs
import app.zlauncher.databinding.FragmentHomeBinding
import app.zlauncher.helper.expandNotificationDrawer
import app.zlauncher.helper.getUserHandleFromString
import app.zlauncher.helper.isPackageInstalled
import app.zlauncher.helper.openAlarmApp
import app.zlauncher.helper.openCalendar
import app.zlauncher.helper.openCameraApp
import app.zlauncher.helper.openDialerApp
import app.zlauncher.helper.openSearch
import app.zlauncher.helper.showToast
import app.zlauncher.listener.OnSwipeTouchListener
import app.zlauncher.listener.ViewSwipeTouchListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment(), View.OnClickListener, View.OnLongClickListener {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        viewModel = activity?.run { ViewModelProvider(this)[MainViewModel::class.java] }
            ?: throw IllegalStateException("Invalid Activity")

        deviceManager = requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        initObservers()
        setHomeAlignment(prefs.homeAlignment)
        initSwipeTouchListener()
        initClickListeners()
        renderCornerIcons()
        binding.root.viewTreeObserver.addOnGlobalLayoutListener { updateHomeAppsLayout() }
    }

    private fun homeAppSlots(): List<TextView> = listOf(
        binding.homeApp1, binding.homeApp2, binding.homeApp3, binding.homeApp4,
        binding.homeApp5, binding.homeApp6, binding.homeApp7, binding.homeApp8,
        binding.homeApp9, binding.homeApp10, binding.homeApp11, binding.homeApp12,
    )

    override fun onResume() {
        super.onResume()
        populateHomeScreen(false)
        viewModel.isZLauncherDefault()
        if (prefs.showStatusBar) showStatusBar() else hideStatusBar()
        renderCornerIcons()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.lock -> {}
            R.id.recents -> {}
            R.id.clock -> openClockApp()
            R.id.date -> openCalendarApp()
            R.id.setDefaultLauncher -> viewModel.resetLauncherLiveData.call()
            R.id.cornerLeftButton -> launchCornerLeft()
            R.id.cornerRightButton -> launchCornerRight()
            else -> {
                try {
                    val location = view.tag.toString().toInt()
                    homeAppClicked(location)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onLongClick(view: View): Boolean {
        when (view.id) {
            R.id.homeApp1 -> showAppListForHomeSlot(1)
            R.id.homeApp2 -> showAppListForHomeSlot(2)
            R.id.homeApp3 -> showAppListForHomeSlot(3)
            R.id.homeApp4 -> showAppListForHomeSlot(4)
            R.id.homeApp5 -> showAppListForHomeSlot(5)
            R.id.homeApp6 -> showAppListForHomeSlot(6)
            R.id.homeApp7 -> showAppListForHomeSlot(7)
            R.id.homeApp8 -> showAppListForHomeSlot(8)
            R.id.homeApp9 -> showAppListForHomeSlot(9)
            R.id.homeApp10 -> showAppListForHomeSlot(10)
            R.id.homeApp11 -> showAppListForHomeSlot(11)
            R.id.homeApp12 -> showAppListForHomeSlot(12)
            R.id.clock -> {
                showAppList(Constants.FLAG_SET_CLOCK_APP)
                prefs.clockAppPackage = ""
                prefs.clockAppClassName = ""
                prefs.clockAppUser = ""
            }
            R.id.date -> {
                showAppList(Constants.FLAG_SET_CALENDAR_APP)
                prefs.calendarAppPackage = ""
                prefs.calendarAppClassName = ""
                prefs.calendarAppUser = ""
            }
            R.id.cornerLeftButton -> showAppList(Constants.FLAG_SET_SWIPE_LEFT_APP)
            R.id.cornerRightButton -> showAppList(Constants.FLAG_SET_SWIPE_RIGHT_APP)
            R.id.setDefaultLauncher -> {
                prefs.hideSetDefaultLauncher = true
                binding.setDefaultLauncher.visibility = View.GONE
                if (viewModel.isZLauncherDefault.value != true) {
                    requireContext().showToast(R.string.set_as_default_launcher)
                    viewModel.openSettings()
                }
            }
        }
        return true
    }

    private fun initObservers() {
        binding.firstRunTips.isVisible = prefs.firstOpen
        if (binding.firstRunTips.isVisible) binding.setDefaultLauncher.visibility = View.GONE

        viewModel.refreshHome.observe(viewLifecycleOwner) { populateHomeScreen(it) }
        viewModel.isZLauncherDefault.observe(viewLifecycleOwner, Observer {
            if (it != true) {
                prefs.homeBottomAlignment = false
                setHomeAlignment()
            }
            if (binding.firstRunTips.isVisible) return@Observer
            binding.setDefaultLauncher.isVisible = !it && !prefs.hideSetDefaultLauncher
        })
        viewModel.homeAppAlignment.observe(viewLifecycleOwner) { setHomeAlignment(it) }
        viewModel.toggleDateTime.observe(viewLifecycleOwner) { populateDateTime() }
        viewModel.updateSwipeApps.observe(viewLifecycleOwner) { renderCornerIcons() }
        viewModel.showRecentApps.observe(viewLifecycleOwner) { binding.recents.performClick() }
    }

    private fun initSwipeTouchListener() {
        val context = requireContext()
        binding.mainLayout.setOnTouchListener(getSwipeGestureListener(context))
        binding.homeApp1.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp1))
        binding.homeApp2.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp2))
        binding.homeApp3.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp3))
        binding.homeApp4.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp4))
        binding.homeApp5.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp5))
        binding.homeApp6.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp6))
        binding.homeApp7.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp7))
        binding.homeApp8.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp8))
        binding.homeApp9.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp9))
        binding.homeApp10.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp10))
        binding.homeApp11.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp11))
        binding.homeApp12.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp12))
    }

    private fun initClickListeners() {
        binding.lock.setOnClickListener(this)
        binding.recents.setOnClickListener(this)
        binding.clock.setOnClickListener(this)
        binding.date.setOnClickListener(this)
        binding.clock.setOnLongClickListener(this)
        binding.date.setOnLongClickListener(this)
        binding.setDefaultLauncher.setOnClickListener(this)
        binding.setDefaultLauncher.setOnLongClickListener(this)
        binding.cornerLeftButton.setOnClickListener(this)
        binding.cornerRightButton.setOnClickListener(this)
        binding.cornerLeftButton.setOnLongClickListener(this)
        binding.cornerRightButton.setOnLongClickListener(this)
    }

    private fun setHomeAlignment(horizontalGravity: Int = prefs.homeAlignment) {
        binding.dateTimeLayout.gravity = horizontalGravity
        homeAppSlots().forEach { it.gravity = horizontalGravity }
        updateHomeAppsLayout()
    }

    private fun updateHomeAppsLayout() {
        if (_binding == null) return
        val homeAppsNum = prefs.homeAppsNum
        val horizontalPad = binding.homeAppsLayout.paddingLeft
        val headerMargin = resources.getDimensionPixelSize(R.dimen.home_apps_header_margin)
        val bottomMargin = resources.getDimensionPixelSize(R.dimen.home_apps_bottom_margin)

        val topInset = if (binding.dateTimeLayout.isVisible && binding.dateTimeLayout.height > 0) {
            binding.dateTimeLayout.bottom + headerMargin
        } else {
            resources.getDimensionPixelSize(R.dimen.home_apps_default_top_padding)
        }

        val cornerTop = minOf(binding.cornerLeftButton.top, binding.cornerRightButton.top)
        val bottomInset = if (cornerTop > 0) {
            (binding.root.height - cornerTop + bottomMargin).coerceAtLeast(
                resources.getDimensionPixelSize(R.dimen.home_apps_default_bottom_padding)
            )
        } else {
            resources.getDimensionPixelSize(R.dimen.home_apps_default_bottom_padding)
        }

        binding.homeAppsLayout.setPadding(horizontalPad, topInset, horizontalPad, bottomInset)

        val defaultPad = resources.getDimensionPixelSize(R.dimen.home_app_padding_vertical)
        val baseVerticalPad = when {
            homeAppsNum <= 4 -> defaultPad
            homeAppsNum <= 8 -> (defaultPad * 0.75f).toInt().coerceAtLeast(2)
            else -> (defaultPad * 0.5f).toInt().coerceAtLeast(1)
        }

        val baseTextSizePx = resources.getDimension(R.dimen.text_app_shortcut)
        val availableHeight = (binding.root.height - topInset - bottomInset).coerceAtLeast(0)
        val scaleFactor = if (homeAppsNum > 0 && availableHeight > 0) {
            val paint = Paint().apply {
                textSize = baseTextSizePx
                typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            }
            val textHeight = paint.fontMetrics.run { descent - ascent }
            val rowHeight = textHeight + 2 * baseVerticalPad
            val totalHeight = rowHeight * homeAppsNum
            if (totalHeight > availableHeight) {
                (availableHeight / totalHeight).coerceIn(0.55f, 1f)
            } else {
                1f
            }
        } else {
            1f
        }

        val scaledTextSizePx = baseTextSizePx * scaleFactor
        val scaledVerticalPad = (baseVerticalPad * scaleFactor).toInt().coerceAtLeast(1)

        homeAppSlots().forEachIndexed { index, slot ->
            if (index < homeAppsNum) {
                slot.setTextSize(TypedValue.COMPLEX_UNIT_PX, scaledTextSizePx)
                slot.setPadding(slot.paddingLeft, scaledVerticalPad, slot.paddingRight, scaledVerticalPad)
            } else {
                slot.setTextSize(TypedValue.COMPLEX_UNIT_PX, baseTextSizePx)
                slot.setPadding(slot.paddingLeft, baseVerticalPad, slot.paddingRight, baseVerticalPad)
            }
        }

        val verticalGravity = if (prefs.homeBottomAlignment) Gravity.BOTTOM else Gravity.CENTER_VERTICAL
        binding.homeAppsLayout.gravity = prefs.homeAlignment or verticalGravity
    }

    private fun populateDateTime() {
        binding.dateTimeLayout.isVisible = prefs.dateTimeVisibility != Constants.DateTime.OFF
        binding.clock.isVisible = Constants.DateTime.isTimeVisible(prefs.dateTimeVisibility)
        val dateVisible = Constants.DateTime.isDateVisible(prefs.dateTimeVisibility)
        binding.date.isVisible = dateVisible

        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        binding.date.text = dateFormat.format(Date())

        val battery = (requireContext().getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
            .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val showBattery = dateVisible && !prefs.showStatusBar && battery > 0
        binding.battery.isVisible = showBattery
        if (showBattery) binding.battery.text = "$battery%"
        updateHomeAppsLayout()
    }

    private fun populateHomeScreen(appCountUpdated: Boolean) {
        if (appCountUpdated) hideHomeApps()
        populateDateTime()

        val homeAppsNum = prefs.homeAppsNum
        if (homeAppsNum == 0) return

        val slots = homeAppSlots()
        for (i in 0 until homeAppsNum) {
            val location = i + 1
            slots[i].visibility = View.VISIBLE
            if (!setHomeAppText(
                    slots[i],
                    prefs.getAppName(location),
                    prefs.getAppPackage(location),
                    prefs.getAppUser(location),
                    prefs.getIsShortcut(location),
                    prefs.getShortcutId(location),
                )
            ) {
                writeEmptySlot(location)
            }
        }
        updateHomeAppsLayout()
    }

    private fun writeEmptySlot(location: Int) {
        when (location) {
            1 -> { prefs.appName1 = ""; prefs.appPackage1 = "" }
            2 -> { prefs.appName2 = ""; prefs.appPackage2 = "" }
            3 -> { prefs.appName3 = ""; prefs.appPackage3 = "" }
            4 -> { prefs.appName4 = ""; prefs.appPackage4 = "" }
            5 -> { prefs.appName5 = ""; prefs.appPackage5 = "" }
            6 -> { prefs.appName6 = ""; prefs.appPackage6 = "" }
            7 -> { prefs.appName7 = ""; prefs.appPackage7 = "" }
            8 -> { prefs.appName8 = ""; prefs.appPackage8 = "" }
            9 -> { prefs.appName9 = ""; prefs.appPackage9 = "" }
            10 -> { prefs.appName10 = ""; prefs.appPackage10 = "" }
            11 -> { prefs.appName11 = ""; prefs.appPackage11 = "" }
            12 -> { prefs.appName12 = ""; prefs.appPackage12 = "" }
        }
    }

    private fun setHomeAppText(
        textView: TextView,
        appName: String,
        packageName: String,
        userString: String,
        isShortcut: Boolean,
        shortcutId: String?,
    ): Boolean {
        val userHandle = getUserHandleFromString(requireContext(), userString)
        if (isShortcut) {
            val launcherApps = requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val query = LauncherApps.ShortcutQuery().apply {
                setPackage(packageName)
                setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
            }
            return try {
                val shortcuts = launcherApps.getShortcuts(query, userHandle)
                if (shortcuts?.any { it.id == shortcutId } == true) {
                    textView.text = appName
                    true
                } else {
                    textView.text = ""
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                textView.text = ""
                false
            }
        }
        return if (isPackageInstalled(requireContext(), packageName, userString)) {
            textView.text = appName
            true
        } else {
            textView.text = ""
            false
        }
    }

    private fun hideHomeApps() {
        homeAppSlots().forEach { it.visibility = View.GONE }
    }

    private fun launchAppOrShortcut(
        appName: String,
        packageName: String,
        activityClassName: String?,
        shortcutId: String?,
        isShortcut: Boolean,
        userString: String,
        fallback: (() -> Unit)? = null,
    ) {
        if (appName.isEmpty()) {
            fallback?.invoke() ?: showLongPressToast()
            return
        }
        if (isShortcut && !shortcutId.isNullOrEmpty()) {
            launchShortcut(packageName, shortcutId, appName, userString)
        } else if (packageName.isNotEmpty()) {
            launchApp(appName, packageName, activityClassName, userString)
        } else {
            fallback?.invoke()
        }
    }

    private fun launchShortcut(packageName: String, shortcutId: String, label: String, userString: String) {
        viewModel.selectedApp(
            AppModel.PinnedShortcut(
                shortcutId = shortcutId,
                appLabel = label,
                user = getUserHandleFromString(requireContext(), userString),
                key = null,
                appPackage = packageName,
                isNew = false,
            ),
            Constants.FLAG_LAUNCH_APP,
        )
    }

    private fun launchApp(appName: String, packageName: String, activityClassName: String?, userString: String) {
        viewModel.selectedApp(
            AppModel.App(
                appLabel = appName,
                key = null,
                appPackage = packageName,
                activityClassName = activityClassName,
                isNew = false,
                user = getUserHandleFromString(requireContext(), userString),
            ),
            Constants.FLAG_LAUNCH_APP,
        )
    }

    private fun homeAppClicked(location: Int) {
        if (prefs.getAppName(location).isEmpty()) {
            showAppListForHomeSlot(location)
            return
        }
        launchAppOrShortcut(
            appName = prefs.getAppName(location),
            packageName = prefs.getAppPackage(location),
            activityClassName = prefs.getAppActivityClassName(location),
            shortcutId = prefs.getShortcutId(location),
            isShortcut = prefs.getIsShortcut(location),
            userString = prefs.getAppUser(location),
        )
    }

    private fun launchCornerLeft() {
        launchAppOrShortcut(
            appName = prefs.appNameSwipeLeft,
            packageName = prefs.appPackageSwipeLeft,
            activityClassName = prefs.appActivityClassNameSwipeLeft,
            shortcutId = prefs.shortcutIdSwipeLeft,
            isShortcut = prefs.isShortcutSwipeLeft,
            userString = prefs.appUserSwipeLeft,
            fallback = { openDialerApp(requireContext()) },
        )
    }

    private fun launchCornerRight() {
        launchAppOrShortcut(
            appName = prefs.appNameSwipeRight,
            packageName = prefs.appPackageSwipeRight,
            activityClassName = prefs.appActivityClassNameSwipeRight,
            shortcutId = prefs.shortcutIdSwipeRight,
            isShortcut = prefs.isShortcutSwipeRight,
            userString = prefs.appUserSwipeRight,
            fallback = { openCameraApp(requireContext()) },
        )
    }

    private fun renderCornerIcons() {
        binding.cornerLeftButton.setImageDrawable(
            iconForPackage(prefs.appPackageSwipeLeft, prefs.appUserSwipeLeft)
                ?: requireContext().getDrawable(R.drawable.ic_phone)
        )
        binding.cornerRightButton.setImageDrawable(
            iconForPackage(prefs.appPackageSwipeRight, prefs.appUserSwipeRight)
                ?: requireContext().getDrawable(R.drawable.ic_camera)
        )
        binding.cornerLeftButton.scaleType = if (prefs.appPackageSwipeLeft.isNotBlank())
            ImageView.ScaleType.FIT_CENTER else ImageView.ScaleType.CENTER_INSIDE
        binding.cornerRightButton.scaleType = if (prefs.appPackageSwipeRight.isNotBlank())
            ImageView.ScaleType.FIT_CENTER else ImageView.ScaleType.CENTER_INSIDE
    }

    private fun iconForPackage(packageName: String, userString: String): Drawable? {
        if (packageName.isBlank()) return null
        return try {
            val launcherApps = requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val user = if (userString.isBlank()) Process.myUserHandle()
            else getUserHandleFromString(requireContext(), userString)
            val activity = launcherApps.getActivityList(packageName, user).firstOrNull()
            activity?.getBadgedIcon(0)
        } catch (e: Exception) {
            null
        }
    }

    private fun openClockApp() {
        if (prefs.clockAppPackage.isBlank()) openAlarmApp(requireContext())
        else launchApp("Clock", prefs.clockAppPackage, prefs.clockAppClassName, prefs.clockAppUser)
    }

    private fun openCalendarApp() {
        if (prefs.calendarAppPackage.isBlank()) openCalendar(requireContext())
        else launchApp("Calendar", prefs.calendarAppPackage, prefs.calendarAppClassName, prefs.calendarAppUser)
    }

    private fun showAppListForHomeSlot(location: Int) {
        showAppList(location, rename = prefs.getAppName(location).isNotEmpty(), includeHiddenApps = true)
    }

    private fun showAppList(flag: Int, rename: Boolean = false, includeHiddenApps: Boolean = false) {
        viewModel.getAppList(includeHiddenApps)
        AppDrawerFragment.pendingFlag = flag
        AppDrawerFragment.pendingRename = rename
        viewModel.openAppDrawerForFlag(flag)
    }

    private fun swipeDownAction() {
        when (prefs.swipeDownAction) {
            Constants.SwipeDownAction.SEARCH -> openSearch(requireContext())
            else -> expandNotificationDrawer(requireContext())
        }
    }

    private fun lockPhone() {
        requireActivity().runOnUiThread {
            try {
                deviceManager.lockNow()
            } catch (e: SecurityException) {
                requireContext().showToast(getString(R.string.please_turn_on_double_tap_to_unlock), Toast.LENGTH_LONG)
                viewModel.openSettings()
            } catch (e: Exception) {
                requireContext().showToast(getString(R.string.launcher_failed_to_lock_device), Toast.LENGTH_LONG)
                prefs.lockModeOn = false
            }
        }
    }

    private fun showStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requireActivity().window.insetsController?.show(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION", "InlinedApi")
            requireActivity().window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
        }
    }

    private fun hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requireActivity().window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_FULLSCREEN
            }
        }
    }

    private fun showLongPressToast() = requireContext().showToast(getString(R.string.long_press_to_select_app))

    private fun getSwipeGestureListener(context: Context): View.OnTouchListener {
        return object : OnSwipeTouchListener(context) {
            override fun onSwipeDown() {
                super.onSwipeDown()
                swipeDownAction()
            }

            override fun onLongClick() {
                super.onLongClick()
                viewModel.openSettings()
                viewModel.firstOpen(false)
            }

            override fun onDoubleClick() {
                super.onDoubleClick()
                if (!prefs.lockModeOn) return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) binding.lock.performClick()
                else lockPhone()
            }
        }
    }

    private fun getViewSwipeTouchListener(context: Context, view: View): View.OnTouchListener {
        return object : ViewSwipeTouchListener(context, view) {
            override fun onSwipeDown() {
                super.onSwipeDown()
                swipeDownAction()
            }

            override fun onLongClick(view: View) {
                super.onLongClick(view)
                this@HomeFragment.onLongClick(view)
            }

            override fun onClick(view: View) {
                super.onClick(view)
                this@HomeFragment.onClick(view)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
