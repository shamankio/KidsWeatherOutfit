package com.rustanovych.kidsoutfit

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.rustanovych.kidsoutfit.ui.home.HomeRoute
import com.rustanovych.kidsoutfit.ui.navigation.HomeKey
import com.rustanovych.kidsoutfit.ui.navigation.SettingsKey
import com.rustanovych.kidsoutfit.ui.navigation.SplashKey
import com.rustanovych.kidsoutfit.ui.settings.SettingsScreen
import com.rustanovych.kidsoutfit.ui.splash.SplashRoute
import com.rustanovych.kidsoutfit.ui.theme.KidsWeatherOutfitTheme

class MainActivity : ComponentActivity() {
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
                        entry<HomeKey> { HomeRoute(onOpenSettings = { backStack.add(SettingsKey()) }) }
                        entry<SettingsKey> { SettingsScreen() }
                    },
                )
            }
        }
    }
}
