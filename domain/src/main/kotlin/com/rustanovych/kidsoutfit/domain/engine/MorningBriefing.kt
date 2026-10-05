package com.rustanovych.kidsoutfit.domain.engine

import com.rustanovych.kidsoutfit.domain.model.ChildProfile
import com.rustanovych.kidsoutfit.domain.model.OutfitSet
import com.rustanovych.kidsoutfit.domain.model.WeatherSnapshot

/**
 * What the morning reminder tells the parent: what to put on at departure, and the next change
 * worth planning for.
 *
 * @property outfit The set worn when the child leaves.
 * @property advice The change right after that set, or `null` when the weather stays stable for the
 * rest of the displayed day.
 */
data class Briefing(
    val outfit: OutfitSet,
    val advice: Advice?,
)

/** Builds the [Briefing] for one day, using the same rules the home screen applies. */
object MorningBriefing {

    /** `null` when [hourly] has nothing in the displayed 7:00-19:00 window. */
    fun build(hourly: List<WeatherSnapshot>, profile: ChildProfile): Briefing? {
        val plan = DaySegmenter.build(hourly, profile)
        val index = DaySegmenter.segmentIndexAt(plan, profile.departureHour, profile.departureMinute)
            ?: return null
        // The advice at position `i` is the change from segment `i` to `i + 1`; earlier changes are
        // already behind the child by the time they leave.
        return Briefing(
            outfit = plan.segments[index].outfit,
            advice = ChangeAdvisor.advise(plan).getOrNull(index),
        )
    }
}
