package com.rustanovych.kidsoutfit.ui.common

import com.rustanovych.kidsoutfit.domain.model.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocationNamesTest {

    @Test
    fun `a full place reads name, region, country`() {
        val kyiv = Place("Kyiv", "Kyiv Oblast", "Ukraine", 50.45, 30.52)

        assertEquals("Kyiv, Kyiv Oblast, Ukraine", kyiv.fullLabel())
        assertEquals("Kyiv Oblast, Ukraine", kyiv.regionLabel())
    }

    @Test
    fun `missing parts are skipped without stray separators`() {
        val onlyCountry = Place("Monaco", null, "Monaco", 43.7, 7.4)
        val bare = Place("Nowhere", null, null, 0.0, 0.0)

        assertEquals("Monaco, Monaco", onlyCountry.fullLabel())
        assertEquals("Nowhere", bare.fullLabel())
        assertNull(bare.regionLabel())
    }
}
