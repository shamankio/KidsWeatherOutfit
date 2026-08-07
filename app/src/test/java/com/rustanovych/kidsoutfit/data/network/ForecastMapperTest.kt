package com.rustanovych.kidsoutfit.data.network

import com.rustanovych.kidsoutfit.data.network.dto.ForecastResponseDto
import com.rustanovych.kidsoutfit.data.network.dto.HourlyDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForecastMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parses a real Open-Meteo payload into today and tomorrow`() {
        val response = json.decodeFromString<ForecastResponseDto>(SAMPLE_RESPONSE)

        val bundle = response.toForecastBundle(fetchedAtEpochSeconds = 1_780_000_000L)

        assertEquals(listOf(0, 1), bundle.today.map { it.hour })
        assertEquals(listOf(0), bundle.tomorrow.map { it.hour })
        assertEquals(1_780_000_000L, bundle.fetchedAtEpochSeconds)

        val firstHour = bundle.today.first()
        assertEquals(17.4, firstHour.temperature, 0.001)
        assertEquals(16.2, firstHour.apparentTemperature, 0.001)
        assertEquals(0.3, firstHour.precipitationMm, 0.001)
        assertEquals(0.7, firstHour.snowfallCm, 0.001)
        assertEquals(11.5, firstHour.windSpeedKmh, 0.001)
        assertEquals(61, firstHour.weatherCode)
        assertEquals(2.15, firstHour.uvIndex, 0.001)
    }

    @Test
    fun `keeps hours ordered by hour of day`() {
        val response = forecastOf(
            time = listOf("2026-07-03T09:00", "2026-07-03T07:00", "2026-07-03T08:00"),
            apparentTemperature = listOf(9.0, 7.0, 8.0),
        )

        val bundle = response.toForecastBundle(fetchedAtEpochSeconds = 0L)

        assertEquals(listOf(7, 8, 9), bundle.today.map { it.hour })
    }

    @Test
    fun `drops hours without a temperature and defaults other missing values`() {
        val response = forecastOf(
            time = listOf("2026-07-03T07:00", "2026-07-03T08:00"),
            apparentTemperature = listOf(null, 8.0),
        )

        val bundle = response.toForecastBundle(fetchedAtEpochSeconds = 0L)

        assertEquals(listOf(8), bundle.today.map { it.hour })
        with(bundle.today.single()) {
            assertEquals(8.0, apparentTemperature, 0.001)
            // temperature_2m absent from the payload, so it falls back to apparent temperature.
            assertEquals(8.0, temperature, 0.001)
            assertEquals(0.0, precipitationMm, 0.001)
            assertEquals(0, weatherCode)
            assertEquals(0.0, uvIndex, 0.001)
        }
    }

    @Test
    fun `skips unparseable timestamps`() {
        val response = forecastOf(
            time = listOf("not-a-timestamp", "2026-07-03T08:00"),
            apparentTemperature = listOf(7.0, 8.0),
        )

        val bundle = response.toForecastBundle(fetchedAtEpochSeconds = 0L)

        assertEquals(listOf(8), bundle.today.map { it.hour })
    }

    @Test
    fun `yields empty days when the hourly block is empty`() {
        val bundle = forecastOf(time = emptyList(), apparentTemperature = emptyList())
            .toForecastBundle(fetchedAtEpochSeconds = 0L)

        assertTrue(bundle.today.isEmpty())
        assertTrue(bundle.tomorrow.isEmpty())
    }

    private fun forecastOf(
        time: List<String>,
        apparentTemperature: List<Double?>,
    ) = ForecastResponseDto(
        hourly = HourlyDto(time = time, apparentTemperature = apparentTemperature),
    )
}

/** Trimmed copy of a live `api.open-meteo.com/v1/forecast` response (three hours, two days). */
private const val SAMPLE_RESPONSE = """
{
  "latitude": 50.45,
  "longitude": 30.5,
  "generationtime_ms": 0.0959634780883789,
  "utc_offset_seconds": 10800,
  "timezone": "Europe/Kyiv",
  "timezone_abbreviation": "GMT+3",
  "elevation": 174.0,
  "hourly_units": {
    "time": "iso8601",
    "temperature_2m": "°C",
    "apparent_temperature": "°C",
    "precipitation": "mm",
    "snowfall": "cm",
    "wind_speed_10m": "km/h",
    "weather_code": "wmo code",
    "uv_index": ""
  },
  "hourly": {
    "time": ["2026-07-03T00:00", "2026-07-03T01:00", "2026-07-04T00:00"],
    "temperature_2m": [17.4, 16.9, 18.1],
    "apparent_temperature": [16.2, 15.8, 17.0],
    "precipitation": [0.3, 0.0, 0.0],
    "snowfall": [0.7, 0.0, 0.0],
    "wind_speed_10m": [11.5, 10.2, 9.8],
    "weather_code": [61, 3, 0],
    "uv_index": [2.15, 0.0, 0.0]
  }
}
"""
