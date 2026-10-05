package com.rustanovych.kidsoutfit.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rustanovych.kidsoutfit.R
import com.rustanovych.kidsoutfit.domain.model.ChildGender
import com.rustanovych.kidsoutfit.domain.model.OutfitSet
import com.rustanovych.kidsoutfit.domain.model.Place
import com.rustanovych.kidsoutfit.ui.assets.outfitDrawables
import com.rustanovych.kidsoutfit.ui.components.OutfitThumbnail
import com.rustanovych.kidsoutfit.ui.theme.LocalGlassColors
import kotlin.math.abs

private const val SLIDER_MIN = -3
private const val SLIDER_MAX = 3
private const val SLIDER_STEPS = SLIDER_MAX - SLIDER_MIN - 1
private const val MIN_QUERY_LENGTH = 2

// ---- Building blocks -------------------------------------------------------------------------

/** Blue caption above a group of rows. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Black,
        color = LocalGlassColors.current.link,
        modifier = modifier.padding(start = 8.dp, top = 12.dp),
    )
}

/** The white rounded container the rows of one section sit in. */
@Composable
fun SettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = LocalGlassColors.current.settingsCard,
        contentColor = LocalGlassColors.current.ink,
    ) {
        Column(content = content)
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = LocalGlassColors.current.ink.copy(alpha = 0.08f),
    )
}

/**
 * A tappable row with a leading icon tile, a title, an optional value and a trailing chevron. The
 * About section is a list of these, so a further row (stage 7's "Privacy settings") is one more
 * call.
 */
@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
    iconTint: Color = LocalGlassColors.current.inkSecondary,
    value: String? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    val glass = LocalGlassColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when {
            leading != null -> leading()
            iconRes != null -> Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(32.dp).padding(5.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = glass.inkSecondary,
            )
        }
        if (showChevron) {
            Icon(
                painter = painterResource(R.drawable.ic_ms_chevron_right),
                contentDescription = null,
                tint = glass.inkSecondary,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_small) + 2.dp),
            )
        }
    }
}

// ---- Child -----------------------------------------------------------------------------------

