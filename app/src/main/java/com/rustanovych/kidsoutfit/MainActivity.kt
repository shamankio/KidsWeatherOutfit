package com.rustanovych.kidsoutfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.rustanovych.kidsoutfit.ui.home.HomeScreen
import com.rustanovych.kidsoutfit.ui.navigation.HomeKey
import com.rustanovych.kidsoutfit.ui.navigation.SettingsKey
import com.rustanovych.kidsoutfit.ui.navigation.SplashKey
import com.rustanovych.kidsoutfit.ui.settings.SettingsScreen
import com.rustanovych.kidsoutfit.ui.splash.SplashScreen
import com.rustanovych.kidsoutfit.ui.theme.KidsWeatherOutfitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KidsWeatherOutfitTheme {
                val backStack = rememberNavBackStack(SplashKey)
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider = entryProvider {
                        entry<SplashKey> { SplashScreen() }
                        entry<HomeKey> { HomeScreen() }
                        entry<SettingsKey> { SettingsScreen() }
                    },
                )
            }
        }
    }
}
