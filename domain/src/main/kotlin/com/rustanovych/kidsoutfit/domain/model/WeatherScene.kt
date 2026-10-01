package com.rustanovych.kidsoutfit.domain.model

/** Overall look of the weather at a given hour, used to pick the home screen background. */
enum class WeatherScene {
    /** Clear sky during the day. */
    SUNNY,

    /** Overcast, fog, or any weather code without a dedicated scene. */
    CLOUDY,

    /** Drizzle, rain or rain showers. */
    RAIN,

    /** Thunderstorm. */
    STORM,

    /** Snowfall or snow showers. */
    SNOW,

    /** Clear or cloudy but severely cold. */
    FROST,

    /** Strong wind without precipitation. */
    WIND,

    /** Clear sky at night. */
    EVENING,
}
