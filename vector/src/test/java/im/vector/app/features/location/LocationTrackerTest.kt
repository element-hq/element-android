/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.location

import android.content.Context
import android.location.Location
import android.location.LocationManager
import im.vector.app.core.utils.Debouncer
import im.vector.app.core.utils.createBackgroundHandler
import im.vector.app.test.fakes.FakeContext
import im.vector.app.test.fakes.FakeHandler
import im.vector.app.test.fakes.FakeLocationManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Test

private const val A_LATITUDE = 1.2
private const val A_LONGITUDE = 44.0
private const val AN_ACCURACY = 5.0f

class LocationTrackerTest {

    private val fakeHandler = FakeHandler()

    init {
        mockkConstructor(Debouncer::class)
        every { anyConstructed<Debouncer>().cancelAll() } just runs
        val runnable = slot<Runnable>()
        every { anyConstructed<Debouncer>().debounce(any(), MIN_TIME_TO_UPDATE_LOCATION_MILLIS, capture(runnable)) } answers {
            runnable.captured.run()
            true
        }
        mockkStatic("im.vector.app.core.utils.HandlerKt")
        every { createBackgroundHandler(any()) } returns fakeHandler.instance
    }

    private val fakeLocationManager = FakeLocationManager()
    private val fakeContext = FakeContext().also {
        it.givenService(Context.LOCATION_SERVICE, android.location.LocationManager::class.java, fakeLocationManager.instance)
    }

    private val locationTracker = LocationTracker(fakeContext.instance)

    @Before
    fun setUp() {
        fakeLocationManager.givenRemoveUpdates(locationTracker)
    }

    @After
    fun tearDown() {
        unmockkStatic("im.vector.app.core.utils.HandlerKt")
        unmockkConstructor(Debouncer::class)
    }

    @Test
    fun `given available list of providers when starting then location updates are requested in priority order`() {
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER)
        mockAvailableProviders(providers)

        locationTracker.start()

        verifyOrder {
            fakeLocationManager.instance.requestLocationUpdates(
                    LocationManager.FUSED_PROVIDER,
                    MIN_TIME_TO_UPDATE_LOCATION_MILLIS,
                    MIN_DISTANCE_TO_UPDATE_LOCATION_METERS,
                    locationTracker
            )
            fakeLocationManager.instance.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_TO_UPDATE_LOCATION_MILLIS,
                    MIN_DISTANCE_TO_UPDATE_LOCATION_METERS,
                    locationTracker
            )
            fakeLocationManager.instance.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_TO_UPDATE_LOCATION_MILLIS,
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
        val callback = mockCallback()

        locationTracker.addCallback(callback)

        locationTracker.callbacks.size shouldBeEqualTo 1
        locationTracker.callbacks.first() shouldBeEqualTo callback

        locationTracker.removeCallback(callback)

        locationTracker.callbacks.size shouldBeEqualTo 0
    }

    @Test
    fun `when location updates are received from fused provider then fused locations are taken in priority`() {
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER)
        mockAvailableProviders(providers)
        val callback = mockCallback()
        locationTracker.addCallback(callback)
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
        locationTracker.onLocationChanged(fusedLocation)
        locationTracker.onLocationChanged(gpsLocation)
        locationTracker.onLocationChanged(networkLocation)

        val expectedLocationData = LocationData(
                latitude = 1.0,
                longitude = 3.0,
                uncertainty = 4.0
        )
        verify { callback.onLocationUpdate(expectedLocationData) }
        verify { anyConstructed<Debouncer>().debounce(any(), MIN_TIME_TO_UPDATE_LOCATION_MILLIS, any()) }
        locationTracker.hasLocationFromFusedProvider shouldBeEqualTo true
        locationTracker.hasLocationFromGPSProvider shouldBeEqualTo false
    }

    @Test
    fun `when location updates are received from gps provider then gps locations are taken if none are received from fused provider`() {
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER)
        mockAvailableProviders(providers)
        val callback = mockCallback()
        locationTracker.addCallback(callback)
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
        locationTracker.onLocationChanged(gpsLocation)
        locationTracker.onLocationChanged(networkLocation)

        val expectedLocationData = LocationData(
                latitude = 1.0,
                longitude = 3.0,
                uncertainty = 4.0
        )
        verify { callback.onLocationUpdate(expectedLocationData) }
        verify { anyConstructed<Debouncer>().debounce(any(), MIN_TIME_TO_UPDATE_LOCATION_MILLIS, any()) }
        locationTracker.hasLocationFromFusedProvider shouldBeEqualTo false
        locationTracker.hasLocationFromGPSProvider shouldBeEqualTo true
    }

    @Test
    fun `when location updates are received from network provider then network locations are taken if none are received from fused or gps provider`() {
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER)
        mockAvailableProviders(providers)
        val callback = mockCallback()
        locationTracker.addCallback(callback)
        locationTracker.start()

        val networkLocation = mockLocation(
                provider = LocationManager.NETWORK_PROVIDER,
                latitude = 1.0,
                longitude = 3.0,
                accuracy = 4f
        )
        locationTracker.onLocationChanged(networkLocation)

        val expectedLocationData = LocationData(
                latitude = 1.0,
                longitude = 3.0,
                uncertainty = 4.0
        )
        verify { callback.onLocationUpdate(expectedLocationData) }
        verify { anyConstructed<Debouncer>().debounce(any(), MIN_TIME_TO_UPDATE_LOCATION_MILLIS, any()) }
        locationTracker.hasLocationFromFusedProvider shouldBeEqualTo false
        locationTracker.hasLocationFromGPSProvider shouldBeEqualTo false
    }

    @Test
    fun `when requesting the last location then last location is notified via callback`() {
        val providers = listOf(LocationManager.GPS_PROVIDER)
        fakeLocationManager.givenActiveProviders(providers)
        val lastLocation = mockLocation(provider = LocationManager.GPS_PROVIDER)
        fakeLocationManager.givenLastLocationForProvider(provider = LocationManager.GPS_PROVIDER, location = lastLocation)
        fakeLocationManager.givenRequestUpdatesForProvider(provider = LocationManager.GPS_PROVIDER, listener = locationTracker)
        val callback = mockCallback()
        locationTracker.addCallback(callback)
        locationTracker.start()

        locationTracker.requestLastKnownLocation()

        val expectedLocationData = LocationData(
                latitude = A_LATITUDE,
                longitude = A_LONGITUDE,
                uncertainty = AN_ACCURACY.toDouble()
        )
        verify { callback.onLocationUpdate(expectedLocationData) }
    }

    @Test
    fun `when stopping then location updates are stopped and callbacks are cleared`() {
        locationTracker.stop()

        verify { fakeLocationManager.instance.removeUpdates(locationTracker) }
        verify { anyConstructed<Debouncer>().cancelAll() }
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
            every { it.onLocationUpdate(any()) } just runs
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
