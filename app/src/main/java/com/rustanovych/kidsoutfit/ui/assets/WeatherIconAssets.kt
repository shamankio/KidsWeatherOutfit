package com.rustanovych.kidsoutfit.ui.assets

import androidx.annotation.DrawableRes
import com.rustanovych.kidsoutfit.R

/*
 * The ONLY place that maps a WMO weather code (Open-Meteo `weather_code`) to an
 * `R.drawable.ic_weather_*` id. Unknown codes fall back to the cloudy icon rather than crashing.
 */

@DrawableRes
fun weatherIcon(weatherCode: Int): Int = when (weatherCode) {
    0 -> R.drawable.ic_weather_clear_day
    1, 2 -> R.drawable.ic_weather_partly_cloudy_day
    3 -> R.drawable.ic_weather_cloudy
    45, 48 -> R.drawable.ic_weather_fog
    51, 53, 55 -> R.drawable.ic_weather_drizzle
    56, 57, 66, 67 -> R.drawable.ic_weather_sleet
    61, 63, 65, 80, 81, 82 -> R.drawable.ic_weather_rain
    71, 73, 75, 77, 85 -> R.drawable.ic_weather_snow
    86 -> R.drawable.ic_weather_heavy_snow
    95, 96, 99 -> R.drawable.ic_weather_thunderstorms
    else -> R.drawable.ic_weather_cloudy
}
