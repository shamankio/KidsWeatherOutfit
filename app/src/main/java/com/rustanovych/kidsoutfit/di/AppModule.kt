package com.rustanovych.kidsoutfit.di

import com.rustanovych.kidsoutfit.data.location.LocationProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Root Koin module. Repositories, data sources, and ViewModels get registered here. */
val appModule = module {

    single { LocationProvider(androidContext()) }
}
