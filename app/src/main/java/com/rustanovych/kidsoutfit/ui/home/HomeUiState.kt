package com.rustanovych.kidsoutfit.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.rustanovych.kidsoutfit.R
import com.rustanovych.kidsoutfit.domain.engine.Direction
import com.rustanovych.kidsoutfit.domain.model.ChildGender
import com.rustanovych.kidsoutfit.domain.model.OutfitSet

enum class Day { TODAY, TOMORROW }

/** One-off requests to the screen, which owns the system dialogs. */
sealed interface HomeEffect {
    /** The user accepted the reminder but the system permission is still missing. */
    data object RequestNotificationPermission : HomeEffect
}

/**
 * Everything the home screen draws. Display strings are final (formatted by the ViewModel); the
 * only text left to the screen is the localized wording around them, which lives in `strings.xml`.
 *
 * @property currentOutfits Hero drawables: one for a single child, two (boy, girl) for
 * [ChildGender.BOTH]. Empty when there is no data.
 * @property selectedSegmentIndex `null` while the hero follows the departure time; set by tapping
 * a strip card.
 * @property isStale Showing cached data after a failed refresh.
 * @property error No data at all and the last refresh failed.
 * @property locationName `null` when the name is unknown (GPS mode before reverse geocoding
 * answers, or it failed); the screen shows a generic label.
 * @property updatedAtLabel When the shown forecast was fetched, for the stale bar.
 * @property showNotificationPrompt Offer the morning reminder: a forecast is on screen and the user
 * was never asked.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val selectedDay: Day = Day.TODAY,
    val gender: ChildGender = ChildGender.BOTH,
    val currentOutfits: List<Int> = emptyList(),
    val currentOutfitLabel: OutfitSet? = null,
    val selectedSegmentIndex: Int? = null,
    val segments: List<SegmentUi> = emptyList(),
    val advices: List<AdviceUi> = emptyList(),
    val weatherNow: WeatherNowUi? = null,
    @DrawableRes val backgroundRes: Int = R.drawable.bg_cloudy,
    val locationName: String? = null,
    val isStale: Boolean = false,
    val updatedAtLabel: String? = null,
    val error: Boolean = false,
    val showNotificationPrompt: Boolean = false,
) {
    /** The segment the hero shows: the tapped one, else the one active at departure. */
    val shownSegmentIndex: Int?
        get() = selectedSegmentIndex ?: segments.indexOfFirst { it.isCurrent }.takeIf { it >= 0 }

    /**
     * The next change after the shown segment — what a parent still needs to plan for. The advice
     * at position `i` is the change from segment `i` to `i + 1`, so it is the one at the shown
     * index itself. `null` on the last segment: nothing is left to warn about.
     */
    val upcomingAdvice: AdviceUi?
        get() = shownSegmentIndex?.let { advices.getOrNull(it) }
}

/** @property timeLabel Start of the segment, e.g. `09:00`. */
data class SegmentUi(
    val drawables: List<Int>,
    val timeLabel: String,
    val outfit: OutfitSet,
    val isCurrent: Boolean,
)

/** @property atTimeLabel When the change happens, e.g. `15:00`. */
data class AdviceUi(
    val direction: Direction,
    val atTimeLabel: String,
    val toOutfit: OutfitSet,
)

/** Numbers are pre-formatted, e.g. `18°`, `12 km/h`, `0.5 mm`. */
data class WeatherNowUi(
    val temperatureLabel: String,
    val apparentLabel: String,
    @DrawableRes val iconRes: Int,
    @StringRes val conditionRes: Int,
    val windLabel: String,
    val precipitationLabel: String,
)
