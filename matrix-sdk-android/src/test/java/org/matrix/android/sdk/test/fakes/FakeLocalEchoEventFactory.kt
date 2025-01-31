/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
