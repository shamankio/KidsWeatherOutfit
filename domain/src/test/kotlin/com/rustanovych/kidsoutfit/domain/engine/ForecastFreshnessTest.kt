package com.rustanovych.kidsoutfit.domain.engine

import com.rustanovych.kidsoutfit.domain.engine.ForecastFreshness.MAX_AGE_SECONDS
import com.rustanovych.kidsoutfit.domain.engine.ForecastFreshness.shouldRefetch
import com.rustanovych.kidsoutfit.domain.model.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Arbitrary "now"; only the difference from the fetch time matters. */
private const val NOW = 1_800_000_000L

private val KYIV = Coordinates(lat = 50.45466, lon = 30.5238)

class ForecastFreshnessTest {

    @Test
    fun `keeps a fresh cache fetched for the same place`() {
        assertFalse(
            shouldRefetch(
                fetchedAtEpochSeconds = NOW - 60,
                nowEpochSeconds = NOW,
                oldCoordinates = KYIV,
                newCoordinates = KYIV,
            ),
        )
    }

    @Test
    fun `keeps a cache fetched one second before the age limit`() {
        assertFalse(
            shouldRefetch(
                fetchedAtEpochSeconds = NOW - (MAX_AGE_SECONDS - 1),
                nowEpochSeconds = NOW,
                oldCoordinates = KYIV,
                newCoordinates = KYIV,
            ),
        )
    }

    @Test
    fun `refetches once the cache reaches the age limit`() {
        assertTrue(
            shouldRefetch(
                fetchedAtEpochSeconds = NOW - MAX_AGE_SECONDS,
                nowEpochSeconds = NOW,
                oldCoordinates = KYIV,
                newCoordinates = KYIV,
            ),
        )
    }

    @Test
    fun `refetches a cache older than the age limit`() {
        assertTrue(
            shouldRefetch(
                fetchedAtEpochSeconds = NOW - 24 * 60 * 60,
                nowEpochSeconds = NOW,
                oldCoordinates = KYIV,
                newCoordinates = KYIV,
            ),
        )
    }

    @Test
    fun `keeps a fresh cache after a move shorter than the distance limit`() {
        // ~4.4 km north of Kyiv: a different neighbourhood, the same forecast grid cell.
        val nearby = KYIV.copy(lat = KYIV.lat + 0.04)
        assertTrue("fixture must stay under the limit", KYIV.distanceMetersTo(nearby) < 5_000.0)

        assertFalse(
            shouldRefetch(
                fetchedAtEpochSeconds = NOW - 60,
                nowEpochSeconds = NOW,
                oldCoordinates = KYIV,
                newCoordinates = nearby,
            ),
        )
    }

    @Test
    fun `refetches a fresh cache after a move beyond the distance limit`() {
        // ~11 km north of Kyiv.
        val faraway = KYIV.copy(lat = KYIV.lat + 0.1)
        assertTrue("fixture must exceed the limit", KYIV.distanceMetersTo(faraway) > 5_000.0)

        assertTrue(
            shouldRefetch(
                fetchedAtEpochSeconds = NOW - 60,
                nowEpochSeconds = NOW,
                oldCoordinates = KYIV,
                newCoordinates = faraway,
            ),
        )
    }

    @Test
    fun `measures distance along longitude too`() {
        // At Kyiv's latitude a degree of longitude is ~71 km, so 0.1 deg is well past the limit.
        val eastwards = KYIV.copy(lon = KYIV.lon + 0.1)

        assertTrue(
            shouldRefetch(
                fetchedAtEpochSeconds = NOW - 60,
                nowEpochSeconds = NOW,
                oldCoordinates = KYIV,
                newCoordinates = eastwards,
            ),
        )
    }

    @Test
    fun `refetches when the cache records no coordinates`() {
        assertTrue(
            shouldRefetch(
                fetchedAtEpochSeconds = NOW,
                nowEpochSeconds = NOW,
                oldCoordinates = null,
                newCoordinates = KYIV,
            ),
        )
    }

    @Test
    fun `refetches when the cache is stamped in the future`() {
        assertTrue(
            shouldRefetch(
                fetchedAtEpochSeconds = NOW + 60,
                nowEpochSeconds = NOW,
                oldCoordinates = KYIV,
                newCoordinates = KYIV,
            ),
        )
    }

    @Test
    fun `refetches a stale cache even when the place is unchanged`() {
        assertTrue(
            shouldRefetch(
                fetchedAtEpochSeconds = NOW - MAX_AGE_SECONDS,
                nowEpochSeconds = NOW,
                oldCoordinates = KYIV,
                newCoordinates = KYIV,
            ),
        )
    }
}

class CoordinatesDistanceTest {

    @Test
    fun `is zero for the same point`() {
        assertEquals(0.0, KYIV.distanceMetersTo(KYIV), 0.0)
    }

    @Test
    fun `is symmetric`() {
        val lviv = Coordinates(lat = 49.83826, lon = 24.02324)
        assertEquals(KYIV.distanceMetersTo(lviv), lviv.distanceMetersTo(KYIV), 0.1)
    }

    @Test
    fun `matches the known Kyiv to Lviv great-circle distance`() {
        val lviv = Coordinates(lat = 49.83826, lon = 24.02324)
        // ~468 km; allow a few km for the spherical approximation.
        assertEquals(468_000.0, KYIV.distanceMetersTo(lviv), 5_000.0)
    }

    @Test
    fun `handles antipodal points without returning NaN`() {
        val north = Coordinates(lat = 90.0, lon = 0.0)
        val south = Coordinates(lat = -90.0, lon = 0.0)
        // Half the circumference of the mean-radius sphere.
        assertEquals(20_015_000.0, north.distanceMetersTo(south), 5_000.0)
    }

    @Test
    fun `handles the antimeridian`() {
        val west = Coordinates(lat = 0.0, lon = 179.95)
        val east = Coordinates(lat = 0.0, lon = -179.95)
        // 0.1 deg of longitude at the equator is ~11.1 km, whichever way you cross.
        assertEquals(11_120.0, west.distanceMetersTo(east), 100.0)
    }
}
