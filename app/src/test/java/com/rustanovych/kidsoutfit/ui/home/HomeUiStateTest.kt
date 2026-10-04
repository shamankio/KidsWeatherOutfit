package com.rustanovych.kidsoutfit.ui.home

import com.rustanovych.kidsoutfit.domain.engine.Direction
import com.rustanovych.kidsoutfit.domain.model.OutfitSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeUiStateTest {

    private fun segment(set: OutfitSet, current: Boolean = false) =
        SegmentUi(drawables = listOf(1), timeLabel = "07:00", outfit = set, isCurrent = current)

    private val toWarm = AdviceUi(Direction.WARMER, "11:00", OutfitSet.WARM)
    private val toRain = AdviceUi(Direction.RAIN_STARTS, "14:00", OutfitSet.COOL_RAIN)

    private val threeSegments = HomeUiState(
        isLoading = false,
        segments = listOf(
            segment(OutfitSet.COOL),
            segment(OutfitSet.WARM, current = true),
            segment(OutfitSet.COOL_RAIN),
        ),
        advices = listOf(toWarm, toRain),
    )

    @Test
    fun `the banner points at the change after the segment active at departure`() {
        assertEquals(1, threeSegments.shownSegmentIndex)
        assertEquals(toRain, threeSegments.upcomingAdvice)
    }

    @Test
    fun `a tapped segment moves the banner with it`() {
        val state = threeSegments.copy(selectedSegmentIndex = 0)

        assertEquals(toWarm, state.upcomingAdvice)
    }

    @Test
    fun `there is no banner on the last segment or with a single segment`() {
        assertNull(threeSegments.copy(selectedSegmentIndex = 2).upcomingAdvice)
        assertNull(threeSegments.copy(segments = listOf(segment(OutfitSet.WARM, current = true)), advices = emptyList()).upcomingAdvice)
    }

    @Test
    fun `an empty state has no shown segment`() {
        assertNull(HomeUiState().shownSegmentIndex)
        assertNull(HomeUiState().upcomingAdvice)
    }
}
