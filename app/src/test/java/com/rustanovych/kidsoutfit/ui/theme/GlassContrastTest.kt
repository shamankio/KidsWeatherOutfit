package com.rustanovych.kidsoutfit.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Text sits on translucent cards over pictures that change with the weather, so the colour behind
 * it is not known. These checks hold the tokens to the WCAG AA ratio for normal text (4.5:1)
 * against the extremes a picture can reach — pure white, pure black and mid greys — which bounds
 * every real background, the brightest (sunny) and the darkest (storm) included.
 */
class GlassContrastTest {

    private companion object {
        const val TEXT_MINIMUM = 4.5
        const val GRAPHIC_MINIMUM = 3.0
    }

    private val worstCaseBackgrounds = listOf(
        Color.White,
        Color.Black,
        Color(0xFF808080),
        Color(0xFFBFBFBF),
        Color(0xFF404040),
    )

    private fun ratio(a: Color, b: Color): Double {
        val l1 = a.luminance().toDouble()
        val l2 = b.luminance().toDouble()
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    private fun assertReadable(label: String, text: Color, surface: Color, minimum: Double = TEXT_MINIMUM) {
        for (background in worstCaseBackgrounds) {
            // The text itself is opaque; only the surface lets the picture through.
            val ratio = ratio(text, surface.compositeOver(background))
            assertTrue("$label over $background: %.2f:1 is below %.1f:1".format(ratio, minimum), ratio >= minimum)
        }
    }

    private fun check(name: String, glass: GlassColors) {
        assertReadable("$name ink on card", glass.ink, glass.card)
        assertReadable("$name secondary ink on card", glass.inkSecondary, glass.card)
        // The link colour is only an icon colour on a card (the location pin, the hanger), where
        // WCAG asks 3:1 for graphics; its uses as text are on the plain surfaces below.
        assertReadable("$name link icon on card", glass.link, glass.card, GRAPHIC_MINIMUM)
        assertReadable("$name text on amber banner", glass.onAmber, glass.amberContainer)
        assertReadable("$name text on selected strip card", glass.ink, glass.accentSoft)
        assertReadable("$name text on accent", glass.onAccent, glass.accent)
        assertReadable("$name text on status bar", glass.onStatusBar, glass.statusBar)

        // Plain screens have no picture behind them: just the two surfaces.
        for ((label, text) in listOf("ink" to glass.ink, "secondary ink" to glass.inkSecondary, "link" to glass.link)) {
            assertTrue("$name $label on settings background", ratio(text, glass.settingsBackground) >= 4.5)
            assertTrue("$name $label on settings card", ratio(text, glass.settingsCard) >= 4.5)
            assertTrue("$name $label on page top", ratio(text, glass.pageTop) >= 4.5)
            assertTrue("$name $label on page bottom", ratio(text, glass.pageBottom) >= 4.5)
        }
    }

    @Test
    fun `light theme text is readable on every surface`() = check("light", LightGlassColors)

    @Test
    fun `dark theme text is readable on every surface`() = check("dark", DarkGlassColors)
}
