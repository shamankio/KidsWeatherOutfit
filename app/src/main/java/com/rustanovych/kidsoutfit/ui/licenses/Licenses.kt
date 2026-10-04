package com.rustanovych.kidsoutfit.ui.licenses

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One third-party component and the licence it is used under. */
@Serializable
data class LicenseEntry(
    val name: String,
    val license: String,
    val url: String? = null,
)

private val json = Json { ignoreUnknownKeys = true }

/**
 * Parses the hand-kept `res/raw/licenses.json`. A malformed file yields an empty list rather than
 * a crash on a screen nobody opens for its own sake.
 */
fun parseLicenses(text: String): List<LicenseEntry> =
    try {
        json.decodeFromString<List<LicenseEntry>>(text)
    } catch (_: kotlinx.serialization.SerializationException) {
        emptyList()
    }
