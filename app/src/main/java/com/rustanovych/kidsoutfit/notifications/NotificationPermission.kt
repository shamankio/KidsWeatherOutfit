package com.rustanovych.kidsoutfit.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Whether the app may post notifications. A seam so ViewModels can be tested without a `Context`;
 * it only reads the state and never prompts — the prompt belongs to the screen, like for location
 * (see `LocationPermission`).
 */
interface NotificationPermission {

    /** Notifications are currently allowed. */
    fun isGranted(): Boolean

    /**
     * Whether allowing them takes a runtime request (Android 13+). Before that the permission does
     * not exist, and there is nothing to ask for.
     */
    val needsRuntimeRequest: Boolean
}

class AndroidNotificationPermission(private val context: Context) : NotificationPermission {

    override val needsRuntimeRequest: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    override fun isGranted(): Boolean =
        if (needsRuntimeRequest) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
}
