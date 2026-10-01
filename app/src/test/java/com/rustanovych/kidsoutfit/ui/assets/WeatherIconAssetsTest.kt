package com.rustanovych.kidsoutfit.ui.assets

import com.rustanovych.kidsoutfit.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WeatherIconAssetsTest {

    private val expectedByIcon: Map<Int, List<Int>> = mapOf(
        R.drawable.ic_weather_clear_day to listOf(0),
        R.drawable.ic_weather_partly_cloudy_day to listOf(1, 2),
        R.drawable.ic_weather_cloudy to listOf(3),
        R.drawable.ic_weather_fog to listOf(45, 48),
        R.drawable.ic_weather_drizzle to listOf(51, 53, 55),
        R.drawable.ic_weather_sleet to listOf(56, 57, 66, 67),
        R.drawable.ic_weather_rain to listOf(61, 63, 65, 80, 81, 82),
        R.drawable.ic_weather_snow to listOf(71, 73, 75, 77, 85),
        R.drawable.ic_weather_heavy_snow to listOf(86),
        R.drawable.ic_weather_thunderstorms to listOf(95, 96, 99),
    )

    @Test
    fun `every listed WMO code maps to its icon`() {
        for ((icon, codes) in expectedByIcon) {
            for (code in codes) {
                assertEquals("code $code", icon, weatherIcon(code))
            }
        }
    }

    @Test
    fun `every listed WMO code resolves to a non-zero drawable`() {
        for (code in expectedByIcon.values.flatten()) {
            assertNotEquals("code $code", 0, weatherIcon(code))
        }
    }

    @Test
    fun `unknown codes fall back to cloudy`() {
        for (code in listOf(4, 10, 44, 100, -1, Int.MIN_VALUE, Int.MAX_VALUE)) {
            assertEquals("code $code", R.drawable.ic_weather_cloudy, weatherIcon(code))
        }
    }
}
