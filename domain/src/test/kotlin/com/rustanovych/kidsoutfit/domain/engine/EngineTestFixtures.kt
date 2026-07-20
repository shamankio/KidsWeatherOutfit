package com.rustanovych.kidsoutfit.domain.engine

import com.rustanovych.kidsoutfit.domain.model.ChildGender
import com.rustanovych.kidsoutfit.domain.model.ChildProfile
import com.rustanovych.kidsoutfit.domain.model.ColdSensitivity
import com.rustanovych.kidsoutfit.domain.model.WeatherSnapshot

/** Builds a [WeatherSnapshot] with sane defaults (clear, calm, dry) for engine tests. */
internal fun testSnapshot(
    hour: Int = 8,
    apparentTemperature: Double = 10.0,
    precipitationMm: Double = 0.0,
    snowfallCm: Double = 0.0,
    windSpeedKmh: Double = 0.0,
    weatherCode: Int = 0,
): WeatherSnapshot = WeatherSnapshot(
    hour = hour,
    apparentTemperature = apparentTemperature,
    precipitationMm = precipitationMm,
    snowfallCm = snowfallCm,
    windSpeedKmh = windSpeedKmh,
    weatherCode = weatherCode,
)

/** Builds a [ChildProfile] with a neutral (zero) cold sensitivity by default. */
internal fun testProfile(
    coldSensitivity: ColdSensitivity = ColdSensitivity.Default,
): ChildProfile = ChildProfile(
    gender = ChildGender.BOTH,
    coldSensitivity = coldSensitivity,
    departureHour = 8,
    departureMinute = 0,
)
