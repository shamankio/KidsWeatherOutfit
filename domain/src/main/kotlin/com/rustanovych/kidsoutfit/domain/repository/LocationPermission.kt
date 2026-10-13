package com.rustanovych.kidsoutfit.domain.repository

/**
 * Whether the app currently holds the location permission. A seam so ViewModels can be tested
 * without a `Context`; it only reads the state and never prompts — the prompt belongs to the
 * screen. Implemented in `:data`.
 */
fun interface LocationPermission {
    fun isGranted(): Boolean
}
