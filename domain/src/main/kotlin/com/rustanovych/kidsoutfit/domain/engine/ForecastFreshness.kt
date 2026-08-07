package com.rustanovych.kidsoutfit.domain.engine

import com.rustanovych.kidsoutfit.domain.model.Coordinates

/**
 * Decides when a cached forecast has to be replaced by a network call.
 *
 * Pure so the policy can be tested without a network, a clock, or a store — the repository in
 * `:app` only feeds it the cache metadata it already has.
 */
object ForecastFreshness {

    /**
     * How long a cached forecast is served without re-fetching. Open-Meteo updates its hourly
     * model roughly this often, so a shorter window would spend requests on identical payloads.
     */
    const val MAX_AGE_SECONDS: Long = 60 * 60

    /**
     * How far the device may drift before the cached forecast is considered to be for the wrong
     * place. Open-Meteo resolves to grid cells several kilometers wide, so smaller moves generally
     * return the same numbers.
     */
    const val MAX_DISTANCE_METERS: Double = 5_000.0

    /**
     * Whether a cached forecast must be re-fetched.
     *
     * Returns `true` when the cache has reached [MAX_AGE_SECONDS], when the target has moved
     * [MAX_DISTANCE_METERS] or more from where the cache was fetched, or when either input cannot
     * be trusted — an unknown fetch location, or a [nowEpochSeconds] that precedes
     * [fetchedAtEpochSeconds], which is what a user rolling the device clock backwards looks like
     * from here. Erring towards re-fetching costs one request; erring the other way pins the user
     * to a forecast that never updates.
     *
     * @param fetchedAtEpochSeconds When the cached bundle was fetched, in epoch seconds.
     * @param nowEpochSeconds Current wall-clock time, in epoch seconds.
     * @param oldCoordinates Where the cached bundle was fetched for, or `null` if not recorded.
     * @param newCoordinates Where the forecast is wanted now.
     */
    fun shouldRefetch(
        fetchedAtEpochSeconds: Long,
        nowEpochSeconds: Long,
        oldCoordinates: Coordinates?,
        newCoordinates: Coordinates,
    ): Boolean {
        if (oldCoordinates == null) return true

        val ageSeconds = nowEpochSeconds - fetchedAtEpochSeconds
        if (ageSeconds < 0 || ageSeconds >= MAX_AGE_SECONDS) return true

        return oldCoordinates.distanceMetersTo(newCoordinates) >= MAX_DISTANCE_METERS
    }
}
