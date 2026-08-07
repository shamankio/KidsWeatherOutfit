package com.rustanovych.kidsoutfit.data.network

import com.rustanovych.kidsoutfit.data.network.dto.ForecastResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo forecast API. Free, no API key, no authentication.
 *
 * Base URL is `https://api.open-meteo.com/`; see https://open-meteo.com/en/docs.
 */
interface OpenMeteoService {

    /**
     * Fetches the hourly forecast for a coordinate.
     *
     * @param latitude WGS84 latitude in degrees.
     * @param longitude WGS84 longitude in degrees.
     * @param hourly Comma-separated hourly variables; defaults to [HOURLY_VARIABLES].
     * @param forecastDays Number of days to return, counted from today. We use [FORECAST_DAYS].
     * @param timezone `auto` resolves the timezone from the coordinate, so returned timestamps are
     * local to the forecast location.
     */
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String,
        @Query("forecast_days") forecastDays: Int,
        @Query("timezone") timezone: String,
    ): ForecastResponseDto

    companion object {
        const val BASE_URL: String = "https://api.open-meteo.com/"

        /** Hourly variables the outfit engine needs. */
        const val HOURLY_VARIABLES: String =
            "temperature_2m,apparent_temperature,precipitation,snowfall,wind_speed_10m," +
                "weather_code,uv_index"

        /** Today plus tomorrow. */
        const val FORECAST_DAYS: Int = 2

        /** Resolve the timezone from the requested coordinate. */
        const val TIMEZONE_AUTO: String = "auto"
    }
}
