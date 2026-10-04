package com.rustanovych.kidsoutfit.data.location

/**
 * Whether the app currently holds the location permission. A seam over [LocationProvider] so
 * ViewModels can be tested without a `Context`; it only reads the state and never prompts — the
 * prompt belongs to the screen (see [LocationProvider]).
 */
fun interface LocationPermission {
    fun isGranted(): Boolean
}
