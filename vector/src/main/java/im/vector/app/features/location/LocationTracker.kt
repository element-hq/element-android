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
import android.location.LocationListener
import android.location.LocationManager
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import timber.log.Timber
import javax.inject.Inject

class LocationTracker @Inject constructor(
        private val context: Context
) : LocationListener {

    interface Callback {
        fun onLocationUpdate(locationData: LocationData)
        fun onLocationProviderIsNotAvailable()
    }

    private var locationManager: LocationManager? = null
    var callback: Callback? = null

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    fun start() {
        val locationManager = context.getSystemService<LocationManager>()

        locationManager?.let {
            val isGpsEnabled = it.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = it.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            val provider = when {
                isGpsEnabled     -> LocationManager.GPS_PROVIDER
                isNetworkEnabled -> LocationManager.NETWORK_PROVIDER
                else             -> {
                    callback?.onLocationProviderIsNotAvailable()
                    Timber.v("## LocationTracker. There is no location provider available")
                    return
                }
            }

            // Send last known location without waiting location updates
            it.getLastKnownLocation(provider)?.let { lastKnownLocation ->
                callback?.onLocationUpdate(lastKnownLocation.toLocationData())
            }

            it.requestLocationUpdates(
                    provider,
                    MIN_TIME_MILLIS_TO_UPDATE_LOCATION,
                    MIN_DISTANCE_METERS_TO_UPDATE_LOCATION,
                    this
            )
        } ?: run {
            callback?.onLocationProviderIsNotAvailable()
            Timber.v("## LocationTracker. LocationManager is not available")
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    fun stop() {
        locationManager?.removeUpdates(this)
        callback = null
    }

    override fun onLocationChanged(location: Location) {
        callback?.onLocationUpdate(location.toLocationData())
    }

    private fun Location.toLocationData(): LocationData {
        return LocationData(latitude, longitude, accuracy.toDouble())
    }
}
