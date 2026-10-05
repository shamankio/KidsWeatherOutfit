package com.rustanovych.kidsoutfit.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rustanovych.kidsoutfit.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Re-creates the work chain after events that invalidate its delay: a reboot, or the clock or time
 * zone changing (the delay was computed against the old wall-clock time).
 */
class BootReceiver : BroadcastReceiver(), KoinComponent {

    private val settings: SettingsRepository by inject()
    private val scheduler: DailyNotificationScheduler by inject()
    private val scope: CoroutineScope by inject()

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            -> Unit
            else -> return
        }
        val pending = goAsync()
        scope.launch {
            try {
                val profile = settings.profile.first()
                val enabled = settings.notificationsEnabled.first() == true
                scheduler.schedule(profile.departureHour, profile.departureMinute, enabled)
            } finally {
                pending.finish()
            }
        }
    }
}
