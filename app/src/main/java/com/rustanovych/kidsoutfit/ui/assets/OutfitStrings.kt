package com.rustanovych.kidsoutfit.ui.assets

import androidx.annotation.StringRes
import com.rustanovych.kidsoutfit.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rustanovych.kidsoutfit.domain.engine.Direction
import com.rustanovych.kidsoutfit.domain.model.ChildGender
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

/**
 * What a screen reader says for the illustration of [set] on one child.
 *
 * @throws IllegalArgumentException for [ChildGender.BOTH]; describe the two children separately,
 * or use [outfitDescription].
 */
@StringRes
fun outfitDescriptionRes(set: OutfitSet, gender: ChildGender): Int = when (gender) {
    ChildGender.BOY -> when (set) {
        OutfitSet.HEAT -> R.string.outfit_desc_heat_boy
        OutfitSet.WARM -> R.string.outfit_desc_warm_boy
        OutfitSet.WARM_RAIN -> R.string.outfit_desc_warm_rain_boy
        OutfitSet.COOL -> R.string.outfit_desc_cool_boy
        OutfitSet.COOL_RAIN -> R.string.outfit_desc_cool_rain_boy
        OutfitSet.WIND -> R.string.outfit_desc_wind_boy
        OutfitSet.COLD -> R.string.outfit_desc_cold_boy
        OutfitSet.COLD_RAIN -> R.string.outfit_desc_cold_rain_boy
        OutfitSet.WINTER_LIGHT -> R.string.outfit_desc_winter_light_boy
        OutfitSet.FROST_HARD -> R.string.outfit_desc_frost_hard_boy
        OutfitSet.SNOWFALL -> R.string.outfit_desc_snowfall_boy
        OutfitSet.RAIN_WIND -> R.string.outfit_desc_rain_wind_boy
    }
    ChildGender.GIRL -> when (set) {
        OutfitSet.HEAT -> R.string.outfit_desc_heat_girl
        OutfitSet.WARM -> R.string.outfit_desc_warm_girl
        OutfitSet.WARM_RAIN -> R.string.outfit_desc_warm_rain_girl
        OutfitSet.COOL -> R.string.outfit_desc_cool_girl
        OutfitSet.COOL_RAIN -> R.string.outfit_desc_cool_rain_girl
        OutfitSet.WIND -> R.string.outfit_desc_wind_girl
        OutfitSet.COLD -> R.string.outfit_desc_cold_girl
        OutfitSet.COLD_RAIN -> R.string.outfit_desc_cold_rain_girl
        OutfitSet.WINTER_LIGHT -> R.string.outfit_desc_winter_light_girl
        OutfitSet.FROST_HARD -> R.string.outfit_desc_frost_hard_girl
        OutfitSet.SNOWFALL -> R.string.outfit_desc_snowfall_girl
        OutfitSet.RAIN_WIND -> R.string.outfit_desc_rain_wind_girl
    }
    ChildGender.BOTH -> throw IllegalArgumentException(
        "ChildGender.BOTH shows two children; describe the boy and the girl separately",
    )
}

/** The description for whoever the screen shows: one child, or both read one after the other. */
@Composable
fun outfitDescription(set: OutfitSet, gender: ChildGender): String = when (gender) {
    ChildGender.BOTH -> stringResource(
        R.string.outfit_desc_pair,
        stringResource(outfitDescriptionRes(set, ChildGender.BOY)),
        stringResource(outfitDescriptionRes(set, ChildGender.GIRL)),
    )
    else -> stringResource(outfitDescriptionRes(set, gender))
}
