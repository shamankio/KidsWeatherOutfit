package com.rustanovych.kidsoutfit.domain.repository

import com.rustanovych.kidsoutfit.domain.model.Coordinates

/**
 * Names the place at a pair of coordinates (reverse geocoding), for the GPS mode where the user
 * never picked a [com.rustanovych.kidsoutfit.domain.model.Place]. Implemented in `:app`.
 */
interface PlaceNameResolver {

    /** The city or locality name at [coordinates], or `null` when it cannot be determined. */
    suspend fun nameFor(coordinates: Coordinates): String?
}
