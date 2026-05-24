package app.zlauncher.data

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume

class WeatherRepository(private val appContext: Context) {

    private val prefs = Prefs(appContext)
    private val fusedClient = LocationServices.getFusedLocationProviderClient(appContext)

    /**
     * Tries (in order): cached fix → last known fix → a single fresh request.
     * Caller is expected to hold ACCESS_COARSE_LOCATION; we re-check defensively.
     */
    @androidx.annotation.RequiresPermission("android.permission.ACCESS_COARSE_LOCATION")
    suspend fun getLocation(): Location? = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            try {
                fusedClient.lastLocation
                    .addOnSuccessListener { last ->
                        if (last != null) {
                            cont.resume(last)
                        } else {
                            fusedClient.getCurrentLocation(Priority.PRIORITY_LOW_POWER, null)
                                .addOnSuccessListener { fresh -> cont.resume(fresh) }
                                .addOnFailureListener { cont.resume(null) }
                        }
                    }
                    .addOnFailureListener { cont.resume(null) }
            } catch (e: SecurityException) {
                cont.resume(null)
            } catch (e: Exception) {
                cont.resume(null)
            }
        }
    }

    /**
     * Returns cached forecast if it is fresh and the device hasn't moved far; otherwise
     * fetches a fresh forecast from Open-Meteo and updates the cache.
     */
    suspend fun getForecast(
        latitude: Double,
        longitude: Double,
        forceRefresh: Boolean = false,
    ): WeatherForecast? {
        if (!forceRefresh) {
            cachedForecast(latitude, longitude)?.let { return it }
        }
        return fetchForecast(latitude, longitude)?.also { cacheForecast(it, latitude, longitude) }
    }

    fun isCacheStale(): Boolean {
        val cacheTime = prefs.weatherCacheTimestamp
        if (cacheTime <= 0L) return true
        return System.currentTimeMillis() - cacheTime > Constants.WEATHER_CACHE_TTL_MS
    }

    fun clearCache() {
        prefs.weatherCacheJson = ""
        prefs.weatherCacheTimestamp = 0L
        prefs.weatherCacheLat = Float.NaN
        prefs.weatherCacheLon = Float.NaN
    }

    private fun cachedForecast(latitude: Double, longitude: Double): WeatherForecast? {
        val cacheJson = prefs.weatherCacheJson
        val cacheTime = prefs.weatherCacheTimestamp
        if (cacheJson.isBlank() || cacheTime <= 0L) return null
        if (System.currentTimeMillis() - cacheTime > Constants.WEATHER_CACHE_TTL_MS) return null
        val cachedLat = prefs.weatherCacheLat
        val cachedLon = prefs.weatherCacheLon
        if (cachedLat.isNaN() || cachedLon.isNaN()) return null
        if (distanceMeters(cachedLat.toDouble(), cachedLon.toDouble(), latitude, longitude) >
            Constants.WEATHER_CACHE_INVALIDATE_DISTANCE_M
        ) return null
        return try {
            parseForecast(JSONObject(cacheJson))
        } catch (_: Exception) {
            null
        }
    }

    private fun cacheForecast(forecast: WeatherForecast, lat: Double, lon: Double) {
        try {
            prefs.weatherCacheJson = forecastToJson(forecast).toString()
            prefs.weatherCacheTimestamp = System.currentTimeMillis()
            prefs.weatherCacheLat = lat.toFloat()
            prefs.weatherCacheLon = lon.toFloat()
        } catch (_: Exception) {
        }
    }

    private suspend fun fetchForecast(latitude: Double, longitude: Double): WeatherForecast? =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(
                    "${Constants.URL_OPEN_METEO_BASE}?latitude=$latitude&longitude=$longitude" +
                            "&current=temperature_2m,weather_code" +
                            "&hourly=temperature_2m,weather_code" +
                            "&forecast_hours=9&timezone=auto"
                )
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "GET"
                }
                connection.inputStream.use { input ->
                    val body = input.bufferedReader().readText()
                    parseForecast(JSONObject(body))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    private fun parseForecast(json: JSONObject): WeatherForecast {
        val current = json.getJSONObject("current")
        val currentTemp = current.getDouble("temperature_2m")
        val currentCode = current.getInt("weather_code")

        val hourly = json.getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val temps = hourly.getJSONArray("temperature_2m")
        val codes = hourly.getJSONArray("weather_code")

        val now = System.currentTimeMillis()
        val hourlyList = mutableListOf<HourlyWeather>()
        for (i in 0 until times.length()) {
            val timeIso = times.getString(i)
            val timeMillis = isoToMillis(timeIso)
            if (timeMillis < now - 30 * 60 * 1000) continue
            hourlyList.add(
                HourlyWeather(
                    timeMillis = timeMillis,
                    tempC = temps.getDouble(i),
                    code = codes.getInt(i),
                )
            )
            if (hourlyList.size >= 8) break
        }
        return WeatherForecast(currentTemp, currentCode, hourlyList)
    }

    private fun forecastToJson(forecast: WeatherForecast): JSONObject {
        val current = JSONObject()
            .put("temperature_2m", forecast.currentTempC)
            .put("weather_code", forecast.currentCode)
        val timesArr = org.json.JSONArray()
        val tempsArr = org.json.JSONArray()
        val codesArr = org.json.JSONArray()
        forecast.hourly.forEach {
            timesArr.put(millisToIso(it.timeMillis))
            tempsArr.put(it.tempC)
            codesArr.put(it.code)
        }
        val hourly = JSONObject()
            .put("time", timesArr)
            .put("temperature_2m", tempsArr)
            .put("weather_code", codesArr)
        return JSONObject()
            .put("current", current)
            .put("hourly", hourly)
    }

    suspend fun reverseGeocode(latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(appContext, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val list = suspendCancellableCoroutine<List<android.location.Address>> { cont ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        cont.resume(addresses)
                    }
                }
                list.firstOrNull()?.locality ?: list.firstOrNull()?.subAdminArea
            } else {
                @Suppress("DEPRECATION")
                val list = geocoder.getFromLocation(latitude, longitude, 1) ?: emptyList()
                list.firstOrNull()?.locality ?: list.firstOrNull()?.subAdminArea
            }
        } catch (_: Exception) {
            null
        }
    }

    fun isLocationEnabled(): Boolean {
        val manager = appContext.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        return LocationManagerCompat.isLocationEnabled(manager)
    }

    private fun isoToMillis(iso: String): Long {
        // Open-Meteo with timezone=auto returns "yyyy-MM-dd'T'HH:mm" in the area's timezone.
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
            format.timeZone = java.util.TimeZone.getDefault()
            format.parse(iso)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun millisToIso(millis: Long): String {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
        format.timeZone = java.util.TimeZone.getDefault()
        return format.format(java.util.Date(millis))
    }

    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }
}
