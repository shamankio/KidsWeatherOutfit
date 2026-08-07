package com.rustanovych.kidsoutfit.data.network

import com.rustanovych.kidsoutfit.domain.model.ForecastBundle
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

/**
 * Remote source of hourly forecasts. Owns the call to [OpenMeteoService] and turns transport,
 * HTTP, and payload failures into a failed [Result] so callers never have to catch.
 */
class OpenMeteoDataSource(
    private val service: OpenMeteoService,
    private val nowEpochSeconds: () -> Long = { System.currentTimeMillis() / 1000 },
) {

    /**
     * Fetches today's and tomorrow's hourly forecast for a coordinate.
     *
     * Returns a failed [Result] on connectivity problems ([IOException]), non-2xx responses
     * ([HttpException]), or an unreadable payload ([SerializationException]). Coroutine
     * cancellation is not swallowed.
     */
    suspend fun fetchForecast(latitude: Double, longitude: Double): Result<ForecastBundle> = try {
        val response = service.getForecast(
            latitude = latitude,
            longitude = longitude,
            hourly = OpenMeteoService.HOURLY_VARIABLES,
            forecastDays = OpenMeteoService.FORECAST_DAYS,
            timezone = OpenMeteoService.TIMEZONE_AUTO,
        )
        Result.success(response.toForecastBundle(nowEpochSeconds()))
    } catch (e: HttpException) {
        Result.failure(e)
    } catch (e: IOException) {
        Result.failure(e)
    } catch (e: SerializationException) {
        Result.failure(e)
    }
}
