package com.rustanovych.kidsoutfit.ui.home

import app.cash.turbine.test
import com.rustanovych.kidsoutfit.R
import com.rustanovych.kidsoutfit.domain.engine.Direction
import com.rustanovych.kidsoutfit.domain.model.ChildGender
import com.rustanovych.kidsoutfit.domain.model.ChildProfile
import com.rustanovych.kidsoutfit.domain.model.Coordinates
import com.rustanovych.kidsoutfit.domain.model.ForecastBundle
import com.rustanovych.kidsoutfit.domain.model.ForecastState
import com.rustanovych.kidsoutfit.domain.model.LocationMode
import com.rustanovych.kidsoutfit.domain.model.OutfitSet
import com.rustanovych.kidsoutfit.domain.model.Place
import com.rustanovych.kidsoutfit.domain.model.WeatherSnapshot
import com.rustanovych.kidsoutfit.fakes.FakeDisplayFormatter
import com.rustanovych.kidsoutfit.fakes.FakePlaceNameResolver
import com.rustanovych.kidsoutfit.fakes.FakeSettingsRepository
import com.rustanovych.kidsoutfit.fakes.FakeWeatherRepository
import com.rustanovych.kidsoutfit.fakes.snapshots
import com.rustanovych.kidsoutfit.ui.assets.outfitDrawables
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // COLD (apparent 5°) 07-11, then WARM (18°) 12-19.
    private val coldThenWarm = snapshots(0..11, apparent = 5.0) + snapshots(12..23, apparent = 18.0)

    // WARM 07-11, then COLD 12-19: the opposite shape, for tomorrow.
    private val warmThenCold = snapshots(0..11, apparent = 18.0) + snapshots(12..23, apparent = 5.0)

    private fun bundle(
        today: List<WeatherSnapshot> = coldThenWarm,
        tomorrow: List<WeatherSnapshot> = warmThenCold,
    ) = ForecastBundle(today = today, tomorrow = tomorrow, fetchedAtEpochSeconds = 1_000L)

    private fun profile(
        gender: ChildGender = ChildGender.BOY,
        departureHour: Int = 8,
    ) = ChildProfile.Default.copy(gender = gender, departureHour = departureHour, departureMinute = 0)

    private fun viewModel(
        weather: FakeWeatherRepository = FakeWeatherRepository(ForecastState.Ready(bundle())),
        settings: FakeSettingsRepository = FakeSettingsRepository(profile()),
        resolver: FakePlaceNameResolver = FakePlaceNameResolver(),
        now: LocalTime = LocalTime.of(10, 0),
    ) = HomeViewModel(weather, settings, resolver, FakeDisplayFormatter, now = { now })

    @Test
    fun `loading forecast yields a loading state`() = runTest {
        val vm = viewModel(weather = FakeWeatherRepository(ForecastState.Loading))

        vm.uiState.test {
            assertTrue(expectMostRecentItem().isLoading)
        }
    }

    @Test
    fun `today shows the outfit active at departure time and marks that segment current`() = runTest {
        val vm = viewModel()

        vm.uiState.test {
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals(OutfitSet.COLD, state.currentOutfitLabel)
            assertEquals(listOf(OutfitSet.COLD, OutfitSet.WARM), state.segments.map { it.outfit })
            assertEquals(listOf("07:00", "12:00"), state.segments.map { it.timeLabel })
            assertEquals(listOf(true, false), state.segments.map { it.isCurrent })
            assertNull(state.selectedSegmentIndex)
        }
    }

    @Test
    fun `a departure after the change makes the later segment current`() = runTest {
        val vm = viewModel(settings = FakeSettingsRepository(profile(departureHour = 13)))

        vm.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(OutfitSet.WARM, state.currentOutfitLabel)
            assertEquals(listOf(false, true), state.segments.map { it.isCurrent })
        }
    }

    @Test
    fun `tomorrow starts from its first segment regardless of departure time`() = runTest {
        // Departure 13:00 would resolve to COLD in tomorrow's plan; the first segment is WARM.
        val vm = viewModel(settings = FakeSettingsRepository(profile(departureHour = 13)))

        vm.uiState.test {
            vm.onDaySelected(Day.TOMORROW)
            val state = expectMostRecentItem()
            assertEquals(Day.TOMORROW, state.selectedDay)
            assertEquals(OutfitSet.WARM, state.currentOutfitLabel)
            assertEquals(listOf(true, false), state.segments.map { it.isCurrent })
        }
    }

    @Test
    fun `selecting a segment overrides the hero and day switch resets it`() = runTest {
        val vm = viewModel()

        vm.uiState.test {
            vm.onSegmentSelected(1)
            val selected = expectMostRecentItem()
            assertEquals(1, selected.selectedSegmentIndex)
            assertEquals(OutfitSet.WARM, selected.currentOutfitLabel)
            assertEquals(outfitDrawables(OutfitSet.WARM, ChildGender.BOY), selected.currentOutfits)

            vm.onDaySelected(Day.TOMORROW)
            assertNull(expectMostRecentItem().selectedSegmentIndex)

            vm.onSegmentSelected(1)
            vm.onDaySelected(Day.TODAY)
            val back = expectMostRecentItem()
            assertNull(back.selectedSegmentIndex)
            assertEquals(OutfitSet.COLD, back.currentOutfitLabel)
        }
    }

    @Test
    fun `reselecting the same day keeps the selected segment`() = runTest {
        val vm = viewModel()

        vm.uiState.test {
            vm.onSegmentSelected(1)
            vm.onDaySelected(Day.TODAY)
            assertEquals(1, expectMostRecentItem().selectedSegmentIndex)
        }
    }

    @Test
    fun `an out of range segment index is ignored`() = runTest {
        val vm = viewModel()

        vm.uiState.test {
            vm.onSegmentSelected(7)
            val state = expectMostRecentItem()
            assertNull(state.selectedSegmentIndex)
            assertEquals(OutfitSet.COLD, state.currentOutfitLabel)
        }
    }

    @Test
    fun `BOTH yields two drawables for the hero and every segment, single genders one`() = runTest {
        val settings = FakeSettingsRepository(profile(gender = ChildGender.BOTH))
        val vm = viewModel(settings = settings)

        vm.uiState.test {
            val both = expectMostRecentItem()
            assertEquals(2, both.currentOutfits.size)
            assertTrue(both.segments.all { it.drawables.size == 2 })

            settings.setGender(ChildGender.GIRL)
            val girl = expectMostRecentItem()
            assertEquals(1, girl.currentOutfits.size)
            assertNotEquals(both.currentOutfits.first(), girl.currentOutfits.first())
        }
    }

    @Test
    fun `advice is derived from the day plan and empty for a single segment`() = runTest {
        val vm = viewModel()

        vm.uiState.test {
            assertEquals(
                listOf(AdviceUi(Direction.WARMER, "12:00", OutfitSet.WARM)),
                expectMostRecentItem().advices,
            )

            val steady = snapshots(0..23, apparent = 18.0)
            // Replace today's plan with a steady one.
            val weather = FakeWeatherRepository(ForecastState.Ready(bundle(today = steady)))
            viewModel(weather = weather).uiState.test {
                assertTrue(expectMostRecentItem().advices.isEmpty())
            }
        }
    }

    @Test
    fun `weather card and background follow the current hour today`() = runTest {
        val sunnyThenStorm = snapshots(0..11, apparent = 18.0, weatherCode = 0) +
            snapshots(12..23, apparent = 18.0, weatherCode = 95)
        val weather = FakeWeatherRepository(ForecastState.Ready(bundle(today = sunnyThenStorm)))

        viewModel(weather = weather, now = LocalTime.of(9, 30)).uiState.test {
            val morning = expectMostRecentItem()
            assertEquals(R.drawable.bg_sunny, morning.backgroundRes)
            assertEquals(R.drawable.ic_weather_clear_day, morning.weatherNow?.iconRes)
        }
        viewModel(weather = weather, now = LocalTime.of(14, 0)).uiState.test {
            val afternoon = expectMostRecentItem()
            assertEquals(R.drawable.bg_storm, afternoon.backgroundRes)
            assertEquals(R.drawable.ic_weather_thunderstorms, afternoon.weatherNow?.iconRes)
        }
    }

    @Test
    fun `weather card carries pre-formatted labels`() = runTest {
        viewModel().uiState.test {
            val now = expectMostRecentItem().weatherNow!!
            assertEquals("6°", now.temperatureLabel) // 5° apparent + 1 in the fixture
            assertEquals("5°", now.apparentLabel)
            assertEquals("5 km/h", now.windLabel)
            assertEquals("0.0 mm", now.precipitationLabel)
        }
    }

    @Test
    fun `an error with a cache is stale, without one it is an error`() = runTest {
        val weather = FakeWeatherRepository(ForecastState.Error(cached = bundle()))
        val vm = viewModel(weather = weather)

        vm.uiState.test {
            val stale = expectMostRecentItem()
            assertTrue(stale.isStale)
            assertFalse(stale.error)
            assertEquals("t1000", stale.updatedAtLabel)
            assertEquals(2, stale.segments.size)

            weather.state.value = ForecastState.Error(cached = null)
            val failed = expectMostRecentItem()
            assertTrue(failed.error)
            assertFalse(failed.isStale)
            assertTrue(failed.segments.isEmpty())

            weather.state.value = ForecastState.Ready(bundle())
            val ready = expectMostRecentItem()
            assertFalse(ready.error)
            assertFalse(ready.isStale)
        }
    }

    @Test
    fun `a forecast with no usable hours for the day is an error`() = runTest {
        val weather = FakeWeatherRepository(
            ForecastState.Ready(bundle(today = emptyList())),
        )

        viewModel(weather = weather).uiState.test {
            assertTrue(expectMostRecentItem().error)
        }
    }

    @Test
    fun `opening refreshes without force and pull-to-refresh forces`() = runTest {
        val weather = FakeWeatherRepository(ForecastState.Ready(bundle()))
        val vm = viewModel(weather = weather)

        vm.uiState.test {
            expectMostRecentItem()
            assertEquals(listOf(false), weather.refreshCalls)

            vm.onRefresh()
            assertEquals(listOf(false, true), weather.refreshCalls)
            // The flag flips on and off inside one dispatch, so only the settled value is observable.
            assertFalse(vm.uiState.value.isRefreshing)
        }
    }

    @Test
    fun `a picked city names the header without asking the geocoder`() = runTest {
        val kyiv = Place("Kyiv", "Kyiv Oblast", "Ukraine", 50.45, 30.52)
        val settings = FakeSettingsRepository(profile(), LocationMode.Manual(kyiv))

        viewModel(settings = settings, resolver = FakePlaceNameResolver("Wrong")).uiState.test {
            assertEquals("Kyiv", expectMostRecentItem().locationName)
        }
    }

    @Test
    fun `in GPS mode the header is named by reverse geocoding the forecast location`() = runTest {
        val weather = FakeWeatherRepository(ForecastState.Ready(bundle()))
        val vm = viewModel(weather = weather, resolver = FakePlaceNameResolver("Poznań"))

        vm.uiState.test {
            assertNull(expectMostRecentItem().locationName)

            weather.location.value = Coordinates(52.4, 16.9)
            assertEquals("Poznań", expectMostRecentItem().locationName)
        }
    }

    @Test
    fun `a failed reverse geocode leaves the name unknown`() = runTest {
        val weather = FakeWeatherRepository(ForecastState.Ready(bundle()))
        weather.location.value = Coordinates(52.4, 16.9)

        viewModel(weather = weather, resolver = FakePlaceNameResolver(null)).uiState.test {
            assertNull(expectMostRecentItem().locationName)
        }
    }
}
