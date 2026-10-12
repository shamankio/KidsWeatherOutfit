package com.rustanovych.kidsoutfit.ui.settings

import app.cash.turbine.test
import com.rustanovych.kidsoutfit.domain.model.ChildGender
import com.rustanovych.kidsoutfit.domain.model.ChildProfile
import com.rustanovych.kidsoutfit.domain.model.ForecastState
import com.rustanovych.kidsoutfit.domain.model.LocationMode
import com.rustanovych.kidsoutfit.domain.model.Place
import com.rustanovych.kidsoutfit.fakes.FakeAdConsent
import com.rustanovych.kidsoutfit.fakes.FakeDisplayFormatter
import com.rustanovych.kidsoutfit.fakes.FakeLocationPermission
import com.rustanovych.kidsoutfit.fakes.FakeNotificationPermission
import com.rustanovych.kidsoutfit.fakes.FakePlaceNameResolver
import com.rustanovych.kidsoutfit.fakes.FakePlaceSearch
import com.rustanovych.kidsoutfit.fakes.FakeScheduler
import com.rustanovych.kidsoutfit.fakes.FakeSettingsRepository
import com.rustanovych.kidsoutfit.fakes.FakeWeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val kyiv = Place("Kyiv", "Kyiv Oblast", "Ukraine", 50.45, 30.52)
    private val kyrenia = Place("Kyrenia", "Kyrenia District", "Cyprus", 35.34, 33.32)

    private class Fixture(
        val settings: FakeSettingsRepository,
        val weather: FakeWeatherRepository,
        val search: FakePlaceSearch,
        val permission: FakeLocationPermission,
        val notificationPermission: FakeNotificationPermission,
        val scheduler: FakeScheduler,
        val adConsent: FakeAdConsent,
        val viewModel: SettingsViewModel,
    )

    private fun TestScope.fixture(
        profile: ChildProfile = ChildProfile.Default,
        mode: LocationMode = LocationMode.Auto,
        permissionGranted: Boolean = true,
        notificationsGranted: Boolean = true,
    ): Fixture {
        val settings = FakeSettingsRepository(profile, mode)
        val weather = FakeWeatherRepository(ForecastState.Loading, settings)
        val search = FakePlaceSearch()
        val permission = FakeLocationPermission(permissionGranted)
        val notificationPermission = FakeNotificationPermission(notificationsGranted)
        val scheduler = FakeScheduler()
        val adConsent = FakeAdConsent()
        val viewModel = SettingsViewModel(
            settingsRepository = settings,
            weatherRepository = weather,
            placeSearch = search,
            placeNameResolver = FakePlaceNameResolver("Poznań"),
            locationPermission = permission,
            formatter = FakeDisplayFormatter,
            applicationScope = CoroutineScope(SupervisorJob() + dispatcher),
            notificationPermission = notificationPermission,
            scheduler = scheduler,
            adConsent = adConsent,
        )
        // The state only runs while collected, like it does on screen.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        runCurrent()
        return Fixture(settings, weather, search, permission, notificationPermission, scheduler, adConsent, viewModel)
    }

    @Test
    fun `the slider is the opposite sign of the stored offset`() = runTest(dispatcher) {
        val f = fixture()

        // Right = gets cold easily = dress warmer = a negative offset on the temperature.
        f.viewModel.onSensitivityChanged(2)
        advanceUntilIdle()
        assertEquals(-2, f.settings.profileState.value.coldSensitivity.degreesCelsius)
        assertEquals(2, f.viewModel.uiState.value.sensitivity)

        f.viewModel.onSensitivityChanged(-3)
        advanceUntilIdle()
        assertEquals(3, f.settings.profileState.value.coldSensitivity.degreesCelsius)
        assertEquals(-3, f.viewModel.uiState.value.sensitivity)
    }

    @Test
    fun `a stored offset shows on the slider with the opposite sign`() = runTest(dispatcher) {
        val stored = ChildProfile.Default.copy(
            coldSensitivity = com.rustanovych.kidsoutfit.domain.model.ColdSensitivity(-3),
        )

        assertEquals(3, fixture(profile = stored).viewModel.uiState.value.sensitivity)
    }

    @Test
    fun `gender and departure time are persisted immediately`() = runTest(dispatcher) {
        val f = fixture()

        f.viewModel.onGenderSelected(ChildGender.GIRL)
        f.viewModel.onDepartureTimeSelected(7, 45)
        advanceUntilIdle()

        val profile = f.settings.profileState.value
        assertEquals(ChildGender.GIRL, profile.gender)
        assertEquals(7 to 45, profile.departureHour to profile.departureMinute)
        assertEquals("07:45", f.viewModel.uiState.value.departureTimeLabel)
    }

    @Test
    fun `the reminder caption time is departure minus 30 minutes, across midnight too`() = runTest(dispatcher) {
        val f = fixture()
        assertEquals("07:30", f.viewModel.uiState.value.reminderTimeLabel)

        f.viewModel.onDepartureTimeSelected(0, 10)
        advanceUntilIdle()
        assertEquals("23:40", f.viewModel.uiState.value.reminderTimeLabel)
    }

    @Test
    fun `the reminder switch is persisted`() = runTest(dispatcher) {
        val f = fixture()

        f.viewModel.onNotificationsToggled(true)
        advanceUntilIdle()

        assertEquals(true, f.settings.notificationsState.value)
        assertTrue(f.viewModel.uiState.value.notificationsEnabled)
    }

    @Test
    fun `switching the reminder on without the permission asks for it instead of storing`() = runTest(dispatcher) {
        val f = fixture(notificationsGranted = false)

        f.viewModel.effects.test {
            f.viewModel.onNotificationsToggled(true)
            advanceUntilIdle()
            assertEquals(SettingsEffect.RequestNotificationPermission, awaitItem())
        }
        assertEquals(null, f.settings.notificationsState.value)

        f.viewModel.onNotificationPermissionResult(granted = true)
        advanceUntilIdle()
        assertEquals(true, f.settings.notificationsState.value)
    }

    @Test
    fun `a refused system dialog leaves the reminder off`() = runTest(dispatcher) {
        val f = fixture(notificationsGranted = false)

        f.viewModel.onNotificationPermissionResult(granted = false)
        advanceUntilIdle()

        assertEquals(false, f.settings.notificationsState.value)
    }

    @Test
    fun `switching the reminder off never asks for the permission`() = runTest(dispatcher) {
        val f = fixture(notificationsGranted = false)

        f.viewModel.onNotificationsToggled(false)
        advanceUntilIdle()

        assertEquals(false, f.settings.notificationsState.value)
    }

    @Test
    fun `the test notification runs the worker once`() = runTest(dispatcher) {
        val f = fixture()

        f.viewModel.onTestNotification()

        assertEquals(listOf<FakeScheduler.Call>(FakeScheduler.Call.RunNow), f.scheduler.calls)
    }

    @Test
    fun `search waits 400 ms of silence and sends only the last query`() = runTest(dispatcher) {
        val f = fixture()
        f.viewModel.onManualLocationSelected()

        f.viewModel.onQueryChanged("Ky")
        advanceTimeBy(200)
        f.viewModel.onQueryChanged("Kyi")
        advanceTimeBy(200)
        f.viewModel.onQueryChanged("Kyiv")
        advanceTimeBy(399)
        runCurrent()
        assertTrue("nothing before the debounce elapses", f.search.queries.isEmpty())

        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("Kyiv"), f.search.queries)
    }

    @Test
    fun `results are shown as name plus region`() = runTest(dispatcher) {
        val f = fixture()
        f.search.results = Result.success(listOf(kyiv, kyrenia))
        f.viewModel.onManualLocationSelected()

        f.viewModel.onQueryChanged("Ky")
        advanceTimeBy(401)
        runCurrent()

        val results = (f.viewModel.uiState.value.search as SearchStatus.Results).places
        assertEquals(listOf("Kyiv", "Kyrenia"), results.map { it.title })
        assertEquals("Kyiv Oblast, Ukraine", results.first().subtitle)
    }

    @Test
    fun `a failed search is reported and a short query is idle`() = runTest(dispatcher) {
        val f = fixture()
        f.search.results = Result.failure(java.io.IOException("offline"))

        f.viewModel.onQueryChanged("Kyiv")
        advanceTimeBy(401)
        runCurrent()
        assertEquals(SearchStatus.Failed, f.viewModel.uiState.value.search)

        f.viewModel.onQueryChanged("K")
        advanceTimeBy(401)
        runCurrent()
        assertEquals(SearchStatus.Idle, f.viewModel.uiState.value.search)
    }

    @Test
    fun `picking a city stores it, refreshes with force and closes the list`() = runTest(dispatcher) {
        val f = fixture()
        f.search.results = Result.success(listOf(kyiv))
        f.viewModel.onManualLocationSelected()
        f.viewModel.onQueryChanged("Kyiv")
        advanceTimeBy(401)
        runCurrent()

        f.viewModel.effects.test {
            f.viewModel.onPlaceSelected(kyiv)
            advanceUntilIdle()
            assertEquals(SettingsEffect.PlacePicked, awaitItem())
        }

        assertEquals(LocationMode.Manual(kyiv), f.settings.locationModeState.value)
        assertEquals(listOf(true), f.weather.refreshCalls)
        val state = f.viewModel.uiState.value
        assertEquals(LocationChoice.MANUAL, state.locationChoice)
        assertEquals("Kyiv, Kyiv Oblast, Ukraine", state.pickedPlaceLabel)
        assertEquals("the list stays closed for the filled-in name", SearchStatus.Idle, state.search)
    }

    @Test
    fun `the refresh after a change sees the new location, not the old one`() = runTest(dispatcher) {
        val f = fixture(mode = LocationMode.Auto)

        f.viewModel.onPlaceSelected(kyiv)
        advanceUntilIdle()
        assertEquals(listOf<LocationMode?>(LocationMode.Manual(kyiv)), f.weather.modeAtRefresh)

        f.viewModel.onAutoLocationSelected()
        advanceUntilIdle()
        assertEquals(LocationMode.Auto, f.weather.modeAtRefresh.last())
    }

    @Test
    fun `choosing a city does not change the stored mode until one is picked`() = runTest(dispatcher) {
        val f = fixture()

        f.viewModel.onManualLocationSelected()
        advanceUntilIdle()

        assertEquals(LocationChoice.MANUAL, f.viewModel.uiState.value.locationChoice)
        assertEquals(LocationMode.Auto, f.settings.locationModeState.value)
        assertTrue(f.weather.refreshCalls.isEmpty())
    }

    @Test
    fun `GPS without the permission asks for it and changes nothing`() = runTest(dispatcher) {
        val f = fixture(mode = LocationMode.Manual(kyiv), permissionGranted = false)

        f.viewModel.effects.test {
            f.viewModel.onAutoLocationSelected()
            assertEquals(SettingsEffect.RequestLocationPermission, awaitItem())
        }
        f.viewModel.onLocationPermissionResult(granted = false)
        advanceUntilIdle()

        assertEquals(LocationMode.Manual(kyiv), f.settings.locationModeState.value)
        assertTrue(f.weather.refreshCalls.isEmpty())
    }

    @Test
    fun `GPS with the permission switches and refreshes with force`() = runTest(dispatcher) {
        val f = fixture(mode = LocationMode.Manual(kyiv), permissionGranted = true)

        f.viewModel.onAutoLocationSelected()
        advanceUntilIdle()

        assertEquals(LocationMode.Auto, f.settings.locationModeState.value)
        assertEquals(listOf(true), f.weather.refreshCalls)
        assertEquals(LocationChoice.AUTO, f.viewModel.uiState.value.locationChoice)
    }

    @Test
    fun `granting the permission after the prompt switches to GPS`() = runTest(dispatcher) {
        val f = fixture(mode = LocationMode.Manual(kyiv), permissionGranted = false)

        f.viewModel.onLocationPermissionResult(granted = true)
        advanceUntilIdle()

        assertEquals(LocationMode.Auto, f.settings.locationModeState.value)
        assertEquals(listOf(true), f.weather.refreshCalls)
    }

    @Test
    fun `in GPS mode the subtitle shows the resolved place`() = runTest(dispatcher) {
        val f = fixture()
        f.weather.location.value = com.rustanovych.kidsoutfit.domain.model.Coordinates(52.4, 16.9)
        advanceUntilIdle()

        assertEquals("Poznań", f.viewModel.uiState.value.autoPlaceName)
    }

    @Test
    fun `the privacy settings row follows the consent requirement`() = runTest {
        val f = fixture()
        assertEquals(false, f.viewModel.showPrivacySettings.value)

        f.adConsent.privacyOptionsRequired.value = true

        assertEquals(true, f.viewModel.showPrivacySettings.value)
    }
}
