package com.rustanovych.kidsoutfit.domain.engine

import com.rustanovych.kidsoutfit.domain.model.ChangeWarning
import com.rustanovych.kidsoutfit.domain.model.DayPlan
import com.rustanovych.kidsoutfit.domain.model.OutfitSet
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class ChangeAdvisorTest(
    private val from: OutfitSet,
    private val to: OutfitSet,
    private val expected: Direction,
) {

    companion object {
        @JvmStatic
        @Parameters(name = "{0} -> {1} => {2}")
        fun cases(): List<Array<Any>> = listOf(
            // The five required transitions.
            arrayOf(OutfitSet.COOL, OutfitSet.WARM, Direction.WARMER),
            arrayOf(OutfitSet.WARM, OutfitSet.COOL, Direction.COLDER),
            arrayOf(OutfitSet.COOL, OutfitSet.COOL_RAIN, Direction.RAIN_STARTS),
            arrayOf(OutfitSet.COOL_RAIN, OutfitSet.COOL, Direction.RAIN_STOPS),
            arrayOf(OutfitSet.COLD, OutfitSet.SNOWFALL, Direction.SNOW_STARTS),
            // Extra transitions so every OutfitSet value is exercised at least once.
            arrayOf(OutfitSet.COOL, OutfitSet.WIND, Direction.WIND_STARTS),
            arrayOf(OutfitSet.HEAT, OutfitSet.WARM_RAIN, Direction.RAIN_STARTS),
            arrayOf(OutfitSet.COLD_RAIN, OutfitSet.COLD, Direction.RAIN_STOPS),
            arrayOf(OutfitSet.WINTER_LIGHT, OutfitSet.FROST_HARD, Direction.COLDER),
            arrayOf(OutfitSet.RAIN_WIND, OutfitSet.WINTER_LIGHT, Direction.RAIN_STOPS),
        )
    }

    @Test
    fun `advise resolves the expected direction`() {
        val plan = DayPlan(
            segments = emptyList(),
            changeWarnings = listOf(ChangeWarning(atHour = 12, from = from, to = to)),
        )

        val advice = ChangeAdvisor.advise(plan).single()

        assertEquals(expected, advice.direction)
        assertEquals(from, advice.from)
        assertEquals(to, advice.to)
        assertEquals(12, advice.atHour)
    }
}
