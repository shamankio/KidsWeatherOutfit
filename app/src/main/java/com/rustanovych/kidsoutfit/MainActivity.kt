package com.rustanovych.kidsoutfit

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.rustanovych.kidsoutfit.ads.AdConsent
import com.rustanovych.kidsoutfit.ui.home.HomeRoute
import com.rustanovych.kidsoutfit.ui.navigation.HomeKey
import com.rustanovych.kidsoutfit.ui.navigation.LicensesKey
import com.rustanovych.kidsoutfit.ui.navigation.SettingsKey
import com.rustanovych.kidsoutfit.ui.navigation.SplashKey
import com.rustanovych.kidsoutfit.ui.licenses.LicensesScreen
import com.rustanovych.kidsoutfit.ui.settings.SettingsRoute
import com.rustanovych.kidsoutfit.ui.splash.SplashRoute
import com.rustanovych.kidsoutfit.ui.theme.KidsWeatherOutfitTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val adConsent: AdConsent by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate: swaps the splash theme for the app theme.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KidsWeatherOutfitTheme {
                val backStack = rememberNavBackStack(SplashKey)
                NavDisplay(
                    backStack = backStack,
                    // Entry-scoped ViewModels: a screen's ViewModel lives as long as its entry is on the back stack.
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider = entryProvider {
                        entry<SplashKey> {
                            SplashRoute(
                                onDone = {
                                    backStack.clear()
                                    backStack.add(HomeKey)
                                },
                                onOpenCitySearch = { backStack.add(SettingsKey(focusCitySearch = true)) },
                            )
                        }
                        entry<HomeKey> {
                            // Consent is gathered here, not on the onboarding screens, so a first launch
                            // is never two dialogs in a row. The manager ignores repeat calls.
                            LaunchedEffect(Unit) { adConsent.gatherConsent(this@MainActivity) }
                            HomeRoute(onOpenSettings = { backStack.add(SettingsKey()) })
                        }
                        entry<SettingsKey> { key ->
                            SettingsRoute(
                                focusCitySearch = key.focusCitySearch,
                                onBack = { backStack.removeLastOrNull() },
                                onOpenLicenses = { backStack.add(LicensesKey) },
                            )
                        }
                        entry<LicensesKey> { LicensesScreen(onBack = { backStack.removeLastOrNull() }) }
                    },
                )
            }
        }
    }
}
