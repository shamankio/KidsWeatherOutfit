package com.rustanovych.kidsoutfit.domain.model

/**
 * A location the user can pick as the forecast target, as returned by a geocoding search.
 *
 * [region] and [country] exist only to disambiguate same-named places in the result list — many
 * queries return several "Springfield" entries that differ solely by those two fields. Both are
 * optional because the geocoder omits them for places that have no administrative parent.
 *
 * @property name Localized place name, e.g. `Kyiv`.
 * @property region First-level administrative area (state, oblast, region), or `null`.
 * @property country Country name, or `null`.
 * @property latitude WGS84 latitude in degrees.
 * @property longitude WGS84 longitude in degrees.
 */
data class Place(
    val name: String,
    val region: String?,
    val country: String?,
    val latitude: Double,
    val longitude: Double,
)
