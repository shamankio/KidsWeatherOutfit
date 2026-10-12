package com.rustanovych.kidsoutfit.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.rustanovych.kidsoutfit.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/** What the UI needs to know about ad consent. A seam so ViewModels can be tested without the SDK. */
interface AdConsent {
    /** True once consent allows requesting ads (or none is required). Refusing leaves it false. */
    val canRequestAds: StateFlow<Boolean>

    /** True when the user is in a region that must be able to reopen the consent form. */
    val privacyOptionsRequired: StateFlow<Boolean>

    /** Collects consent (showing the UMP form if required) and starts the ads SDK if allowed. */
    fun gatherConsent(activity: Activity, onDone: (canRequestAds: Boolean) -> Unit = {})

    /** Re-shows the consent form so the user can change their choice. */
    fun showPrivacyOptionsForm(activity: Activity)
}

/**
 * Google User Messaging Platform wrapper. The ads SDK is initialised only here, and only after
 * consent allows it, so nothing touches the advertising ID before then.
 */
class UmpConsentManager(
    context: Context,
    private val scope: CoroutineScope,
) : AdConsent {

    private val appContext = context.applicationContext
    private val consentInformation = UserMessagingPlatform.getConsentInformation(appContext)

    private val _canRequestAds = MutableStateFlow(false)
    override val canRequestAds: StateFlow<Boolean> = _canRequestAds

    private val _privacyOptionsRequired = MutableStateFlow(false)
    override val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired

    private val adsInitialised = AtomicBoolean(false)
    private var gathering = false
    private var gathered = false

    override fun gatherConsent(activity: Activity, onDone: (Boolean) -> Unit) {
        if (gathering || gathered) {
            onDone(_canRequestAds.value)
            return
        }
        gathering = true

        // A consent given on an earlier launch already allows ads; do not wait for the network.
        publish()

        val params = ConsentRequestParameters.Builder()
            // No setTagForUnderAgeOfConsent: the audience is parents, not children.
            .apply { if (BuildConfig.DEBUG) setConsentDebugSettings(debugSettings()) }
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    // A form error (offline, no message published) is not fatal: canRequestAds()
                    // still reflects whatever consent state is known.
                    finishGathering(onDone)
                }
            },
            { finishGathering(onDone) },
        )
    }

    override fun showPrivacyOptionsForm(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { publish() }
    }

    private fun finishGathering(onDone: (Boolean) -> Unit) {
        gathering = false
        gathered = true
        publish()
        onDone(_canRequestAds.value)
    }

    private fun publish() {
        _privacyOptionsRequired.value = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        val allowed = consentInformation.canRequestAds()
        if (allowed) initialiseAds()
        _canRequestAds.value = allowed
    }

    private fun initialiseAds() {
        if (!adsInitialised.compareAndSet(false, true)) return
        scope.launch(Dispatchers.IO) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    // A child may look at the screen with the parent. Child-directed treatment is
                    // deliberately not set: the audience is parents.
                    .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                    .setTestDeviceIds(listOfNotNull(BuildConfig.ADS_TEST_DEVICE_ID.takeIf { it.isNotBlank() }))
                    .build(),
            )
            MobileAds.initialize(appContext)
        }
    }

    private fun debugSettings(): ConsentDebugSettings =
        ConsentDebugSettings.Builder(appContext)
            // Shows the form outside the EEA while testing.
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            .apply { BuildConfig.UMP_TEST_DEVICE_ID.takeIf { it.isNotBlank() }?.let(::addTestDeviceHashedId) }
            .build()
}
