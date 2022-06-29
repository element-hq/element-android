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
