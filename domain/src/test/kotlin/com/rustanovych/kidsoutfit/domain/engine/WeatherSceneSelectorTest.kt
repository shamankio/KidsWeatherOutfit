package com.rustanovych.kidsoutfit.domain.engine

import com.rustanovych.kidsoutfit.domain.model.WeatherScene
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Enclosed::class)
class WeatherSceneSelectorTest {

    /** Every WMO code Open-Meteo can return, at a mild, calm, daytime hour (no extra rules apply). */
    @RunWith(Parameterized::class)
    class CodeTable(
        private val weatherCode: Int,
        private val expected: WeatherScene,
    ) {
        companion object {
            @JvmStatic
            @Parameters(name = "code {0} -> {1}")
            fun cases(): List<Array<Any>> = listOf(
                arrayOf(0, WeatherScene.SUNNY),
                arrayOf(1, WeatherScene.SUNNY),
                arrayOf(2, WeatherScene.CLOUDY),
                arrayOf(3, WeatherScene.CLOUDY),
                arrayOf(45, WeatherScene.CLOUDY),
                arrayOf(48, WeatherScene.CLOUDY),
                arrayOf(51, WeatherScene.RAIN),
                arrayOf(53, WeatherScene.RAIN),
                arrayOf(55, WeatherScene.RAIN),
                arrayOf(56, WeatherScene.RAIN),
                arrayOf(57, WeatherScene.RAIN),
                arrayOf(61, WeatherScene.RAIN),
                arrayOf(63, WeatherScene.RAIN),
                arrayOf(65, WeatherScene.RAIN),
                arrayOf(66, WeatherScene.RAIN),
                arrayOf(67, WeatherScene.RAIN),
                arrayOf(71, WeatherScene.SNOW),
                arrayOf(73, WeatherScene.SNOW),
                arrayOf(75, WeatherScene.SNOW),
                arrayOf(77, WeatherScene.SNOW),
                arrayOf(80, WeatherScene.RAIN),
                arrayOf(81, WeatherScene.RAIN),
                arrayOf(82, WeatherScene.RAIN),
                arrayOf(85, WeatherScene.SNOW),
                arrayOf(86, WeatherScene.SNOW),
                arrayOf(95, WeatherScene.STORM),
                arrayOf(96, WeatherScene.STORM),
                arrayOf(99, WeatherScene.STORM),
                arrayOf(4, WeatherScene.CLOUDY), // unknown code falls back to cloudy
            )
        }

        @Test
        fun `maps the weather code to the expected scene`() {
            assertEquals(expected, WeatherSceneSelector.select(testSnapshot(weatherCode = weatherCode)))
        }
    }

    /** Clear sky switches to EVENING from 20:00 until 05:59; other codes ignore the clock. */
    @RunWith(Parameterized::class)
    class EveningWindow(
        private val hour: Int,
        private val weatherCode: Int,
        private val expected: WeatherScene,
    ) {
        companion object {
            @JvmStatic
            @Parameters(name = "hour {0}, code {1} -> {2}")
            fun cases(): List<Array<Any>> = listOf(
                arrayOf(5, 0, WeatherScene.EVENING),
                arrayOf(6, 0, WeatherScene.SUNNY),
                arrayOf(19, 0, WeatherScene.SUNNY),
                arrayOf(20, 0, WeatherScene.EVENING),
                arrayOf(0, 1, WeatherScene.EVENING),
                arrayOf(22, 1, WeatherScene.EVENING),
                arrayOf(22, 3, WeatherScene.CLOUDY),
                arrayOf(22, 61, WeatherScene.RAIN),
            )
        }

        @Test
        fun `applies the night window only to clear codes`() {
            val snapshot = testSnapshot(hour = hour, weatherCode = weatherCode)
            assertEquals(expected, WeatherSceneSelector.select(snapshot))
        }
    }

    /** Precedence between the extra rules (frost, wind) and the code table. */
    class RulePrecedence {

        @Test
        fun `frost applies to clear and cloudy codes below -8`() {
            assertEquals(
                WeatherScene.FROST,
                WeatherSceneSelector.select(testSnapshot(weatherCode = 0, apparentTemperature = -8.1)),
            )
            assertEquals(
                WeatherScene.FROST,
                WeatherSceneSelector.select(testSnapshot(weatherCode = 45, apparentTemperature = -8.1)),
            )
        }

