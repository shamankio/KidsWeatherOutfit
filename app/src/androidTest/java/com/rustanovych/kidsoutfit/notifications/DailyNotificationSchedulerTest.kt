package com.rustanovych.kidsoutfit.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

/** The real WorkManager (test build) against the real scheduler: what gets enqueued, and how. */
@RunWith(AndroidJUnit4::class)
class DailyNotificationSchedulerTest {

    private lateinit var workManager: WorkManager
    private lateinit var scheduler: WorkManagerDailyNotificationScheduler

    // 06:00 UTC, so a departure at 08:00 fires at 07:30, 90 minutes from now.
    private val clock = Clock.fixed(LocalDateTime.of(2026, 10, 5, 6, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        workManager = WorkManager.getInstance(context)
        scheduler = WorkManagerDailyNotificationScheduler(workManager, clock)
    }

    private fun chain(): List<WorkInfo> =
        workManager.getWorkInfosForUniqueWork(DailyOutfitWorker.UNIQUE_NAME).get()

    private fun live(): List<WorkInfo> = chain().filter { !it.state.isFinished }

    @Test
    fun scheduleEnqueuesOneRequestThirtyMinutesBeforeDeparture() {
        scheduler.schedule(8, 0, enabled = true)

        val work = live().single()
        assertEquals(WorkInfo.State.ENQUEUED, work.state)
        assertEquals(TimeUnit.MINUTES.toMillis(90), work.initialDelayMillis)
        assertEquals(NetworkType.CONNECTED, work.constraints.requiredNetworkType)
    }

    @Test
    fun aSlotThatHasPassedFiresTomorrow() {
        scheduler.schedule(6, 15, enabled = true) // trigger 05:45, already past

        assertEquals(TimeUnit.MINUTES.toMillis(23 * 60 + 45), live().single().initialDelayMillis)
    }

    @Test
    fun scheduleAgainReplacesTheChain() {
        scheduler.schedule(8, 0, enabled = true)
        scheduler.schedule(9, 0, enabled = true)

        assertEquals(TimeUnit.MINUTES.toMillis(150), live().single().initialDelayMillis)
    }

    @Test
    fun ensureScheduledLeavesAnExistingChainAlone() {
        scheduler.schedule(8, 0, enabled = true)
        scheduler.ensureScheduled(9, 0, enabled = true)

        assertEquals(TimeUnit.MINUTES.toMillis(90), live().single().initialDelayMillis)
    }

    @Test
    fun disablingCancelsTheChain() {
        scheduler.schedule(8, 0, enabled = true)
        scheduler.schedule(8, 0, enabled = false)

        assertEquals(emptyList<WorkInfo>(), live())
    }

    @Test
    fun scheduleNextAppendsALinkWithoutCancellingTheRunningOne() {
        scheduler.schedule(8, 0, enabled = true)
        scheduler.scheduleNext(8, 0)

        val states = chain().map { it.state }
        assertEquals(2, states.size)
        assertEquals(false, states.contains(WorkInfo.State.CANCELLED))
    }
}
