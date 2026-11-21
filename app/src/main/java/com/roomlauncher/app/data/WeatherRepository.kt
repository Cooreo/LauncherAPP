package com.roomlauncher.app.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

enum class WeatherCondition {
    CLEAR_DAY,
    CLEAR_NIGHT,
    PARTLY_CLOUDY,
    CLOUDY,
    RAIN,
    SNOW,
    THUNDERSTORM
}

@Immutable
data class WeatherState(
    val temperatureC: Int? = null,
    val condition: WeatherCondition = WeatherCondition.CLEAR_DAY,
    val isDay: Boolean = true,
    val description: String = "Clear",
    val hasLocationPermission: Boolean = false,
    val lastUpdatedMillis: Long = 0L
)

@Serializable
private data class OpenMeteoResponse(
    val current: OpenMeteoCurrent? = null
)

@Serializable
private data class OpenMeteoCurrent(
    @SerialName("temperature_2m") val temperature: Double? = null,
    @SerialName("weather_code") val weatherCode: Int = 0,
    @SerialName("is_day") val isDay: Int = 1
)

class WeatherRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private var cachedWeather: WeatherState? = null

    fun hasLocationPermission(): Boolean {
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return coarse || fine
    }

    suspend fun fetchWeather(forceRefresh: Boolean = false): WeatherState = withContext(Dispatchers.IO) {
        val permitted = hasLocationPermission()
        val now = System.currentTimeMillis()
        val existing = cachedWeather
        if (!forceRefresh && existing != null && existing.hasLocationPermission == permitted &&
            (now - existing.lastUpdatedMillis) < CACHE_DURATION_MS
        ) {
            return@withContext existing
        }

        if (!permitted) {
            return@withContext WeatherState(
                hasLocationPermission = false,
                condition = defaultTimeCondition(),
                isDay = isLocalDaytime()
            ).also { cachedWeather = it }
        }

        val location = getBestLastKnownLocation()
        if (location == null) {
            return@withContext WeatherState(
                hasLocationPermission = true,
                condition = defaultTimeCondition(),
                isDay = isLocalDaytime(),
                description = "Location unavailable"
            ).also { cachedWeather = it }
        }

        val fetched = runCatching {
            val urlString = String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code,is_day",
                location.latitude,
                location.longitude
            )
            val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
            }
            try {
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    val parsed = json.decodeFromString<OpenMeteoResponse>(body)
                    val current = parsed.current
                    if (current != null) {
                        val isDay = current.isDay == 1
                        val (cond, desc) = mapWmoCode(current.weatherCode, isDay)
                        WeatherState(
                            temperatureC = current.temperature?.toInt(),
                            condition = cond,
                            isDay = isDay,
                            description = desc,
                            hasLocationPermission = true,
                            lastUpdatedMillis = now
                        )
                    } else {
                        null
                    }
                } else {
                    null
                }
            } finally {
                connection.disconnect()
            }
        }.getOrNull()

        val result = fetched ?: existing?.copy(hasLocationPermission = true) ?: WeatherState(
            hasLocationPermission = true,
            condition = defaultTimeCondition(),
            isDay = isLocalDaytime(),
            description = "Offline"
        )
        cachedWeather = result
        result
    }

    @SuppressLint("MissingPermission")
    private fun getBestLastKnownLocation(): Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = lm.getProviders(true)
        var best: Location? = null
        for (provider in providers) {
            val loc = runCatching { lm.getLastKnownLocation(provider) }.getOrNull() ?: continue
            if (best == null || loc.time > best.time) {
                best = loc
            }
        }
        return best
    }

    private fun isLocalDaytime(): Boolean {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return hour in 6..19
    }

    private fun defaultTimeCondition(): WeatherCondition =
        if (isLocalDaytime()) WeatherCondition.CLEAR_DAY else WeatherCondition.CLEAR_NIGHT

    private fun mapWmoCode(code: Int, isDay: Boolean): Pair<WeatherCondition, String> = when (code) {
        0 -> (if (isDay) WeatherCondition.CLEAR_DAY else WeatherCondition.CLEAR_NIGHT) to "Clear"
        1, 2 -> WeatherCondition.PARTLY_CLOUDY to "Partly Cloudy"
        3, 45, 48 -> WeatherCondition.CLOUDY to "Cloudy"
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> WeatherCondition.RAIN to "Rain"
        71, 73, 75, 77, 85, 86 -> WeatherCondition.SNOW to "Snow"
        95, 96, 99 -> WeatherCondition.THUNDERSTORM to "Storm"
        else -> (if (isDay) WeatherCondition.CLEAR_DAY else WeatherCondition.CLEAR_NIGHT) to "Clear"
    }

    companion object {
        private const val CACHE_DURATION_MS = 30L * 60L * 1000L
    }
}
