package com.rustanovych.kidsoutfit.data.network

import com.rustanovych.kidsoutfit.data.network.dto.ForecastResponseDto
import com.rustanovych.kidsoutfit.data.network.dto.HourlyDto
import com.rustanovych.kidsoutfit.domain.model.ForecastBundle
import com.rustanovych.kidsoutfit.domain.model.WeatherSnapshot
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

/**
 * Converts the parallel-array [ForecastResponseDto] into a [ForecastBundle] split by local day.
 *
 * Timestamps are local to the forecast location (we always request `timezone=auto`), so the first
 * date present is today at that location and the second is tomorrow — no device clock involved.
 * Hours whose timestamp or apparent temperature is missing are dropped; a missing value for any
 * other variable falls back to a neutral default rather than discarding the whole hour.
 */
fun ForecastResponseDto.toForecastBundle(fetchedAtEpochSeconds: Long): ForecastBundle {
    val byDate = sortedMapOf<LocalDate, MutableList<WeatherSnapshot>>()

    hourly.time.forEachIndexed { index, rawTimestamp ->
        val timestamp = parseLocalDateTime(rawTimestamp) ?: return@forEachIndexed
        val snapshot = hourly.snapshotAt(index, timestamp.hour) ?: return@forEachIndexed
        byDate.getOrPut(timestamp.toLocalDate()) { mutableListOf() }.add(snapshot)
    }

    val days = byDate.values.toList()
    return ForecastBundle(
        today = days.getOrElse(0) { emptyList() }.sortedBy { it.hour },
        tomorrow = days.getOrElse(1) { emptyList() }.sortedBy { it.hour },
        fetchedAtEpochSeconds = fetchedAtEpochSeconds,
    )
}

/** Builds the snapshot at [index], or `null` when the hour carries no usable temperature. */
private fun HourlyDto.snapshotAt(index: Int, hour: Int): WeatherSnapshot? {
    val apparentTemperature = apparentTemperature.getOrNull(index)
        ?: temperature.getOrNull(index)
        ?: return null
    return WeatherSnapshot(
        hour = hour,
        temperature = temperature.getOrNull(index) ?: apparentTemperature,
        apparentTemperature = apparentTemperature,
        precipitationMm = precipitation.getOrNull(index) ?: 0.0,
        snowfallCm = snowfall.getOrNull(index) ?: 0.0,
        windSpeedKmh = windSpeed.getOrNull(index) ?: 0.0,
        weatherCode = weatherCode.getOrNull(index) ?: 0,
        uvIndex = uvIndex.getOrNull(index) ?: 0.0,
    )
}

/** Open-Meteo emits `2026-07-03T14:00`, which is [LocalDateTime]'s ISO format without seconds. */
private fun parseLocalDateTime(raw: String): LocalDateTime? = try {
    LocalDateTime.parse(raw)
} catch (_: DateTimeParseException) {
    null
}
