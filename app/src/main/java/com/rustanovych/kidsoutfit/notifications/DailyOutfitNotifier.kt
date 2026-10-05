package com.rustanovych.kidsoutfit.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.rustanovych.kidsoutfit.MainActivity
import com.rustanovych.kidsoutfit.R
import com.rustanovych.kidsoutfit.domain.engine.Briefing
import com.rustanovych.kidsoutfit.domain.model.ChildGender
import com.rustanovych.kidsoutfit.ui.assets.adviceRes
import com.rustanovych.kidsoutfit.ui.assets.outfitNameRes
import com.rustanovych.kidsoutfit.ui.format.DisplayFormatter

/** Shows the morning briefing. A seam so [DailyOutfitRun] is testable without Android. */
interface DailyOutfitNotifier {

    /** Posts the notification, or silently does nothing when notifications are not allowed. */
    fun show(briefing: Briefing, gender: ChildGender)
}

class AndroidDailyOutfitNotifier(
    private val context: Context,
    private val formatter: DisplayFormatter,
    private val permission: NotificationPermission,
) : DailyOutfitNotifier {

    override fun show(briefing: Briefing, gender: ChildGender) {
        if (!permission.isGranted()) return

        val outfitName = context.getString(outfitNameRes(briefing.outfit))
        val text = briefing.advice?.let { advice ->
            context.getString(
                adviceRes(advice.direction),
                formatter.time(advice.atHour),
                context.getString(outfitNameRes(advice.to)),
            )
        } ?: context.getString(R.string.notification_daily_stable)

        val tapIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, NotificationChannels.DAILY_OUTFIT)
            .setSmallIcon(R.drawable.ic_ms_checkroom)
            .setLargeIcon(outfitLargeIcon(context, briefing.outfit, gender))
            .setContentTitle(context.getString(R.string.notification_daily_title, outfitName))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .build()

        try {
            // The permission can be revoked between the check above and this call.
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    private companion object {
        /** One slot: tomorrow's briefing replaces today's instead of piling up. */
        const val NOTIFICATION_ID = 1
    }
}
