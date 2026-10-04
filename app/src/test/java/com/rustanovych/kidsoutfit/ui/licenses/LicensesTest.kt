package com.rustanovych.kidsoutfit.ui.licenses

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LicensesTest {

    @Test
    fun `entries parse with an optional url`() {
        val entries = parseLicenses(
            """[{"name":"Koin","license":"Apache License 2.0","url":"https://insert-koin.io"},
                {"name":"Nunito","license":"SIL OFL 1.1"}]""",
        )

        assertEquals(listOf("Koin", "Nunito"), entries.map { it.name })
        assertEquals("https://insert-koin.io", entries[0].url)
        assertNull(entries[1].url)
    }

    @Test
    fun `a malformed file is an empty list, not a crash`() {
        assertTrue(parseLicenses("not json").isEmpty())
    }

    @Test
    fun `the shipped file parses and credits Open-Meteo`() {
        val shipped = java.io.File("src/main/res/raw/licenses.json").readText()

        val entries = parseLicenses(shipped)

        assertTrue(entries.isNotEmpty())
        assertTrue(entries.any { it.name.contains("Open-Meteo") })
    }
}
