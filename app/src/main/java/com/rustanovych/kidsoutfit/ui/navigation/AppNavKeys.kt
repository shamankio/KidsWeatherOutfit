package com.rustanovych.kidsoutfit.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Navigation 3 keys for the app's screens. */
@Serializable
data object SplashKey : NavKey

@Serializable
data object HomeKey : NavKey

/**
 * @property focusCitySearch Open with the manual location selected and the city search focused —
 * where onboarding sends a user who declined the location permission.
 */
@Serializable
data class SettingsKey(val focusCitySearch: Boolean = false) : NavKey
