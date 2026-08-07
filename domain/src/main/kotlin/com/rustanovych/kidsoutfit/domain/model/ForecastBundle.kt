package com.rustanovych.kidsoutfit.domain.model

/**
 * Two days of hourly forecast, as fetched from the weather provider.
 *
 * Both lists are ordered by hour and contain only the hours the provider actually returned data
 * for, so they may be shorter than 24 entries (or empty, if the provider returned nothing usable).
 *
 * @property today Hourly snapshots for the current local day at the forecast location.
 * @property tomorrow Hourly snapshots for the following local day.
 * @property fetchedAtEpochSeconds Wall-clock time the forecast was fetched, in epoch seconds,
 * used to decide when the cached bundle is stale.
 */
data class ForecastBundle(
    val today: List<WeatherSnapshot>,
    val tomorrow: List<WeatherSnapshot>,
    val fetchedAtEpochSeconds: Long,
)
