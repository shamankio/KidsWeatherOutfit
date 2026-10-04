package com.rustanovych.kidsoutfit.ui.assets

import androidx.annotation.StringRes
import com.rustanovych.kidsoutfit.R
import com.rustanovych.kidsoutfit.domain.engine.Direction
import com.rustanovych.kidsoutfit.domain.model.OutfitSet

/*
 * The ONLY place that maps domain enums to `strings.xml` ids. The domain carries no user-facing
 * text (see `Advice`), so every localized wording is looked up here.
 */

@StringRes
fun outfitNameRes(set: OutfitSet): Int = when (set) {
    OutfitSet.HEAT -> R.string.outfit_name_heat
    OutfitSet.WARM -> R.string.outfit_name_warm
    OutfitSet.WARM_RAIN -> R.string.outfit_name_warm_rain
    OutfitSet.COOL -> R.string.outfit_name_cool
    OutfitSet.COOL_RAIN -> R.string.outfit_name_cool_rain
    OutfitSet.WIND -> R.string.outfit_name_wind
    OutfitSet.COLD -> R.string.outfit_name_cold
    OutfitSet.COLD_RAIN -> R.string.outfit_name_cold_rain
    OutfitSet.WINTER_LIGHT -> R.string.outfit_name_winter_light
    OutfitSet.FROST_HARD -> R.string.outfit_name_frost_hard
    OutfitSet.SNOWFALL -> R.string.outfit_name_snowfall
    OutfitSet.RAIN_WIND -> R.string.outfit_name_rain_wind
}

/** Warning-banner template for a change; arguments are the time label and the target set's name. */
@StringRes
fun adviceRes(direction: Direction): Int = when (direction) {
    Direction.WARMER -> R.string.advice_warmer
    Direction.COLDER -> R.string.advice_colder
    Direction.RAIN_STARTS -> R.string.advice_rain_starts
    Direction.RAIN_STOPS -> R.string.advice_rain_stops
    Direction.SNOW_STARTS -> R.string.advice_snow_starts
    Direction.WIND_STARTS -> R.string.advice_wind_starts
}
