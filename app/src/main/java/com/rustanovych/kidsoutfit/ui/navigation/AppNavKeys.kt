package com.rustanovych.kidsoutfit.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Navigation 3 keys for the app's screens. */
@Serializable
data object SplashKey : NavKey

@Serializable
data object HomeKey : NavKey

@Serializable
data object SettingsKey : NavKey
