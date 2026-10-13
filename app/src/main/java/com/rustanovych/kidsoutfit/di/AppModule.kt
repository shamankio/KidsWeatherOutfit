package com.rustanovych.kidsoutfit.di

import androidx.work.WorkManager
import com.rustanovych.kidsoutfit.ads.AdConsent
import com.rustanovych.kidsoutfit.ads.UmpConsentManager
import com.rustanovych.kidsoutfit.notifications.AndroidDailyOutfitNotifier
import com.rustanovych.kidsoutfit.notifications.AndroidNotificationPermission
import com.rustanovych.kidsoutfit.notifications.DailyNotificationScheduler
import com.rustanovych.kidsoutfit.notifications.DailyOutfitNotifier
import com.rustanovych.kidsoutfit.notifications.DailyOutfitRun
import com.rustanovych.kidsoutfit.notifications.NotificationPermission
import com.rustanovych.kidsoutfit.notifications.NotificationScheduleSync
import com.rustanovych.kidsoutfit.notifications.WorkManagerDailyNotificationScheduler
import com.rustanovych.kidsoutfit.ui.format.AndroidDisplayFormatter
import com.rustanovych.kidsoutfit.ui.format.DisplayFormatter
import com.rustanovych.kidsoutfit.ui.home.HomeViewModel
import com.rustanovych.kidsoutfit.ui.settings.SettingsViewModel
import com.rustanovych.kidsoutfit.ui.splash.SplashViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Root Koin module for the app layer: ViewModels, notifications, ads and display formatting.
 * Repositories and device services come from `:data` (see `dataModule`, `networkModule`).
 */
val appModule = module {

    // Work that must finish even if the screen that started it is gone.
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single<AdConsent> { UmpConsentManager(androidContext(), scope = get()) }
    single<DisplayFormatter> { AndroidDisplayFormatter(androidContext()) }

    single<NotificationPermission> { AndroidNotificationPermission(androidContext()) }
    single<DailyNotificationScheduler> {
        WorkManagerDailyNotificationScheduler(WorkManager.getInstance(androidContext()))
    }
    single<DailyOutfitNotifier> {
        AndroidDailyOutfitNotifier(androidContext(), formatter = get(), permission = get())
    }
    single {
        DailyOutfitRun(
            settings = get(),
            weather = get(),
            notifier = get(),
            scheduler = get(),
        )
    }
    single { NotificationScheduleSync(settings = get(), scheduler = get(), scope = get()) }

    viewModel {
        HomeViewModel(
            weatherRepository = get(),
            settingsRepository = get(),
            placeNameResolver = get(),
            formatter = get(),
            notificationPermission = get(),
            adConsent = get(),
        )
    }

    viewModel {
        SplashViewModel(
            weatherRepository = get(),
            settingsRepository = get(),
            locationPermission = get(),
        )
    }

    viewModel {
        SettingsViewModel(
            settingsRepository = get(),
            weatherRepository = get(),
            placeSearch = get(),
            placeNameResolver = get(),
            locationPermission = get(),
            formatter = get(),
            applicationScope = get(),
            notificationPermission = get(),
            scheduler = get(),
            adConsent = get(),
        )
    }
}
