package com.rustanovych.kidsoutfit.domain.engine

import org.junit.Assert.assertEquals
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Test

class ReminderScheduleTest {

    private val day = LocalDate.of(2026, 10, 5)
    private fun at(hour: Int, minute: Int = 0, date: LocalDate = day): LocalDateTime = date.atTime(hour, minute)
    private val departure = LocalTime.of(8, 0)

    @Test
    fun `today before departure fires today`() {
        assertEquals(Duration.ofMinutes(30), ReminderSchedule.nextTriggerDelay(at(7, 0), departure))
        assertEquals(at(7, 30), ReminderSchedule.nextTriggerAt(at(7, 0), departure))
    }

    @Test
    fun `today after the slot has passed fires tomorrow`() {
        assertEquals(Duration.ofHours(22).plusMinutes(30), ReminderSchedule.nextTriggerDelay(at(9, 0), departure))
        assertEquals(at(7, 30, day.plusDays(1)), ReminderSchedule.nextTriggerAt(at(9, 0), departure))
    }

    @Test
    fun `between the slot and departure fires tomorrow`() {
        assertEquals(Duration.ofHours(23).plusMinutes(45), ReminderSchedule.nextTriggerDelay(at(7, 45), departure))
    }

    @Test
    fun `exactly thirty minutes before departure fires right now`() {
        assertEquals(Duration.ZERO, ReminderSchedule.nextTriggerDelay(at(7, 30), departure))
    }

    @Test
    fun `one minute past the slot waits a day less a minute`() {
        assertEquals(Duration.ofHours(23).plusMinutes(59), ReminderSchedule.nextTriggerDelay(at(7, 31), departure))
    }

    @Test
    fun `departure shortly after midnight triggers the evening before`() {
        val early = LocalTime.of(0, 15)
        assertEquals(LocalTime.of(23, 45), ReminderSchedule.triggerTime(early))
        assertEquals(Duration.ofMinutes(5), ReminderSchedule.nextTriggerDelay(at(23, 40), early))
        assertEquals(Duration.ofHours(23).plusMinutes(55), ReminderSchedule.nextTriggerDelay(at(23, 50), early))
        assertEquals(Duration.ofHours(23).plusMinutes(40), ReminderSchedule.nextTriggerDelay(at(0, 5), early))
    }

    @Test
    fun `just before midnight with a morning departure waits through the night`() {
        assertEquals(Duration.ofHours(7).plusMinutes(31), ReminderSchedule.nextTriggerDelay(at(23, 59), departure))
        assertEquals(at(7, 30, day.plusDays(1)), ReminderSchedule.nextTriggerAt(at(23, 59), departure))
    }

    @Test
    fun `midnight departure triggers at 23-30`() {
        val midnight = LocalTime.MIDNIGHT
        assertEquals(LocalTime.of(23, 30), ReminderSchedule.triggerTime(midnight))
        assertEquals(Duration.ofMinutes(30), ReminderSchedule.nextTriggerDelay(at(23, 0), midnight))
    }

    @Test
    fun `target date is the day of departure`() {
        assertEquals(day, ReminderSchedule.targetDate(at(7, 30)))
        assertEquals(day.plusDays(1), ReminderSchedule.targetDate(at(23, 45)))
    }
}
