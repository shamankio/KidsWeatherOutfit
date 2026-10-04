package com.rustanovych.kidsoutfit.ui.home.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.rustanovych.kidsoutfit.R
import com.rustanovych.kidsoutfit.ui.components.GlassSurface
import com.rustanovych.kidsoutfit.ui.home.Day
import com.rustanovych.kidsoutfit.ui.theme.LocalGlassColors

/** "Today / Tomorrow" as a Material 3 segmented button on a glass track. */
@Composable
fun DaySwitcher(
    selectedDay: Day,
    onDaySelected: (Day) -> Unit,
    modifier: Modifier = Modifier,
) {
    val glass = LocalGlassColors.current
    val days = Day.entries
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_radius_pill)),
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.home_day_switcher_padding)),
        ) {
            days.forEachIndexed { index, day ->
                val selected = day == selectedDay
                SegmentedButton(
                    selected = selected,
                    onClick = { onDaySelected(day) },
                    shape = SegmentedButtonDefaults.itemShape(index, days.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = glass.accent,
                        activeContentColor = glass.onAccent,
                        activeBorderColor = Color.Transparent,
                        inactiveContainerColor = Color.Transparent,
                        inactiveContentColor = glass.ink,
                        inactiveBorderColor = Color.Transparent,
                    ),
                    modifier = Modifier.height(dimensionResource(R.dimen.home_day_switcher_height)),
                ) {
                    Text(
                        text = stringResource(
                            if (day == Day.TODAY) R.string.home_day_today else R.string.home_day_tomorrow,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}
