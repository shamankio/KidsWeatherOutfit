package com.rustanovych.kidsoutfit.domain.model

/**
 * A span of time during which a single [OutfitSet] is recommended, as displayed in the home
 * screen's horizontal outfit-change strip.
 *
 * @property outfit The recommended outfit set for this span.
 * @property startHour Hour of day the segment starts, in `0..23`. The segment runs until the
 * [startHour] of the next segment in the same [DayPlan], or until the end of the displayed range.
 */
data class OutfitSegment(
    val outfit: OutfitSet,
    val startHour: Int,
)
