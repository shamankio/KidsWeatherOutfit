package com.rustanovych.kidsoutfit.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ColdSensitivityTest {

    @Test
    fun `accepts values within range`() {
        assertEquals(-3, ColdSensitivity(-3).degreesCelsius)
        assertEquals(0, ColdSensitivity.Default.degreesCelsius)
        assertEquals(3, ColdSensitivity(3).degreesCelsius)
    }

    @Test
    fun `rejects values outside range`() {
        assertThrows(IllegalArgumentException::class.java) { ColdSensitivity(-4) }
        assertThrows(IllegalArgumentException::class.java) { ColdSensitivity(4) }
    }

    @Test
    fun `fromSlider clamps out-of-range values`() {
        assertEquals(-3, ColdSensitivity.fromSlider(-10).degreesCelsius)
        assertEquals(3, ColdSensitivity.fromSlider(10).degreesCelsius)
        assertEquals(2, ColdSensitivity.fromSlider(2).degreesCelsius)
    }
}
