package com.rustanovych.kidsoutfit.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.rustanovych.kidsoutfit.domain.model.Coordinates
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await

/**
 * Reads the device's current position through the fused location provider.
 *
 * The app only ever needs a city-scale fix to pick a forecast grid cell, so this asks for
 * [Priority.PRIORITY_BALANCED_POWER_ACCURACY] and the manifest declares only
 * `ACCESS_COARSE_LOCATION` — no fine location, no `ACCESS_BACKGROUND_LOCATION`.
 *
 * **Permission handling is the UI layer's responsibility.** This class only *checks* whether
 * `ACCESS_COARSE_LOCATION` is already granted and reports a failure when it is not; it never
 * prompts. The prompt belongs to the screen that needs the location, via the standard
 * `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` — Accompanist's
 * permissions API is deliberately not used. Callers should request the permission first and only
 * then call [getCurrentLocation].
 */
class LocationProvider(private val context: Context) {

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * Returns a fresh coarse fix for the device.
     *
     * Fails with:
     * - [SecurityException] when `ACCESS_COARSE_LOCATION` is not granted — the caller must request
     *   it (see the class docs) and retry;
     * - [IllegalStateException] when the provider yields no fix at all, which is what a device with
     *   location services switched off looks like from here;
     * - [ApiException] when Google Play services rejects or cannot serve the request.
     *
     * Coroutine cancellation is propagated and also cancels the underlying location request.
     */
    // Lint cannot see through the runtime check below; permission is verified before the call.
    @SuppressLint("MissingPermission")
    // await(CancellationTokenSource) has been experimental since 1.5.1 with a stable signature.
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getCurrentLocation(): Result<Coordinates> {
        if (!hasCoarseLocationPermission()) {
            return Result.failure(
                SecurityException("ACCESS_COARSE_LOCATION is not granted"),
            )
        }

        val cancellationTokenSource = CancellationTokenSource()
        return try {
            val location = fusedLocationClient
                .getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellationTokenSource.token,
                )
                // This await() overload cancels the token when the coroutine is cancelled.
                .await(cancellationTokenSource)
                ?: return Result.failure(
                    IllegalStateException("Location provider returned no fix"),
                )

            Result.success(Coordinates(lat = location.latitude, lon = location.longitude))
        } catch (e: ApiException) {
            Result.failure(e)
        } catch (e: SecurityException) {
            // The permission can be revoked between the check above and the callback.
            Result.failure(e)
        }
    }

    private fun hasCoarseLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
}
