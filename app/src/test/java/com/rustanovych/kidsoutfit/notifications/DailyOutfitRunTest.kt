package com.rustanovych.kidsoutfit.notifications

import com.rustanovych.kidsoutfit.domain.engine.Direction
import com.rustanovych.kidsoutfit.domain.model.ChildGender
import com.rustanovych.kidsoutfit.domain.model.ChildProfile
import com.rustanovych.kidsoutfit.domain.model.ForecastBundle
import com.rustanovych.kidsoutfit.domain.model.ForecastState
import com.rustanovych.kidsoutfit.domain.model.OutfitSet
import com.rustanovych.kidsoutfit.fakes.FakeNotifier
import com.rustanovych.kidsoutfit.fakes.FakeScheduler
import com.rustanovych.kidsoutfit.fakes.FakeSettingsRepository
import com.rustanovych.kidsoutfit.fakes.FakeWeatherRepository
import com.rustanovych.kidsoutfit.fakes.snapshots
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset

class DailyOutfitRunTest {

    private val zone = ZoneOffset.UTC
    private fun clockAt(dateTime: LocalDateTime) = Clock.fixed(dateTime.toInstant(zone), zone)
    private fun epoch(dateTime: LocalDateTime) = dateTime.toEpochSecond(zone)

    private val morning = LocalDateTime.of(2026, 10, 5, 7, 30)

    // COLD (apparent 5°) until 11:00, WARM (18°) from 12:00.
    private val coldThenWarm = snapshots(0..11, apparent = 5.0) + snapshots(12..23, apparent = 18.0)
    private val stableWarm = snapshots(0..23, apparent = 18.0)
    private val stableCold = snapshots(0..23, apparent = 5.0)

    private class Fixture(
        val settings: FakeSettingsRepository,
        val weather: FakeWeatherRepository,
        val notifier: FakeNotifier,
        val scheduler: FakeScheduler,
        val run: DailyOutfitRun,
    )

    private fun fixture(
        state: ForecastState,
        now: LocalDateTime = morning,
        enabled: Boolean? = true,
        profile: ChildProfile = ChildProfile.Default.copy(gender = ChildGender.BOY),
        refreshResult: Result<Unit> = Result.success(Unit),
    ): Fixture {
        val settings = FakeSettingsRepository(profile).apply { notificationsState.value = enabled }
        val weather = FakeWeatherRepository(state).apply { this.refreshResult = refreshResult }
        val notifier = FakeNotifier()
        val scheduler = FakeScheduler()
        return Fixture(settings, weather, notifier, scheduler, DailyOutfitRun(settings, weather, notifier, scheduler, clockAt(now)))
    }

    private fun bundle(
        today: List<com.rustanovych.kidsoutfit.domain.model.WeatherSnapshot>,
        tomorrow: List<com.rustanovych.kidsoutfit.domain.model.WeatherSnapshot> = stableWarm,
        fetchedAt: LocalDateTime = morning.minusMinutes(10),
    ) = ForecastBundle(today, tomorrow, epoch(fetchedAt))

    @Test
    fun `shows the departure outfit with the next change and schedules the next run`() = runTest {
        val f = fixture(ForecastState.Ready(bundle(today = coldThenWarm)))

        val outcome = f.run.execute(attempt = 0, isTest = false)

        assertEquals(RunOutcome.Done, outcome)
        val (briefing, gender) = f.notifier.shown.single()
        assertEquals(OutfitSet.COLD, briefing.outfit)
        assertEquals(12, briefing.advice!!.atHour)
        assertEquals(Direction.WARMER, briefing.advice!!.direction)
        assertEquals(ChildGender.BOY, gender)
        assertEquals(listOf<FakeScheduler.Call>(FakeScheduler.Call.Next(8, 0)), f.scheduler.calls)
        assertEquals(listOf(false), f.weather.refreshCalls)
    }

    @Test
    fun `a stable day shows no advice`() = runTest {
        val f = fixture(ForecastState.Ready(bundle(today = stableWarm)))

        f.run.execute(attempt = 0, isTest = false)

        assertNull(f.notifier.shown.single().first.advice)
    }

