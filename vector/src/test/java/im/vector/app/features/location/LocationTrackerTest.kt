/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location

import android.content.Context
import android.location.Location
import android.location.LocationManager
import im.vector.app.features.session.coroutineScope
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeContext
import im.vector.app.test.fakes.FakeLocationManager
import im.vector.app.test.fixtures.aBuildMeta
import im.vector.app.test.test
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Test

private const val A_LATITUDE = 1.2
private const val A_LONGITUDE = 44.0
private const val AN_ACCURACY = 5.0f

class LocationTrackerTest {

    private val fakeLocationManager = FakeLocationManager()
    private val fakeContext = FakeContext().also {
        it.givenService(Context.LOCATION_SERVICE, android.location.LocationManager::class.java, fakeLocationManager.instance)
    }
    private val fakeActiveSessionHolder = FakeActiveSessionHolder()

    private lateinit var locationTracker: LocationTracker

    @Before
    fun setUp() {
        mockkStatic("im.vector.app.features.session.SessionCoroutineScopesKt")
        locationTracker = LocationTracker(fakeContext.instance, fakeActiveSessionHolder.instance, aBuildMeta())
        fakeLocationManager.givenRemoveUpdates(locationTracker)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given available list of providers when starting then location updates are requested in priority order`() {
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER)
        mockAvailableProviders(providers)

        locationTracker.start()

        verifyOrder {
            fakeLocationManager.instance.requestLocationUpdates(
                    LocationManager.FUSED_PROVIDER,
                    locationTracker.minDurationToUpdateLocationMillis,
                    MIN_DISTANCE_TO_UPDATE_LOCATION_METERS,
                    locationTracker
            )
            fakeLocationManager.instance.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    locationTracker.minDurationToUpdateLocationMillis,
                    MIN_DISTANCE_TO_UPDATE_LOCATION_METERS,
                    locationTracker
            )
            fakeLocationManager.instance.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    locationTracker.minDurationToUpdateLocationMillis,
                    MIN_DISTANCE_TO_UPDATE_LOCATION_METERS,
                    locationTracker
            )
        }
    }

    @Test
    fun `given available list of providers when list is empty then callbacks are notified`() {
        val providers = emptyList<String>()
        val callback = mockCallback()

        locationTracker.addCallback(callback)
        fakeLocationManager.givenActiveProviders(providers)

        locationTracker.start()

        verify { callback.onNoLocationProviderAvailable() }
        locationTracker.removeCallback(callback)
    }

    @Test
    fun `when adding or removing a callback then it is added into or removed from the list of callbacks`() {
        val callback1 = mockCallback()
        val callback2 = mockCallback()

        locationTracker.addCallback(callback1)
        locationTracker.addCallback(callback2)

        locationTracker.callbacks.size shouldBeEqualTo 2
        locationTracker.callbacks.first() shouldBeEqualTo callback1
        locationTracker.callbacks[1] shouldBeEqualTo callback2

        locationTracker.removeCallback(callback1)

        locationTracker.callbacks.size shouldBeEqualTo 1

        locationTracker.removeCallback(callback2)

        locationTracker.callbacks.size shouldBeEqualTo 0
        verify { locationTracker.stop() }
    }

    @Test
    fun `when location updates are received from fused provider then fused locations are taken in priority`() = runTest {
        every { fakeActiveSessionHolder.fakeSession.coroutineScope } returns this
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER)
        mockAvailableProviders(providers)
        locationTracker.start()
        val fusedLocation = mockLocation(
                provider = LocationManager.FUSED_PROVIDER,
                latitude = 1.0,
                longitude = 3.0,
                accuracy = 4f
        )
        val gpsLocation = mockLocation(
                provider = LocationManager.GPS_PROVIDER
        )

        val networkLocation = mockLocation(
                provider = LocationManager.NETWORK_PROVIDER
        )
        val resultUpdates = locationTracker.locations.test(this)

        locationTracker.onLocationChanged(fusedLocation)
        locationTracker.onLocationChanged(gpsLocation)
        locationTracker.onLocationChanged(networkLocation)
        advanceTimeBy(locationTracker.minDurationToUpdateLocationMillis + 1)

        val expectedLocationData = LocationData(
                latitude = 1.0,
                longitude = 3.0,
                uncertainty = 4.0
        )
        resultUpdates
                .assertValues(listOf(expectedLocationData))
                .finish()
        locationTracker.hasLocationFromFusedProvider shouldBeEqualTo true
        locationTracker.hasLocationFromGPSProvider shouldBeEqualTo false
    }

    @Test
    fun `when location updates are received from gps provider then gps locations are taken if none are received from fused provider`() = runTest {
        every { fakeActiveSessionHolder.fakeSession.coroutineScope } returns this
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER)
        mockAvailableProviders(providers)
        locationTracker.start()
        val gpsLocation = mockLocation(
                provider = LocationManager.GPS_PROVIDER,
                latitude = 1.0,
                longitude = 3.0,
                accuracy = 4f
        )

        val networkLocation = mockLocation(
                provider = LocationManager.NETWORK_PROVIDER
        )
        val resultUpdates = locationTracker.locations.test(this)

        locationTracker.onLocationChanged(gpsLocation)
        locationTracker.onLocationChanged(networkLocation)
        advanceTimeBy(locationTracker.minDurationToUpdateLocationMillis + 1)

        val expectedLocationData = LocationData(
                latitude = 1.0,
                longitude = 3.0,
                uncertainty = 4.0
        )
        resultUpdates
                .assertValues(listOf(expectedLocationData))
                .finish()
        locationTracker.hasLocationFromFusedProvider shouldBeEqualTo false
        locationTracker.hasLocationFromGPSProvider shouldBeEqualTo true
    }

    @Test
    fun `when location updates are received from network provider then network locations are taken if none are received from fused, gps provider`() = runTest {
        every { fakeActiveSessionHolder.fakeSession.coroutineScope } returns this
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER)
        mockAvailableProviders(providers)
        locationTracker.start()
        val networkLocation = mockLocation(
                provider = LocationManager.NETWORK_PROVIDER,
                latitude = 1.0,
                longitude = 3.0,
                accuracy = 4f
        )
        val resultUpdates = locationTracker.locations.test(this)

        locationTracker.onLocationChanged(networkLocation)
        advanceTimeBy(locationTracker.minDurationToUpdateLocationMillis + 1)

        val expectedLocationData = LocationData(
                latitude = 1.0,
                longitude = 3.0,
                uncertainty = 4.0
        )
        resultUpdates
                .assertValues(listOf(expectedLocationData))
                .finish()
        locationTracker.hasLocationFromFusedProvider shouldBeEqualTo false
        locationTracker.hasLocationFromGPSProvider shouldBeEqualTo false
    }

    @Test
    fun `when requesting the last location then last location is notified via location updates flow`() = runTest {
        every { fakeActiveSessionHolder.fakeSession.coroutineScope } returns this
        val providers = listOf(LocationManager.GPS_PROVIDER)
        fakeLocationManager.givenActiveProviders(providers)
        val lastLocation = mockLocation(provider = LocationManager.GPS_PROVIDER)
        fakeLocationManager.givenLastLocationForProvider(provider = LocationManager.GPS_PROVIDER, location = lastLocation)
        fakeLocationManager.givenRequestUpdatesForProvider(provider = LocationManager.GPS_PROVIDER, listener = locationTracker)
        locationTracker.start()
        val resultUpdates = locationTracker.locations.test(this)

        locationTracker.requestLastKnownLocation()
        advanceTimeBy(locationTracker.minDurationToUpdateLocationMillis + 1)

        val expectedLocationData = LocationData(
                latitude = A_LATITUDE,
                longitude = A_LONGITUDE,
                uncertainty = AN_ACCURACY.toDouble()
        )
        resultUpdates
                .assertValues(listOf(expectedLocationData))
                .finish()
    }

    @Test
    fun `when stopping then location updates are stopped and callbacks are cleared`() {
        locationTracker.stop()

        verify { fakeLocationManager.instance.removeUpdates(locationTracker) }
        locationTracker.callbacks.isEmpty() shouldBeEqualTo true
        locationTracker.hasLocationFromGPSProvider shouldBeEqualTo false
        locationTracker.hasLocationFromFusedProvider shouldBeEqualTo false
    }

    private fun mockAvailableProviders(providers: List<String>) {
        fakeLocationManager.givenActiveProviders(providers)
        providers.forEach { provider ->
            fakeLocationManager.givenLastLocationForProvider(provider = provider, location = null)
            fakeLocationManager.givenRequestUpdatesForProvider(provider = provider, listener = locationTracker)
        }
    }

    private fun mockCallback(): LocationTracker.Callback {
        return mockk<LocationTracker.Callback>().also {
            every { it.onNoLocationProviderAvailable() } just runs
        }
    }

    private fun mockLocation(
            provider: String,
            latitude: Double = A_LATITUDE,
            longitude: Double = A_LONGITUDE,
            accuracy: Float = AN_ACCURACY
    ): Location {
        return mockk<Location>().also {
            every { it.time } returns 123
            every { it.latitude } returns latitude
            every { it.longitude } returns longitude
            every { it.accuracy } returns accuracy
            every { it.provider } returns provider
        }
    }
}
