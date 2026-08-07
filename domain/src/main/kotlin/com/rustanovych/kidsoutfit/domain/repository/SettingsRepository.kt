package com.rustanovych.kidsoutfit.domain.repository

import com.rustanovych.kidsoutfit.domain.model.ChildGender
import com.rustanovych.kidsoutfit.domain.model.ChildProfile
import com.rustanovych.kidsoutfit.domain.model.LocationMode
import kotlinx.coroutines.flow.Flow

/**
 * Persisted user settings. Implemented in :app on top of DataStore Preferences.
 *
 * Both flows emit the stored value immediately on collection and re-emit on every change, so
 * screens can collect them as their single source of truth. Values that fail to parse (missing,
 * corrupt, or written by an older version) fall back to the documented defaults rather than
 * throwing.
 */
interface SettingsRepository {

    /** The child profile, defaulting to [ChildProfile.Default]. */
    val profile: Flow<ChildProfile>

    /** Where the forecast location comes from, defaulting to [LocationMode.Auto]. */
    val locationMode: Flow<LocationMode>

    suspend fun setGender(gender: ChildGender)

    /**
     * Stores the perceived-temperature offset in whole degrees Celsius, clamped to the range
     * [com.rustanovych.kidsoutfit.domain.model.ColdSensitivity] accepts.
     */
    suspend fun setColdSensitivity(shift: Int)

    /**
     * Stores the time the child usually leaves home.
     *
     * @param hour hour of day, clamped to `0..23`.
     * @param minute minute within the hour, clamped to `0..59`.
     */
    suspend fun setDepartureTime(hour: Int, minute: Int)

    suspend fun setLocationMode(mode: LocationMode)
}
