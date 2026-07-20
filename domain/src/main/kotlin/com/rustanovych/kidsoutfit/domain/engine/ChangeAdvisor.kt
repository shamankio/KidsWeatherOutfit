package com.rustanovych.kidsoutfit.domain.engine

import com.rustanovych.kidsoutfit.domain.model.DayPlan
import com.rustanovych.kidsoutfit.domain.model.OutfitSet
import com.rustanovych.kidsoutfit.domain.model.warmthRank

/**
 * Turns a [DayPlan]'s outfit changes into structured [Advice], so that `:app` can localize the
 * actual warning text from `strings.xml`.
 */
object ChangeAdvisor {

    private val RAIN_OUTFITS =
        setOf(OutfitSet.WARM_RAIN, OutfitSet.COOL_RAIN, OutfitSet.COLD_RAIN, OutfitSet.RAIN_WIND)

    fun advise(plan: DayPlan): List<Advice> = plan.changeWarnings.map { warning ->
        Advice(
            atHour = warning.atHour,
            direction = direction(from = warning.from, to = warning.to),
            from = warning.from,
            to = warning.to,
        )
    }

    private fun direction(from: OutfitSet, to: OutfitSet): Direction = when {
        to in RAIN_OUTFITS -> Direction.RAIN_STARTS
        from in RAIN_OUTFITS && isDry(to) -> Direction.RAIN_STOPS
        to == OutfitSet.SNOWFALL -> Direction.SNOW_STARTS
        to == OutfitSet.WIND -> Direction.WIND_STARTS
        to.warmthRank() > from.warmthRank() -> Direction.WARMER
        else -> Direction.COLDER
    }

    private fun isDry(outfit: OutfitSet): Boolean = outfit !in RAIN_OUTFITS && outfit != OutfitSet.SNOWFALL
}

/** Direction of a single outfit change, for picking the right localized warning in `:app`. */
enum class Direction {
    WARMER,
    COLDER,
    RAIN_STARTS,
    RAIN_STOPS,
    SNOW_STARTS,
    WIND_STARTS,
}

/**
 * A single outfit-change warning worth surfacing to a parent, with the structured data needed to
 * localize a warning message in `:app` — no user-facing text lives here.
 *
 * @property atHour Hour of day, in `0..23`, at which the change happens.
 * @property direction What kind of change this is.
 * @property from Outfit set worn before the change.
 * @property to Outfit set worn after the change.
 */
data class Advice(
    val atHour: Int,
    val direction: Direction,
    val from: OutfitSet,
    val to: OutfitSet,
)
