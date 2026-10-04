package com.rustanovych.kidsoutfit.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rustanovych.kidsoutfit.R
import com.rustanovych.kidsoutfit.ui.theme.LocalGlassColors

/** Thin bar shown above the content while the forecast is cached data after a failed refresh. */
@Composable
fun StaleBar(updatedAtLabel: String?, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    val glass = LocalGlassColors.current
    val message = if (updatedAtLabel != null) {
        stringResource(R.string.home_stale_with_time, updatedAtLabel)
    } else {
        stringResource(R.string.home_stale)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(glass.statusBar)
            .clickable(
                onClickLabel = stringResource(R.string.home_refresh),
                role = Role.Button,
                onClick = onRefresh,
            )
            .heightIn(min = dimensionResource(R.dimen.home_stale_bar_min_height))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_ms_cloud_off),
            contentDescription = null,
            tint = glass.onStatusBar,
            modifier = Modifier.size(dimensionResource(R.dimen.icon_tiny)),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = glass.onStatusBar,
        )
        Icon(
            painter = painterResource(R.drawable.ic_ms_refresh),
            contentDescription = null,
            tint = glass.accent,
            modifier = Modifier.size(dimensionResource(R.dimen.icon_tiny)),
        )
    }
}
