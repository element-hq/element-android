/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.resources.BuildMeta
import im.vector.app.features.session.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@VisibleForTesting
const val MIN_DISTANCE_TO_UPDATE_LOCATION_METERS = 10f

@Singleton
class LocationTracker @Inject constructor(
        context: Context,
        private val activeSessionHolder: ActiveSessionHolder,
        private val buildMeta: BuildMeta,
) : LocationListenerCompat {

    private val locationManager = context.getSystemService<LocationManager>()

    interface Callback {
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

    private var isStarted = false
    private var isStarting = false
    private var firstLocationHandled = false
    private val _locations = MutableSharedFlow<Location>(replay = 1)

    @VisibleForTesting
    val minDurationToUpdateLocationMillis = 5.seconds.inWholeMilliseconds

    /**
     * SharedFlow to collect location updates.
     */
    val locations = _locations.asSharedFlow()
            .onEach { Timber.d("new location emitted") }
            .debounce {
                if (firstLocationHandled) {
                    minDurationToUpdateLocationMillis
                } else {
                    firstLocationHandled = true
                    0
                }
            }
            .onEach { Timber.d("new location emitted after debounce") }
            .map { it.toLocationData() }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    fun start() {
        if (!isStarting && !isStarted) {
            isStarting = true
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
                                    minDurationToUpdateLocationMillis,
                                    MIN_DISTANCE_TO_UPDATE_LOCATION_METERS,
                                    this
                            )

                            locationManager.getLastKnownLocation(provider)
                        }
                        .maxByOrNull { location -> location.time }
                        ?.let { latestKnownLocation ->
                            if (buildMeta.lowPrivacyLoggingEnabled) {
                                Timber.d("lastKnownLocation: $latestKnownLocation")
                            } else {
                                Timber.d("lastKnownLocation: ${latestKnownLocation.provider}")
                            }
                            notifyLocation(latestKnownLocation)
                        }
            }
            isStarted = true
            isStarting = false
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
    @VisibleForTesting
    fun stop() {
        Timber.d("stop()")
        locationManager?.removeUpdates(this)
        callbacks.clear()
        hasLocationFromGPSProvider = false
        hasLocationFromFusedProvider = false
        isStarting = false
        isStarted = false
    }

    /**
     * Request the last known location. It will be given async through corresponding flow.
     * Please ensure collecting the flow before calling this method.
     */
    fun requestLastKnownLocation() {
        Timber.d("requestLastKnownLocation")
        activeSessionHolder.getSafeActiveSession()?.coroutineScope?.launch {
            _locations.replayCache.firstOrNull()?.let {
                Timber.d("emitting last location from cache")
                _locations.emit(it)
            }
        }
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
        if (buildMeta.lowPrivacyLoggingEnabled) {
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

        notifyLocation(location)
    }

    private fun notifyLocation(location: Location) {
        activeSessionHolder.getSafeActiveSession()?.coroutineScope?.launch {
            if (buildMeta.lowPrivacyLoggingEnabled) {
                Timber.d("notify location: $location")
            } else {
                Timber.d("notify location: ${location.provider}")
            }

            _locations.emit(location)
        }
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
        callbacks.toList().forEach {
            try {
                it.onNoLocationProviderAvailable()
            } catch (error: Exception) {
                Timber.e(error, "error in onNoLocationProviderAvailable callback $it")
            }
        }
    }

    private fun Location.toLocationData(): LocationData {
        return LocationData(latitude, longitude, accuracy.toDouble())
    }
}
