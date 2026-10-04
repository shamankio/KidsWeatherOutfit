package com.rustanovych.kidsoutfit.ui.settings

import android.Manifest
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rustanovych.kidsoutfit.BuildConfig
import com.rustanovych.kidsoutfit.R
import com.rustanovych.kidsoutfit.domain.model.ChildGender
import com.rustanovych.kidsoutfit.domain.model.Place
import com.rustanovych.kidsoutfit.ui.theme.KidsWeatherOutfitTheme
import com.rustanovych.kidsoutfit.ui.theme.LocalGlassColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsRoute(
    focusCitySearch: Boolean,
    onBack: () -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
        viewModel::onLocationPermissionResult,
    )

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SettingsEffect.RequestLocationPermission ->
                    permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                // Onboarding sent the user here only to pick a city; once they have, they are done.
                SettingsEffect.PlacePicked -> if (focusCitySearch) onBack()
            }
        }
    }
    LaunchedEffect(focusCitySearch) {
        if (focusCitySearch) viewModel.onManualLocationSelected()
    }

    SettingsContent(
        state = state,
        appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        focusCitySearch = focusCitySearch,
        onBack = onBack,
        onGenderSelected = viewModel::onGenderSelected,
        onSensitivityChanged = viewModel::onSensitivityChanged,
        onDepartureTimeSelected = viewModel::onDepartureTimeSelected,
        onAutoLocationSelected = viewModel::onAutoLocationSelected,
        onManualLocationSelected = viewModel::onManualLocationSelected,
        onQueryChanged = viewModel::onQueryChanged,
        onPlaceSelected = viewModel::onPlaceSelected,
        onNotificationsToggled = viewModel::onNotificationsToggled,
        onOpenLicenses = onOpenLicenses,
        modifier = modifier,
    )
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    appVersion: String,
    focusCitySearch: Boolean,
    onBack: () -> Unit,
    onGenderSelected: (ChildGender) -> Unit,
    onSensitivityChanged: (Int) -> Unit,
    onDepartureTimeSelected: (Int, Int) -> Unit,
    onAutoLocationSelected: () -> Unit,
    onManualLocationSelected: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onPlaceSelected: (Place) -> Unit,
    onNotificationsToggled: (Boolean) -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glass = LocalGlassColors.current
    var timePickerOpen by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = glass.settingsBackground,
        topBar = { SettingsTopBar(onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ChildSection(
                gender = state.gender,
                sensitivity = state.sensitivity,
                onGenderSelected = onGenderSelected,
                onSensitivityChanged = onSensitivityChanged,
            )
            DepartureSection(timeLabel = state.departureTimeLabel, onClick = { timePickerOpen = true })
            LocationSection(
                state = state,
                focusCitySearch = focusCitySearch,
                onAutoSelected = onAutoLocationSelected,
                onManualSelected = onManualLocationSelected,
                onQueryChanged = onQueryChanged,
                onPlaceSelected = onPlaceSelected,
            )
            NotificationsSection(
                enabled = state.notificationsEnabled,
                reminderTimeLabel = state.reminderTimeLabel,
                onToggled = onNotificationsToggled,
            )
            AboutSection {
                SettingsRow(
                    title = stringResource(R.string.settings_app_version),
                    value = appVersion,
                    leading = { AppIcon() },
                )
                AboutDivider()
                SettingsRow(
                    title = stringResource(R.string.settings_licenses),
                    iconRes = R.drawable.ic_ms_description,
                    showChevron = true,
                    onClick = onOpenLicenses,
                )
                // Stage 7: a "Privacy settings" row (UMP consent form) goes here, as another SettingsRow.
            }
        }
    }

    if (timePickerOpen) {
        DepartureTimeDialog(
            hour = state.departureHour,
            minute = state.departureMinute,
            onConfirm = { hour, minute ->
                onDepartureTimeSelected(hour, minute)
                timePickerOpen = false
            },
            onDismiss = { timePickerOpen = false },
        )
    }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LocalGlassColors.current.settingsBackground)
            .padding(horizontal = 8.dp)
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_ms_arrow_back),
                contentDescription = stringResource(R.string.settings_back),
            )
        }
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(vertical = 12.dp),
        )
    }
}

/** The launcher icon: its adaptive XML cannot be drawn directly, so compose its two layers. */
@Composable
private fun AppIcon() {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(colorResource(R.color.ic_launcher_background), RoundedCornerShape(10.dp)),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepartureTimeDialog(
    hour: Int,
    minute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val picker = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = DateFormat.is24HourFormat(LocalContext.current),
    )
    TimePickerDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_leaves_home)) },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_time_cancel)) }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(picker.hour, picker.minute) }) {
                Text(stringResource(R.string.settings_time_ok))
            }
        },
    ) {
        TimePicker(state = picker)
    }
}

// ---- Previews ---------------------------------------------------------------------------------

private val previewState = SettingsUiState(
    gender = ChildGender.BOTH,
    sensitivity = 2,
    departureTimeLabel = "08:00",
    locationChoice = LocationChoice.MANUAL,
    pickedPlaceLabel = "Kyiv, Kyiv Oblast, Ukraine",
    query = "Ky",
    search = SearchStatus.Results(
        listOf(
            PlaceUi(Place("Kyiv", "Kyiv Oblast", "Ukraine", 0.0, 0.0), "Kyiv", "Kyiv Oblast, Ukraine"),
            PlaceUi(Place("Kyrenia", "Kyrenia District", "Cyprus", 0.0, 0.0), "Kyrenia", "Kyrenia District, Cyprus"),
        ),
    ),
    notificationsEnabled = true,
    reminderTimeLabel = "07:30",
)

@Composable
private fun PreviewSettings(state: SettingsUiState) {
    KidsWeatherOutfitTheme {
        SettingsContent(
            state = state, appVersion = "1.0 (1)", focusCitySearch = false, onBack = {},
            onGenderSelected = {}, onSensitivityChanged = {}, onDepartureTimeSelected = { _, _ -> },
            onAutoLocationSelected = {}, onManualLocationSelected = {}, onQueryChanged = {},
            onPlaceSelected = {}, onNotificationsToggled = {}, onOpenLicenses = {},
        )
    }
}

@Preview(name = "Settings", widthDp = 360, heightDp = 1250)
@Composable
private fun SettingsPreview() = PreviewSettings(previewState)

@Preview(name = "Settings, GPS", widthDp = 360, heightDp = 1000)
@Composable
private fun SettingsGpsPreview() = PreviewSettings(
    previewState.copy(
        sensitivity = -1,
        locationChoice = LocationChoice.AUTO,
        autoPlaceName = "Poznań",
        notificationsEnabled = false,
    ),
)
