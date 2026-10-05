package com.rustanovych.kidsoutfit.ui.assets

import androidx.annotation.DrawableRes
import com.rustanovych.kidsoutfit.R
import com.rustanovych.kidsoutfit.domain.model.ChildGender
import com.rustanovych.kidsoutfit.domain.model.OutfitSet

/*
 * The ONLY place that maps an [OutfitSet] + [ChildGender] to an `R.drawable` id. Nothing outside
 * this file may reference `R.drawable.outfit_*`.
 *
 * Dedicated thumbnails (`outfit_{set}_{gender}_thumb.webp`, square) exist only for the sets whose
 * default upper-body crop of the full figure is too tight to tell from their neighbours; see
 * [outfitThumbDrawables].
 */

/**
 * Illustration for a single child.
 *
 * @throws IllegalArgumentException for [ChildGender.BOTH]; use [outfitDrawables] instead.
 */
@DrawableRes
fun outfitDrawable(set: OutfitSet, gender: ChildGender): Int = when (gender) {
    ChildGender.BOY -> boyDrawable(set)
    ChildGender.GIRL -> girlDrawable(set)
    ChildGender.BOTH -> throw IllegalArgumentException(
        "ChildGender.BOTH has two illustrations; use outfitDrawables(set, gender)",
    )
}

/**
 * Illustrations to show for [gender]: boy then girl for [ChildGender.BOTH], otherwise a single
 * element.
 */
fun outfitDrawables(set: OutfitSet, gender: ChildGender): List<Int> = when (gender) {
    ChildGender.BOTH -> listOf(boyDrawable(set), girlDrawable(set))
    ChildGender.BOY, ChildGender.GIRL -> listOf(outfitDrawable(set, gender))
}

/**
 * What a small card shows for [gender]: like [outfitDrawables], but a single child gets the dedicated
 * square thumbnail where there is one (warm and cool, whose full-figure crop is little more than a
 * face). A pair keeps the full figures, which fit side by side without a thumbnail.
 */
fun outfitThumbDrawables(set: OutfitSet, gender: ChildGender): List<Int> = when (gender) {
    ChildGender.BOTH -> outfitDrawables(set, gender)
    ChildGender.BOY -> listOf(boyThumbDrawable(set) ?: boyDrawable(set))
    ChildGender.GIRL -> listOf(girlThumbDrawable(set) ?: girlDrawable(set))
}

@DrawableRes
private fun boyThumbDrawable(set: OutfitSet): Int? = when (set) {
    OutfitSet.WARM -> R.drawable.outfit_warm_boy_thumb
    OutfitSet.COOL -> R.drawable.outfit_cool_boy_thumb
    else -> null
}

@DrawableRes
private fun girlThumbDrawable(set: OutfitSet): Int? = when (set) {
    OutfitSet.WARM -> R.drawable.outfit_warm_girl_thumb
    OutfitSet.COOL -> R.drawable.outfit_cool_girl_thumb
    else -> null
}

@DrawableRes
private fun boyDrawable(set: OutfitSet): Int = when (set) {
    OutfitSet.HEAT -> R.drawable.outfit_heat_boy
    OutfitSet.WARM -> R.drawable.outfit_warm_boy
    OutfitSet.WARM_RAIN -> R.drawable.outfit_warm_rain_boy
    OutfitSet.COOL -> R.drawable.outfit_cool_boy
    OutfitSet.COOL_RAIN -> R.drawable.outfit_cool_rain_boy
    OutfitSet.WIND -> R.drawable.outfit_wind_boy
    OutfitSet.COLD -> R.drawable.outfit_cold_boy
    OutfitSet.COLD_RAIN -> R.drawable.outfit_cold_rain_boy
    OutfitSet.WINTER_LIGHT -> R.drawable.outfit_winter_light_boy
    OutfitSet.FROST_HARD -> R.drawable.outfit_frost_hard_boy
    OutfitSet.SNOWFALL -> R.drawable.outfit_snowfall_boy
    OutfitSet.RAIN_WIND -> R.drawable.outfit_rain_wind_boy
}

@DrawableRes
private fun girlDrawable(set: OutfitSet): Int = when (set) {
    OutfitSet.HEAT -> R.drawable.outfit_heat_girl
    OutfitSet.WARM -> R.drawable.outfit_warm_girl
    OutfitSet.WARM_RAIN -> R.drawable.outfit_warm_rain_girl
    OutfitSet.COOL -> R.drawable.outfit_cool_girl
    OutfitSet.COOL_RAIN -> R.drawable.outfit_cool_rain_girl
    OutfitSet.WIND -> R.drawable.outfit_wind_girl
    OutfitSet.COLD -> R.drawable.outfit_cold_girl
    OutfitSet.COLD_RAIN -> R.drawable.outfit_cold_rain_girl
    OutfitSet.WINTER_LIGHT -> R.drawable.outfit_winter_light_girl
    OutfitSet.FROST_HARD -> R.drawable.outfit_frost_hard_girl
    OutfitSet.SNOWFALL -> R.drawable.outfit_snowfall_girl
    OutfitSet.RAIN_WIND -> R.drawable.outfit_rain_wind_girl
}
