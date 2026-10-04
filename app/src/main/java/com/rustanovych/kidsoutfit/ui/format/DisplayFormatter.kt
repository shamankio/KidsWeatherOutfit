package com.rustanovych.kidsoutfit.ui.format

import android.content.Context
import android.text.format.DateFormat
import com.rustanovych.kidsoutfit.R
import java.util.Date
import kotlin.math.roundToInt

/**
 * Turns raw numbers into the display strings a UiState carries. A seam, so ViewModels stay free of
 * `Context` and tests can pin the output instead of depending on the device locale.
 */
interface DisplayFormatter {

    /** A time of day in the user's locale and 12/24-hour preference, e.g. `09:00` or `9:00 AM`. */
    fun time(hour: Int, minute: Int = 0): String

    /** The local time of day of an instant given in epoch seconds. */
    fun timeOfEpoch(epochSeconds: Long): String

    /** `18°` — whole degrees, unit-less because the app shows Celsius only. */
    fun temperature(celsius: Double): String

    /** `12 km/h` */
    fun wind(kmh: Double): String

    /** `0.5 mm` */
    fun precipitation(mm: Double): String
}

class AndroidDisplayFormatter(private val context: Context) : DisplayFormatter {

    // Re-read per call: the user can flip the 12/24-hour switch while the process lives.
    private fun timeFormat() = DateFormat.getTimeFormat(context)

    override fun time(hour: Int, minute: Int): String {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
        }
        return timeFormat().format(calendar.time)
    }

    override fun timeOfEpoch(epochSeconds: Long): String =
        timeFormat().format(Date(epochSeconds * 1000))

    override fun temperature(celsius: Double): String =
        context.getString(R.string.format_temperature, celsius.roundToInt())

    override fun wind(kmh: Double): String =
        context.getString(R.string.format_wind, kmh.roundToInt())

    override fun precipitation(mm: Double): String =
        context.getString(R.string.format_precipitation, mm)
}
