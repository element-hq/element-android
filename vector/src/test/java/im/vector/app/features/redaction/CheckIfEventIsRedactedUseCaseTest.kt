/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.redaction

import im.vector.app.test.fakes.FakeSession
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.UnsignedData

private const val A_ROOM_ID = "room_id"
private const val AN_EVENT_ID = "event_id"

class CheckIfEventIsRedactedUseCaseTest {

    private val fakeSession = FakeSession()

    private val checkIfEventIsRedactedUseCase = CheckIfEventIsRedactedUseCase(
            session = fakeSession
    )

    @Test
    fun `given a room id and event id for redacted event when calling use case then true is returned`() = runTest {
        val event = Event(
                unsignedData = UnsignedData(age = 123, redactedEvent = Event())
        )
        fakeSession.eventService()
                .givenGetEventReturns(event)

        val result = checkIfEventIsRedactedUseCase.execute(A_ROOM_ID, AN_EVENT_ID)

        result shouldBeEqualTo true
    }

    @Test
    fun `given a room id and event id for non redacted event when calling use case then false is returned`() = runTest {
        val event = Event()
        fakeSession.eventService()
                .givenGetEventReturns(event)

        val result = checkIfEventIsRedactedUseCase.execute(A_ROOM_ID, AN_EVENT_ID)

        result shouldBeEqualTo false
    }
}
