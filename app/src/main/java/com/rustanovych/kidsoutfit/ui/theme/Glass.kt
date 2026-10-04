package com.rustanovych.kidsoutfit.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colours for surfaces that sit on top of the full-colour weather backgrounds. They are not
 * Material roles on purpose: the background changes with the weather, so every surface above it is
 * a translucent card whose own colour carries the contrast, in both light and dark themes.
 */
@Immutable
class GlassColors(
    /** Translucent card fill. Opaque enough that text clears 4.5:1 over the darkest background. */
    val card: Color,
    val cardBorder: Color,
    /** Primary text on [card]. */
    val ink: Color,
    /** Secondary text on [card]. */
    val inkSecondary: Color,
    /** Selection and primary actions. */
    val accent: Color,
    val onAccent: Color,
    /** Quiet tint for "current" states. */
    val accentSoft: Color,
    /** Icons and links on [card]. */
    val link: Color,
    val amberContainer: Color,
    val amberBorder: Color,
    val onAmber: Color,
    val statusBar: Color,
    val onStatusBar: Color,
    val skeletonBase: Color,
    val skeletonHighlight: Color,
    /** Backdrop for the states that have no weather yet (loading, error). */
    val pageTop: Color,
    val pageBottom: Color,
    /** Soft light behind the character, so it reads on any background. */
    val halo: Color,
    /** Plain backdrop and card of the settings-style screens, which have no weather picture. */
    val settingsBackground: Color,
    val settingsCard: Color,
    /** Fades the top of a background so status-bar content stays readable over it. */
    val scrim: Color,
)

val LightGlassColors = GlassColors(
    card = Color(0xC7FFFFFF),
    cardBorder = Color(0xB3FFFFFF),
    ink = Color(0xFF15324A),
    inkSecondary = Color(0xFF3E5668),
    accent = Color(0xFF4FC3F7),
    onAccent = Color(0xFF0B2A3F),
    accentSoft = Color(0xFFE1F5FE),
    link = Color(0xFF0277BD),
    amberContainer = Color(0xE6FFECB3),
    amberBorder = Color(0x99FFD54F),
    onAmber = Color(0xFF4A3500),
    statusBar = Color(0xDB15324A),
    onStatusBar = Color.White,
    skeletonBase = Color(0xFFDCEAF2),
    skeletonHighlight = Color(0xFFF4F9FC),
    pageTop = Color(0xFFD4ECF9),
    pageBottom = Color(0xFFF4F8FA),
    halo = Color.White,
    scrim = Color(0x80FFFFFF),
    settingsBackground = Color(0xFFF2F7FA),
    settingsCard = Color.White,
)

val DarkGlassColors = GlassColors(
    card = Color(0xD9181A1D),
    cardBorder = Color(0x33FFFFFF),
    ink = Color(0xFFE4E5E7),
    inkSecondary = Color(0xFFC5CDD3),
    accent = Color(0xFF4FC3F7),
    onAccent = Color(0xFF0B2A3F),
    accentSoft = Color(0xFF0B3C57),
    link = Color(0xFF81D4FA),
    amberContainer = Color(0xF2433300),
    amberBorder = Color(0x99FFD54F),
    onAmber = Color(0xFFFFECB3),
    statusBar = Color(0xF20B1A24),
    onStatusBar = Color.White,
    skeletonBase = Color(0xFF2A3036),
    skeletonHighlight = Color(0xFF3A434B),
    pageTop = Color(0xFF14212B),
    pageBottom = Color(0xFF1E2A33),
    halo = Color(0xB3FFFFFF),
    scrim = Color(0x66000000),
    settingsBackground = Color(0xFF101418),
    settingsCard = Color(0xFF1B2128),
)

val LocalGlassColors = staticCompositionLocalOf { LightGlassColors }
