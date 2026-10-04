package com.rustanovych.kidsoutfit.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rustanovych.kidsoutfit.domain.engine.ChangeAdvisor
import com.rustanovych.kidsoutfit.domain.engine.DaySegmenter
import com.rustanovych.kidsoutfit.domain.model.ChildProfile
import com.rustanovych.kidsoutfit.domain.model.DayPlan
import com.rustanovych.kidsoutfit.domain.model.ForecastBundle
import com.rustanovych.kidsoutfit.domain.model.ForecastState
import com.rustanovych.kidsoutfit.domain.model.WeatherSnapshot
import com.rustanovych.kidsoutfit.domain.repository.PlaceNameResolver
import com.rustanovych.kidsoutfit.domain.repository.SettingsRepository
import com.rustanovych.kidsoutfit.domain.repository.WeatherRepository
import com.rustanovych.kidsoutfit.ui.assets.backgroundFor
import com.rustanovych.kidsoutfit.ui.assets.outfitDrawables
import com.rustanovych.kidsoutfit.ui.assets.weatherConditionRes
import com.rustanovych.kidsoutfit.ui.assets.weatherIcon
import com.rustanovych.kidsoutfit.ui.common.forecastPlaceName
import com.rustanovych.kidsoutfit.ui.format.DisplayFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlin.math.abs

private const val STATE_SUBSCRIPTION_TIMEOUT_MS = 5_000L

/**
 * Turns forecast + settings + the user's taps into a [HomeUiState]. All outfit decisions are made
 * by the `:domain` engine; this class only picks which day and segment to show and formats the
 * result.
 *
 * @param now Injected so "current hour" can be pinned in tests.
 */
class HomeViewModel(
    private val weatherRepository: WeatherRepository,
    private val settingsRepository: SettingsRepository,
    private val placeNameResolver: PlaceNameResolver,
    private val formatter: DisplayFormatter,
    private val now: () -> LocalTime = { LocalTime.now() },
) : ViewModel() {

    /**
     * The day and the tapped segment are one value so that switching the day clears the segment in
     * the same emission — two separate flows could briefly pair the new day with the old index.
     */
    private data class Selection(val day: Day = Day.TODAY, val segmentIndex: Int? = null)

    private val selection = MutableStateFlow(Selection())
    private val isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> = combine(
        weatherRepository.forecast(),
        settingsRepository.profile,
        forecastPlaceName(settingsRepository, weatherRepository, placeNameResolver),
        selection,
        isRefreshing,
    ) { forecast, profile, locationName, selection, refreshing ->
        buildState(forecast, profile, locationName, selection, refreshing)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STATE_SUBSCRIPTION_TIMEOUT_MS),
        initialValue = HomeUiState(),
    )

    init {
        // Cheap when the cache is fresh (see ForecastFreshness); the failure, if any, is already
        // reflected in forecast(), so the result is not needed here.
        viewModelScope.launch { weatherRepository.refresh(force = false) }
    }

    fun onDaySelected(day: Day) {
        selection.update { if (it.day == day) it else Selection(day = day, segmentIndex = null) }
    }

    fun onSegmentSelected(index: Int) {
        selection.update { it.copy(segmentIndex = index) }
    }

    fun onRefresh() {
        if (isRefreshing.value) return
        viewModelScope.launch {
            isRefreshing.value = true
            try {
                weatherRepository.refresh(force = true)
            } finally {
                isRefreshing.value = false
            }
        }
    }

    private fun buildState(
        forecast: ForecastState,
        profile: ChildProfile,
        locationName: String?,
        selection: Selection,
        refreshing: Boolean,
    ): HomeUiState {
        val base = HomeUiState(
            isLoading = false,
            isRefreshing = refreshing,
            selectedDay = selection.day,
            gender = profile.gender,
            locationName = locationName,
        )
        return when (forecast) {
            ForecastState.Loading -> base.copy(isLoading = true)
            is ForecastState.Ready -> withForecast(base, forecast.bundle, profile, selection)
            is ForecastState.Error -> {
                val cached = forecast.cached ?: return base.copy(error = true)
                withForecast(base, cached, profile, selection).let {
                    // No usable day in the cache is the same dead end as no cache.
                    if (it.error) it else it.copy(isStale = true)
                }
            }
        }
    }

    private fun withForecast(
        base: HomeUiState,
        bundle: ForecastBundle,
        profile: ChildProfile,
        selection: Selection,
    ): HomeUiState {
        val hourly = if (selection.day == Day.TODAY) bundle.today else bundle.tomorrow
        val plan = DaySegmenter.build(hourly, profile)
        if (plan.segments.isEmpty()) return base.copy(error = true)

        val currentIndex = currentSegmentIndex(plan, profile, selection.day)
        val shownIndex = selection.segmentIndex?.takeIf { it in plan.segments.indices } ?: currentIndex
        val shownOutfit = plan.segments[shownIndex].outfit

        val snapshot = referenceSnapshot(hourly, profile, selection.day)
        return base.copy(
            currentOutfits = outfitDrawables(shownOutfit, profile.gender),
            currentOutfitLabel = shownOutfit,
            selectedSegmentIndex = selection.segmentIndex?.takeIf { it in plan.segments.indices },
            segments = plan.segments.mapIndexed { index, segment ->
                SegmentUi(
                    drawables = outfitDrawables(segment.outfit, profile.gender),
                    timeLabel = formatter.time(segment.startHour),
                    outfit = segment.outfit,
                    isCurrent = index == currentIndex,
                )
            },
            advices = ChangeAdvisor.advise(plan).map {
                AdviceUi(it.direction, formatter.time(it.atHour), it.to)
            },
            weatherNow = snapshot?.let(::weatherNowOf),
            backgroundRes = snapshot?.let(::backgroundFor) ?: base.backgroundRes,
            updatedAtLabel = formatter.timeOfEpoch(bundle.fetchedAtEpochSeconds),
        )
    }

    /** Today follows the child's departure time; tomorrow starts from the first segment. */
    private fun currentSegmentIndex(plan: DayPlan, profile: ChildProfile, day: Day): Int =
        when (day) {
            Day.TODAY -> DaySegmenter.segmentIndexAt(plan, profile.departureHour, profile.departureMinute)
            Day.TOMORROW -> 0
        } ?: 0

    /** The hour the weather card and background describe: now for today, departure for tomorrow. */
    private fun referenceSnapshot(
        hourly: List<WeatherSnapshot>,
        profile: ChildProfile,
        day: Day,
    ): WeatherSnapshot? {
        val hour = if (day == Day.TODAY) now().hour else profile.departureHour
        return hourly.minByOrNull { abs(it.hour - hour) }
    }

    private fun weatherNowOf(snapshot: WeatherSnapshot) = WeatherNowUi(
        temperatureLabel = formatter.temperature(snapshot.temperature),
        apparentLabel = formatter.temperature(snapshot.apparentTemperature),
        iconRes = weatherIcon(snapshot.weatherCode),
        conditionRes = weatherConditionRes(snapshot.weatherCode),
        windLabel = formatter.wind(snapshot.windSpeedKmh),
        precipitationLabel = formatter.precipitation(snapshot.precipitationMm),
    )
}
