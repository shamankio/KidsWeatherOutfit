package com.rustanovych.kidsoutfit.fakes

import com.rustanovych.kidsoutfit.domain.engine.Briefing
import com.rustanovych.kidsoutfit.domain.model.ChildGender
import com.rustanovych.kidsoutfit.domain.model.ChildProfile
import com.rustanovych.kidsoutfit.domain.model.ColdSensitivity
import com.rustanovych.kidsoutfit.domain.model.Coordinates
import com.rustanovych.kidsoutfit.domain.model.ForecastState
import com.rustanovych.kidsoutfit.domain.model.LocationMode
import com.rustanovych.kidsoutfit.domain.model.Place
import com.rustanovych.kidsoutfit.domain.model.WeatherSnapshot
import com.rustanovych.kidsoutfit.domain.repository.PlaceNameResolver
import com.rustanovych.kidsoutfit.domain.repository.PlaceSearch
import com.rustanovych.kidsoutfit.domain.repository.SettingsRepository
import com.rustanovych.kidsoutfit.domain.repository.WeatherRepository
import android.app.Activity
import com.rustanovych.kidsoutfit.ads.AdConsent
import com.rustanovych.kidsoutfit.notifications.DailyNotificationScheduler
import com.rustanovych.kidsoutfit.notifications.DailyOutfitNotifier
import com.rustanovych.kidsoutfit.notifications.NotificationPermission
import com.rustanovych.kidsoutfit.ui.format.DisplayFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeWeatherRepository(
    initial: ForecastState = ForecastState.Loading,
    /** When given, the stored location mode is sampled on every [refresh], like the real repository does. */
    private val settings: FakeSettingsRepository? = null,
) : WeatherRepository {
    val state = MutableStateFlow(initial)
    val location = MutableStateFlow<Coordinates?>(null)

    /** The `force` argument of every [refresh] call, in order. */
    val refreshCalls = mutableListOf<Boolean>()

    /** The stored location mode at the moment of each [refresh]; needs [settings]. */
    val modeAtRefresh = mutableListOf<LocationMode?>()
    var refreshResult: Result<Unit> = Result.success(Unit)

    override fun forecast(): Flow<ForecastState> = state

    override fun forecastLocation(): Flow<Coordinates?> = location

    override suspend fun refresh(force: Boolean): Result<Unit> {
        refreshCalls += force
        modeAtRefresh += settings?.locationModeState?.value
        return refreshResult
    }
}

class FakeSettingsRepository(
    profile: ChildProfile = ChildProfile.Default,
    locationMode: LocationMode = LocationMode.Auto,
) : SettingsRepository {
    val profileState = MutableStateFlow(profile)
    val locationModeState = MutableStateFlow(locationMode)
    val notificationsState = MutableStateFlow<Boolean?>(null)

    override val profile: Flow<ChildProfile> = profileState
    override val locationMode: Flow<LocationMode> = locationModeState
    override val notificationsEnabled: Flow<Boolean?> = notificationsState

    override suspend fun setGender(gender: ChildGender) {
        profileState.update { it.copy(gender = gender) }
    }

    override suspend fun setColdSensitivity(shift: Int) {
        profileState.update { it.copy(coldSensitivity = ColdSensitivity.fromSlider(shift)) }
    }

    override suspend fun setDepartureTime(hour: Int, minute: Int) {
        profileState.update { it.copy(departureHour = hour, departureMinute = minute) }
    }

    override suspend fun setLocationMode(mode: LocationMode) {
        locationModeState.value = mode
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        notificationsState.value = enabled
    }
}

class FakePlaceNameResolver(var name: String? = null) : PlaceNameResolver {
    override suspend fun nameFor(coordinates: Coordinates): String? = name
}

class FakePlaceSearch(var results: Result<List<Place>> = Result.success(emptyList())) : PlaceSearch {
    val queries = mutableListOf<String>()

    override suspend fun search(query: String): Result<List<Place>> {
        queries += query
        return results
    }
}

/** Locale-independent output, so assertions read the same on every machine. */
object FakeDisplayFormatter : DisplayFormatter {
    override fun time(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)
    override fun timeOfEpoch(epochSeconds: Long): String = "t$epochSeconds"
    override fun temperature(celsius: Double): String = "${celsius.toInt()}°"
    override fun wind(kmh: Double): String = "${kmh.toInt()} km/h"
    override fun precipitation(mm: Double): String = "$mm mm"
}

/** One snapshot per hour in [hours], all with the same apparent temperature and clear weather. */
fun snapshots(hours: IntRange, apparent: Double, weatherCode: Int = 0): List<WeatherSnapshot> =
    hours.map { hour ->
        WeatherSnapshot(
            hour = hour,
            temperature = apparent + 1,
            apparentTemperature = apparent,
            precipitationMm = 0.0,
            snowfallCm = 0.0,
            windSpeedKmh = 5.0,
            weatherCode = weatherCode,
            uvIndex = 0.0,
        )
    }

class FakeLocationPermission(var granted: Boolean = false) : com.rustanovych.kidsoutfit.domain.repository.LocationPermission {
    override fun isGranted(): Boolean = granted
}

class FakeNotificationPermission(
    var granted: Boolean = false,
    override val needsRuntimeRequest: Boolean = true,
) : NotificationPermission {
    override fun isGranted(): Boolean = granted
}

class FakeNotifier : DailyOutfitNotifier {
    val shown = mutableListOf<Pair<Briefing, ChildGender>>()

    override fun show(briefing: Briefing, gender: ChildGender) {
        shown += briefing to gender
    }
}

/** Records every call so tests can assert which enqueue flavour was used. */
class FakeScheduler : DailyNotificationScheduler {
    sealed interface Call {
        data class Schedule(val hour: Int, val minute: Int, val enabled: Boolean) : Call
        data class Ensure(val hour: Int, val minute: Int, val enabled: Boolean) : Call
        data class Next(val hour: Int, val minute: Int) : Call
        data object RunNow : Call
    }

    val calls = mutableListOf<Call>()

    override fun schedule(departureHour: Int, departureMinute: Int, enabled: Boolean) {
        calls += Call.Schedule(departureHour, departureMinute, enabled)
    }

    override fun ensureScheduled(departureHour: Int, departureMinute: Int, enabled: Boolean) {
        calls += Call.Ensure(departureHour, departureMinute, enabled)
    }

    override fun scheduleNext(departureHour: Int, departureMinute: Int) {
        calls += Call.Next(departureHour, departureMinute)
    }

    override fun runNow() {
        calls += Call.RunNow
    }
}

class FakeAdConsent(canRequestAds: Boolean = false, privacyOptionsRequired: Boolean = false) : AdConsent {
    override val canRequestAds = MutableStateFlow(canRequestAds)
    override val privacyOptionsRequired = MutableStateFlow(privacyOptionsRequired)

    override fun gatherConsent(activity: Activity, onDone: (Boolean) -> Unit) = onDone(canRequestAds.value)

    override fun showPrivacyOptionsForm(activity: Activity) = Unit
}
