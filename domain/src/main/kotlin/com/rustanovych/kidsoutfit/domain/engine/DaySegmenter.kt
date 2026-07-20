package com.rustanovych.kidsoutfit.domain.engine

import com.rustanovych.kidsoutfit.domain.model.ChangeWarning
import com.rustanovych.kidsoutfit.domain.model.ChildProfile
import com.rustanovych.kidsoutfit.domain.model.DayPlan
import com.rustanovych.kidsoutfit.domain.model.OutfitSegment
import com.rustanovych.kidsoutfit.domain.model.OutfitSet
import com.rustanovych.kidsoutfit.domain.model.WeatherSnapshot

/**
 * Turns an hourly forecast into the [DayPlan] shown in the home screen's outfit-change strip.
 *
 * Only the displayed 7:00-19:00 window is considered. Each hour in that window is resolved to an
 * [OutfitSet] via [OutfitEngine.resolve], and consecutive hours with the same outfit are merged
 * into a single [OutfitSegment].
 */
object DaySegmenter {

    private val DISPLAY_HOURS = 7..19

    fun build(hourly: List<WeatherSnapshot>, profile: ChildProfile): DayPlan {
        val windowed = hourly.filter { it.hour in DISPLAY_HOURS }
        if (windowed.isEmpty()) return DayPlan(emptyList(), emptyList())

        val segments = mutableListOf<OutfitSegment>()
        for (snapshot in windowed) {
            val outfit = OutfitEngine.resolve(snapshot, profile)
            if (segments.isEmpty() || segments.last().outfit != outfit) {
                segments.add(OutfitSegment(outfit = outfit, startHour = snapshot.hour))
            }
        }

        val changeWarnings = segments.zipWithNext { current, next ->
            ChangeWarning(atHour = next.startHour, from = current.outfit, to = next.outfit)
        }
        return DayPlan(segments, changeWarnings)
    }

    /**
     * The [OutfitSet] active in [plan] at the given time: the last segment whose [OutfitSegment.startHour]
     * is at or before [hour]:[minute], or the first segment if the time is before it starts. `null`
     * when [plan] has no segments.
     */
    fun outfitAt(plan: DayPlan, hour: Int, minute: Int): OutfitSet? {
        if (plan.segments.isEmpty()) return null
        val totalMinutes = hour * 60 + minute
        val active = plan.segments.lastOrNull { it.startHour * 60 <= totalMinutes }
        return (active ?: plan.segments.first()).outfit
    }
}
