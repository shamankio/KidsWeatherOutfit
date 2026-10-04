package com.rustanovych.kidsoutfit.di

import com.rustanovych.kidsoutfit.data.location.GeocoderPlaceNameResolver
import com.rustanovych.kidsoutfit.data.location.LocationPermission
import com.rustanovych.kidsoutfit.data.location.LocationProvider
import com.rustanovych.kidsoutfit.data.network.GeocodingDataSource
import com.rustanovych.kidsoutfit.domain.repository.PlaceNameResolver
import com.rustanovych.kidsoutfit.domain.repository.PlaceSearch
import com.rustanovych.kidsoutfit.ui.format.AndroidDisplayFormatter
import com.rustanovych.kidsoutfit.ui.format.DisplayFormatter
import com.rustanovych.kidsoutfit.ui.home.HomeViewModel
import com.rustanovych.kidsoutfit.ui.splash.SplashViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Root Koin module. Repositories, data sources, and ViewModels get registered here. */
val appModule = module {

    single { LocationProvider(androidContext()) }
    single { LocationPermission { get<LocationProvider>().hasCoarseLocationPermission() } }
    single<PlaceNameResolver> { GeocoderPlaceNameResolver(androidContext()) }
    single<PlaceSearch> { get<GeocodingDataSource>() }
    single<DisplayFormatter> { AndroidDisplayFormatter(androidContext()) }

    viewModel {
        HomeViewModel(
            weatherRepository = get(),
            settingsRepository = get(),
            placeNameResolver = get(),
            formatter = get(),
        )
    }

    viewModel {
        SplashViewModel(
            weatherRepository = get(),
            settingsRepository = get(),
            locationPermission = get(),
        )
    }
}
