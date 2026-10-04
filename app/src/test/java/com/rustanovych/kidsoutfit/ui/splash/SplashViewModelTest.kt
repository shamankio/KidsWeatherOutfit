package com.rustanovych.kidsoutfit.ui.splash

import app.cash.turbine.test
import com.rustanovych.kidsoutfit.domain.model.ChildProfile
import com.rustanovych.kidsoutfit.domain.model.ForecastBundle
import com.rustanovych.kidsoutfit.domain.model.ForecastState
import com.rustanovych.kidsoutfit.domain.model.LocationMode
import com.rustanovych.kidsoutfit.domain.model.Place
import com.rustanovych.kidsoutfit.fakes.FakeLocationPermission
import com.rustanovych.kidsoutfit.fakes.FakeSettingsRepository
import com.rustanovych.kidsoutfit.fakes.FakeWeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val cached = ForecastBundle(emptyList(), emptyList(), fetchedAtEpochSeconds = 1L)
    private val kyiv = Place("Kyiv", "Kyiv Oblast", "Ukraine", 50.45, 30.52)

    private fun viewModel(
        weather: FakeWeatherRepository = FakeWeatherRepository(ForecastState.Loading),
        settings: FakeSettingsRepository = FakeSettingsRepository(ChildProfile.Default, LocationMode.Auto),
        permission: FakeLocationPermission = FakeLocationPermission(granted = false),
    ) = SplashViewModel(weather, settings, permission)

    @Test
    fun `a cached forecast skips straight to done without touching the network`() = runTest {
        val weather = FakeWeatherRepository(ForecastState.Ready(cached))

        viewModel(weather = weather).uiState.test {
            assertEquals(SplashUiState.Done, expectMostRecentItem())
        }
        assertTrue(weather.refreshCalls.isEmpty())
    }

    @Test
    fun `an error with a stale cache still counts as done`() = runTest {
        val weather = FakeWeatherRepository(ForecastState.Error(cached))

        viewModel(weather = weather).uiState.test {
            assertEquals(SplashUiState.Done, expectMostRecentItem())
        }
    }

    @Test
    fun `first launch in GPS mode without the permission asks for location and fetches nothing`() = runTest {
        val weather = FakeWeatherRepository(ForecastState.Loading)

        viewModel(weather = weather).uiState.test {
            assertEquals(SplashUiState.NeedsLocation, expectMostRecentItem())
        }
        assertTrue(weather.refreshCalls.isEmpty())
    }

    @Test
    fun `granting the permission fetches the forecast and finishes`() = runTest {
        val weather = FakeWeatherRepository(ForecastState.Loading)
        val permission = FakeLocationPermission(granted = false)
        val vm = viewModel(weather = weather, permission = permission)

        vm.uiState.test {
            assertEquals(SplashUiState.NeedsLocation, expectMostRecentItem())

            permission.granted = true
            vm.onPermissionResult(granted = true)

            assertEquals(SplashUiState.Done, expectMostRecentItem())
        }
        assertEquals(listOf(false), weather.refreshCalls)
    }

    @Test
    fun `declining the permission opens manual city selection and never fetches`() = runTest {
        val weather = FakeWeatherRepository(ForecastState.Loading)
        val vm = viewModel(weather = weather)

        vm.effects.test {
            vm.onPermissionResult(granted = false)
            assertEquals(SplashEffect.OpenManualCity, awaitItem())
        }
        assertEquals(SplashUiState.NeedsLocation, vm.uiState.value)
        assertTrue(weather.refreshCalls.isEmpty())
    }

    @Test
    fun `a manual city needs no permission and fetches straight away`() = runTest {
        val weather = FakeWeatherRepository(ForecastState.Loading)
        val settings = FakeSettingsRepository(ChildProfile.Default, LocationMode.Manual(kyiv))

        viewModel(weather = weather, settings = settings).uiState.test {
            assertEquals(SplashUiState.Done, expectMostRecentItem())
        }
        assertEquals(listOf(false), weather.refreshCalls)
    }

    @Test
    fun `picking a city while waiting for the permission moves on`() = runTest {
        val settings = FakeSettingsRepository(ChildProfile.Default, LocationMode.Auto)
        val weather = FakeWeatherRepository(ForecastState.Loading)

        viewModel(weather = weather, settings = settings).uiState.test {
            assertEquals(SplashUiState.NeedsLocation, expectMostRecentItem())

            settings.setLocationMode(LocationMode.Manual(kyiv))

            assertEquals(SplashUiState.Done, expectMostRecentItem())
        }
    }

    @Test
    fun `a failed fetch is an error and retry can recover`() = runTest {
        val weather = FakeWeatherRepository(ForecastState.Loading)
        weather.refreshResult = Result.failure(java.io.IOException("offline"))
        val settings = FakeSettingsRepository(ChildProfile.Default, LocationMode.Manual(kyiv))
        val vm = viewModel(weather = weather, settings = settings)

        vm.uiState.test {
            assertEquals(SplashUiState.Error, expectMostRecentItem())

            weather.refreshResult = Result.success(Unit)
            vm.onRetry()

            assertEquals(SplashUiState.Done, expectMostRecentItem())
        }
        assertEquals(listOf(false, false), weather.refreshCalls)
    }
}
