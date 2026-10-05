package com.rustanovych.kidsoutfit.domain.engine

import com.rustanovych.kidsoutfit.domain.model.OutfitSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MorningBriefingTest {

    private val profile = testProfile() // departs 08:00

    @Test
    fun `stable day has the departure outfit and no advice`() {
        val hourly = (7..19).map { testSnapshot(hour = it, apparentTemperature = 18.0) }

        val briefing = MorningBriefing.build(hourly, profile)

        assertEquals(Briefing(OutfitSet.WARM, advice = null), briefing)
    }

    @Test
    fun `warming at noon is advised against the departure outfit`() {
        val hourly = (7..19).map { testSnapshot(hour = it, apparentTemperature = if (it >= 12) 18.0 else 3.0) }

        val briefing = MorningBriefing.build(hourly, profile)!!

        assertEquals(OutfitSet.COLD, briefing.outfit)
        assertEquals(12, briefing.advice!!.atHour)
        assertEquals(Direction.WARMER, briefing.advice!!.direction)
        assertEquals(OutfitSet.WARM, briefing.advice!!.to)
    }

    @Test
    fun `a change before departure is not advised`() {
        // Cold until 07:59, warm from 08:00 on: the child leaves in the warm segment.
        val hourly = (7..19).map { testSnapshot(hour = it, apparentTemperature = if (it >= 8) 18.0 else 3.0) }

        val briefing = MorningBriefing.build(hourly, profile)!!

        assertEquals(OutfitSet.WARM, briefing.outfit)
        assertNull(briefing.advice)
    }

    @Test
    fun `no hours in the displayed window is null`() {
        assertNull(MorningBriefing.build(emptyList(), profile))
        assertNull(MorningBriefing.build(listOf(testSnapshot(hour = 3)), profile))
    }
}