    @Test
    fun `when the switch is off nothing is shown and the chain ends`() = runTest {
        for (enabled in listOf(false, null)) {
            val f = fixture(ForecastState.Ready(bundle(today = stableWarm)), enabled = enabled)

            assertEquals(RunOutcome.Done, f.run.execute(attempt = 0, isTest = false))

            assertEquals(emptyList<Any>(), f.notifier.shown)
            assertEquals(emptyList<FakeScheduler.Call>(), f.scheduler.calls)
            assertEquals(emptyList<Boolean>(), f.weather.refreshCalls)
        }
    }

    @Test
    fun `offline with a cached forecast still shows it`() = runTest {
        val f = fixture(
            ForecastState.Error(cached = bundle(today = stableCold)),
            refreshResult = Result.failure(java.io.IOException("offline")),
        )

        assertEquals(RunOutcome.Done, f.run.execute(attempt = 0, isTest = false))

        assertEquals(OutfitSet.COLD, f.notifier.shown.single().first.outfit)
        assertEquals(listOf<FakeScheduler.Call>(FakeScheduler.Call.Next(8, 0)), f.scheduler.calls)
    }

    @Test
    fun `a cache fetched yesterday evening serves its tomorrow as today`() = runTest {
        val f = fixture(
            ForecastState.Error(
                cached = bundle(
                    today = stableWarm,
                    tomorrow = stableCold,
                    fetchedAt = LocalDateTime.of(2026, 10, 4, 21, 0),
                ),
            ),
            refreshResult = Result.failure(java.io.IOException("offline")),
        )

        f.run.execute(attempt = 0, isTest = false)

        assertEquals(OutfitSet.COLD, f.notifier.shown.single().first.outfit)
    }

    @Test
    fun `a cache too old to cover the day counts as no data`() = runTest {
        val f = fixture(
            ForecastState.Error(cached = bundle(today = stableWarm, fetchedAt = LocalDateTime.of(2026, 10, 1, 9, 0))),
            refreshResult = Result.failure(java.io.IOException("offline")),
        )

        assertEquals(RunOutcome.Retry, f.run.execute(attempt = 0, isTest = false))
        assertEquals(emptyList<Any>(), f.notifier.shown)
    }

    @Test
    fun `no data on the first attempt retries without scheduling the next run`() = runTest {
        val f = fixture(ForecastState.Error(cached = null), refreshResult = Result.failure(java.io.IOException()))

        assertEquals(RunOutcome.Retry, f.run.execute(attempt = 0, isTest = false))

        assertEquals(emptyList<Any>(), f.notifier.shown)
        assertEquals(emptyList<FakeScheduler.Call>(), f.scheduler.calls)
    }

    @Test
    fun `no data on the last attempt gives up quietly but keeps the chain alive`() = runTest {
        val f = fixture(ForecastState.Loading)

        assertEquals(RunOutcome.Done, f.run.execute(attempt = DailyOutfitRun.MAX_ATTEMPTS - 1, isTest = false))

        assertEquals(emptyList<Any>(), f.notifier.shown)
        assertEquals(listOf<FakeScheduler.Call>(FakeScheduler.Call.Next(8, 0)), f.scheduler.calls)
    }

    @Test
    fun `a departure just after midnight is briefed from tomorrow's hours the evening before`() = runTest {
        val f = fixture(
            ForecastState.Ready(
                bundle(
                    today = stableWarm,
                    tomorrow = stableCold,
                    fetchedAt = LocalDateTime.of(2026, 10, 5, 23, 0),
                ),
            ),
            now = LocalDateTime.of(2026, 10, 5, 23, 45),
            profile = ChildProfile.Default.copy(departureHour = 0, departureMinute = 15),
        )

        f.run.execute(attempt = 0, isTest = false)

        // Tomorrow's 00:15 departure has no segment before 07:00, so it takes the first one: COLD.
        assertEquals(OutfitSet.COLD, f.notifier.shown.single().first.outfit)
        assertEquals(listOf<FakeScheduler.Call>(FakeScheduler.Call.Next(0, 15)), f.scheduler.calls)
    }

    @Test
    fun `a test run shows even when the switch is off and never reschedules`() = runTest {
        val f = fixture(ForecastState.Ready(bundle(today = stableWarm)), enabled = null)

        assertEquals(RunOutcome.Done, f.run.execute(attempt = 0, isTest = true))

        assertEquals(OutfitSet.WARM, f.notifier.shown.single().first.outfit)
        assertEquals(emptyList<FakeScheduler.Call>(), f.scheduler.calls)
    }
}
