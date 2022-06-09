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
import androidx.annotation.VisibleForTesting
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
        /**
         * Called on every location update.
         */
        fun onLocationUpdate(locationData: LocationData)

        /**
         * Called when no location provider is available to request location updates.
         */
        fun onNoLocationProviderAvailable()
    }

    @VisibleForTesting
    val callbacks = mutableListOf<Callback>()

    @VisibleForTesting
    var hasLocationFromFusedProvider = false

    @VisibleForTesting
    var hasLocationFromGPSProvider = false

    private var lastLocation: LocationData? = null

    private val debouncer = Debouncer(createBackgroundHandler(BKG_HANDLER_NAME))

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    fun start() {
        Timber.d("start()")

        if (locationManager == null) {
            Timber.v("LocationManager is not available")
            onNoLocationProviderAvailable()
            return
        }

        val providers = locationManager.allProviders

        if (providers.isEmpty()) {
            Timber.v("There is no location provider available")
            onNoLocationProviderAvailable()
        } else {
            // Take GPS first
            providers.sortedByDescending(::getProviderPriority)
                    .mapNotNull { provider ->
                        Timber.d("track location using $provider")

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
                            Timber.d("lastKnownLocation: $latestKnownLocation")
                        } else {
                            Timber.d("lastKnownLocation: ${latestKnownLocation.provider}")
                        }
                        notifyLocation(latestKnownLocation)
                    }
        }
    }

    /**
     * Compute the priority of the given provider name.
     * @return an integer representing the priority: the higher the value, the higher the priority is.
     */
    private fun getProviderPriority(provider: String): Int = when (provider) {
        LocationManager.FUSED_PROVIDER -> 2
        LocationManager.GPS_PROVIDER -> 1
        else -> 0
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    fun stop() {
        Timber.d("stop()")
        locationManager?.removeUpdates(this)
        callbacks.clear()
        debouncer.cancelAll()
        hasLocationFromGPSProvider = false
        hasLocationFromFusedProvider = false
    }

    /**
     * Request the last known location. It will be given async through Callback.
     * Please ensure adding a callback to receive the value.
     */
    fun requestLastKnownLocation() {
        lastLocation?.let { locationData -> onLocationUpdate(locationData) }
    }

    fun addCallback(callback: Callback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback)
        }
    }

    fun removeCallback(callback: Callback) {
        callbacks.remove(callback)
    }

    override fun onLocationChanged(location: Location) {
        if (BuildConfig.LOW_PRIVACY_LOG_ENABLE) {
            Timber.d("onLocationChanged: $location")
        } else {
            Timber.d("onLocationChanged: ${location.provider}")
        }

        when (location.provider) {
            LocationManager.FUSED_PROVIDER -> {
                hasLocationFromFusedProvider = true
            }
            LocationManager.GPS_PROVIDER -> {
                if (hasLocationFromFusedProvider) {
                    hasLocationFromGPSProvider = false
                    // Ignore this update
                    Timber.d("ignoring location from ${location.provider}, we have location from fused provider")
                    return
                } else {
                    hasLocationFromGPSProvider = true
                }
            }
            else -> {
                if (hasLocationFromFusedProvider || hasLocationFromGPSProvider) {
                    // Ignore this update
                    Timber.d("ignoring location from ${location.provider}, we have location from GPS provider")
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
            Timber.d("notify location: $location")
        } else {
            Timber.d("notify location: ${location.provider}")
        }

        val locationData = location.toLocationData()
        lastLocation = locationData
        onLocationUpdate(locationData)
    }

    override fun onProviderDisabled(provider: String) {
        Timber.d("onProviderDisabled: $provider")
        when (provider) {
            LocationManager.FUSED_PROVIDER -> hasLocationFromFusedProvider = false
            LocationManager.GPS_PROVIDER -> hasLocationFromGPSProvider = false
        }

        locationManager?.allProviders
                ?.takeIf { it.isEmpty() }
                ?.let {
                    Timber.e("all providers have been disabled")
                    onNoLocationProviderAvailable()
                }
    }

    private fun onNoLocationProviderAvailable() {
        callbacks.forEach {
            try {
                it.onNoLocationProviderAvailable()
            } catch (error: Exception) {
                Timber.e(error, "error in onNoLocationProviderAvailable callback $it")
            }
        }
    }

    private fun onLocationUpdate(locationData: LocationData) {
        callbacks.forEach {
            try {
                it.onLocationUpdate(locationData)
            } catch (error: Exception) {
                Timber.e(error, "error in onLocationUpdate callback $it")
            }
        }
    }

    private fun Location.toLocationData(): LocationData {
        return LocationData(latitude, longitude, accuracy.toDouble())
    }
}
