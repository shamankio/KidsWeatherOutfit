package com.rustanovych.kidsoutfit.data.network

import com.rustanovych.kidsoutfit.data.network.dto.GeocodingResponseDto
import com.rustanovych.kidsoutfit.data.network.dto.GeocodingResultDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class GeocodingDataSourceTest {

    @Test
    fun `maps a successful search to places`() = runTest {
        val service = FakeGeocodingService(
            response = GeocodingResponseDto(
                results = listOf(
                    GeocodingResultDto(
                        id = 703448,
                        name = "Kyiv",
                        latitude = 50.45466,
                        longitude = 30.5238,
                        country = "Ukraine",
                        admin1 = "Kyiv City",
                    ),
                ),
            ),
        )

        val result = GeocodingDataSource(service).searchPlaces("Kyiv")

        val place = result.getOrThrow().single()
        assertEquals("Kyiv", place.name)
        assertEquals("Kyiv City", place.region)
        assertEquals("Ukraine", place.country)
    }

    @Test
    fun `sends the query with the configured count`() = runTest {
        val service = FakeGeocodingService()

        GeocodingDataSource(service, language = { "en" }).searchPlaces("Lviv")

        assertEquals(listOf("Lviv"), service.queries)
        assertEquals(GeocodingService.RESULT_COUNT, service.lastCount)
    }

    @Test
    fun `searches in the device language`() = runTest {
        // The geocoder searches one language's name index at a time, so a Cyrillic query only
        // matches when the language travels with it.
        val service = FakeGeocodingService()

        GeocodingDataSource(service, language = { "uk" }).searchPlaces("Київ")

        assertEquals(listOf("Київ"), service.queries)
        assertEquals("uk", service.lastLanguage)
    }

    @Test
    fun `re-reads the language on every call`() = runTest {
        val service = FakeGeocodingService()
        var current = "uk"
        val dataSource = GeocodingDataSource(service, language = { current })

        dataSource.searchPlaces("Київ")
        current = "de"
        dataSource.searchPlaces("Berlin")

        assertEquals("de", service.lastLanguage)
    }

    @Test
    fun `falls back to English when the device reports no language`() = runTest {
        val service = FakeGeocodingService()

        GeocodingDataSource(service, language = { "" }).searchPlaces("Lviv")

        assertEquals(GeocodingService.LANGUAGE_FALLBACK, service.lastLanguage)
    }

    @Test
    fun `returns empty without calling the API for a query shorter than two characters`() =
        runTest {
            val service = FakeGeocodingService()
            val dataSource = GeocodingDataSource(service)

            listOf("", "K", "  ", " a ").forEach { query ->
                assertTrue(query, dataSource.searchPlaces(query).getOrThrow().isEmpty())
            }

            assertTrue(service.queries.isEmpty())
        }

    @Test
    fun `trims the query before searching`() = runTest {
        val service = FakeGeocodingService()

        GeocodingDataSource(service).searchPlaces("  Kyiv  ")

        assertEquals(listOf("Kyiv"), service.queries)
    }

    @Test
    fun `wraps a transport failure in a failed result`() = runTest {
        val failure = IOException("offline")
        val service = FakeGeocodingService(error = failure)

        val result = GeocodingDataSource(service).searchPlaces("Kyiv")

        assertTrue(result.isFailure)
        assertSame(failure, result.exceptionOrNull())
    }
}

/** Records what the data source asked for and replays a canned response or error. */
private class FakeGeocodingService(
    private val response: GeocodingResponseDto = GeocodingResponseDto(),
    private val error: Throwable? = null,
) : GeocodingService {

    val queries = mutableListOf<String>()
    var lastCount: Int? = null
    var lastLanguage: String? = null

    override suspend fun searchPlaces(
        name: String,
        count: Int,
        language: String,
    ): GeocodingResponseDto {
        queries += name
        lastCount = count
        lastLanguage = language
        error?.let { throw it }
        return response
    }
}
