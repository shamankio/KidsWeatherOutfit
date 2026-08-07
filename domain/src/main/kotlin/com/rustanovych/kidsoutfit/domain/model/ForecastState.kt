package com.rustanovych.kidsoutfit.domain.model

/**
 * What the app currently knows about the forecast.
 *
 * [Error] deliberately carries the cached bundle rather than replacing it: a refresh that fails
 * while a previous forecast is still on disk should leave the user looking at the stale data with
 * a badge, not at an empty screen.
 */
sealed interface ForecastState {

    /** Nothing cached yet and no attempt has failed — the first fetch is still outstanding. */
    data object Loading : ForecastState

    /** A forecast is available. It may still be stale; see [ForecastBundle.fetchedAtEpochSeconds]. */
    data class Ready(val bundle: ForecastBundle) : ForecastState

    /**
     * The last refresh failed.
     *
     * @property cached The last successfully fetched forecast, or `null` when none was ever
     * stored — in which case the UI has nothing to show but the error.
     */
    data class Error(val cached: ForecastBundle?) : ForecastState
}
