package com.rustanovych.kidsoutfit.domain.engine

import com.rustanovych.kidsoutfit.domain.model.WeatherScene
import com.rustanovych.kidsoutfit.domain.model.WeatherSnapshot

/**
 * Picks the [WeatherScene] (home screen background) for a single hour from its WMO weather code
 * and a few extra signals.
 *
 * Rules apply in priority order, first match wins:
 *
 * | # | Condition | Result |
 * |---|-----------|--------|
 * | 1 | Clear/cloudy code (0, 1, 2, 3, 45, 48) and `apparentTemperature < -8` | [WeatherScene.FROST] |
 * | 2 | `windSpeedKmh >= 25` and no precipitation | [WeatherScene.WIND] |
 * | 3 | Code 0, 1 and `hour >= 20 \|\| hour < 6` | [WeatherScene.EVENING] |
 * | 3 | Code 0, 1 otherwise | [WeatherScene.SUNNY] |
 * | 4 | Code 2, 3, 45, 48 | [WeatherScene.CLOUDY] |
 * | 5 | Code 51-67, 80-82 | [WeatherScene.RAIN] |
 * | 6 | Code 95, 96, 99 | [WeatherScene.STORM] |
 * | 7 | Code 71-77, 85, 86 | [WeatherScene.SNOW] |
 * | 8 | Any other code | [WeatherScene.CLOUDY] |
 *
 * "No precipitation" means the weather code is not a rain/storm/snow code, `precipitationMm` is
 * below the same threshold [OutfitEngine] uses for rain, and `snowfallCm` is zero.
 */
object WeatherSceneSelector {

    private const val FROST_APPARENT_THRESHOLD_C = -8.0
    private const val WINDY_THRESHOLD_KMH = 25.0
    private const val RAIN_PRECIPITATION_THRESHOLD_MM = 0.3
    private const val EVENING_START_HOUR = 20
    private const val MORNING_START_HOUR = 6

    private val CLEAR_CODES = setOf(0, 1)
    private val CLOUDY_CODES = setOf(2, 3, 45, 48)
    private val RAIN_CODES = (51..67).toSet() + (80..82).toSet()
    private val STORM_CODES = setOf(95, 96, 99)
    private val SNOW_CODES = (71..77).toSet() + setOf(85, 86)

    private val PRECIPITATION_CODES = RAIN_CODES + STORM_CODES + SNOW_CODES

    fun select(snapshot: WeatherSnapshot): WeatherScene {
        val code = snapshot.weatherCode
        val clearOrCloudy = code in CLEAR_CODES || code in CLOUDY_CODES

        if (clearOrCloudy && snapshot.apparentTemperature < FROST_APPARENT_THRESHOLD_C) {
            return WeatherScene.FROST
        }
        if (snapshot.windSpeedKmh >= WINDY_THRESHOLD_KMH && !hasPrecipitation(snapshot)) {
            return WeatherScene.WIND
        }
        return when (code) {
            in CLEAR_CODES ->
                if (isNight(snapshot.hour)) WeatherScene.EVENING else WeatherScene.SUNNY
            in CLOUDY_CODES -> WeatherScene.CLOUDY
            in RAIN_CODES -> WeatherScene.RAIN
            in STORM_CODES -> WeatherScene.STORM
            in SNOW_CODES -> WeatherScene.SNOW
            else -> WeatherScene.CLOUDY
        }
    }

    private fun hasPrecipitation(snapshot: WeatherSnapshot): Boolean =
        snapshot.weatherCode in PRECIPITATION_CODES ||
            snapshot.precipitationMm >= RAIN_PRECIPITATION_THRESHOLD_MM ||
            snapshot.snowfallCm > 0.0

    private fun isNight(hour: Int): Boolean = hour >= EVENING_START_HOUR || hour < MORNING_START_HOUR
}
