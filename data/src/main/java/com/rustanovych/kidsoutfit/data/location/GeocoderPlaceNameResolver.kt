package com.rustanovych.kidsoutfit.data.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import com.rustanovych.kidsoutfit.domain.model.Coordinates
import com.rustanovych.kidsoutfit.domain.repository.PlaceNameResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Reverse-geocodes with the platform [Geocoder]. Open-Meteo has no reverse geocoding, and the
 * platform one needs no key and no extra dependency.
 *
 * The platform geocoder is backed by a system service that may be missing or offline, so every
 * failure reads as "no name" and the UI falls back to a generic label.
 */
internal class GeocoderPlaceNameResolver(private val context: Context) : PlaceNameResolver {

    override suspend fun nameFor(coordinates: Coordinates): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        val address = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                fetchAsync(geocoder, coordinates)
            } else {
                withContext(Dispatchers.IO) { fetchBlocking(geocoder, coordinates) }
            }
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
        return address?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun fetchAsync(geocoder: Geocoder, coordinates: Coordinates): Address? =
        suspendCancellableCoroutine { continuation ->
            geocoder.getFromLocation(
                coordinates.lat,
                coordinates.lon,
                1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        continuation.resume(addresses.firstOrNull())
                    }

                    override fun onError(errorMessage: String?) {
                        continuation.resume(null)
                    }
                },
            )
        }

    @Suppress("DEPRECATION") // The listener overload only exists from API 33.
    private fun fetchBlocking(geocoder: Geocoder, coordinates: Coordinates): Address? =
        geocoder.getFromLocation(coordinates.lat, coordinates.lon, 1)?.firstOrNull()
}
