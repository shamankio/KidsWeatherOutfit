package com.rustanovych.kidsoutfit.notifications

import com.rustanovych.kidsoutfit.fakes.FakeScheduler
import com.rustanovych.kidsoutfit.fakes.FakeScheduler.Call
import com.rustanovych.kidsoutfit.fakes.FakeSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationScheduleSyncTest {

    private fun kotlinx.coroutines.test.TestScope.start(settings: FakeSettingsRepository, scheduler: FakeScheduler) {
        NotificationScheduleSync(
            settings,
            scheduler,
            CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler)),
        ).start()
        runCurrent()
    }

    @Test
    fun `app start keeps an existing chain and treats never-asked as off`() = runTest {
        val scheduler = FakeScheduler()
        start(FakeSettingsRepository(), scheduler)

        assertEquals(listOf<Call>(Call.Ensure(8, 0, enabled = false)), scheduler.calls)
    }

    @Test
    fun `enabling the reminder replaces the chain`() = runTest {
        val settings = FakeSettingsRepository()
        val scheduler = FakeScheduler()
        start(settings, scheduler)

        settings.setNotificationsEnabled(true)
        runCurrent()

        assertEquals(Call.Schedule(8, 0, enabled = true), scheduler.calls.last())
    }

    @Test
    fun `changing the departure time reschedules, unrelated changes do not`() = runTest {
        val settings = FakeSettingsRepository().apply { notificationsState.value = true }
        val scheduler = FakeScheduler()
        start(settings, scheduler)
        scheduler.calls.clear()

        settings.setGender(com.rustanovych.kidsoutfit.domain.model.ChildGender.GIRL)
        runCurrent()
        assertEquals(emptyList<Call>(), scheduler.calls)

        settings.setDepartureTime(7, 45)
        runCurrent()
        assertEquals(listOf<Call>(Call.Schedule(7, 45, enabled = true)), scheduler.calls)
    }

    @Test
    fun `switching off cancels`() = runTest {
        val settings = FakeSettingsRepository().apply { notificationsState.value = true }
        val scheduler = FakeScheduler()
        start(settings, scheduler)

        settings.setNotificationsEnabled(false)
        runCurrent()

        assertEquals(Call.Schedule(8, 0, enabled = false), scheduler.calls.last())
    }
}
