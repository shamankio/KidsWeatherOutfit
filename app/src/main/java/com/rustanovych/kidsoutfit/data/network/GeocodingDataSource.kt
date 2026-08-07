package com.rustanovych.kidsoutfit.data.network

import com.rustanovych.kidsoutfit.domain.model.Place
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.util.Locale

/**
 * Remote source of place lookups. Owns the call to [GeocodingService] and turns transport, HTTP,
 * and payload failures into a failed [Result] so callers never have to catch.
 *
 * @param language Supplies the search language on every call — read lazily because the user can
 * change the device language while the process lives. It has to follow the device rather than be
 * pinned to English: the geocoder searches one language's name index at a time, so a hardcoded
 * `en` makes a Ukrainian parent typing `Київ` get no results at all. See
 * [GeocodingService.searchPlaces].
 */
class GeocodingDataSource(
    private val service: GeocodingService,
    private val language: () -> String = { Locale.getDefault().language },
) {

    /**
     * Searches places matching [query].
     *
     * A query shorter than [GeocodingService.MIN_QUERY_LENGTH] characters (after trimming) is
     * answered with an empty success and no network call — this runs on every keystroke, and the
     * API rejects such terms anyway.
     *
     * Returns a failed [Result] on connectivity problems ([IOException]), non-2xx responses
     * ([HttpException]), or an unreadable payload ([SerializationException]). Coroutine
     * cancellation is not swallowed.
     */
    suspend fun searchPlaces(query: String): Result<List<Place>> {
        val term = query.trim()
        if (term.length < GeocodingService.MIN_QUERY_LENGTH) return Result.success(emptyList())

        return try {
            val response = service.searchPlaces(
                name = term,
                count = GeocodingService.RESULT_COUNT,
                // Locale.getDefault().language is empty for a locale with no language set.
                language = language().ifBlank { GeocodingService.LANGUAGE_FALLBACK },
            )
            Result.success(response.toPlaces())
        } catch (e: HttpException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: SerializationException) {
            Result.failure(e)
        }
    }
}
