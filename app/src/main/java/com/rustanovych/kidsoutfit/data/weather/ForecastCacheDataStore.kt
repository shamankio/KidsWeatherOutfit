package com.rustanovych.kidsoutfit.data.weather

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private const val FORECAST_CACHE_DATASTORE_NAME = "forecast_cache"

/**
 * The forecast cache store for the process, kept in its own file rather than sharing the settings
 * store: the cache is disposable derived data, so clearing it must never risk the user's settings.
 *
 * The delegate enforces one instance per file, so it must stay a top-level property and be reached
 * only through Koin (see `dataModule`).
 */
val Context.forecastCacheDataStore: DataStore<Preferences> by preferencesDataStore(
    name = FORECAST_CACHE_DATASTORE_NAME,
)
