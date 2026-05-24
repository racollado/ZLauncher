package app.zlauncher.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.zlauncher.data.Prefs
import app.zlauncher.data.WeatherForecast
import app.zlauncher.data.WeatherRepository
import kotlinx.coroutines.launch
import org.json.JSONArray

class WidgetsViewModel(application: Application) : AndroidViewModel(application) {

    sealed interface WeatherState {
        data object Loading : WeatherState
        data object NeedsPermission : WeatherState
        data object LocationDisabled : WeatherState
        data object Error : WeatherState
        data class Loaded(
            val forecast: WeatherForecast,
            val locationLabel: String?,
        ) : WeatherState
    }

    private val appContext = application.applicationContext
    private val prefs = Prefs(appContext)
    private val repository = WeatherRepository(appContext)

    val weatherState = MutableLiveData<WeatherState>(WeatherState.Loading)
    val todoTasks = MutableLiveData<List<String>>(loadTasks())

    fun refreshWeatherIfNeeded() {
        if (!hasLocationPermission()) {
            weatherState.value = WeatherState.NeedsPermission
            return
        }
        if (!repository.isLocationEnabled()) {
            weatherState.value = WeatherState.LocationDisabled
            return
        }
        if (weatherState.value is WeatherState.Loaded && !repository.isCacheStale()) return
        loadWeather()
    }

    fun forceRefreshWeather() {
        if (!hasLocationPermission()) {
            weatherState.value = WeatherState.NeedsPermission
            return
        }
        if (!repository.isLocationEnabled()) {
            weatherState.value = WeatherState.LocationDisabled
            return
        }
        repository.clearCache()
        loadWeather(forceRefresh = true)
    }

    @SuppressLint("MissingPermission")
    fun loadWeather(forceRefresh: Boolean = false) {
        if (!hasLocationPermission()) {
            weatherState.value = WeatherState.NeedsPermission
            return
        }
        weatherState.value = WeatherState.Loading
        viewModelScope.launch {
            val location = repository.getLocation()
            if (location == null) {
                weatherState.value = WeatherState.LocationDisabled
                return@launch
            }
            val forecast = repository.getForecast(
                location.latitude,
                location.longitude,
                forceRefresh = forceRefresh,
            )
            if (forecast == null) {
                weatherState.value = WeatherState.Error
                return@launch
            }
            val label = repository.reverseGeocode(location.latitude, location.longitude)
            weatherState.value = WeatherState.Loaded(forecast, label)
        }
    }

    fun hasLocationPermission(): Boolean = ContextCompat.checkSelfPermission(
        appContext, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    fun addTask(task: String) {
        val trimmed = task.trim()
        if (trimmed.isEmpty()) return
        val updated = (todoTasks.value ?: emptyList()) + trimmed
        todoTasks.value = updated
        saveTasks(updated)
    }

    fun removeTask(index: Int) {
        val current = todoTasks.value ?: return
        if (index !in current.indices) return
        val updated = current.toMutableList().also { it.removeAt(index) }
        todoTasks.value = updated
        saveTasks(updated)
    }

    private fun loadTasks(): List<String> = try {
        val json = JSONArray(prefs.todoTasksJson)
        buildList {
            for (i in 0 until json.length()) add(json.getString(i))
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun saveTasks(tasks: List<String>) {
        val arr = JSONArray()
        tasks.forEach { arr.put(it) }
        prefs.todoTasksJson = arr.toString()
    }
}
