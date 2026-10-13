package com.rustanovych.kidsoutfit

import android.app.Application
import com.rustanovych.kidsoutfit.data.di.dataModule
import com.rustanovych.kidsoutfit.data.di.networkModule
import com.rustanovych.kidsoutfit.di.appModule
import com.rustanovych.kidsoutfit.notifications.NotificationChannels
import com.rustanovych.kidsoutfit.notifications.NotificationScheduleSync
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class KidsOutfitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val koin = startKoin {
            androidLogger()
            androidContext(this@KidsOutfitApp)
            modules(appModule, networkModule, dataModule)
        }.koin
        NotificationChannels.register(this)
        koin.get<NotificationScheduleSync>().start()
    }
}
