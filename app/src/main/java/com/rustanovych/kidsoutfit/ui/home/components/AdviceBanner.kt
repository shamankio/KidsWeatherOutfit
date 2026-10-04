package com.rustanovych.kidsoutfit.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rustanovych.kidsoutfit.R
import com.rustanovych.kidsoutfit.ui.assets.adviceRes
import com.rustanovych.kidsoutfit.ui.assets.outfitNameRes
import com.rustanovych.kidsoutfit.ui.components.GlassSurface
import com.rustanovych.kidsoutfit.ui.home.AdviceUi
import com.rustanovych.kidsoutfit.ui.theme.LocalGlassColors

/** "At 15:00 it gets warmer — Warm weather outfit": a soft amber card so it stands out from the glass. */
@Composable
fun AdviceBanner(advice: AdviceUi, modifier: Modifier = Modifier) {
    val glass = LocalGlassColors.current
    val shape = RoundedCornerShape(dimensionResource(R.dimen.home_banner_radius))
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = glass.amberContainer,
        borderColor = glass.amberBorder,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = dimensionResource(R.dimen.home_banner_min_height))
                .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(dimensionResource(R.dimen.home_banner_icon_tile))
                    .background(
                        Color.White,
                        RoundedCornerShape(dimensionResource(R.dimen.home_banner_icon_tile_radius)),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ms_schedule),
                    contentDescription = null,
                    tint = Color(0xFF8A6200),
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_small)),
                )
            }
            Text(
                text = stringResource(
                    adviceRes(advice.direction),
                    advice.atTimeLabel,
                    stringResource(outfitNameRes(advice.toOutfit)),
                ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = glass.onAmber,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
