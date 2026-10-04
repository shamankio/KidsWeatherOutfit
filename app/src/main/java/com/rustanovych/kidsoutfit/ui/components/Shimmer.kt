package com.rustanovych.kidsoutfit.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.rustanovych.kidsoutfit.ui.theme.LocalGlassColors

private const val SHIMMER_DURATION_MS = 1400

/** Placeholder fill with a highlight sweeping across, for skeleton blocks. */
@Composable
fun Modifier.shimmer(): Modifier {
    val glass = LocalGlassColors.current
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress = transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(SHIMMER_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )
    return drawBehind {
        // Read in the draw phase, so the sweep does not recompose anything.
        val centre = size.width * progress.value
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(glass.skeletonBase, glass.skeletonHighlight, glass.skeletonBase),
                start = Offset(centre - size.width * 0.5f, 0f),
                end = Offset(centre + size.width * 0.5f, 0f),
            ),
        )
    }
}
