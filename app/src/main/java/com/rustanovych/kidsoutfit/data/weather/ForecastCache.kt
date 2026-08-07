package com.rustanovych.kidsoutfit.data.weather

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rustanovych.kidsoutfit.domain.model.Coordinates
import com.rustanovych.kidsoutfit.domain.model.ForecastBundle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Stores the single most recent forecast so the app has something to show before (and instead of)
 * a successful network call.
 */
interface ForecastCache {

    /**
     * The cached forecast, or `null` when nothing usable is stored. Emits immediately on
     * collection and re-emits on every [save]. Never throws: an unreadable store or an unparseable
     * payload reads as `null`, because a disposable cache is not worth crashing over.
     */
    val entry: Flow<CachedForecast?>

    /** Replaces the cached forecast. Throws [IOException] when the write cannot be persisted. */
    suspend fun save(bundle: ForecastBundle, coordinates: Coordinates)
}

/** DataStore Preferences keys. The name is persisted, so renaming it drops the cached forecast. */
private object Keys {
    val Forecast = stringPreferencesKey("forecast")
}

/** Keeps the cache as one JSON document under a single preferences key, so writes stay atomic. */
class DataStoreForecastCache(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) : ForecastCache {

    override val entry: Flow<CachedForecast?> = dataStore.data
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }
        .map { preferences -> preferences[Keys.Forecast]?.let(::decode) }
        .distinctUntilChanged()

    override suspend fun save(bundle: ForecastBundle, coordinates: Coordinates) {
        val payload = json.encodeToString(bundle.toCacheDto(coordinates))
        dataStore.edit { it[Keys.Forecast] = payload }
    }

    /** A payload this build cannot read (corrupt, or from a future version) counts as no cache. */
    private fun decode(payload: String): CachedForecast? = try {
        json.decodeFromString<CachedForecastDto>(payload).toCachedForecast()
    } catch (_: SerializationException) {
        null
    }
}
