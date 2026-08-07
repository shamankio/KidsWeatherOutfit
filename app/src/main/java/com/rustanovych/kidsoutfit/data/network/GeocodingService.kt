package com.rustanovych.kidsoutfit.data.network

import com.rustanovych.kidsoutfit.data.network.dto.GeocodingResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo geocoding API, used to turn a typed city name into coordinates. Free, no API key.
 *
 * Base URL is `https://geocoding-api.open-meteo.com/` — a different host from the forecast API;
 * see https://open-meteo.com/en/docs/geocoding-api.
 */
interface GeocodingService {

    /**
     * Searches places by name.
     *
     * @param name Search term. The API rejects a term shorter than [MIN_QUERY_LENGTH] characters,
     * so callers are expected to filter those out before calling.
     * @param count Maximum number of results; we use [RESULT_COUNT].
     * @param language Picks **which name index is searched**, not just the language results come
     * back in — `name=Київ&language=en` matches nothing, while `language=uk` finds it (and still
     * matches `Kyiv`). An unrecognized code falls back to the API's default rather than failing,
     * so passing the device language through is safe.
     */
    @GET("v1/search")
    suspend fun searchPlaces(
        @Query("name") name: String,
        @Query("count") count: Int,
        @Query("language") language: String,
    ): GeocodingResponseDto

    companion object {
        const val BASE_URL: String = "https://geocoding-api.open-meteo.com/"

        /** Enough hits to disambiguate same-named cities without overflowing the picker. */
        const val RESULT_COUNT: Int = 8

        /** Used when the device reports no usable language. */
        const val LANGUAGE_FALLBACK: String = "en"

        /** Shorter terms are rejected by the API, so we never send them. */
        const val MIN_QUERY_LENGTH: Int = 2
    }
}
