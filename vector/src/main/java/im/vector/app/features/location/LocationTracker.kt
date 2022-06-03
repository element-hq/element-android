/*
 * Copyright (c) 2021 New Vector Ltd
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

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import androidx.core.location.LocationListenerCompat
import im.vector.app.BuildConfig
import im.vector.app.core.utils.Debouncer
import im.vector.app.core.utils.createBackgroundHandler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val BKG_HANDLER_NAME = "LocationTracker.BKG_HANDLER_NAME"
private const val LOCATION_DEBOUNCE_ID = "LocationTracker.LOCATION_DEBOUNCE_ID"

@Singleton
class LocationTracker @Inject constructor(
        context: Context
) : LocationListenerCompat {

    private val locationManager = context.getSystemService<LocationManager>()

    interface Callback {
        fun onLocationUpdate(locationData: LocationData)
        fun onLocationProviderIsNotAvailable()
    }

    private val callbacks = mutableListOf<Callback>()

    private var hasGpsProviderLiveLocation = false

    private var lastLocation: LocationData? = null

    private val debouncer = Debouncer(createBackgroundHandler(BKG_HANDLER_NAME))

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    fun start() {
        Timber.d("## LocationTracker. start()")

        if (locationManager == null) {
            callbacks.forEach { it.onLocationProviderIsNotAvailable() }
            Timber.v("## LocationTracker. LocationManager is not available")
            return
        }

        val providers = locationManager.allProviders

        if (providers.isEmpty()) {
            callbacks.forEach { it.onLocationProviderIsNotAvailable() }
            Timber.v("## LocationTracker. There is no location provider available")
        } else {
            // Take GPS first
            providers.sortedByDescending { if (it == LocationManager.GPS_PROVIDER) 1 else 0 }
                    .mapNotNull { provider ->
                        Timber.d("## LocationTracker. track location using $provider")

                        locationManager.requestLocationUpdates(
                                provider,
                                MIN_TIME_TO_UPDATE_LOCATION_MILLIS,
                                MIN_DISTANCE_TO_UPDATE_LOCATION_METERS,
                                this
                        )

                        locationManager.getLastKnownLocation(provider)
                    }
                    .maxByOrNull { location -> location.time }
                    ?.let { latestKnownLocation ->
                        if (BuildConfig.LOW_PRIVACY_LOG_ENABLE) {
                            Timber.d("## LocationTracker. lastKnownLocation: $latestKnownLocation")
                        } else {
                            Timber.d("## LocationTracker. lastKnownLocation: ${latestKnownLocation.provider}")
                        }
                        notifyLocation(latestKnownLocation)
                    }
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    fun stop() {
        Timber.d("## LocationTracker. stop()")
        locationManager?.removeUpdates(this)
        callbacks.clear()
        debouncer.cancelAll()
        hasGpsProviderLiveLocation = false
    }

    /**
     * Request the last known location. It will be given async through Callback.
     * Please ensure adding a callback to receive the value.
     */
    fun requestLastKnownLocation() {
        lastLocation?.let { location -> callbacks.forEach { it.onLocationUpdate(location) } }
    }

    fun addCallback(callback: Callback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback)
        }
    }

    fun removeCallback(callback: Callback) {
        callbacks.remove(callback)
        if (callbacks.size == 0) {
            stop()
        }
    }

    override fun onLocationChanged(location: Location) {
        if (BuildConfig.LOW_PRIVACY_LOG_ENABLE) {
            Timber.d("## LocationTracker. onLocationChanged: $location")
        } else {
            Timber.d("## LocationTracker. onLocationChanged: ${location.provider}")
        }

        when (location.provider) {
            LocationManager.GPS_PROVIDER -> {
                hasGpsProviderLiveLocation = true
            }
            else -> {
                if (hasGpsProviderLiveLocation) {
                    // Ignore this update
                    Timber.d("## LocationTracker. ignoring location from ${location.provider}, we have gps live location")
                    return
                }
            }
        }

        debouncer.debounce(LOCATION_DEBOUNCE_ID, MIN_TIME_TO_UPDATE_LOCATION_MILLIS) {
            notifyLocation(location)
        }
    }

    private fun notifyLocation(location: Location) {
        if (BuildConfig.LOW_PRIVACY_LOG_ENABLE) {
            Timber.d("## LocationTracker. notify location: $location")
        } else {
            Timber.d("## LocationTracker. notify location: ${location.provider}")
        }

        val locationData = location.toLocationData()
        lastLocation = locationData
        callbacks.forEach { it.onLocationUpdate(location.toLocationData()) }
    }

    override fun onProviderDisabled(provider: String) {
        Timber.d("## LocationTracker. onProviderDisabled: $provider")
        callbacks.forEach { it.onLocationProviderIsNotAvailable() }
    }

    private fun Location.toLocationData(): LocationData {
        return LocationData(latitude, longitude, accuracy.toDouble())
    }
}
