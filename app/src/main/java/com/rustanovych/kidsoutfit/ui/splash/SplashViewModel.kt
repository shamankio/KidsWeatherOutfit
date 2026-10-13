package com.rustanovych.kidsoutfit.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rustanovych.kidsoutfit.domain.repository.LocationPermission
import com.rustanovych.kidsoutfit.domain.model.ForecastState
import com.rustanovych.kidsoutfit.domain.model.LocationMode
import com.rustanovych.kidsoutfit.domain.repository.SettingsRepository
import com.rustanovych.kidsoutfit.domain.repository.WeatherRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** What the screen after the system splash is showing. */
sealed interface SplashUiState {

    /** Reading the cache and settings to decide; usually over in a blink, so it draws nothing. */
    data object Checking : SplashUiState

    /** First launch with no way to find the user's city yet: explain, then ask. */
    data object NeedsLocation : SplashUiState

    /** Fetching the first forecast: the only state that shows a spinner. */
    data object Loading : SplashUiState

    /** Ready: go to Home. */
    data object Done : SplashUiState

    /** The first forecast could not be fetched. */
    data object Error : SplashUiState
}

/** One-off requests to the screen. */
sealed interface SplashEffect {

    /** The user declined the permission: send them to pick a city by hand. Never re-prompt. */
    data object OpenManualCity : SplashEffect
}

/**
 * Decides whether the user can go straight to Home, has to say where they live first, or has to
 * wait for the first forecast.
 *
 * Only the first launch lands here for real: once a forecast is cached, the user has been through
 * this and Home takes over (it refreshes itself), so a returning user is never held up by the
 * network.
 *
 * This class never asks for the permission: the prompt is the screen's, and it is shown only when
 * the user taps the button.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModel(
    private val weatherRepository: WeatherRepository,
    settingsRepository: SettingsRepository,
    private val locationPermission: LocationPermission,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Checking)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SplashEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /** Bumped to re-run the decision: after a permission grant, or a retry. */
    private val attempt = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            // A change of location mode (a city picked in Settings) re-runs the decision too, and
            // supersedes any fetch still in flight.
            combine(settingsRepository.locationMode, attempt) { mode, _ -> mode }
                .collectLatest { mode -> decide(mode) }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            attempt.value++
        } else {
            _effects.trySend(SplashEffect.OpenManualCity)
        }
    }

    fun onRetry() {
        attempt.value++
    }

    private suspend fun decide(mode: LocationMode) {
        when {
            hasCachedForecast() -> _uiState.value = SplashUiState.Done
            mode == LocationMode.Auto && !locationPermission.isGranted() ->
                _uiState.value = SplashUiState.NeedsLocation
            else -> {
                _uiState.value = SplashUiState.Loading
                _uiState.value = weatherRepository.refresh(force = false).fold(
                    onSuccess = { SplashUiState.Done },
                    onFailure = { SplashUiState.Error },
                )
            }
        }
    }

    private suspend fun hasCachedForecast(): Boolean = when (val state = weatherRepository.forecast().first()) {
        ForecastState.Loading -> false
        is ForecastState.Ready -> true
        is ForecastState.Error -> state.cached != null
    }
}
