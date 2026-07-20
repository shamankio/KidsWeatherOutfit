package com.rustanovych.kidsoutfit.domain.model

/**
 * The full outfit schedule computed for a single day, covering the displayed 7:00-19:00 range.
 *
 * @property segments Ordered outfit segments, one per outfit change, covering the displayed hours.
 * @property changeWarnings Notable outfit changes worth calling out to the parent, e.g. changes
 * that require swapping to a significantly different outfit.
 */
data class DayPlan(
    val segments: List<OutfitSegment>,
    val changeWarnings: List<ChangeWarning>,
)

/**
 * Flags an outfit change within a [DayPlan] that a parent should be aware of, such as a change
 * that happens close to the child's departure time.
 *
 * @property atHour Hour of day, in `0..23`, at which the change happens.
 * @property from Outfit set worn before the change.
 * @property to Outfit set worn after the change.
 */
data class ChangeWarning(
    val atHour: Int,
    val from: OutfitSet,
    val to: OutfitSet,
)
