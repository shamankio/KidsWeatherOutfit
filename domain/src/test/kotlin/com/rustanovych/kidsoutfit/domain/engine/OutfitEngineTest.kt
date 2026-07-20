package com.rustanovych.kidsoutfit.domain.engine

import com.rustanovych.kidsoutfit.domain.model.ColdSensitivity
import com.rustanovych.kidsoutfit.domain.model.OutfitSet
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Enclosed::class)
class OutfitEngineTest {

    /** One case per [OutfitSet] value, each crafted to yield exactly that result. */
    @RunWith(Parameterized::class)
    class AllOutfitSets(
        private val label: String,
        private val apparentTemperature: Double,
        private val precipitationMm: Double,
        private val snowfallCm: Double,
        private val windSpeedKmh: Double,
        private val expected: OutfitSet,
    ) {
        companion object {
            @JvmStatic
            @Parameters(name = "{0} -> {5}")
            fun cases(): List<Array<Any>> = listOf(
                arrayOf("hot and dry", 25.0, 0.0, 0.0, 0.0, OutfitSet.HEAT),
                arrayOf("mild and dry", 18.0, 0.0, 0.0, 0.0, OutfitSet.WARM),
                arrayOf("mild and rainy", 18.0, 1.0, 0.0, 0.0, OutfitSet.WARM_RAIN),
                arrayOf("cool and dry", 10.0, 0.0, 0.0, 0.0, OutfitSet.COOL),
                arrayOf("cool and rainy", 10.0, 1.0, 0.0, 0.0, OutfitSet.COOL_RAIN),
                arrayOf("cool and windy, dry", 10.0, 0.0, 0.0, 30.0, OutfitSet.WIND),
                arrayOf("cold and dry", 3.0, 0.0, 0.0, 0.0, OutfitSet.COLD),
                arrayOf("cold and rainy", 3.0, 1.0, 0.0, 0.0, OutfitSet.COLD_RAIN),
                arrayOf("light winter, dry", -5.0, 0.0, 0.0, 0.0, OutfitSet.WINTER_LIGHT),
                arrayOf("hard frost, no snow", -10.0, 0.0, 0.0, 0.0, OutfitSet.FROST_HARD),
                arrayOf("cold and snowing", 3.0, 0.0, 2.0, 0.0, OutfitSet.SNOWFALL),
                arrayOf("cold, rainy and windy", 5.0, 1.0, 0.0, 30.0, OutfitSet.RAIN_WIND),
            )
        }

        @Test
        fun `resolves the expected outfit set`() {
            val snapshot = testSnapshot(
                apparentTemperature = apparentTemperature,
                precipitationMm = precipitationMm,
                snowfallCm = snowfallCm,
                windSpeedKmh = windSpeedKmh,
            )
            assertEquals(expected, OutfitEngine.resolve(snapshot, testProfile()))
        }
    }

    /** Temperature category boundaries, with a neutral (zero) cold sensitivity. */
    @RunWith(Parameterized::class)
    class TemperatureBoundaries(
        private val apparentTemperature: Double,
        private val expected: OutfitSet,
    ) {
        companion object {
            @JvmStatic
            @Parameters(name = "{0}C -> {1}")
            fun cases(): List<Array<Any>> = listOf(
                arrayOf(22.9, OutfitSet.WARM),
                arrayOf(23.0, OutfitSet.HEAT),
                arrayOf(14.9, OutfitSet.COOL),
                arrayOf(15.0, OutfitSet.WARM),
                arrayOf(7.9, OutfitSet.COLD),
                arrayOf(8.0, OutfitSet.COOL),
                arrayOf(-0.1, OutfitSet.WINTER_LIGHT),
                arrayOf(0.0, OutfitSet.COLD),
                arrayOf(-8.0, OutfitSet.WINTER_LIGHT),
                arrayOf(-8.1, OutfitSet.FROST_HARD),
            )
        }

        @Test
        fun `resolves the expected category at the boundary`() {
            val snapshot = testSnapshot(apparentTemperature = apparentTemperature)
            assertEquals(expected, OutfitEngine.resolve(snapshot, testProfile()))
        }
    }

    class SensitivityShift {

