package com.rustanovych.kidsoutfit.ui.common

import com.rustanovych.kidsoutfit.domain.model.LocationMode
import com.rustanovych.kidsoutfit.domain.model.Place
import com.rustanovych.kidsoutfit.domain.repository.PlaceNameResolver
import com.rustanovych.kidsoutfit.domain.repository.SettingsRepository
import com.rustanovych.kidsoutfit.domain.repository.WeatherRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest

/**
 * The name of the place the forecast is for. A picked city names itself; for GPS the cached
 * forecast's coordinates are reverse-geocoded. Emits `null` first so no screen waits on the
 * geocoder, and `null` again whenever the name cannot be determined.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun forecastPlaceName(
    settings: SettingsRepository,
    weather: WeatherRepository,
    resolver: PlaceNameResolver,
): Flow<String?> = settings.locationMode.flatMapLatest { mode ->
    when (mode) {
        is LocationMode.Manual -> flowOf<String?>(mode.place.name)
        LocationMode.Auto -> flow<String?> {
            emit(null)
            emitAll(
                weather.forecastLocation().mapLatest { coordinates ->
                    coordinates?.let { resolver.nameFor(it) }
                },
            )
        }
    }
}

/** `Kyiv, Kyiv Oblast, Ukraine` — the name, then whatever disambiguation the geocoder had. */
fun Place.fullLabel(): String = listOfNotNull(name, region, country).joinToString(", ")

/** `Kyiv Oblast, Ukraine` — [fullLabel] without the name, `null` when there is nothing to add. */
fun Place.regionLabel(): String? =
    listOfNotNull(region, country).joinToString(", ").takeIf { it.isNotEmpty() }