        @Test
        fun `exactly -8 is not frost`() {
            assertEquals(
                WeatherScene.SUNNY,
                WeatherSceneSelector.select(testSnapshot(weatherCode = 0, apparentTemperature = -8.0)),
            )
        }

        @Test
        fun `frost beats evening`() {
            val snapshot = testSnapshot(hour = 22, weatherCode = 0, apparentTemperature = -10.0)
            assertEquals(WeatherScene.FROST, WeatherSceneSelector.select(snapshot))
        }

        @Test
        fun `frost beats wind`() {
            val snapshot = testSnapshot(weatherCode = 2, apparentTemperature = -10.0, windSpeedKmh = 40.0)
            assertEquals(WeatherScene.FROST, WeatherSceneSelector.select(snapshot))
        }

        @Test
        fun `frost does not apply to precipitation codes`() {
            assertEquals(
                WeatherScene.SNOW,
                WeatherSceneSelector.select(testSnapshot(weatherCode = 73, apparentTemperature = -15.0)),
            )
            assertEquals(
                WeatherScene.RAIN,
                WeatherSceneSelector.select(testSnapshot(weatherCode = 61, apparentTemperature = -15.0)),
            )
        }

        @Test
        fun `frost does not apply to unknown codes`() {
            val snapshot = testSnapshot(weatherCode = 4, apparentTemperature = -15.0)
            assertEquals(WeatherScene.CLOUDY, WeatherSceneSelector.select(snapshot))
        }

        @Test
        fun `wind at the threshold overrides a clear code`() {
            assertEquals(
                WeatherScene.WIND,
                WeatherSceneSelector.select(testSnapshot(weatherCode = 0, windSpeedKmh = 25.0)),
            )
        }

        @Test
        fun `wind just below the threshold does not apply`() {
            assertEquals(
                WeatherScene.SUNNY,
                WeatherSceneSelector.select(testSnapshot(weatherCode = 0, windSpeedKmh = 24.9)),
            )
        }

        @Test
        fun `wind beats evening`() {
            val snapshot = testSnapshot(hour = 21, weatherCode = 0, windSpeedKmh = 30.0)
            assertEquals(WeatherScene.WIND, WeatherSceneSelector.select(snapshot))
        }

        @Test
        fun `wind overrides a cloudy code`() {
            val snapshot = testSnapshot(weatherCode = 3, windSpeedKmh = 30.0)
            assertEquals(WeatherScene.WIND, WeatherSceneSelector.select(snapshot))
        }

        @Test
        fun `wind does not override rain, storm or snow codes`() {
            assertEquals(
                WeatherScene.RAIN,
                WeatherSceneSelector.select(testSnapshot(weatherCode = 63, windSpeedKmh = 40.0)),
            )
            assertEquals(
                WeatherScene.STORM,
                WeatherSceneSelector.select(testSnapshot(weatherCode = 95, windSpeedKmh = 40.0)),
            )
            assertEquals(
                WeatherScene.SNOW,
                WeatherSceneSelector.select(testSnapshot(weatherCode = 71, windSpeedKmh = 40.0)),
            )
        }

        @Test
        fun `wind does not override measured rain under a cloudy code`() {
            val snapshot = testSnapshot(weatherCode = 2, windSpeedKmh = 30.0, precipitationMm = 0.5)
            assertEquals(WeatherScene.CLOUDY, WeatherSceneSelector.select(snapshot))
        }

        @Test
        fun `wind does not override measured snowfall under a cloudy code`() {
            val snapshot = testSnapshot(weatherCode = 2, windSpeedKmh = 30.0, snowfallCm = 0.2)
            assertEquals(WeatherScene.CLOUDY, WeatherSceneSelector.select(snapshot))
        }

        @Test
        fun `trace precipitation below the rain threshold does not block wind`() {
            val snapshot = testSnapshot(weatherCode = 2, windSpeedKmh = 30.0, precipitationMm = 0.2)
            assertEquals(WeatherScene.WIND, WeatherSceneSelector.select(snapshot))
        }

        @Test
        fun `wind applies to unknown codes without precipitation`() {
            val snapshot = testSnapshot(weatherCode = 4, windSpeedKmh = 30.0)
            assertEquals(WeatherScene.WIND, WeatherSceneSelector.select(snapshot))
        }
    }
}
