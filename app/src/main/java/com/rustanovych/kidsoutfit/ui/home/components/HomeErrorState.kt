package com.rustanovych.kidsoutfit.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rustanovych.kidsoutfit.R
import com.rustanovych.kidsoutfit.ui.theme.LocalGlassColors

/** No forecast and the fetch failed: a friendly explanation and a retry. */
@Composable
fun HomeErrorState(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val glass = LocalGlassColors.current
    val haloInner = dimensionResource(R.dimen.home_error_halo_inner)
    val haloOuter = dimensionResource(R.dimen.home_error_halo_outer)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(dimensionResource(R.dimen.home_error_illustration))
                .drawBehind {
                    drawCircle(glass.accent.copy(alpha = 0.05f), radius = size.minDimension / 2 + haloOuter.toPx())
                    drawCircle(glass.accent.copy(alpha = 0.10f), radius = size.minDimension / 2 + haloInner.toPx())
                }
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_weather_cloudy),
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(R.dimen.home_error_icon)),
            )
        }
        Text(
            text = stringResource(R.string.home_error_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            color = glass.ink,
            modifier = Modifier.padding(top = 26.dp),
        )
        Text(
            text = stringResource(R.string.home_error_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = glass.inkSecondary,
            modifier = Modifier.widthIn(max = dimensionResource(R.dimen.home_error_text_max_width)),
        )
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = glass.accent, contentColor = glass.onAccent),
            modifier = Modifier
                .padding(top = 10.dp)
                .height(dimensionResource(R.dimen.button_height)),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_ms_refresh),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = stringResource(R.string.home_error_retry),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
