package com.rustanovych.kidsoutfit.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rustanovych.kidsoutfit.R
import com.rustanovych.kidsoutfit.ui.components.GlassSurface
import com.rustanovych.kidsoutfit.ui.components.OutfitThumbnail
import com.rustanovych.kidsoutfit.ui.home.SegmentUi
import com.rustanovych.kidsoutfit.ui.theme.LocalGlassColors

/**
 * The day's outfit changes as a horizontal strip of cards, each an upper-body thumbnail over its
 * start time. The card active at departure ([SegmentUi.isCurrent]) gets a quiet accent; the tapped
 * one ([selectedIndex]) a stronger one.
 *
 * @param focusIndex Card scrolled into view whenever it changes: the shown segment.
 */
@Composable
fun SegmentStrip(
    segments: List<SegmentUi>,
    selectedIndex: Int?,
    focusIndex: Int?,
    onSegmentSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = dimensionResource(R.dimen.screen_horizontal_padding)),
) {
    val listState = rememberLazyListState()
    LaunchedEffect(focusIndex) {
        if (focusIndex != null && focusIndex in segments.indices) listState.animateScrollToItem(focusIndex)
    }
    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.home_strip_card_spacing)),
    ) {
        itemsIndexed(segments) { index, segment ->
            SegmentCard(
                segment = segment,
                selected = index == selectedIndex,
                onClick = { onSegmentSelected(index) },
            )
        }
    }
}

@Composable
private fun SegmentCard(segment: SegmentUi, selected: Boolean, onClick: () -> Unit) {
    val glass = LocalGlassColors.current
    val shape = RoundedCornerShape(dimensionResource(R.dimen.home_strip_card_radius))
    val highlighted = selected || segment.isCurrent
    GlassSurface(
        modifier = Modifier
            .width(dimensionResource(R.dimen.home_strip_card_width))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = shape,
        color = if (highlighted) glass.accentSoft else glass.card,
        // Selected is a thick solid accent rim, current a thinner softer one: told apart by width
        // as well as colour.
        borderColor = when {
            selected -> glass.accent
            segment.isCurrent -> glass.accent.copy(alpha = 0.6f)
            else -> glass.cardBorder
        },
        borderWidth = when {
            selected -> dimensionResource(R.dimen.home_strip_border_selected)
            segment.isCurrent -> dimensionResource(R.dimen.home_strip_border_current)
            else -> dimensionResource(R.dimen.glass_border_width)
        },
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.home_strip_card_padding)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            OutfitThumbnail(
                drawables = segment.drawables,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.home_segment_from, segment.timeLabel),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
    }
}
