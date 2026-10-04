package com.rustanovych.kidsoutfit.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import com.rustanovych.kidsoutfit.R

/**
 * A square upper-body crop of one or two full-height outfit illustrations.
 *
 * The source drawables are ~1600 px tall standing figures; scaled whole into a card they turn into
 * a coloured blob and similar sets become indistinguishable. [ContentScale.Crop] anchored to
 * [Alignment.TopCenter] fills the width and keeps the head, hat and jacket instead.
 *
 * Two drawables (a boy and a girl) share the same square, side by side.
 */
@Composable
fun OutfitThumbnail(
    @DrawableRes drawables: List<Int>,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(dimensionResource(R.dimen.home_strip_thumb_radius)))
            .background(Color.White),
    ) {
        if (drawables.size == 1) {
            Image(
                painter = painterResource(drawables.single()),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            drawables.forEach { drawable ->
                Image(
                    painter = painterResource(drawable),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}
