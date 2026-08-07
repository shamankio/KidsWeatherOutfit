package com.rustanovych.kidsoutfit.data.network

import com.rustanovych.kidsoutfit.data.network.dto.GeocodingResponseDto
import com.rustanovych.kidsoutfit.data.network.dto.GeocodingResultDto
import com.rustanovych.kidsoutfit.domain.model.Place

/** Maps the geocoding payload to domain [Place]s, in the order the API ranked them. */
fun GeocodingResponseDto.toPlaces(): List<Place> = results.orEmpty().map { it.toPlace() }

/** Maps a single hit; `admin1` becomes the human-facing region label. */
fun GeocodingResultDto.toPlace(): Place = Place(
    name = name,
    region = admin1,
    country = country,
    latitude = latitude,
    longitude = longitude,
)
