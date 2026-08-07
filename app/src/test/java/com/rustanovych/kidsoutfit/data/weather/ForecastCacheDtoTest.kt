package com.rustanovych.kidsoutfit.data.weather

import com.rustanovych.kidsoutfit.domain.model.Coordinates
import com.rustanovych.kidsoutfit.domain.model.ForecastBundle
import com.rustanovych.kidsoutfit.domain.model.WeatherSnapshot
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mirrors the instance Koin builds in `networkModule`. */
private val json = Json { ignoreUnknownKeys = true }

private val KYIV = Coordinates(lat = 50.45466, lon = 30.5238)

class ForecastCacheDtoTest {

    @Test
    fun `survives a round trip through JSON`() {
        val bundle = ForecastBundle(
            today = listOf(snapshot(hour = 7), snapshot(hour = 8)),
            tomorrow = listOf(snapshot(hour = 7)),
            fetchedAtEpochSeconds = 1_800_000_000L,
        )

        val restored = roundTrip(bundle, KYIV)

        assertEquals(bundle, restored.bundle)
        assertEquals(KYIV, restored.coordinates)
    }

    @Test
    fun `preserves every weather field`() {
        val hour = WeatherSnapshot(
            hour = 13,
            temperature = -4.5,
            apparentTemperature = -9.25,
            precipitationMm = 1.75,
            snowfallCm = 0.6,
            windSpeedKmh = 32.5,
            weatherCode = 73,
            uvIndex = 2.4,
        )
        val bundle = ForecastBundle(listOf(hour), emptyList(), 1_800_000_000L)

        assertEquals(hour, roundTrip(bundle, KYIV).bundle.today.single())
    }

    @Test
    fun `round trips an empty forecast`() {
        val bundle = ForecastBundle(emptyList(), emptyList(), 1_800_000_000L)

        val restored = roundTrip(bundle, KYIV)

        assertTrue(restored.bundle.today.isEmpty())
        assertTrue(restored.bundle.tomorrow.isEmpty())
        assertEquals(1_800_000_000L, restored.bundle.fetchedAtEpochSeconds)
    }

    @Test
    fun `reads a payload written before the hour lists existed`() {
        val legacy = """{"lat":50.45466,"lon":30.5238,"fetchedAtEpochSeconds":1800000000}"""

        val restored = json.decodeFromString<CachedForecastDto>(legacy).toCachedForecast()

        assertTrue(restored.bundle.today.isEmpty())
        assertEquals(KYIV, restored.coordinates)
    }

    @Test
    fun `defaults missing weather variables instead of dropping the hour`() {
        val partial = """
            {"lat":50.45466,"lon":30.5238,"fetchedAtEpochSeconds":1800000000,
             "today":[{"hour":9,"temperature":3.5}]}
        """.trimIndent()

        val hour = json.decodeFromString<CachedForecastDto>(partial).toCachedForecast()
            .bundle.today.single()

        assertEquals(9, hour.hour)
        assertEquals(3.5, hour.temperature, 0.0)
        assertEquals(0.0, hour.uvIndex, 0.0)
        assertEquals(0, hour.weatherCode)
    }

    private fun roundTrip(bundle: ForecastBundle, coordinates: Coordinates): CachedForecast {
        val encoded = json.encodeToString(bundle.toCacheDto(coordinates))
        return json.decodeFromString<CachedForecastDto>(encoded).toCachedForecast()
    }

    private fun snapshot(hour: Int) = WeatherSnapshot(
        hour = hour,
        temperature = 12.0,
        apparentTemperature = 10.0,
        precipitationMm = 0.0,
        snowfallCm = 0.0,
        windSpeedKmh = 5.0,
        weatherCode = 1,
        uvIndex = 3.0,
    )
}
