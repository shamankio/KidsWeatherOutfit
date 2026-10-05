package com.rustanovych.kidsoutfit.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rustanovych.kidsoutfit.R
import com.rustanovych.kidsoutfit.ui.components.GlassSurface
import com.rustanovych.kidsoutfit.ui.theme.LocalGlassColors

/**
 * A soft, dismissible offer of the morning reminder, shown once the first forecast is on screen.
 * Both answers are final: "No, thanks" is remembered and the card never returns.
 */
@Composable
fun NotificationPromptCard(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glass = LocalGlassColors.current
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.home_banner_radius)),
    ) {
        Column(
            modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ms_notifications_active),
                    contentDescription = null,
                    tint = glass.link,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_small)),
                )
                Text(
                    text = stringResource(R.string.home_notif_prompt_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDecline) { Text(stringResource(R.string.home_notif_prompt_no)) }
                Button(onClick = onAccept) { Text(stringResource(R.string.home_notif_prompt_yes)) }
            }
        }
    }
}
