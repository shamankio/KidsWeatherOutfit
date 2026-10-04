package com.rustanovych.kidsoutfit.ui.assets

import com.rustanovych.kidsoutfit.domain.engine.Direction
import com.rustanovych.kidsoutfit.domain.model.ChildGender
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

    @Test
    fun `every set has a distinct description for the boy and for the girl`() {
        val ids = OutfitSet.entries.flatMap { set ->
            listOf(ChildGender.BOY, ChildGender.GIRL).map { outfitDescriptionRes(set, it) }
        }

        ids.forEach { assertNotEquals(0, it) }
        assertEquals(OutfitSet.entries.size * 2, ids.toSet().size)
    }

    @Test
    fun `BOTH has no single description`() {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            outfitDescriptionRes(OutfitSet.WARM, ChildGender.BOTH)
        }
    }
}
