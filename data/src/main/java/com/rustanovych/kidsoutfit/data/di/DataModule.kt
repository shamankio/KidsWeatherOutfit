package com.rustanovych.kidsoutfit.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.rustanovych.kidsoutfit.data.location.GeocoderPlaceNameResolver
import com.rustanovych.kidsoutfit.data.location.LocationProvider
import com.rustanovych.kidsoutfit.data.network.GeocodingDataSource
import com.rustanovych.kidsoutfit.data.settings.SettingsRepositoryImpl
import com.rustanovych.kidsoutfit.data.settings.settingsDataStore
import com.rustanovych.kidsoutfit.data.weather.DataStoreForecastCache
import com.rustanovych.kidsoutfit.data.weather.ForecastCache
import com.rustanovych.kidsoutfit.data.weather.WeatherRepositoryImpl
import com.rustanovych.kidsoutfit.data.weather.forecastCacheDataStore
import com.rustanovych.kidsoutfit.domain.repository.LocationPermission
import com.rustanovych.kidsoutfit.domain.repository.PlaceNameResolver
import com.rustanovych.kidsoutfit.domain.repository.PlaceSearch
import com.rustanovych.kidsoutfit.domain.repository.SettingsRepository
import com.rustanovych.kidsoutfit.domain.repository.WeatherRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Settings and the forecast cache live in separate DataStore files, so both bindings are qualified
 * — neither is an implicit default, and a missing qualifier fails loudly instead of resolving to
 * the wrong store.
 */
private val SETTINGS_DATASTORE = named("settingsDataStore")
private val FORECAST_CACHE_DATASTORE = named("forecastCacheDataStore")

/**
 * Data graph: DataStore, the repositories reading from it, and the device and geocoding services
 * the domain interfaces are implemented on. The only bindings :app is meant to consume.
 */
val dataModule = module {

    single { LocationProvider(androidContext()) }
    single { LocationPermission { get<LocationProvider>().hasCoarseLocationPermission() } }
    single<PlaceNameResolver> { GeocoderPlaceNameResolver(androidContext()) }
    single<PlaceSearch> { get<GeocodingDataSource>() }

    single<DataStore<Preferences>>(SETTINGS_DATASTORE) { androidContext().settingsDataStore }

    single<DataStore<Preferences>>(FORECAST_CACHE_DATASTORE) {
        androidContext().forecastCacheDataStore
    }

    single<SettingsRepository> { SettingsRepositoryImpl(get(SETTINGS_DATASTORE)) }

    single<ForecastCache> {
        DataStoreForecastCache(dataStore = get(FORECAST_CACHE_DATASTORE), json = get())
    }

    single<WeatherRepository> {
        WeatherRepositoryImpl(
            cache = get(),
            remote = get(),
            settings = get(),
            locationProvider = get(),
        )
    }
}
