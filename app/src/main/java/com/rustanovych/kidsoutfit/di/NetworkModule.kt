package com.rustanovych.kidsoutfit.di

import com.rustanovych.kidsoutfit.BuildConfig
import com.rustanovych.kidsoutfit.data.network.GeocodingDataSource
import com.rustanovych.kidsoutfit.data.network.GeocodingService
import com.rustanovych.kidsoutfit.data.network.OpenMeteoDataSource
import com.rustanovych.kidsoutfit.data.network.OpenMeteoService
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

private const val TIMEOUT_SECONDS = 10L
private val JSON_MEDIA_TYPE = "application/json".toMediaType()

/**
 * Forecast and geocoding live on different hosts, so each gets its own [Retrofit] — same JSON and
 * same OkHttp client, different base URL. Both are qualified so neither is an implicit default.
 */
private val FORECAST_RETROFIT = named("forecastRetrofit")
private val GEOCODING_RETROFIT = named("geocodingRetrofit")

/** Networking graph: JSON, OkHttp, Retrofit, and the Open-Meteo endpoints built on top. */
val networkModule = module {

    single {
        Json {
            // Open-Meteo adds fields (and units blocks) we do not model.
            ignoreUnknownKeys = true
        }
    }

    single {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY),
                    )
                }
            }
            .build()
    }

    single(FORECAST_RETROFIT) { retrofit(OpenMeteoService.BASE_URL, get(), get()) }

    single(GEOCODING_RETROFIT) { retrofit(GeocodingService.BASE_URL, get(), get()) }

    single { get<Retrofit>(FORECAST_RETROFIT).create(OpenMeteoService::class.java) }

    single { get<Retrofit>(GEOCODING_RETROFIT).create(GeocodingService::class.java) }

    single { OpenMeteoDataSource(service = get()) }

    single { GeocodingDataSource(service = get()) }
}

private fun retrofit(baseUrl: String, client: OkHttpClient, json: Json): Retrofit =
    Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
        .build()
