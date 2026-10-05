package com.rustanovych.kidsoutfit.notifications

import com.rustanovych.kidsoutfit.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Keeps the work chain in step with the settings that define it, so no screen has to remember to
 * reschedule: a changed departure time or a flipped switch lands here whoever caused it.
 */
class NotificationScheduleSync(
    private val settings: SettingsRepository,
    private val scheduler: DailyNotificationScheduler,
    private val scope: CoroutineScope,
) {

    private data class Plan(val hour: Int, val minute: Int, val enabled: Boolean)

    fun start() {
        scope.launch {
            var first = true
            combine(
                settings.profile.map { Pair(it.departureHour, it.departureMinute) },
                // Never asked is not enabled.
                settings.notificationsEnabled.map { it == true },
            ) { (hour, minute), enabled -> Plan(hour, minute, enabled) }
                .distinctUntilChanged()
                .collect { plan ->
                    if (first) {
                        // App start (possibly started by the chain's own run): leave a live chain be.
                        scheduler.ensureScheduled(plan.hour, plan.minute, plan.enabled)
                        first = false
                    } else {
                        scheduler.schedule(plan.hour, plan.minute, plan.enabled)
                    }
                }
        }
    }
}
