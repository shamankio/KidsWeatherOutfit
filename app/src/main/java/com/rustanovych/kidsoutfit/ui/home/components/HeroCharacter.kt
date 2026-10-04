package com.rustanovych.kidsoutfit.ui.home.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rustanovych.kidsoutfit.R
import com.rustanovych.kidsoutfit.domain.model.OutfitSet
import com.rustanovych.kidsoutfit.ui.assets.outfitNameRes
import com.rustanovych.kidsoutfit.ui.components.GlassSurface
import com.rustanovych.kidsoutfit.ui.theme.LocalGlassColors

private const val HERO_TRANSITION_MS = 280
private const val HERO_ENTER_SCALE = 0.92f

/**
 * The large illustration of the child (or both children) with the outfit name below.
 *
 * Both figures are drawn with [ContentScale.Fit]: whatever the space, the characters shrink rather
 * than being cropped, so a pair stays fully visible top to bottom. Pass a fixed height in
 * [modifier]; the hero itself does not decide how tall it is.
 */
@Composable
fun HeroCharacter(
    @DrawableRes outfits: List<Int>,
    outfitLabel: OutfitSet,
    modifier: Modifier = Modifier,
) {
    val glass = LocalGlassColors.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            // Soft light behind the figure: keeps it the brightest thing on any background.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxHeight()
                    .width(dimensionResource(R.dimen.home_hero_halo_width))
                    .drawBehind {
                        drawOval(
                            brush = Brush.radialGradient(
                                0.52f to glass.halo.copy(alpha = glass.halo.alpha * 0.96f),
                                0.78f to glass.halo.copy(alpha = glass.halo.alpha * 0.55f),
                                1f to Color.Transparent,
                                center = Offset(size.width / 2, size.height / 2),
                                radius = size.width / 2,
                            ),
                        )
                    },
            )
            AnimatedContent(
                targetState = outfits,
                transitionSpec = {
                    (fadeIn(tween(HERO_TRANSITION_MS)) + scaleIn(tween(HERO_TRANSITION_MS), HERO_ENTER_SCALE))
                        .togetherWith(
                            fadeOut(tween(HERO_TRANSITION_MS)) + scaleOut(tween(HERO_TRANSITION_MS), HERO_ENTER_SCALE),
                        )
                },
                label = "heroFigures",
                modifier = Modifier.fillMaxSize(),
            ) { drawables ->
                Figures(drawables)
            }
        }
        AnimatedContent(
            targetState = outfitLabel,
            transitionSpec = { fadeIn(tween(HERO_TRANSITION_MS)) togetherWith fadeOut(tween(HERO_TRANSITION_MS)) },
            label = "heroName",
        ) { set ->
            OutfitNamePill(stringResource(outfitNameRes(set)), modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun Figures(@DrawableRes drawables: List<Int>) {
    if (drawables.size == 1) {
        Image(
            painter = painterResource(drawables.single()),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            alignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize(),
        )
        return
    }
    // Two figures read as one group: equal halves, each hugging the centre and pulled slightly
    // into the other so they overlap.
    val overlap = dimensionResource(R.dimen.home_hero_pair_overlap)
    Row(modifier = Modifier.fillMaxSize()) {
        drawables.forEachIndexed { index, drawable ->
            val isFirst = index == 0
            Image(
                painter = painterResource(drawable),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alignment = if (isFirst) Alignment.BottomEnd else Alignment.BottomStart,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .offset(x = if (isFirst) overlap / 2 else -overlap / 2),
            )
        }
    }
}

@Composable
private fun OutfitNamePill(name: String, modifier: Modifier = Modifier) {
    val glass = LocalGlassColors.current
    GlassSurface(
        modifier = modifier,
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_radius_pill)),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = dimensionResource(R.dimen.home_hero_name_pill_height))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_ms_checkroom),
                contentDescription = null,
                tint = glass.link,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_small)),
            )
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
            )
        }
    }
}
