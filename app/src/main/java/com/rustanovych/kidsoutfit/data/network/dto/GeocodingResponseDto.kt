package com.rustanovych.kidsoutfit.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Top-level Open-Meteo `/v1/search` response.
 *
 * [results] is absent — not an empty array — when nothing matches the query, hence the nullable
 * default.
 */
@Serializable
data class GeocodingResponseDto(
    val results: List<GeocodingResultDto>? = null,
    @SerialName("generationtime_ms") val generationTimeMs: Double? = null,
)

/**
 * One geocoding hit. Only the identity and coordinate fields are guaranteed; the administrative
 * labels are missing for places that have no such parent (city-states, small islands).
 *
 * @property admin1 First-level administrative area, mapped to `Place.region`.
 */
@Serializable
data class GeocodingResultDto(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val admin1: String? = null,
)
