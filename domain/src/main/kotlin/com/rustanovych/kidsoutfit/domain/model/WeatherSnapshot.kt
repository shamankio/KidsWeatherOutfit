package com.rustanovych.kidsoutfit.domain.model

/**
 * Hourly weather observation or forecast point, as sourced from Open-Meteo.
 *
 * @property hour Hour of day the snapshot applies to, in `0..23`.
 * @property apparentTemperature "Feels like" temperature in degrees Celsius.
 * @property precipitationMm Total precipitation (rain + showers) in millimeters.
 * @property snowfallCm Snowfall amount in centimeters.
 * @property windSpeedKmh Wind speed in kilometers per hour.
 * @property weatherCode WMO weather interpretation code, as returned by Open-Meteo.
 */
data class WeatherSnapshot(
    val hour: Int,
    val apparentTemperature: Double,
    val precipitationMm: Double,
    val snowfallCm: Double,
    val windSpeedKmh: Double,
    val weatherCode: Int,
)
