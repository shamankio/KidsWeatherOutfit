package com.rustanovych.kidsoutfit.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.rustanovych.kidsoutfit.R
import com.rustanovych.kidsoutfit.ui.components.GlassSurface
import com.rustanovych.kidsoutfit.ui.components.shimmer

private const val SKELETON_STRIP_CARDS = 5

/** Shimmering stand-ins for the weather card, the hero and the strip while there is no data yet. */
@Composable
fun HomeSkeleton(heroHeight: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val spacing = dimensionResource(R.dimen.home_section_spacing)
    val sidePadding = dimensionResource(R.dimen.screen_horizontal_padding)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        GlassSurface(modifier = Modifier.padding(horizontal = sidePadding).fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(dimensionResource(R.dimen.home_weather_card_padding)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SkeletonBlock(
                    Modifier
                        .width(dimensionResource(R.dimen.home_skeleton_temp_width))
                        .height(dimensionResource(R.dimen.home_skeleton_temp_height)),
                    RoundedCornerShape(14.dp),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonBlock(
                        Modifier
                            .width(dimensionResource(R.dimen.home_skeleton_line_short))
                            .height(dimensionResource(R.dimen.home_skeleton_line_height)),
                        RoundedCornerShape(6.dp),
                    )
                    SkeletonBlock(
                        Modifier
                            .width(dimensionResource(R.dimen.home_skeleton_line_long))
                            .height(dimensionResource(R.dimen.home_skeleton_line_height)),
                        RoundedCornerShape(6.dp),
                    )
                }
                SkeletonBlock(Modifier.size(dimensionResource(R.dimen.home_weather_icon_circle)), CircleShape)
            }
        }
        // A figure-shaped silhouette: head, body, legs, then the name pill.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        ) {
            SkeletonBlock(Modifier.size(dimensionResource(R.dimen.home_skeleton_head)), CircleShape)
            SkeletonBlock(
                Modifier
                    .width(dimensionResource(R.dimen.home_skeleton_body_width))
                    .height(dimensionResource(R.dimen.home_skeleton_body_height)),
                RoundedCornerShape(44.dp, 44.dp, 24.dp, 24.dp),
            )
            SkeletonBlock(
                Modifier
                    .width(dimensionResource(R.dimen.home_skeleton_pill_width))
                    .height(dimensionResource(R.dimen.home_skeleton_pill_height)),
                RoundedCornerShape(17.dp),
            )
        }
        Row(
            modifier = Modifier
                .padding(horizontal = sidePadding)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.home_strip_card_spacing)),
        ) {
            repeat(SKELETON_STRIP_CARDS) {
                SkeletonBlock(
                    Modifier
                        .width(dimensionResource(R.dimen.home_strip_card_width))
                        .height(dimensionResource(R.dimen.home_skeleton_strip_height)),
                    RoundedCornerShape(dimensionResource(R.dimen.home_strip_card_radius)),
                )
            }
        }
    }
}

@Composable
private fun SkeletonBlock(modifier: Modifier, shape: androidx.compose.ui.graphics.Shape) {
    Box(modifier = modifier.clip(shape).shimmer())
}
