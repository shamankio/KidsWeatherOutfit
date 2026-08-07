package com.rustanovych.kidsoutfit.domain.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Mean Earth radius in meters, per the WGS84 sphere approximation. */
private const val EARTH_RADIUS_METERS = 6_371_008.8

/**
 * A geographic point the forecast can be requested for.
 *
 * Unlike [Place], this carries no naming or administrative context — it is the bare pair the
 * forecast API needs, whether it came from a picked place or from device location.
 *
 * @property lat WGS84 latitude in degrees.
 * @property lon WGS84 longitude in degrees.
 */
data class Coordinates(
    val lat: Double,
    val lon: Double,
) {

    /**
     * Great-circle distance to [other] in meters, via the haversine formula.
     *
     * A sphere is close enough here: the only caller compares the result against a kilometer-scale
     * threshold, where the ellipsoid correction is well under the error of a coarse location fix.
     */
    fun distanceMetersTo(other: Coordinates): Double {
        val deltaLat = (other.lat - lat).toRadians()
        val deltaLon = (other.lon - lon).toRadians()
        val a = sin(deltaLat / 2).squared() +
            cos(lat.toRadians()) * cos(other.lat.toRadians()) * sin(deltaLon / 2).squared()
        // Clamped because rounding can push `a` a hair above 1 for antipodal points, and asin
        // would then return NaN.
        return 2 * EARTH_RADIUS_METERS * asin(min(1.0, sqrt(a)))
    }
}

private fun Double.toRadians(): Double = this * kotlin.math.PI / 180.0

private fun Double.squared(): Double = this * this