@Composable
fun ChildSection(
    gender: ChildGender,
    sensitivity: Int,
    onGenderSelected: (ChildGender) -> Unit,
    onSensitivityChanged: (Int) -> Unit,
) {
    SectionHeader(stringResource(R.string.settings_section_child))
    SettingsCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChildGender.entries.forEach { option ->
                    GenderCard(
                        gender = option,
                        selected = option == gender,
                        onClick = { onGenderSelected(option) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            HorizontalDivider(color = LocalGlassColors.current.ink.copy(alpha = 0.08f))
            SensitivitySlider(sensitivity, onSensitivityChanged)
        }
    }
}

@Composable
private fun GenderCard(
    gender: ChildGender,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glass = LocalGlassColors.current
    val label = stringResource(
        when (gender) {
            ChildGender.BOY -> R.string.settings_gender_boy
            ChildGender.GIRL -> R.string.settings_gender_girl
            ChildGender.BOTH -> R.string.settings_gender_both
        },
    )
    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (selected) glass.accentSoft else glass.settingsBackground,
            border = BorderStroke(2.dp, if (selected) glass.accent else glass.ink.copy(alpha = 0.08f)),
            modifier = Modifier
                .fillMaxWidth()
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        ) {
            Column(
                modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // The same upper-body crop as the Home strip, so the child is recognisable at card size.
                OutfitThumbnail(
                    drawables = outfitDrawables(OutfitSet.COOL, gender),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
            }
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 2.dp)
                    .size(22.dp)
                    .background(glass.accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ms_check),
                    contentDescription = null,
                    tint = glass.onAccent,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

@Composable
private fun SensitivitySlider(value: Int, onValueChange: (Int) -> Unit) {
    val glass = LocalGlassColors.current
    val chip = when {
        value > 0 -> stringResource(R.string.settings_sensitivity_warmer, value)
        value < 0 -> stringResource(R.string.settings_sensitivity_lighter, -value)
        else -> stringResource(R.string.settings_sensitivity_neutral)
    }
    val caption = when {
        value > 0 -> stringResource(R.string.settings_sensitivity_caption_colder, value)
        value < 0 -> stringResource(R.string.settings_sensitivity_caption_warmer, abs(value))
        else -> stringResource(R.string.settings_sensitivity_caption_neutral)
    }
    val chipColor = when {
        value > 0 -> glass.accent.copy(alpha = 0.35f)
        value < 0 -> Color(0xFFFFB74D).copy(alpha = 0.35f)
        else -> glass.ink.copy(alpha = 0.08f)
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(R.string.settings_sensitivity_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
            Surface(shape = RoundedCornerShape(14.dp), color = chipColor, contentColor = glass.ink) {
                Text(
                    text = chip,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = SLIDER_MIN.toFloat()..SLIDER_MAX.toFloat(),
            steps = SLIDER_STEPS,
            colors = SliderDefaults.colors(
                thumbColor = glass.link,
                // One colour on both sides: the range is centred on 0, so a fill running from the
                // left edge would read "0" as half-way to "gets cold easily".
                activeTrackColor = glass.ink.copy(alpha = 0.15f),
                inactiveTrackColor = glass.ink.copy(alpha = 0.15f),
                activeTickColor = glass.link,
                inactiveTickColor = glass.link,
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            EdgeLabel(R.drawable.ic_ms_local_fire_department, Color(0xFFE65100), stringResource(R.string.settings_sensitivity_runs_hot), iconFirst = true)
            EdgeLabel(R.drawable.ic_ms_ac_unit, glass.link, stringResource(R.string.settings_sensitivity_gets_cold), iconFirst = false)
        }
        Text(
            text = caption,
            style = MaterialTheme.typography.bodySmall,
            color = glass.inkSecondary,
        )
    }
}

@Composable
private fun EdgeLabel(@DrawableRes icon: Int, tint: Color, text: String, iconFirst: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        val image = @Composable {
            Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(dimensionResource(R.dimen.icon_tiny)))
        }
        if (iconFirst) image()
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = LocalGlassColors.current.inkSecondary,
        )
        if (!iconFirst) image()
    }
}

// ---- Departure -------------------------------------------------------------------------------

@Composable
fun DepartureSection(timeLabel: String, onClick: () -> Unit) {
    SectionHeader(stringResource(R.string.settings_section_departure))
    SettingsCard {
        SettingsRow(
            title = stringResource(R.string.settings_leaves_home),
            iconRes = R.drawable.ic_ms_directions_walk,
            iconTint = LocalGlassColors.current.link,
            value = timeLabel,
            showChevron = true,
            onClick = onClick,
        )
    }
}

// ---- Location --------------------------------------------------------------------------------

@Composable
fun LocationSection(
    state: SettingsUiState,
    focusCitySearch: Boolean,
    onAutoSelected: () -> Unit,
    onManualSelected: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onPlaceSelected: (Place) -> Unit,
) {
    SectionHeader(stringResource(R.string.settings_section_location))
    SettingsCard {
        Column(Modifier.selectableGroup()) {
            LocationOption(
                title = stringResource(R.string.settings_location_auto),
                subtitle = state.autoPlaceName,
                iconRes = R.drawable.ic_ms_my_location,
                selected = state.locationChoice == LocationChoice.AUTO,
                onClick = onAutoSelected,
            )
            LocationOption(
                title = stringResource(R.string.settings_location_manual),
                subtitle = state.pickedPlaceLabel ?: stringResource(R.string.settings_location_manual_hint),
                iconRes = R.drawable.ic_ms_travel_explore,
                selected = state.locationChoice == LocationChoice.MANUAL,
                onClick = onManualSelected,
            )
        }
        if (state.locationChoice == LocationChoice.MANUAL) {
            CitySearch(state, focusCitySearch, onQueryChanged, onPlaceSelected)
        }
    }
}

@Composable
private fun LocationOption(
    title: String,
    subtitle: String?,
    @DrawableRes iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val glass = LocalGlassColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // The whole row is the radio's target, so the button itself does not handle clicks.
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = glass.link),
            modifier = Modifier.padding(start = 4.dp, end = 4.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = glass.inkSecondary)
            }
        }
        Icon(painterResource(iconRes), contentDescription = null, tint = glass.inkSecondary, modifier = Modifier.size(dimensionResource(R.dimen.icon_small)))
    }
}

