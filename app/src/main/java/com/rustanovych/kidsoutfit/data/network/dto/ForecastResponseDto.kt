package com.rustanovych.kidsoutfit.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Top-level Open-Meteo `/v1/forecast` response.
 *
 * Only [hourly] is required; every other field is metadata we tolerate but do not depend on.
 */
@Serializable
data class ForecastResponseDto(
    val hourly: HourlyDto,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val elevation: Double? = null,
    @SerialName("generationtime_ms") val generationTimeMs: Double? = null,
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int? = null,
    val timezone: String? = null,
    @SerialName("timezone_abbreviation") val timezoneAbbreviation: String? = null,
    @SerialName("hourly_units") val hourlyUnits: Map<String, String>? = null,
)

/**
 * Hourly block of the forecast. Open-Meteo returns *parallel arrays*: index `i` of every list
 * describes the timestamp at `time[i]`.
 *
 * Values are nullable because Open-Meteo emits `null` for hours a model has no data for; the
 * mapper decides which nulls are fatal for a given hour and which fall back to a default.
 */
@Serializable
data class HourlyDto(
    /** Local ISO 8601 timestamps without offset, e.g. `2026-07-03T14:00`. */
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m") val temperature: List<Double?> = emptyList(),
    @SerialName("apparent_temperature") val apparentTemperature: List<Double?> = emptyList(),
    val precipitation: List<Double?> = emptyList(),
    val snowfall: List<Double?> = emptyList(),
    @SerialName("wind_speed_10m") val windSpeed: List<Double?> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
    @SerialName("uv_index") val uvIndex: List<Double?> = emptyList(),
)
