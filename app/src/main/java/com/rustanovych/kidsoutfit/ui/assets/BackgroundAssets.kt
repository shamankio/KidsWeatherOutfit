package com.rustanovych.kidsoutfit.ui.assets

import androidx.annotation.DrawableRes
import com.rustanovych.kidsoutfit.R
import com.rustanovych.kidsoutfit.domain.engine.WeatherSceneSelector
import com.rustanovych.kidsoutfit.domain.model.WeatherScene
import com.rustanovych.kidsoutfit.domain.model.WeatherSnapshot

/*
 * The ONLY place that maps weather to an `R.drawable.bg_*` id. The decision itself (WMO code,
 * wind, frost, time of day) lives in [WeatherSceneSelector] so it can be unit-tested without
 * Android; this file stays a thin scene -> drawable lookup.
 */

/** Background for the weather at [snapshot]'s hour. */
@DrawableRes
fun backgroundFor(snapshot: WeatherSnapshot): Int =
    backgroundDrawable(WeatherSceneSelector.select(snapshot))

@DrawableRes
fun backgroundDrawable(scene: WeatherScene): Int = when (scene) {
    WeatherScene.SUNNY -> R.drawable.bg_sunny
    WeatherScene.CLOUDY -> R.drawable.bg_cloudy
    WeatherScene.RAIN -> R.drawable.bg_rain
    WeatherScene.STORM -> R.drawable.bg_storm
    WeatherScene.SNOW -> R.drawable.bg_snow
    WeatherScene.FROST -> R.drawable.bg_frost
    WeatherScene.WIND -> R.drawable.bg_wind
    WeatherScene.EVENING -> R.drawable.bg_evening
}
