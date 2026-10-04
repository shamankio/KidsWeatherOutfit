package com.rustanovych.kidsoutfit.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import com.rustanovych.kidsoutfit.R
import com.rustanovych.kidsoutfit.ui.theme.LocalGlassColors

/**
 * A translucent card for content that floats over a weather background. Fill, border and text
 * colour all come from [com.rustanovych.kidsoutfit.ui.theme.GlassColors] so contrast holds in both
 * themes.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = LocalGlassColors.current.card,
    borderColor: Color = LocalGlassColors.current.cardBorder,
    borderWidth: Dp = dimensionResource(R.dimen.glass_border_width),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = LocalGlassColors.current.ink,
        border = BorderStroke(borderWidth, borderColor),
        shadowElevation = dimensionResource(R.dimen.glass_shadow_elevation),
        content = { Box { content() } },
    )
}
