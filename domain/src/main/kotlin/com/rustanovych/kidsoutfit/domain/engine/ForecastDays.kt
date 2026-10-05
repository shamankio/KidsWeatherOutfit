package com.rustanovych.kidsoutfit.domain.engine

import com.rustanovych.kidsoutfit.domain.model.ForecastBundle
import com.rustanovych.kidsoutfit.domain.model.WeatherSnapshot
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Picks the right day out of a [ForecastBundle].
 *
 * A bundle's `today` and `tomorrow` are relative to when it was fetched, so a cached bundle read the
 * next morning has the day the reader wants in `tomorrow`, not `today`.
 */
object ForecastDays {

    /**
     * The hourly snapshots for [date], or `null` when the bundle does not cover it (it was fetched
     * more than a day away from [date], or the covered day came back empty).
     *
     * @param zone Zone in which "the day the bundle was fetched" is read; the forecast's own days
     * are local to the forecast location, which for this app is the device's zone.
     */
    fun hoursOn(bundle: ForecastBundle, date: LocalDate, zone: ZoneId): List<WeatherSnapshot>? {
        val fetchedOn = Instant.ofEpochSecond(bundle.fetchedAtEpochSeconds).atZone(zone).toLocalDate()
        val hours = when (date) {
            fetchedOn -> bundle.today
            fetchedOn.plusDays(1) -> bundle.tomorrow
            else -> return null
        }
        return hours.takeIf { it.isNotEmpty() }
    }
}
