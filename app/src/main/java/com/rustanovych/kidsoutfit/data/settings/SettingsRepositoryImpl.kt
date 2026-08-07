package com.rustanovych.kidsoutfit.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rustanovych.kidsoutfit.domain.model.ChildGender
import com.rustanovych.kidsoutfit.domain.model.ChildProfile
import com.rustanovych.kidsoutfit.domain.model.ColdSensitivity
import com.rustanovych.kidsoutfit.domain.model.LocationMode
import com.rustanovych.kidsoutfit.domain.model.Place
import com.rustanovych.kidsoutfit.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/** DataStore Preferences keys. The names are persisted, so renaming one drops the stored value. */
private object Keys {
    val Gender = stringPreferencesKey("gender")
    val ColdShift = intPreferencesKey("cold_shift")
    val DepartureHour = intPreferencesKey("departure_hour")
    val DepartureMinute = intPreferencesKey("departure_minute")
    val LocationMode = stringPreferencesKey("loc_mode")
    val PlaceName = stringPreferencesKey("place_name")
    val PlaceLat = doublePreferencesKey("place_lat")
    val PlaceLon = doublePreferencesKey("place_lon")
    val PlaceRegion = stringPreferencesKey("place_region")
    val PlaceCountry = stringPreferencesKey("place_country")
}

private const val LOCATION_MODE_AUTO = "auto"
private const val LOCATION_MODE_MANUAL = "manual"

private val DEPARTURE_HOURS = 0..23
private val DEPARTURE_MINUTES = 0..59

/**
 * Stores settings in DataStore Preferences.
 *
 * Reads are defensive: a value the app cannot make sense of (unknown enum name, out-of-range
 * number, manual mode without a complete place) is treated as absent and replaced by the default,
 * because settings written by an older version outlive an upgrade.
 */
class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    /** A read error on the store's file is not recoverable per-collector; fall back to defaults. */
    private val preferences: Flow<Preferences> = dataStore.data
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }

    override val profile: Flow<ChildProfile> = preferences
        .map { it.toChildProfile() }
        .distinctUntilChanged()

    override val locationMode: Flow<LocationMode> = preferences
        .map { it.toLocationMode() }
        .distinctUntilChanged()

    override suspend fun setGender(gender: ChildGender) {
        dataStore.edit { it[Keys.Gender] = gender.name }
    }

    override suspend fun setColdSensitivity(shift: Int) {
        val clamped = ColdSensitivity.fromSlider(shift)
        dataStore.edit { it[Keys.ColdShift] = clamped.degreesCelsius }
    }

    override suspend fun setDepartureTime(hour: Int, minute: Int) {
        dataStore.edit {
            it[Keys.DepartureHour] = hour.coerceIn(DEPARTURE_HOURS)
            it[Keys.DepartureMinute] = minute.coerceIn(DEPARTURE_MINUTES)
        }
    }

    override suspend fun setLocationMode(mode: LocationMode) {
        dataStore.edit { prefs ->
            when (mode) {
                LocationMode.Auto -> {
                    prefs[Keys.LocationMode] = LOCATION_MODE_AUTO
                    // Drop the pinned place so a later switch back to manual cannot resurrect it.
                    prefs.remove(Keys.PlaceName)
                    prefs.remove(Keys.PlaceLat)
                    prefs.remove(Keys.PlaceLon)
                    prefs.remove(Keys.PlaceRegion)
                    prefs.remove(Keys.PlaceCountry)
                }

                is LocationMode.Manual -> {
                    val place = mode.place
                    prefs[Keys.LocationMode] = LOCATION_MODE_MANUAL
                    prefs[Keys.PlaceName] = place.name
                    prefs[Keys.PlaceLat] = place.latitude
                    prefs[Keys.PlaceLon] = place.longitude
                    // Optional on Place: absent means "the geocoder had no value", not "unchanged".
                    val region = place.region
                    if (region != null) prefs[Keys.PlaceRegion] = region else prefs.remove(Keys.PlaceRegion)
                    val country = place.country
                    if (country != null) prefs[Keys.PlaceCountry] = country else prefs.remove(Keys.PlaceCountry)
                }
            }
        }
    }
}

private fun Preferences.toChildProfile(): ChildProfile {
    val default = ChildProfile.Default
    return ChildProfile(
        gender = this[Keys.Gender]
            ?.let { name -> ChildGender.entries.firstOrNull { it.name == name } }
            ?: default.gender,
        coldSensitivity = this[Keys.ColdShift]
            ?.let { ColdSensitivity.fromSlider(it) }
            ?: default.coldSensitivity,
        departureHour = this[Keys.DepartureHour]
            ?.takeIf { it in DEPARTURE_HOURS }
            ?: default.departureHour,
        departureMinute = this[Keys.DepartureMinute]
            ?.takeIf { it in DEPARTURE_MINUTES }
            ?: default.departureMinute,
    )
}

private fun Preferences.toLocationMode(): LocationMode {
    if (this[Keys.LocationMode] != LOCATION_MODE_MANUAL) return LocationMode.Auto

    // Manual without a usable place would leave the app with nowhere to forecast for.
    val name = this[Keys.PlaceName] ?: return LocationMode.Auto
    val lat = this[Keys.PlaceLat] ?: return LocationMode.Auto
    val lon = this[Keys.PlaceLon] ?: return LocationMode.Auto
    return LocationMode.Manual(
        Place(
            name = name,
            region = this[Keys.PlaceRegion],
            country = this[Keys.PlaceCountry],
            latitude = lat,
            longitude = lon,
        ),
    )
}
