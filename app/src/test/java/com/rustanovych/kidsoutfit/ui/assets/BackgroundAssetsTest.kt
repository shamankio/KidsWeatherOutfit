package com.rustanovych.kidsoutfit.ui.assets

import com.rustanovych.kidsoutfit.domain.model.WeatherScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BackgroundAssetsTest {

    @Test
    fun `every weather scene resolves to a non-zero drawable`() {
        for (scene in WeatherScene.entries) {
            assertNotEquals("$scene", 0, backgroundDrawable(scene))
        }
    }

    @Test
    fun `no two scenes share a drawable`() {
        val ids = WeatherScene.entries.map(::backgroundDrawable)
        assertEquals(WeatherScene.entries.size, ids.toSet().size)
    }
}
