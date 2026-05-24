package app.zlauncher.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import app.zlauncher.MainViewModel
import app.zlauncher.R
import app.zlauncher.data.Constants
import app.zlauncher.data.HourlyWeather
import app.zlauncher.data.Prefs
import app.zlauncher.data.WeatherCode
import app.zlauncher.databinding.FragmentWidgetsBinding
import app.zlauncher.helper.dpToPx
import app.zlauncher.helper.getColorFromAttr
import app.zlauncher.helper.getUserHandleFromString
import app.zlauncher.helper.hideKeyboard
import app.zlauncher.helper.isPackageInstalled
import app.zlauncher.helper.showToast
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class WidgetsFragment : Fragment() {

    private var _binding: FragmentWidgetsBinding? = null
    private val binding get() = _binding!!

    private val widgetsViewModel: WidgetsViewModel by activityViewModels()
    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var prefs: Prefs

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) widgetsViewModel.loadWeather()
        else requireContext().showToast(R.string.weather_location_denied)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWidgetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        bindWeather()
        bindTodo()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        widgetsViewModel.refreshWeatherIfNeeded()
    }

    private fun observeViewModel() {
        widgetsViewModel.weatherState.observe(viewLifecycleOwner) { state ->
            renderWeather(state)
        }
        widgetsViewModel.todoTasks.observe(viewLifecycleOwner) { tasks ->
            renderTodo(tasks)
        }
    }

    private fun bindWeather() {
        binding.weatherCard.setOnClickListener {
            when (val state = widgetsViewModel.weatherState.value) {
                is WidgetsViewModel.WeatherState.NeedsPermission ->
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)

                is WidgetsViewModel.WeatherState.LocationDisabled -> {
                    requireContext().showToast(R.string.weather_unavailable)
                }

                is WidgetsViewModel.WeatherState.Error -> widgetsViewModel.loadWeather()

                is WidgetsViewModel.WeatherState.Loaded -> openWeatherApp()
                is WidgetsViewModel.WeatherState.Loading -> Unit
                null -> Unit
            }
        }
        binding.weatherCard.setOnLongClickListener {
            mainViewModel.openAppDrawerForFlag(Constants.FLAG_SET_WEATHER_APP)
            true
        }
    }

    private fun bindTodo() {
        binding.todoInput.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val text = v.text.toString()
                if (text.isNotBlank()) {
                    widgetsViewModel.addTask(text)
                    binding.todoInput.text.clear()
                }
                true
            } else false
        }
    }

    private fun renderWeather(state: WidgetsViewModel.WeatherState) {
        when (state) {
            is WidgetsViewModel.WeatherState.Loading -> {
                binding.weatherStatus.text = getString(R.string.weather_loading)
                binding.weatherCondition.text = ""
                binding.weatherAction.isVisible = false
                binding.hourlyRow.removeAllViews()
            }
            is WidgetsViewModel.WeatherState.NeedsPermission -> {
                binding.weatherStatus.text = getString(R.string.weather)
                binding.weatherCondition.text = ""
                binding.weatherAction.isVisible = true
                binding.weatherAction.text = getString(R.string.weather_location_needed)
                binding.hourlyRow.removeAllViews()
            }
            is WidgetsViewModel.WeatherState.LocationDisabled -> {
                binding.weatherStatus.text = getString(R.string.weather_unavailable)
                binding.weatherCondition.text = ""
                binding.weatherAction.isVisible = false
                binding.hourlyRow.removeAllViews()
            }
            is WidgetsViewModel.WeatherState.Error -> {
                binding.weatherStatus.text = getString(R.string.weather_unavailable)
                binding.weatherCondition.text = ""
                binding.weatherAction.isVisible = false
                binding.hourlyRow.removeAllViews()
            }
            is WidgetsViewModel.WeatherState.Loaded -> {
                val temp = state.forecast.currentTempC.cToF().roundToInt()
                val glyph = WeatherCode.glyph(state.forecast.currentCode)
                val location = state.locationLabel ?: ""
                binding.weatherStatus.text = if (location.isBlank()) {
                    getString(R.string.weather_summary_no_location, glyph, temp)
                } else {
                    getString(R.string.weather_summary, glyph, temp, location)
                }
                binding.weatherCondition.text = WeatherCode.label(state.forecast.currentCode)
                binding.weatherAction.isVisible = false
                renderHourly(state.forecast.hourly)
            }
        }
    }

    private fun renderHourly(hourly: List<HourlyWeather>) {
        binding.hourlyRow.removeAllViews()
        if (hourly.isEmpty()) return
        val textColor = requireContext().getColorFromAttr(R.attr.primaryColor)
        val pattern = if (DateFormat.is24HourFormat(requireContext())) "HH" else "ha"
        hourly.forEach { hour ->
            val cell = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                orientation = LinearLayout.VERTICAL
            }
            val time = TextView(requireContext()).apply {
                text = DateFormat.format(pattern, Date(hour.timeMillis)).toString().lowercase(Locale.getDefault())
                textSize = 12f
                setTextColor(textColor)
                setShadowLayer(0f, 0f, 0f, 0)
            }
            val glyph = TextView(requireContext()).apply {
                text = WeatherCode.glyph(hour.code)
                textSize = 16f
                setTextColor(textColor)
                setShadowLayer(0f, 0f, 0f, 0)
                setPadding(0, 4.dpToPx(), 0, 0)
            }
            val temp = TextView(requireContext()).apply {
                text = String.format(Locale.getDefault(), "%d\u00B0", hour.tempC.cToF().roundToInt())
                textSize = 12f
                setTextColor(textColor)
                setShadowLayer(0f, 0f, 0f, 0)
                setPadding(0, 2.dpToPx(), 0, 0)
            }
            cell.addView(time)
            cell.addView(glyph)
            cell.addView(temp)
            binding.hourlyRow.addView(cell)
        }
    }

    private fun Double.cToF(): Double = this * 9.0 / 5.0 + 32.0

    private fun renderTodo(tasks: List<String>) {
        binding.todoList.removeAllViews()
        tasks.forEachIndexed { index, task ->
            val row = TextView(requireContext()).apply {
                text = task
                textSize = 18f
                setPadding(0, 12.dpToPx(), 0, 12.dpToPx())
                setBackgroundResource(android.R.color.transparent)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    animate().alpha(0f).setDuration(150).withEndAction {
                        widgetsViewModel.removeTask(index)
                    }.start()
                }
            }
            binding.todoList.addView(row)
        }
    }

    private fun openWeatherApp() {
        val packageName = prefs.weatherAppPackage
        if (packageName.isNotBlank()) {
            launchByPackage(packageName, prefs.weatherAppClassName, prefs.weatherAppUser)
            return
        }
        Constants.WEATHER_APP_PACKAGES.firstOrNull { requireContext().isPackageInstalled(it) }
            ?.let { launchByPackage(it, null, "") }
            ?: run {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=weather")))
                } catch (_: Exception) {
                    requireContext().showToast(R.string.unable_to_open_app)
                }
            }
    }

    private fun launchByPackage(packageName: String, className: String?, userString: String) {
        val launcherApps = requireContext().getSystemService(android.content.Context.LAUNCHER_APPS_SERVICE)
                as LauncherApps
        val user = if (userString.isBlank()) Process.myUserHandle()
        else getUserHandleFromString(requireContext(), userString)
        val activities = launcherApps.getActivityList(packageName, user)
        if (activities.isEmpty()) {
            requireContext().showToast(R.string.unable_to_open_app)
            return
        }
        val component = if (!className.isNullOrBlank() && activities.any { it.componentName.className == className }) {
            ComponentName(packageName, className)
        } else {
            activities.first().componentName
        }
        try {
            launcherApps.startMainActivity(component, user, null, null)
        } catch (_: Exception) {
            requireContext().showToast(R.string.unable_to_open_app)
        }
    }

    fun closeKeyboard() {
        _binding?.todoInput?.hideKeyboard()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
