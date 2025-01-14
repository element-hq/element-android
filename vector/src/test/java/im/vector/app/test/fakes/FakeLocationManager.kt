/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs

class FakeLocationManager {
    val instance = mockk<LocationManager>()

    fun givenActiveProviders(providers: List<String>) {
        every { instance.allProviders } returns providers
    }

    fun givenRequestUpdatesForProvider(
            provider: String,
            listener: LocationListener
    ) {
        every { instance.requestLocationUpdates(provider, any<Long>(), any(), listener) } just runs
    }

    fun givenRemoveUpdates(listener: LocationListener) {
        every { instance.removeUpdates(listener) } just runs
    }

    fun givenLastLocationForProvider(provider: String, location: Location?) {
        every { instance.getLastKnownLocation(provider) } returns location
    }
}
