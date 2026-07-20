package com.rustanovych.kidsoutfit.domain.model

/**
 * A named combination of clothing items that can be illustrated and recommended for a given
 * weather situation. Each set has a pre-rendered WebP illustration per [ChildGender].
 */
enum class OutfitSet {
    /** Very hot weather: minimal, breathable clothing. */
    HEAT,

    /** Warm and dry. */
    WARM,

    /** Warm but wet. */
    WARM_RAIN,

    /** Cool and dry. */
    COOL,

    /** Cool and wet. */
    COOL_RAIN,

    /** Noticeable wind, otherwise mild. */
    WIND,

    /** Cold and dry. */
    COLD,

    /** Cold and wet. */
    COLD_RAIN,

    /** Light winter conditions: cold with little or no snow. */
    WINTER_LIGHT,

    /** Severe frost. */
    FROST_HARD,

    /** Active snowfall. */
    SNOWFALL,

    /** Rain combined with strong wind. */
    RAIN_WIND,
}

/**
 * Relative warmth of this outfit's underlying temperature category, for comparing two sets
 * regardless of their rain/snow/wind modifier. Higher is warmer:
 * HEAT > WARM(_RAIN) > COOL(_RAIN)/WIND > COLD(_RAIN)/RAIN_WIND > WINTER_LIGHT > FROST_HARD/SNOWFALL.
 */
fun OutfitSet.warmthRank(): Int = when (this) {
    OutfitSet.HEAT -> 6
    OutfitSet.WARM, OutfitSet.WARM_RAIN -> 5
    OutfitSet.COOL, OutfitSet.COOL_RAIN, OutfitSet.WIND -> 4
    OutfitSet.COLD, OutfitSet.COLD_RAIN, OutfitSet.RAIN_WIND -> 3
    OutfitSet.WINTER_LIGHT -> 2
    OutfitSet.FROST_HARD, OutfitSet.SNOWFALL -> 1
}
