package com.rustanovych.kidsoutfit.domain.model

/**
 * Parent-configured settings that personalize outfit recommendations for a specific child.
 *
 * @property gender Which child illustration(s) to show on the home screen.
 * @property coldSensitivity Offset applied to the perceived temperature before picking an
 * outfit; see [ColdSensitivity].
 * @property departureHour Hour of day the child usually leaves home, in `0..23`.
 * @property departureMinute Minute within [departureHour] the child usually leaves home, in
 * `0..59`.
 */
data class ChildProfile(
    val gender: ChildGender,
    val coldSensitivity: ColdSensitivity,
    val departureHour: Int,
    val departureMinute: Int,
) {
    companion object {

        /** What a parent sees before touching settings: both children, no offset, leaving at 08:00. */
        val Default = ChildProfile(
            gender = ChildGender.BOTH,
            coldSensitivity = ColdSensitivity.Default,
            departureHour = 8,
            departureMinute = 0,
        )
    }
}
