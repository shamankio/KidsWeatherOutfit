package com.rustanovych.kidsoutfit.domain.engine

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * When the morning outfit reminder fires: [LEAD] before the child's departure time, every day.
 *
 * Pure so the timing — including the midnight edge — can be tested without a clock or WorkManager.
 * Everything here is in wall-clock time; the caller turns the result into an instant in the right
 * time zone, so a daylight-saving switch cannot shift the reminder.
 */
object ReminderSchedule {

    /** How long before departure the reminder fires. */
    val LEAD: Duration = Duration.ofMinutes(30)

    /** The time of day the reminder fires for the given [departure]; wraps past midnight. */
    fun triggerTime(departure: LocalTime): LocalTime = departure.minus(LEAD)

    /**
     * The next moment the reminder is due: today's slot when it has not passed yet (a slot exactly
     * at [now] counts as not passed), otherwise tomorrow's.
     */
    fun nextTriggerAt(now: LocalDateTime, departure: LocalTime): LocalDateTime {
        val trigger = triggerTime(departure)
        val today = now.toLocalDate().atTime(trigger)
        return if (today.isBefore(now)) today.plusDays(1) else today
    }

    /** How long from [now] until [nextTriggerAt]; never negative. */
    fun nextTriggerDelay(now: LocalDateTime, departure: LocalTime): Duration =
        Duration.between(now, nextTriggerAt(now, departure))

    /**
     * The calendar day a reminder firing at [now] is about: the day of departure. Differs from
     * today's date only when the reminder fires just before midnight for a departure just after it.
     */
    fun targetDate(now: LocalDateTime): LocalDate = now.plus(LEAD).toLocalDate()
}
