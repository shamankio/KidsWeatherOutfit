package com.rustanovych.kidsoutfit.notifications

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.rustanovych.kidsoutfit.domain.engine.ReminderSchedule
import java.time.Clock
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Owns the morning reminder's work chain.
 *
 * A 24 h periodic request is not exact, so the reminder is a one-time request that re-enqueues its
 * successor when it finishes (see [DailyOutfitRun]). The three enqueue flavours differ only in what
 * they do to a chain that already exists.
 */
interface DailyNotificationScheduler {

    /** Replaces the chain (or cancels it when [enabled] is false): the user changed something. */
    fun schedule(departureHour: Int, departureMinute: Int, enabled: Boolean)

    /** Like [schedule], but leaves an existing chain alone: app start must not disturb a run. */
    fun ensureScheduled(departureHour: Int, departureMinute: Int, enabled: Boolean)

    /**
     * Enqueues the next link from inside a running link. It appends instead of replacing, because
     * replacing would cancel the very work that is calling.
     */
    fun scheduleNext(departureHour: Int, departureMinute: Int)

    /** Runs the worker once, now, outside the chain. For the debug "Test notification now" item. */
    fun runNow()
}

class WorkManagerDailyNotificationScheduler(
    private val workManager: WorkManager,
    private val clock: Clock = Clock.systemDefaultZone(),
) : DailyNotificationScheduler {

    override fun schedule(departureHour: Int, departureMinute: Int, enabled: Boolean) {
        if (!enabled) {
            workManager.cancelUniqueWork(DailyOutfitWorker.UNIQUE_NAME)
            return
        }
        enqueue(departureHour, departureMinute, ExistingWorkPolicy.REPLACE, from = ZonedDateTime.now(clock))
    }

    override fun ensureScheduled(departureHour: Int, departureMinute: Int, enabled: Boolean) {
        if (!enabled) {
            workManager.cancelUniqueWork(DailyOutfitWorker.UNIQUE_NAME)
            return
        }
        enqueue(departureHour, departureMinute, ExistingWorkPolicy.KEEP, from = ZonedDateTime.now(clock))
    }

    override fun scheduleNext(departureHour: Int, departureMinute: Int) {
        val now = ZonedDateTime.now(clock)
        // Looking a minute ahead keeps a run that fired slightly early from targeting its own slot.
        enqueue(
            departureHour,
            departureMinute,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            from = now.plusMinutes(1),
            delayFrom = now,
        )
    }

    override fun runNow() {
        val request = OneTimeWorkRequestBuilder<DailyOutfitWorker>()
            .setInputData(workDataOf(DailyOutfitWorker.KEY_IS_TEST to true))
            .build()
        workManager.enqueueUniqueWork(DailyOutfitWorker.TEST_UNIQUE_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    /**
     * @param from The moment the next slot is looked up after.
     * @param delayFrom The moment the initial delay is measured from.
     */
    private fun enqueue(
        hour: Int,
        minute: Int,
        policy: ExistingWorkPolicy,
        from: ZonedDateTime,
        delayFrom: ZonedDateTime = from,
    ) {
        // Wall-clock arithmetic, then back to an instant in the zone: across a daylight-saving
        // switch the reminder stays at the same local time instead of drifting by an hour.
        val trigger = ReminderSchedule
            .nextTriggerAt(from.toLocalDateTime(), LocalTime.of(hour, minute))
            .atZone(from.zone)
        val delay = Duration.between(delayFrom, trigger).coerceAtLeast(Duration.ZERO)

        val request = OneTimeWorkRequestBuilder<DailyOutfitWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(DailyOutfitWorker.UNIQUE_NAME, policy, request)
    }

    private companion object {
        const val BACKOFF_SECONDS = 30L
    }
}
