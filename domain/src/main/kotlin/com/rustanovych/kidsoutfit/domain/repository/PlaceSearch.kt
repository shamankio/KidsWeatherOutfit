package com.rustanovych.kidsoutfit.domain.repository

import com.rustanovych.kidsoutfit.domain.model.Place

/** Looks up places by name for the manual city picker. Implemented in `:app` on top of geocoding. */
interface PlaceSearch {

    /**
     * Places matching [query], best match first. A query too short to search answers with an empty
     * success rather than a failure; connectivity and payload problems come back as a failed
     * [Result].
     */
    suspend fun search(query: String): Result<List<Place>>
}