@Composable
private fun CitySearch(
    state: SettingsUiState,
    focusCitySearch: Boolean,
    onQueryChanged: (String) -> Unit,
    onPlaceSelected: (Place) -> Unit,
) {
    val glass = LocalGlassColors.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusCitySearch) {
        if (focusCitySearch) runCatching { focusRequester.requestFocus() }
    }
    Column(
        modifier = Modifier.padding(start = 52.dp, end = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChanged,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            placeholder = { Text(stringResource(R.string.settings_city_search)) },
            leadingIcon = {
                Icon(painterResource(R.drawable.ic_ms_search), contentDescription = null)
            },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChanged("") }) {
                        Icon(painterResource(R.drawable.ic_ms_close), contentDescription = stringResource(R.string.settings_city_clear))
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = glass.accent,
                unfocusedBorderColor = glass.ink.copy(alpha = 0.2f),
                focusedContainerColor = glass.settingsBackground,
                unfocusedContainerColor = glass.settingsBackground,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
        )
        SearchResults(state, onPlaceSelected)
    }
}

/** The dropdown under the field: results, or a one-line reason there are none. */
@Composable
private fun SearchResults(state: SettingsUiState, onPlaceSelected: (Place) -> Unit) {
    val glass = LocalGlassColors.current
    when (val search = state.search) {
        SearchStatus.Idle -> Unit
        SearchStatus.Loading -> Hint(stringResource(R.string.settings_city_searching))
        SearchStatus.Failed -> Hint(stringResource(R.string.settings_city_search_failed))
        is SearchStatus.Results -> if (search.places.isEmpty()) {
            if (state.query.trim().length >= MIN_QUERY_LENGTH) {
                Hint(stringResource(R.string.settings_city_no_results, state.query.trim()))
            }
        } else {
            // The list opens under the field, which sits just above the keyboard: scroll it into view.
            val requester = remember { BringIntoViewRequester() }
            LaunchedEffect(search.places) { requester.bringIntoView() }
            Surface(
                modifier = Modifier.bringIntoViewRequester(requester),
                shape = RoundedCornerShape(16.dp),
                color = glass.settingsCard,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, glass.ink.copy(alpha = 0.08f)),
            ) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    search.places.forEach { result ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) { onPlaceSelected(result.place) }
                                .heightIn(min = 48.dp)
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(painterResource(R.drawable.ic_ms_location_on), contentDescription = null, tint = glass.link, modifier = Modifier.size(18.dp))
                            Column {
                                Text(result.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black)
                                result.subtitle?.let {
                                    Text(it, style = MaterialTheme.typography.labelSmall, color = glass.inkSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        color = LocalGlassColors.current.inkSecondary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
    )
}

// ---- Notifications ---------------------------------------------------------------------------

@Composable
fun NotificationsSection(
    enabled: Boolean,
    reminderTimeLabel: String,
    onToggled: (Boolean) -> Unit,
    onTestNotification: (() -> Unit)? = null,
) {
    val glass = LocalGlassColors.current
    SectionHeader(stringResource(R.string.settings_section_notifications))
    SettingsCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = enabled, role = Role.Switch, onValueChange = onToggled)
                .heightIn(min = 72.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_ms_notifications_active),
                contentDescription = null,
                tint = Color(0xFFE65100),
                modifier = Modifier.size(32.dp).padding(4.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_reminder_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                Text(
                    text = if (enabled) {
                        stringResource(R.string.settings_reminder_caption_at, reminderTimeLabel)
                    } else {
                        stringResource(R.string.settings_reminder_caption)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.inkSecondary,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(checkedTrackColor = glass.link),
                thumbContent = if (enabled) {
                    { Icon(painterResource(R.drawable.ic_ms_check), contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else {
                    null
                },
            )
        }
        if (onTestNotification != null) {
            RowDivider()
            SettingsRow(
                title = stringResource(R.string.settings_debug_test_notification),
                iconRes = R.drawable.ic_ms_notifications_active,
                onClick = onTestNotification,
            )
        }
    }
}

// ---- About -----------------------------------------------------------------------------------

/**
 * Rows are slotted in by the caller, so a further one (the stage 7 "Privacy settings") is added at
 * the call site without touching this layout. Dividers go between rows via [AboutDivider].
 */
@Composable
fun AboutSection(rows: @Composable ColumnScope.() -> Unit) {
    SectionHeader(stringResource(R.string.settings_section_about))
    SettingsCard(content = rows)
}

@Composable
fun AboutDivider() = RowDivider()
