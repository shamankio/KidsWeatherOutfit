package com.rustanovych.kidsoutfit.domain.repository

import com.rustanovych.kidsoutfit.domain.model.ForecastState
import kotlinx.coroutines.flow.Flow

/**
 * Provides hourly weather data. Implemented in `:app` against Open-Meteo, on top of a cache that
 * survives process death.
 *
 * Reading and refreshing are separate on purpose: [forecast] is a cold view of whatever is known
 * right now, and only [refresh] talks to the network. A screen collects the first and calls the
 * second on open and on pull-to-refresh.
 */
interface WeatherRepository {

    /**
     * The current forecast, re-emitting on every cache write and on every change of refresh
     * outcome. Never throws — failures surface as [ForecastState.Error].
     */
    fun forecast(): Flow<ForecastState>

    /**
     * Resolves the target coordinates, fetches a forecast for them, and stores it.
     *
     * @param force `true` skips the freshness check and always hits the network, which is what
     * pull-to-refresh wants. `false` re-fetches only when
     * [com.rustanovych.kidsoutfit.domain.engine.ForecastFreshness.shouldRefetch] says the cache no
     * longer covers the request.
     * @return success once a usable forecast is cached — including when the cached one was fresh
     * enough to keep — or the failure that stopped it (no location fix, no connectivity, bad
     * payload). The same failure is reflected in [forecast].
     */
    suspend fun refresh(force: Boolean = false): Result<Unit>
}
