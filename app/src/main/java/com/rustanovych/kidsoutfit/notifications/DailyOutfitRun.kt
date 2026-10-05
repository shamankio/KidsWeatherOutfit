package com.rustanovych.kidsoutfit.notifications

import com.rustanovych.kidsoutfit.domain.engine.Briefing
import com.rustanovych.kidsoutfit.domain.engine.ForecastDays
import com.rustanovych.kidsoutfit.domain.engine.MorningBriefing
import com.rustanovych.kidsoutfit.domain.engine.ReminderSchedule
import com.rustanovych.kidsoutfit.domain.model.ChildProfile
import com.rustanovych.kidsoutfit.domain.model.ForecastState
import com.rustanovych.kidsoutfit.domain.repository.SettingsRepository
import com.rustanovych.kidsoutfit.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime

/** What the worker should report back to WorkManager. */
enum class RunOutcome { Done, Retry }

/**
 * One link of the reminder chain: build today's briefing, show it, and enqueue the next link.
 * The worker is a thin shell around this so the logic runs on the plain JVM in tests.
 */
class DailyOutfitRun(
    private val settings: SettingsRepository,
    private val weather: WeatherRepository,
    private val notifier: DailyOutfitNotifier,
    private val scheduler: DailyNotificationScheduler,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    /**
     * @param attempt Zero-based number of times this link already ran and asked for a retry.
     * @param isTest A manual run from the debug menu: shown regardless of the switch, and outside
     * the chain, so it never enqueues a successor.
     */
    suspend fun execute(attempt: Int, isTest: Boolean): RunOutcome {
        if (!isTest && settings.notificationsEnabled.first() != true) {
            // Switched off while the work was waiting: end the chain.
            return RunOutcome.Done
        }

        val profile = settings.profile.first()
        val now = ZonedDateTime.now(clock)

        // The result is irrelevant: a failed refresh leaves the cache, and the cache is the fallback.
        weather.refresh(force = false)

        val date = if (isTest) now.toLocalDate() else ReminderSchedule.targetDate(now.toLocalDateTime())
        val briefing = briefingFor(date, now, profile)
        if (briefing == null) {
            // Nothing to show. Retry with backoff a bounded number of times; after that give up on
            // today but keep the chain alive for tomorrow.
            if (attempt + 1 < MAX_ATTEMPTS) return RunOutcome.Retry
        } else {
            notifier.show(briefing, profile.gender)
        }

        if (!isTest) scheduler.scheduleNext(profile.departureHour, profile.departureMinute)
        return RunOutcome.Done
    }

    private suspend fun briefingFor(
        date: LocalDate,
        now: ZonedDateTime,
        profile: ChildProfile,
    ): Briefing? {
        val bundle = when (val state = weather.forecast().first()) {
            is ForecastState.Ready -> state.bundle
            is ForecastState.Error -> state.cached
            ForecastState.Loading -> null
        } ?: return null
        val hourly = ForecastDays.hoursOn(bundle, date, now.zone) ?: return null
        return MorningBriefing.build(hourly, profile)
    }

    companion object {
        /** First run plus one retry. */
        const val MAX_ATTEMPTS = 2
    }
}
