package com.rustanovych.kidsoutfit.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rustanovych.kidsoutfit.data.location.LocationPermission
import com.rustanovych.kidsoutfit.ads.AdConsent
import com.rustanovych.kidsoutfit.domain.engine.ReminderSchedule
import com.rustanovych.kidsoutfit.domain.model.ChildGender
import com.rustanovych.kidsoutfit.domain.model.ChildProfile
import com.rustanovych.kidsoutfit.domain.model.LocationMode
import com.rustanovych.kidsoutfit.domain.model.Place
import com.rustanovych.kidsoutfit.domain.repository.PlaceNameResolver
import com.rustanovych.kidsoutfit.domain.repository.PlaceSearch
import com.rustanovych.kidsoutfit.domain.repository.SettingsRepository
import com.rustanovych.kidsoutfit.domain.repository.WeatherRepository
import com.rustanovych.kidsoutfit.notifications.DailyNotificationScheduler
import com.rustanovych.kidsoutfit.notifications.NotificationPermission
import com.rustanovych.kidsoutfit.ui.common.forecastPlaceName
import com.rustanovych.kidsoutfit.ui.common.fullLabel
import com.rustanovych.kidsoutfit.ui.common.regionLabel
import com.rustanovych.kidsoutfit.ui.format.DisplayFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import java.time.LocalTime

private const val SEARCH_DEBOUNCE_MS = 400L
private const val MIN_QUERY_LENGTH = 2
private const val STATE_SUBSCRIPTION_TIMEOUT_MS = 5_000L

enum class LocationChoice { AUTO, MANUAL }

/** One row of the city search results. */
data class PlaceUi(val place: Place, val title: String, val subtitle: String?)

sealed interface SearchStatus {
    /** Nothing typed that is worth searching for. */
    data object Idle : SearchStatus
    data object Loading : SearchStatus
    data class Results(val places: List<PlaceUi>) : SearchStatus
    data object Failed : SearchStatus
}

/**
 * @property sensitivity Slider position, `-3..+3`. Positive means "gets cold easily": dress
 * warmer. That is the opposite sign of the stored offset, which is added to the temperature.
 * @property reminderTimeLabel When the reminder would fire, i.e. departure minus 30 minutes.
 * @property autoPlaceName What the GPS mode currently resolves to, for its subtitle.
 * @property pickedPlaceLabel The chosen city, for the manual option's subtitle.
 */
data class SettingsUiState(
    val gender: ChildGender = ChildGender.BOTH,
    val sensitivity: Int = 0,
    val departureHour: Int = 8,
    val departureMinute: Int = 0,
    val departureTimeLabel: String = "",
    val locationChoice: LocationChoice = LocationChoice.AUTO,
    val autoPlaceName: String? = null,
    val pickedPlaceLabel: String? = null,
    val query: String = "",
    val search: SearchStatus = SearchStatus.Idle,
    val notificationsEnabled: Boolean = false,
    val reminderTimeLabel: String = "",
)

sealed interface SettingsEffect {
    /** GPS was chosen without the permission: the screen must show the system prompt. */
    data object RequestLocationPermission : SettingsEffect

    /** The reminder was switched on without the permission: the screen must show the system prompt. */
    data object RequestNotificationPermission : SettingsEffect

    /** A city was picked and stored. */
    data object PlacePicked : SettingsEffect
}

/**
 * Every change is written to the repositories the moment it happens; there is no Save step.
 *
 * @param applicationScope Runs the forecast refresh after a location change. It must outlive this
 * ViewModel: leaving the screen right after picking a city must not cancel the fetch, or Home
 * would keep showing the old city.
 */
