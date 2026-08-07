package com.rustanovych.kidsoutfit.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private const val SETTINGS_DATASTORE_NAME = "settings"

/**
 * The single settings store for the process. The delegate enforces one instance per file, so it
 * must stay a top-level property and be reached only through Koin (see `dataModule`).
 */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SETTINGS_DATASTORE_NAME,
)
