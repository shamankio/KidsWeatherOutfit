package com.rustanovych.kidsoutfit.domain.model

/**
 * Hourly weather observation or forecast point, as sourced from Open-Meteo.
 *
 * @property hour Hour of day the snapshot applies to, in `0..23`.
 * @property temperature Air temperature at 2 m in degrees Celsius.
 * @property apparentTemperature "Feels like" temperature in degrees Celsius.
 * @property precipitationMm Total precipitation (rain + showers) in millimeters.
 * @property snowfallCm Snowfall amount in centimeters.
 * @property windSpeedKmh Wind speed in kilometers per hour.
 * @property weatherCode WMO weather interpretation code, as returned by Open-Meteo.
 * @property uvIndex UV index; `0.0` at night.
 */
data class WeatherSnapshot(
    val hour: Int,
    val temperature: Double,
    val apparentTemperature: Double,
    val precipitationMm: Double,
    val snowfallCm: Double,
    val windSpeedKmh: Double,
    val weatherCode: Int,
    val uvIndex: Double,
)
