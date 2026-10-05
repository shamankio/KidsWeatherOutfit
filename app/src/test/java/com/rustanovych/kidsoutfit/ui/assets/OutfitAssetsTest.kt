package com.rustanovych.kidsoutfit.ui.assets

import com.rustanovych.kidsoutfit.domain.model.ChildGender
import com.rustanovych.kidsoutfit.domain.model.OutfitSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OutfitAssetsTest {

    @Test
    fun `every outfit set and gender combination resolves to a non-zero drawable`() {
        for (set in OutfitSet.entries) {
            for (gender in ChildGender.entries) {
                val ids = outfitDrawables(set, gender)
                val expectedCount = if (gender == ChildGender.BOTH) 2 else 1
                assertEquals("$set/$gender count", expectedCount, ids.size)
                ids.forEach { assertNotEquals("$set/$gender", 0, it) }
                if (gender != ChildGender.BOTH) {
                    assertNotEquals("$set/$gender", 0, outfitDrawable(set, gender))
                }
            }
        }
    }

    @Test
    fun `BOTH returns the boy drawable followed by the girl drawable`() {
        for (set in OutfitSet.entries) {
            assertEquals(
                "$set",
                listOf(outfitDrawable(set, ChildGender.BOY), outfitDrawable(set, ChildGender.GIRL)),
                outfitDrawables(set, ChildGender.BOTH),
            )
        }
    }

    @Test
    fun `single-gender outfitDrawables matches outfitDrawable`() {
        for (set in OutfitSet.entries) {
            for (gender in listOf(ChildGender.BOY, ChildGender.GIRL)) {
                assertEquals("$set/$gender", listOf(outfitDrawable(set, gender)), outfitDrawables(set, gender))
            }
        }
    }

    @Test
    fun `outfitDrawable rejects BOTH`() {
        for (set in OutfitSet.entries) {
            assertThrows(IllegalArgumentException::class.java) { outfitDrawable(set, ChildGender.BOTH) }
        }
    }

    @Test
    fun `no two outfits share a drawable`() {
        val ids = OutfitSet.entries.flatMap { outfitDrawables(it, ChildGender.BOTH) }
        assertEquals(OutfitSet.entries.size * 2, ids.toSet().size)
    }

    @Test
    fun `warm and cool use a dedicated thumb for a single child, other sets the full figure`() {
        for (set in OutfitSet.entries) {
            for (gender in listOf(ChildGender.BOY, ChildGender.GIRL)) {
                val full = outfitDrawables(set, gender)
                val thumb = outfitThumbDrawables(set, gender)
                assertEquals("$set/$gender count", 1, thumb.size)
                if (set == OutfitSet.WARM || set == OutfitSet.COOL) {
                    assertNotEquals("$set/$gender", full, thumb)
                } else {
                    assertEquals("$set/$gender", full, thumb)
                }
            }
        }
    }

    @Test
    fun `a pair keeps the full figures as its thumbnail`() {
        for (set in OutfitSet.entries) {
            assertEquals("$set", outfitDrawables(set, ChildGender.BOTH), outfitThumbDrawables(set, ChildGender.BOTH))
        }
    }
}
