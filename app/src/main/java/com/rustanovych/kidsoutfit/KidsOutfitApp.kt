package com.rustanovych.kidsoutfit

import android.app.Application
import com.rustanovych.kidsoutfit.di.appModule
import com.rustanovych.kidsoutfit.di.dataModule
import com.rustanovych.kidsoutfit.di.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class KidsOutfitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@KidsOutfitApp)
            modules(appModule, networkModule, dataModule)
        }
    }
}