        @Test
        fun `positive sensitivity shifts a borderline COOL apparent temperature up into WARM`() {
            // Apparent 14C alone is COOL; a child who runs hot (+3) is dressed as if it's 17C -> WARM.
            val snapshot = testSnapshot(apparentTemperature = 14.0)
            val profile = testProfile(coldSensitivity = ColdSensitivity(3))

            assertEquals(OutfitSet.WARM, OutfitEngine.resolve(snapshot, profile))
        }

        @Test
        fun `negative sensitivity shifts a borderline WARM apparent temperature down into COOL`() {
            // Apparent 16C alone is WARM; a child who gets cold easily (-3) is dressed as if it's 13C -> COOL.
            val snapshot = testSnapshot(apparentTemperature = 16.0)
            val profile = testProfile(coldSensitivity = ColdSensitivity(-3))

            assertEquals(OutfitSet.COOL, OutfitEngine.resolve(snapshot, profile))
        }
    }

    class ModifierPriorities {

        @Test
        fun `snow beats rain at sub-zero temperatures`() {
            val snapshot = testSnapshot(apparentTemperature = -2.0, precipitationMm = 1.0, snowfallCm = 1.5)

            assertEquals(OutfitSet.SNOWFALL, OutfitEngine.resolve(snapshot, testProfile()))
        }

        @Test
        fun `hard frost with only light snow stays FROST_HARD instead of SNOWFALL`() {
            val snapshot = testSnapshot(apparentTemperature = -10.0, snowfallCm = 0.5)

            assertEquals(OutfitSet.FROST_HARD, OutfitEngine.resolve(snapshot, testProfile()))
        }

        @Test
        fun `hard frost snowfall just below the SNOWFALL threshold stays FROST_HARD`() {
            val snapshot = testSnapshot(apparentTemperature = -10.0, snowfallCm = 0.99)

            assertEquals(OutfitSet.FROST_HARD, OutfitEngine.resolve(snapshot, testProfile()))
        }

        @Test
        fun `hard frost snowfall exactly at the SNOWFALL threshold becomes SNOWFALL`() {
            val snapshot = testSnapshot(apparentTemperature = -10.0, snowfallCm = 1.0)

            assertEquals(OutfitSet.SNOWFALL, OutfitEngine.resolve(snapshot, testProfile()))
        }

        @Test
        fun `rain combined with wind produces RAIN_WIND`() {
            val snapshot = testSnapshot(apparentTemperature = 5.0, precipitationMm = 1.0, windSpeedKmh = 30.0)

            assertEquals(OutfitSet.RAIN_WIND, OutfitEngine.resolve(snapshot, testProfile()))
        }
    }

    class RainDetection {

        @Test
        fun `rain is detected from precipitationMm even with a non-rain weather code`() {
            val snapshot = testSnapshot(apparentTemperature = 18.0, precipitationMm = 0.5, weatherCode = 0)

            assertEquals(OutfitSet.WARM_RAIN, OutfitEngine.resolve(snapshot, testProfile()))
        }

        @Test
        fun `rain is detected from weatherCode even with zero precipitationMm`() {
            val snapshot = testSnapshot(apparentTemperature = 18.0, precipitationMm = 0.0, weatherCode = 61)

            assertEquals(OutfitSet.WARM_RAIN, OutfitEngine.resolve(snapshot, testProfile()))
        }

        @Test
        fun `precipitation just below the rain threshold is not rain`() {
            val snapshot = testSnapshot(apparentTemperature = 18.0, precipitationMm = 0.29, weatherCode = 0)

            assertEquals(OutfitSet.WARM, OutfitEngine.resolve(snapshot, testProfile()))
        }

        @Test
        fun `precipitation exactly at the rain threshold is rain`() {
            val snapshot = testSnapshot(apparentTemperature = 18.0, precipitationMm = 0.3, weatherCode = 0)

            assertEquals(OutfitSet.WARM_RAIN, OutfitEngine.resolve(snapshot, testProfile()))
        }
    }

    class WindDetection {

        @Test
        fun `wind speed just below the windy threshold is not windy`() {
            val snapshot = testSnapshot(apparentTemperature = 10.0, windSpeedKmh = 24.9)

            assertEquals(OutfitSet.COOL, OutfitEngine.resolve(snapshot, testProfile()))
        }

        @Test
        fun `wind speed exactly at the windy threshold is windy`() {
            val snapshot = testSnapshot(apparentTemperature = 10.0, windSpeedKmh = 25.0)

            assertEquals(OutfitSet.WIND, OutfitEngine.resolve(snapshot, testProfile()))
        }
    }
}
