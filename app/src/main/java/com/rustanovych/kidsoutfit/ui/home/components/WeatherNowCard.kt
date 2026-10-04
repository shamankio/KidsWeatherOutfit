package com.rustanovych.kidsoutfit.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.rustanovych.kidsoutfit.ui.components.GlassSurface
import com.rustanovych.kidsoutfit.ui.home.WeatherNowUi
import com.rustanovych.kidsoutfit.ui.theme.LocalGlassColors

/** Big temperature, "feels like", wind and precipitation, and the weather icon. */
@Composable
fun WeatherNowCard(weather: WeatherNowUi, modifier: Modifier = Modifier) {
    val glass = LocalGlassColors.current
    GlassSurface(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(
                start = 16.dp,
                top = dimensionResource(R.dimen.home_weather_card_padding),
                end = dimensionResource(R.dimen.home_weather_card_padding),
                bottom = dimensionResource(R.dimen.home_weather_card_padding),
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = weather.temperatureLabel,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.home_feels_like, weather.apparentLabel),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Stat(R.drawable.ic_ms_air, stringResource(R.string.home_wind), weather.windLabel)
                    Stat(
                        R.drawable.ic_ms_water_drop,
                        stringResource(R.string.home_precipitation),
                        weather.precipitationLabel,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(dimensionResource(R.dimen.home_weather_icon_circle))
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(weather.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(R.dimen.home_weather_icon)),
                )
            }
        }
    }
}

@Composable
private fun Stat(iconRes: Int, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        // The label is the icon's description: "Wind 12 km/h" reads naturally in TalkBack.
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = LocalGlassColors.current.inkSecondary,
            modifier = Modifier.size(dimensionResource(R.dimen.icon_tiny)),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = LocalGlassColors.current.inkSecondary,
        )
    }
}
