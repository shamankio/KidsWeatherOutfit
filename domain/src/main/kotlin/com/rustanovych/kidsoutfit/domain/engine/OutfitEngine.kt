package com.rustanovych.kidsoutfit.domain.engine

import com.rustanovych.kidsoutfit.domain.model.ChildProfile
import com.rustanovych.kidsoutfit.domain.model.OutfitSet
import com.rustanovych.kidsoutfit.domain.model.WeatherSnapshot

/**
 * Picks the [OutfitSet] to recommend for a single hour, given the forecast at that hour and the
 * child's profile.
 *
 * Resolution happens in four steps, each with its own private function:
 * 1. [effectiveTemperature] shifts the forecast's apparent temperature by the child's cold
 *    sensitivity.
 * 2. [temperatureCategory] buckets that effective temperature into a base [TemperatureCategory].
 * 3. [isRaining], [isSnowing] and [isWindy] detect the relevant weather states for the hour.
 * 4. The category and weather states are combined into a final [OutfitSet], applying the
 *    following rules in priority order:
 *
 * | # | Condition | Result |
 * |---|-----------|--------|
 * | 1 | Snowing, category is COLD/WINTER_LIGHT/FROST_HARD | [OutfitSet.SNOWFALL] |
 * | 1a | ...except FROST_HARD with `snowfallCm < 1.0` | stays [OutfitSet.FROST_HARD] |
 * | 2 | Raining and windy, category is WARM/COOL/COLD | [OutfitSet.RAIN_WIND] |
 * | 3 | Raining, category HEAT or WARM | [OutfitSet.WARM_RAIN] |
 * | 3 | Raining, category COOL | [OutfitSet.COOL_RAIN] |
 * | 3 | Raining, category COLD/WINTER_LIGHT/FROST_HARD | [OutfitSet.COLD_RAIN] |
 * | 4 | Windy (no rain), category WARM or COOL | [OutfitSet.WIND] |
 * | 5 | Otherwise | the base category's [OutfitSet] |
 */
object OutfitEngine {

    private const val HEAT_THRESHOLD_C = 23.0
    private const val WARM_THRESHOLD_C = 15.0
    private const val COOL_THRESHOLD_C = 8.0
    private const val COLD_THRESHOLD_C = 0.0
    private const val WINTER_LIGHT_THRESHOLD_C = -8.0

    private const val RAIN_PRECIPITATION_THRESHOLD_MM = 0.3
    private const val WINDY_THRESHOLD_KMH = 25.0
    private const val HARD_FROST_SNOWFALL_THRESHOLD_CM = 1.0

    private val RAIN_WEATHER_CODES = setOf(51, 53, 55, 61, 63, 65, 80, 81, 82, 95, 96, 99)
    private val SNOW_WEATHER_CODES = setOf(71, 73, 75, 77, 85, 86)

    private val SNOWFALL_ELIGIBLE_CATEGORIES = setOf(
        TemperatureCategory.COLD,
        TemperatureCategory.WINTER_LIGHT,
        TemperatureCategory.FROST_HARD,
    )
    private val RAIN_WIND_ELIGIBLE_CATEGORIES = setOf(
        TemperatureCategory.WARM,
        TemperatureCategory.COOL,
        TemperatureCategory.COLD,
    )
    private val WIND_ELIGIBLE_CATEGORIES = setOf(TemperatureCategory.WARM, TemperatureCategory.COOL)

    /** Base temperature bucket, before rain/snow/wind modifiers are applied. */
    private enum class TemperatureCategory { HEAT, WARM, COOL, COLD, WINTER_LIGHT, FROST_HARD }

    fun resolve(snapshot: WeatherSnapshot, profile: ChildProfile): OutfitSet {
        val category = temperatureCategory(effectiveTemperature(snapshot, profile))
        val raining = isRaining(snapshot)
        val snowing = isSnowing(snapshot)
        val windy = isWindy(snapshot)

        if (snowing && category in SNOWFALL_ELIGIBLE_CATEGORIES) {
            val hardFrostOutranksLightSnow =
                category == TemperatureCategory.FROST_HARD &&
                    snapshot.snowfallCm < HARD_FROST_SNOWFALL_THRESHOLD_CM
            if (!hardFrostOutranksLightSnow) return OutfitSet.SNOWFALL
        }
        if (raining && windy && category in RAIN_WIND_ELIGIBLE_CATEGORIES) return OutfitSet.RAIN_WIND
        if (raining) return rainOutfitFor(category)
        if (windy && category in WIND_ELIGIBLE_CATEGORIES) return OutfitSet.WIND
        return baseOutfitFor(category)
    }

    /** Step 1: apparent temperature shifted by the child's cold sensitivity. */
    private fun effectiveTemperature(snapshot: WeatherSnapshot, profile: ChildProfile): Double =
        snapshot.apparentTemperature + profile.coldSensitivity.degreesCelsius

    /** Step 2: buckets [effectiveTempC] into a base temperature category. */
    private fun temperatureCategory(effectiveTempC: Double): TemperatureCategory = when {
        effectiveTempC >= HEAT_THRESHOLD_C -> TemperatureCategory.HEAT
        effectiveTempC >= WARM_THRESHOLD_C -> TemperatureCategory.WARM
        effectiveTempC >= COOL_THRESHOLD_C -> TemperatureCategory.COOL
        effectiveTempC >= COLD_THRESHOLD_C -> TemperatureCategory.COLD
        effectiveTempC >= WINTER_LIGHT_THRESHOLD_C -> TemperatureCategory.WINTER_LIGHT
        else -> TemperatureCategory.FROST_HARD
    }

    /** Step 3: is it raining at [snapshot]'s hour? */
    private fun isRaining(snapshot: WeatherSnapshot): Boolean =
        snapshot.precipitationMm >= RAIN_PRECIPITATION_THRESHOLD_MM ||
            snapshot.weatherCode in RAIN_WEATHER_CODES

    /** Step 3: is it snowing at [snapshot]'s hour? */
    private fun isSnowing(snapshot: WeatherSnapshot): Boolean =
        snapshot.snowfallCm > 0.0 || snapshot.weatherCode in SNOW_WEATHER_CODES

    /** Step 3: is it windy at [snapshot]'s hour? */
    private fun isWindy(snapshot: WeatherSnapshot): Boolean =
        snapshot.windSpeedKmh >= WINDY_THRESHOLD_KMH

    /** Step 4: rain outfit for a category, when it's raining but not rainy-and-windy. */
    private fun rainOutfitFor(category: TemperatureCategory): OutfitSet = when (category) {
        TemperatureCategory.HEAT, TemperatureCategory.WARM -> OutfitSet.WARM_RAIN
        TemperatureCategory.COOL -> OutfitSet.COOL_RAIN
        TemperatureCategory.COLD,
        TemperatureCategory.WINTER_LIGHT,
        TemperatureCategory.FROST_HARD,
        -> OutfitSet.COLD_RAIN
    }

    /** Step 4: the plain [OutfitSet] for a category, when no modifier applies. */
    private fun baseOutfitFor(category: TemperatureCategory): OutfitSet = when (category) {
        TemperatureCategory.HEAT -> OutfitSet.HEAT
        TemperatureCategory.WARM -> OutfitSet.WARM
        TemperatureCategory.COOL -> OutfitSet.COOL
        TemperatureCategory.COLD -> OutfitSet.COLD
        TemperatureCategory.WINTER_LIGHT -> OutfitSet.WINTER_LIGHT
        TemperatureCategory.FROST_HARD -> OutfitSet.FROST_HARD
    }
}
