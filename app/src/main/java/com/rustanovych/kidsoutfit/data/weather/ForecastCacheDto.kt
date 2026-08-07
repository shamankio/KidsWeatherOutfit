package com.rustanovych.kidsoutfit.data.weather

import com.rustanovych.kidsoutfit.domain.model.Coordinates
import com.rustanovych.kidsoutfit.domain.model.ForecastBundle
import com.rustanovych.kidsoutfit.domain.model.WeatherSnapshot
import kotlinx.serialization.Serializable

/**
 * The last forecast that was fetched, together with where it was fetched for.
 *
 * The coordinates travel with the bundle because freshness is not just a matter of age: a forecast
 * fetched half an hour ago is useless once the device has moved to another city.
 */
data class CachedForecast(
    val bundle: ForecastBundle,
    val coordinates: Coordinates,
)

/**
 * On-disk shape of a cached forecast.
 *
 * The domain models are deliberately free of `kotlinx.serialization` (`:domain` is a pure Kotlin
 * module with no third-party dependencies), so the cache carries its own mirror of them and maps
 * across. That separation also means the storage format can change without touching the domain.
 *
 * Every weather field has a default: a payload written by an older version that predates a field
 * still parses, with the same neutral fallbacks the network mapper uses, instead of dropping the
 * whole cache. [lat], [lon], and [fetchedAtEpochSeconds] have none — without them the entry cannot
 * be freshness-checked and is worthless.
 *
 * @property lat Latitude the forecast was fetched for.
 * @property lon Longitude the forecast was fetched for.
 */
@Serializable
data class CachedForecastDto(
    val lat: Double,
    val lon: Double,
    val fetchedAtEpochSeconds: Long,
    val today: List<CachedHourDto> = emptyList(),
    val tomorrow: List<CachedHourDto> = emptyList(),
)

/** On-disk shape of a single [WeatherSnapshot]. */
@Serializable
data class CachedHourDto(
    val hour: Int,
    val temperature: Double = 0.0,
    val apparentTemperature: Double = 0.0,
    val precipitationMm: Double = 0.0,
    val snowfallCm: Double = 0.0,
    val windSpeedKmh: Double = 0.0,
    val weatherCode: Int = 0,
    val uvIndex: Double = 0.0,
)

fun ForecastBundle.toCacheDto(coordinates: Coordinates): CachedForecastDto = CachedForecastDto(
    lat = coordinates.lat,
    lon = coordinates.lon,
    fetchedAtEpochSeconds = fetchedAtEpochSeconds,
    today = today.map { it.toCacheDto() },
    tomorrow = tomorrow.map { it.toCacheDto() },
)

fun CachedForecastDto.toCachedForecast(): CachedForecast = CachedForecast(
    bundle = ForecastBundle(
        today = today.map { it.toSnapshot() },
        tomorrow = tomorrow.map { it.toSnapshot() },
        fetchedAtEpochSeconds = fetchedAtEpochSeconds,
    ),
    coordinates = Coordinates(lat = lat, lon = lon),
)

private fun WeatherSnapshot.toCacheDto(): CachedHourDto = CachedHourDto(
    hour = hour,
    temperature = temperature,
    apparentTemperature = apparentTemperature,
    precipitationMm = precipitationMm,
    snowfallCm = snowfallCm,
    windSpeedKmh = windSpeedKmh,
    weatherCode = weatherCode,
    uvIndex = uvIndex,
)

private fun CachedHourDto.toSnapshot(): WeatherSnapshot = WeatherSnapshot(
    hour = hour,
    temperature = temperature,
    apparentTemperature = apparentTemperature,
    precipitationMm = precipitationMm,
    snowfallCm = snowfallCm,
    windSpeedKmh = windSpeedKmh,
    weatherCode = weatherCode,
    uvIndex = uvIndex,
)
