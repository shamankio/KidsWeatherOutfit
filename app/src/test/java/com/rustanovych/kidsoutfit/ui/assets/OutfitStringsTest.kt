package com.rustanovych.kidsoutfit.ui.assets

import com.rustanovych.kidsoutfit.domain.engine.Direction
import com.rustanovych.kidsoutfit.domain.model.OutfitSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class OutfitStringsTest {

    @Test
    fun `every outfit set has its own name string`() {
        val ids = OutfitSet.entries.map { outfitNameRes(it) }
        ids.forEach { assertNotEquals(0, it) }
        assertEquals(OutfitSet.entries.size, ids.toSet().size)
    }

    @Test
    fun `every advice direction has its own template`() {
        val ids = Direction.entries.map { adviceRes(it) }
        ids.forEach { assertNotEquals(0, it) }
        assertEquals(Direction.entries.size, ids.toSet().size)
    }
}
