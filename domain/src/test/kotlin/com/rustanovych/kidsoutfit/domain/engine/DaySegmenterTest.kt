package com.rustanovych.kidsoutfit.domain.engine

import com.rustanovych.kidsoutfit.domain.model.ChangeWarning
import com.rustanovych.kidsoutfit.domain.model.DayPlan
import com.rustanovych.kidsoutfit.domain.model.OutfitSegment
import com.rustanovych.kidsoutfit.domain.model.OutfitSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DaySegmenterTest {

    private val profile = testProfile()

    @Test
    fun `day without outfit changes yields a single segment and no warnings`() {
        val hourly = (7..19).map { hour -> testSnapshot(hour = hour, apparentTemperature = 18.0) }

        val plan = DaySegmenter.build(hourly, profile)

        assertEquals(listOf(OutfitSegment(OutfitSet.WARM, startHour = 7)), plan.segments)
        assertEquals(emptyList<ChangeWarning>(), plan.changeWarnings)
    }

    @Test
    fun `cold morning, warm noon, cold evening yields three segments with warnings`() {
        val hourly = (7..19).map { hour ->
            val apparentTemperature = if (hour in 11..14) 18.0 else 3.0
            testSnapshot(hour = hour, apparentTemperature = apparentTemperature)
        }

        val plan = DaySegmenter.build(hourly, profile)

        assertEquals(
            listOf(
                OutfitSegment(OutfitSet.COLD, startHour = 7),
                OutfitSegment(OutfitSet.WARM, startHour = 11),
                OutfitSegment(OutfitSet.COLD, startHour = 15),
            ),
            plan.segments,
        )
        assertEquals(
            listOf(
                ChangeWarning(atHour = 11, from = OutfitSet.COLD, to = OutfitSet.WARM),
                ChangeWarning(atHour = 15, from = OutfitSet.WARM, to = OutfitSet.COLD),
            ),
            plan.changeWarnings,
        )
    }

    @Test
    fun `hours outside the 7-19 display window are ignored`() {
        val outsideWindow = listOf(
            testSnapshot(hour = 6, apparentTemperature = -10.0),
            testSnapshot(hour = 20, apparentTemperature = 25.0),
        )
        val insideWindow = (7..19).map { hour -> testSnapshot(hour = hour, apparentTemperature = 18.0) }

        val plan = DaySegmenter.build(outsideWindow + insideWindow, profile)

        assertEquals(listOf(OutfitSegment(OutfitSet.WARM, startHour = 7)), plan.segments)
        assertEquals(emptyList<ChangeWarning>(), plan.changeWarnings)
    }

    @Test
    fun `outfitAt before the first segment falls back to the first segment's outfit`() {
        val plan = threeSegmentPlan()

        assertEquals(OutfitSet.COLD, DaySegmenter.outfitAt(plan, hour = 7, minute = 0))
    }

    @Test
    fun `outfitAt inside a segment returns that segment's outfit`() {
        val plan = threeSegmentPlan()

        assertEquals(OutfitSet.WARM, DaySegmenter.outfitAt(plan, hour = 13, minute = 30))
    }

    @Test
    fun `outfitAt exactly on a segment boundary returns the new segment's outfit`() {
        val plan = threeSegmentPlan()

        assertEquals(OutfitSet.COOL, DaySegmenter.outfitAt(plan, hour = 15, minute = 0))
    }

    @Test
    fun `outfitAt on a plan with no segments returns null`() {
        val emptyPlan = DayPlan(segments = emptyList(), changeWarnings = emptyList())

        assertNull(DaySegmenter.outfitAt(emptyPlan, hour = 10, minute = 0))
    }

    private fun threeSegmentPlan(): DayPlan = DayPlan(
        segments = listOf(
            OutfitSegment(OutfitSet.COLD, startHour = 9),
            OutfitSegment(OutfitSet.WARM, startHour = 12),
            OutfitSegment(OutfitSet.COOL, startHour = 15),
        ),
        changeWarnings = emptyList(),
    )
}
