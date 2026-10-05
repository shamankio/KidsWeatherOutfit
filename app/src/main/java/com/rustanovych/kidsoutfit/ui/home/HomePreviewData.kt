package com.rustanovych.kidsoutfit.ui.home

import com.rustanovych.kidsoutfit.R
import com.rustanovych.kidsoutfit.domain.engine.Direction
import com.rustanovych.kidsoutfit.domain.model.ChildGender
import com.rustanovych.kidsoutfit.domain.model.OutfitSet
import com.rustanovych.kidsoutfit.ui.assets.outfitDrawables
import com.rustanovych.kidsoutfit.ui.assets.outfitThumbDrawables

/** Fake [HomeUiState]s for `@Preview`s. Not used at runtime. */
internal object HomePreviewData {

    /** Every set, in the order of the enum — for judging whether the thumbnails can be told apart. */
    fun allSetsSegments(gender: ChildGender, currentIndex: Int? = null): List<SegmentUi> =
        OutfitSet.entries.mapIndexed { index, set ->
            SegmentUi(
                drawables = outfitThumbDrawables(set, gender),
                timeLabel = "%02d:00".format(7 + index),
                outfit = set,
                isCurrent = index == currentIndex,
            )
        }

    private val dayPlan = listOf(OutfitSet.COOL, OutfitSet.WARM, OutfitSet.COOL_RAIN, OutfitSet.COLD)
    private val dayHours = listOf(7, 11, 14, 18)

    private fun segmentsOf(gender: ChildGender, currentIndex: Int) =
        dayPlan.mapIndexed { index, set ->
            SegmentUi(
                drawables = outfitThumbDrawables(set, gender),
                timeLabel = "%02d:00".format(dayHours[index]),
                outfit = set,
                isCurrent = index == currentIndex,
            )
        }

    fun loaded(
        gender: ChildGender = ChildGender.BOTH,
        backgroundRes: Int = R.drawable.bg_sunny,
        currentIndex: Int = 1,
        selectedIndex: Int? = null,
        locationName: String? = "Poznań",
        iconRes: Int = R.drawable.ic_weather_partly_cloudy_day,
    ): HomeUiState {
        val shown = selectedIndex ?: currentIndex
        return HomeUiState(
            isLoading = false,
            gender = gender,
            currentOutfits = outfitDrawables(dayPlan[shown], gender),
            currentOutfitLabel = dayPlan[shown],
            selectedSegmentIndex = selectedIndex,
            segments = segmentsOf(gender, currentIndex),
            advices = listOf(
                AdviceUi(Direction.WARMER, "11:00", OutfitSet.WARM),
                AdviceUi(Direction.RAIN_STARTS, "14:00", OutfitSet.COOL_RAIN),
                AdviceUi(Direction.COLDER, "18:00", OutfitSet.COLD),
            ),
            weatherNow = WeatherNowUi(
                temperatureLabel = "18°",
                apparentLabel = "16°",
                iconRes = iconRes,
                conditionRes = R.string.weather_condition_partly_cloudy,
                windLabel = "12 km/h",
                precipitationLabel = "0.5 mm",
            ),
            backgroundRes = backgroundRes,
            locationName = locationName,
            updatedAtLabel = "07:42",
        )
    }

    val loading = HomeUiState(isLoading = true, locationName = "Poznań")
    val error = HomeUiState(isLoading = false, error = true, locationName = "Poznań")
    val stale = loaded().copy(isStale = true)
}
