package com.rustanovych.kidsoutfit.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.rustanovych.kidsoutfit.R

object NotificationChannels {

    /** The channel id is persisted by the system, so it must never change. */
    const val DAILY_OUTFIT = "daily_outfit"

    /** Idempotent: re-registering an existing channel only refreshes its name and description. */
    fun register(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            DAILY_OUTFIT,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
