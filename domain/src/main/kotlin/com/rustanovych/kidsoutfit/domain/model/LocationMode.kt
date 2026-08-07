package com.rustanovych.kidsoutfit.domain.model

/**
 * Where the forecast location comes from.
 *
 * [Auto] resolves the device location on every refresh, so the forecast follows the user around.
 * [Manual] pins the forecast to one place the user picked from geocoding search and needs no
 * location permission.
 */
sealed interface LocationMode {

    /** Follow the device location. */
    data object Auto : LocationMode

    /** Always forecast for [place], regardless of where the device is. */
    data class Manual(val place: Place) : LocationMode
}
