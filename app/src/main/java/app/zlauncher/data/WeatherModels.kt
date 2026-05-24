package app.zlauncher.data

data class WeatherForecast(
    val currentTempC: Double,
    val currentCode: Int,
    val hourly: List<HourlyWeather>,
)

data class HourlyWeather(
    /** Epoch millis */
    val timeMillis: Long,
    val tempC: Double,
    val code: Int,
)

/** Open-Meteo WMO weather codes mapped to a unicode glyph + short label. */
object WeatherCode {
    fun glyph(code: Int): String = when (code) {
        0 -> "\u2600"        // clear sky
        1, 2 -> "\uD83C\uDF24"   // mainly clear / partly cloudy
        3 -> "\u2601"        // overcast
        45, 48 -> "\uD83C\uDF2B" // fog
        51, 53, 55, 56, 57 -> "\uD83C\uDF26" // drizzle
        61, 63, 65, 66, 67, 80, 81, 82 -> "\uD83C\uDF27" // rain
        71, 73, 75, 77, 85, 86 -> "\uD83C\uDF28"         // snow
        95, 96, 99 -> "\uD83C\uDF29"                     // thunderstorm
        else -> "\u2600"
    }

    fun label(code: Int): String = when (code) {
        0 -> "Clear"
        1 -> "Mostly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing drizzle"
        61, 63, 65 -> "Rain"
        66, 67 -> "Freezing rain"
        71, 73, 75, 77 -> "Snow"
        80, 81, 82 -> "Rain showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm w/ hail"
        else -> "Unknown"
    }
}