@OptIn(FlowPreview::class)
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val weatherRepository: WeatherRepository,
    private val placeSearch: PlaceSearch,
    placeNameResolver: PlaceNameResolver,
    private val locationPermission: LocationPermission,
    private val formatter: DisplayFormatter,
    private val applicationScope: CoroutineScope,
    private val notificationPermission: NotificationPermission,
    private val scheduler: DailyNotificationScheduler,
    adConsent: AdConsent,
) : ViewModel() {

    /** The "Privacy settings" row is offered only where the consent form must be reopenable. */
    val showPrivacySettings: StateFlow<Boolean> = adConsent.privacyOptionsRequired

    private val query = MutableStateFlow("")

    /** "Choose a city" was tapped but no city is stored yet, so the persisted mode is still GPS. */
    private val manualRequested = MutableStateFlow(false)

    /** The query that was filled in by picking a result; searching it again would reopen the list. */
    private val pickedQuery = MutableStateFlow<String?>(null)

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects: Flow<SettingsEffect> = _effects.receiveAsFlow()

    private val search: Flow<SearchStatus> = query
        .map { it.trim() }
        .debounce(SEARCH_DEBOUNCE_MS)
        .distinctUntilChanged()
        .transformLatest<String, SearchStatus> { term ->
            if (term.length < MIN_QUERY_LENGTH) {
                emit(SearchStatus.Idle)
                return@transformLatest
            }
            emit(SearchStatus.Loading)
            emit(
                placeSearch.search(term).fold(
                    onSuccess = { places -> SearchStatus.Results(places.map(::toUi)) },
                    onFailure = { SearchStatus.Failed },
                ),
            )
        }
        // Debounce holds back the first emission, and with it the whole screen state.
        .onStart { emit(SearchStatus.Idle) }

    private data class Persisted(
        val profile: ChildProfile,
        val mode: LocationMode,
        val notifications: Boolean?,
        val autoName: String?,
    )

    private data class Transient(
        val query: String,
        val manualRequested: Boolean,
        val search: SearchStatus,
        val pickedQuery: String?,
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            settingsRepository.profile,
            settingsRepository.locationMode,
            settingsRepository.notificationsEnabled,
            forecastPlaceName(settingsRepository, weatherRepository, placeNameResolver),
            ::Persisted,
        ),
        combine(query, manualRequested, search, pickedQuery, ::Transient),
        ::buildState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STATE_SUBSCRIPTION_TIMEOUT_MS),
        initialValue = SettingsUiState(),
    )

    fun onGenderSelected(gender: ChildGender) {
        viewModelScope.launch { settingsRepository.setGender(gender) }
    }

    /** @param value Slider position, `-3..+3`, positive = dress warmer. */
    fun onSensitivityChanged(value: Int) {
        viewModelScope.launch { settingsRepository.setColdSensitivity(-value) }
    }

    fun onDepartureTimeSelected(hour: Int, minute: Int) {
        viewModelScope.launch { settingsRepository.setDepartureTime(hour, minute) }
    }

    fun onNotificationsToggled(enabled: Boolean) {
        if (enabled && notificationPermission.needsRuntimeRequest && !notificationPermission.isGranted()) {
            _effects.trySend(SettingsEffect.RequestNotificationPermission)
        } else {
            viewModelScope.launch { settingsRepository.setNotificationsEnabled(enabled) }
        }
    }

    /** The system prompt answered. Without the permission the switch stays off. */
    fun onNotificationPermissionResult(granted: Boolean) {
        viewModelScope.launch { settingsRepository.setNotificationsEnabled(granted) }
    }

    /** Debug menu: run the morning worker once, right now, to check what the notification says. */
    fun onTestNotification() = scheduler.runNow()

    fun onAutoLocationSelected() {
        if (locationPermission.isGranted()) {
            applyAuto()
        } else {
            _effects.trySend(SettingsEffect.RequestLocationPermission)
        }
    }

    /** The system prompt answered. A refusal changes nothing: the previous choice stays. */
    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) applyAuto()
    }

    fun onManualLocationSelected() {
        manualRequested.value = true
    }

    fun onQueryChanged(text: String) {
        query.value = text
        pickedQuery.value = null
    }

    fun onPlaceSelected(place: Place) {
        pickedQuery.value = place.name
        query.value = place.name
        manualRequested.value = false
        changeLocation(LocationMode.Manual(place))
    }

    private fun applyAuto() {
        manualRequested.value = false
        query.value = ""
        pickedQuery.value = null
        changeLocation(LocationMode.Auto)
    }

    /**
     * Stores the new mode and only then refreshes. The two must be sequential: the repository reads
     * the stored mode to decide which coordinates to fetch, so a refresh racing the write would
     * fetch the old location. It runs in [applicationScope] so that leaving the screen right away
     * cancels neither step.
     */
    private fun changeLocation(mode: LocationMode) {
        applicationScope.launch {
            settingsRepository.setLocationMode(mode)
            if (mode is LocationMode.Manual) _effects.send(SettingsEffect.PlacePicked)
            weatherRepository.refresh(force = true)
        }
    }

    private fun toUi(place: Place) = PlaceUi(place = place, title = place.name, subtitle = place.regionLabel())

    private fun buildState(persisted: Persisted, transient: Transient): SettingsUiState {
        val profile = persisted.profile
        val manual = persisted.mode as? LocationMode.Manual
        val reminderTime = ReminderSchedule.triggerTime(LocalTime.of(profile.departureHour, profile.departureMinute))
        return SettingsUiState(
            gender = profile.gender,
            sensitivity = -profile.coldSensitivity.degreesCelsius,
            departureHour = profile.departureHour,
            departureMinute = profile.departureMinute,
            departureTimeLabel = formatter.time(profile.departureHour, profile.departureMinute),
            locationChoice = if (manual != null || transient.manualRequested) {
                LocationChoice.MANUAL
            } else {
                LocationChoice.AUTO
            },
            autoPlaceName = persisted.autoName.takeIf { manual == null },
            pickedPlaceLabel = manual?.place?.fullLabel(),
            query = transient.query,
            // Right after a pick the field holds the city's own name; listing matches for it again
            // would just reopen the dropdown the user closed.
            search = if (transient.pickedQuery != null && transient.query == transient.pickedQuery) {
                SearchStatus.Idle
            } else {
                transient.search
            },
            notificationsEnabled = persisted.notifications == true,
            reminderTimeLabel = formatter.time(reminderTime.hour, reminderTime.minute),
        )
    }
}
