package com.rustanovych.kidsoutfit.data.network

import com.rustanovych.kidsoutfit.data.network.dto.GeocodingResponseDto
import com.rustanovych.kidsoutfit.domain.model.Place
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parses a real geocoding payload into places, preserving rank order`() {
        val response = json.decodeFromString<GeocodingResponseDto>(SAMPLE_RESPONSE)

        val places = response.toPlaces()

        assertEquals(listOf("Kyiv", "Kyiv"), places.map { it.name })
        assertEquals(
            Place(
                name = "Kyiv",
                region = "Kyiv City",
                country = "Ukraine",
                latitude = 50.45466,
                longitude = 30.5238,
            ),
            places.first(),
        )
        assertEquals("Kyiv Oblast", places[1].region)
    }

    @Test
    fun `leaves region and country null when the hit has no administrative parent`() {
        val response = json.decodeFromString<GeocodingResponseDto>(MINIMAL_RESPONSE)

        val place = response.toPlaces().single()

        assertEquals("Singapore", place.name)
        assertNull(place.region)
        assertNull(place.country)
        assertEquals(1.28967, place.latitude, 0.00001)
        assertEquals(103.85007, place.longitude, 0.00001)
    }

    @Test
    fun `yields no places when the API omits the results block`() {
        val response = json.decodeFromString<GeocodingResponseDto>(NO_RESULTS_RESPONSE)

        assertTrue(response.toPlaces().isEmpty())
    }
}

/** Trimmed copy of a live `geocoding-api.open-meteo.com/v1/search?name=Kyiv` response. */
private const val SAMPLE_RESPONSE = """
{
  "results": [
    {
      "id": 703448,
      "name": "Kyiv",
      "latitude": 50.45466,
      "longitude": 30.5238,
      "elevation": 187.0,
      "feature_code": "PPLC",
      "country_code": "UA",
      "admin1": "Kyiv City",
      "timezone": "Europe/Kyiv",
      "population": 2797553,
      "country_id": 690791,
      "country": "Ukraine",
      "admin1_id": 703447
    },
    {
      "id": 703447,
      "name": "Kyiv",
      "latitude": 50.5,
      "longitude": 30.5,
      "feature_code": "ADM1",
      "country_code": "UA",
      "admin1": "Kyiv Oblast",
      "timezone": "Europe/Kyiv",
      "country": "Ukraine"
    }
  ],
  "generationtime_ms": 0.6949902
}
"""

/** A hit carrying only the fields the API always returns. */
private const val MINIMAL_RESPONSE = """
{
  "results": [
    { "id": 1880252, "name": "Singapore", "latitude": 1.28967, "longitude": 103.85007 }
  ]
}
"""

/** Open-Meteo drops `results` entirely instead of returning an empty array. */
private const val NO_RESULTS_RESPONSE = """{ "generationtime_ms": 0.28 }"""
