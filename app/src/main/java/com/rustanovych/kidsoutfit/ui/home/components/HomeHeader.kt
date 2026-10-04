package com.rustanovych.kidsoutfit.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rustanovych.kidsoutfit.R
import com.rustanovych.kidsoutfit.ui.components.GlassSurface
import com.rustanovych.kidsoutfit.ui.theme.LocalGlassColors

/** Place name on the left, settings on the right. [locationName] `null` shows a generic label. */
@Composable
fun HomeHeader(
    locationName: String?,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glass = LocalGlassColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        GlassSurface(
            shape = RoundedCornerShape(dimensionResource(R.dimen.card_radius_pill)),
            modifier = Modifier.weight(1f, fill = false),
        ) {
            Row(
                modifier = Modifier
                    .heightIn(min = dimensionResource(R.dimen.home_place_pill_height))
                    .widthIn(max = dimensionResource(R.dimen.home_place_pill_max_width))
                    .padding(start = 10.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ms_location_on),
                    contentDescription = null,
                    tint = glass.link,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_small)),
                )
                Text(
                    text = locationName ?: stringResource(R.string.home_location_current),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        GlassSurface(shape = CircleShape) {
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_button_size)),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ms_settings),
                    contentDescription = stringResource(R.string.home_settings_button),
                )
            }
        }
    }
}
