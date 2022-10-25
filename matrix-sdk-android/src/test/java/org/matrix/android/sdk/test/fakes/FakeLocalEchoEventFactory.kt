/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.test.fakes

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.internal.session.room.send.LocalEchoEventFactory

internal class FakeLocalEchoEventFactory {

    val instance = mockk<LocalEchoEventFactory>()

    fun givenCreateStaticLocationEvent(withLocalEcho: Boolean): Event {
        val event = Event()
        every {
            instance.createStaticLocationEvent(
                    roomId = any(),
                    latitude = any(),
                    longitude = any(),
                    uncertainty = any(),
                    isUserLocation = any()
            )
        } returns event

        if (withLocalEcho) {
            every { instance.createLocalEcho(event) } just runs
        }
        return event
    }

    fun verifyCreateStaticLocationEvent(
            roomId: String,
            latitude: Double,
            longitude: Double,
            uncertainty: Double?,
            isUserLocation: Boolean
    ) {
        verify {
            instance.createStaticLocationEvent(
                    roomId = roomId,
                    latitude = latitude,
                    longitude = longitude,
                    uncertainty = uncertainty,
                    isUserLocation = isUserLocation
            )
        }
    }

    fun givenCreateLiveLocationEvent(withLocalEcho: Boolean): Event {
        val event = Event()
        every {
            instance.createLiveLocationEvent(
                    beaconInfoEventId = any(),
                    roomId = any(),
                    latitude = any(),
                    longitude = any(),
                    uncertainty = any()
            )
        } returns event

        if (withLocalEcho) {
            every { instance.createLocalEcho(event) } just runs
        }
        return event
    }

    fun verifyCreateLiveLocationEvent(
            roomId: String,
            beaconInfoEventId: String,
            latitude: Double,
            longitude: Double,
            uncertainty: Double?
    ) {
        verify {
            instance.createLiveLocationEvent(
                    roomId = roomId,
                    beaconInfoEventId = beaconInfoEventId,
                    latitude = latitude,
                    longitude = longitude,
                    uncertainty = uncertainty
            )
        }
    }

    fun givenCreateRedactEvent(eventId: String, withLocalEcho: Boolean): Event {
        val event = Event()
        every {
            instance.createRedactEvent(
                    roomId = any(),
                    eventId = eventId,
                    reason = any()
            )
        } returns event

        if (withLocalEcho) {
            every { instance.createLocalEcho(event) } just runs
        }
        return event
    }

    fun verifyCreateRedactEvent(
            roomId: String,
            eventId: String,
            reason: String?
    ) {
        verify {
            instance.createRedactEvent(
                    roomId = roomId,
                    eventId = eventId,
                    reason = reason
            )
        }
    }

    fun verifyCreateLocalEcho(event: Event) {
        verify { instance.createLocalEcho(event) }
    }
}
