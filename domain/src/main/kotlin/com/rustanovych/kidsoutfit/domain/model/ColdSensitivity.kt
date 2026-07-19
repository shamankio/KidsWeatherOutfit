package com.rustanovych.kidsoutfit.domain.model

/**
 * Parent-configured offset applied to the perceived temperature before picking an outfit,
 * in whole degrees Celsius. Negative values mean the child gets cold easily
 * (dress warmer), positive values mean the child runs hot (dress lighter).
 */
@JvmInline
value class ColdSensitivity(val degreesCelsius: Int) {
    init {
        require(degreesCelsius in MIN_DEGREES..MAX_DEGREES) {
            "Cold sensitivity must be within $MIN_DEGREES..$MAX_DEGREES, was $degreesCelsius"
        }
    }

    companion object {
        const val MIN_DEGREES = -3
        const val MAX_DEGREES = 3

        val Default = ColdSensitivity(0)

        /** Builds a sensitivity from an unconstrained slider value, clamping to the valid range. */
        fun fromSlider(value: Int): ColdSensitivity =
            ColdSensitivity(value.coerceIn(MIN_DEGREES, MAX_DEGREES))
    }
}
