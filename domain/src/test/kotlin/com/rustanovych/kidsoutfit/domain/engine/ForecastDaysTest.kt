package com.rustanovych.kidsoutfit.domain.engine

import com.rustanovych.kidsoutfit.domain.model.ForecastBundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ForecastDaysTest {

    private val zone = ZoneId.of("Europe/Kyiv")
    private val fetchedDay = LocalDate.of(2026, 10, 4)
    private val today = listOf(testSnapshot(hour = 8, apparentTemperature = 10.0))
    private val tomorrow = listOf(testSnapshot(hour = 8, apparentTemperature = 20.0))
    private val bundle = ForecastBundle(
        today = today,
        tomorrow = tomorrow,
        fetchedAtEpochSeconds = fetchedDay.atTime(21, 0).atZone(zone).toEpochSecond(),
    )

    @Test
    fun `the fetch day maps to today`() {
        assertEquals(today, ForecastDays.hoursOn(bundle, fetchedDay, zone))
    }

    @Test
    fun `the day after the fetch maps to tomorrow`() {
        assertEquals(tomorrow, ForecastDays.hoursOn(bundle, fetchedDay.plusDays(1), zone))
    }

    @Test
    fun `days the bundle does not cover are null`() {
        assertNull(ForecastDays.hoursOn(bundle, fetchedDay.plusDays(2), zone))
        assertNull(ForecastDays.hoursOn(bundle, fetchedDay.minusDays(1), zone))
    }

    @Test
    fun `an empty day is null`() {
        assertNull(ForecastDays.hoursOn(bundle.copy(tomorrow = emptyList()), fetchedDay.plusDays(1), zone))
    }

    @Test
    fun `the fetch day is read in the given zone`() {
        // 01:00 on the 5th in Kyiv is still the 4th in UTC, so the same instant is a different fetch day per zone.
        val late = bundle.copy(fetchedAtEpochSeconds = fetchedDay.plusDays(1).atTime(1, 0).atZone(zone).toEpochSecond())
        assertEquals(today, ForecastDays.hoursOn(late, fetchedDay.plusDays(1), zone))
        assertEquals(today, ForecastDays.hoursOn(late, fetchedDay, ZoneId.of("UTC")))
    }
}
