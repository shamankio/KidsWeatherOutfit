package com.rustanovych.kidsoutfit.data.weather

import com.rustanovych.kidsoutfit.data.location.LocationProvider
import com.rustanovych.kidsoutfit.data.network.OpenMeteoDataSource
import com.rustanovych.kidsoutfit.domain.engine.ForecastFreshness
import com.rustanovych.kidsoutfit.domain.model.Coordinates
import com.rustanovych.kidsoutfit.domain.model.ForecastState
import com.rustanovych.kidsoutfit.domain.model.LocationMode
import com.rustanovych.kidsoutfit.domain.repository.SettingsRepository
import com.rustanovych.kidsoutfit.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

/**
 * Serves the forecast from a disk cache and refreshes it from Open-Meteo.
 *
 * The cache is the single source of truth for [forecast]: a refresh never emits anything itself,
 * it only writes, and the flow re-emits off the store. That keeps the screen consistent across
 * process death, and means a failed refresh leaves the previous forecast exactly where it was.
 *
 * @param nowEpochSeconds Injected so the freshness window can be exercised without waiting.
 */
class WeatherRepositoryImpl(
    private val cache: ForecastCache,
    private val remote: OpenMeteoDataSource,
    private val settings: SettingsRepository,
    private val locationProvider: LocationProvider,
    private val nowEpochSeconds: () -> Long = { System.currentTimeMillis() / 1000 },
) : WeatherRepository {

    /** Serializes refreshes so a pull-to-refresh landing mid-fetch cannot double-spend a request. */
    private val refreshLock = Mutex()

    /** What stopped the last refresh, or `null` if it succeeded (or none has run yet). */
    private val lastFailure = MutableStateFlow<Throwable?>(null)

    override fun forecast(): Flow<ForecastState> =
        combine(cache.entry, lastFailure) { cached, failure ->
            when {
                // Stale data beats an empty screen, so the cache rides along with the error.
                failure != null -> ForecastState.Error(cached?.bundle)
                cached != null -> ForecastState.Ready(cached.bundle)
                else -> ForecastState.Loading
            }
        }.distinctUntilChanged()

    override suspend fun refresh(force: Boolean): Result<Unit> = refreshLock.withLock {
        val coordinates = resolveCoordinates()
            .getOrElse { return@withLock fail(it) }

        val cached = cache.entry.first()
        val mustRefetch = force || cached == null || ForecastFreshness.shouldRefetch(
            fetchedAtEpochSeconds = cached.bundle.fetchedAtEpochSeconds,
            nowEpochSeconds = nowEpochSeconds(),
            oldCoordinates = cached.coordinates,
            newCoordinates = coordinates,
        )
        if (!mustRefetch) {
            // The cache already answers this request; whatever failed before no longer matters.
            lastFailure.value = null
            return@withLock Result.success(Unit)
        }

        val bundle = remote.fetchForecast(coordinates.lat, coordinates.lon)
            .getOrElse { return@withLock fail(it) }

        try {
            cache.save(bundle, coordinates)
        } catch (e: IOException) {
            // Nothing was persisted, so `forecast()` will not emit it — reporting success here
            // would leave the screen waiting on data that never arrives.
            return@withLock fail(e)
        }

        lastFailure.value = null
        Result.success(Unit)
    }

    /** Where the forecast is wanted, per the user's location mode. */
    private suspend fun resolveCoordinates(): Result<Coordinates> =
        when (val mode = settings.locationMode.first()) {
            LocationMode.Auto -> locationProvider.getCurrentLocation()
            is LocationMode.Manual -> Result.success(
                Coordinates(lat = mode.place.latitude, lon = mode.place.longitude),
            )
        }

    private fun fail(cause: Throwable): Result<Unit> {
        lastFailure.value = cause
        return Result.failure(cause)
    }
}
