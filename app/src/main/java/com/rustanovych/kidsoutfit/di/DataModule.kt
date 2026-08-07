package com.rustanovych.kidsoutfit.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.rustanovych.kidsoutfit.data.settings.SettingsRepositoryImpl
import com.rustanovych.kidsoutfit.data.settings.settingsDataStore
import com.rustanovych.kidsoutfit.data.weather.DataStoreForecastCache
import com.rustanovych.kidsoutfit.data.weather.ForecastCache
import com.rustanovych.kidsoutfit.data.weather.WeatherRepositoryImpl
import com.rustanovych.kidsoutfit.data.weather.forecastCacheDataStore
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

/** Persistence graph: DataStore and the repositories reading from it. */
val dataModule = module {

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
