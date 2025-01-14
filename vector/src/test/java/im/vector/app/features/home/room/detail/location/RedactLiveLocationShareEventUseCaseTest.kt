/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.location

import im.vector.app.test.fakes.FakeRoom
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.matrix.android.sdk.api.session.events.model.Event

private const val AN_EVENT_ID = "event-id"
private const val A_REASON = "reason"

class RedactLiveLocationShareEventUseCaseTest {

    private val fakeRoom = FakeRoom()

    private val redactLiveLocationShareEventUseCase = RedactLiveLocationShareEventUseCase()

    @Test
    fun `given an event with valid id when calling use case then event is redacted in the room`() = runTest {
        val event = Event(eventId = AN_EVENT_ID)
        fakeRoom.locationSharingService().givenRedactLiveLocationShare(beaconInfoEventId = AN_EVENT_ID, reason = A_REASON)

        redactLiveLocationShareEventUseCase.execute(event = event, room = fakeRoom, reason = A_REASON)

        fakeRoom.locationSharingService().verifyRedactLiveLocationShare(beaconInfoEventId = AN_EVENT_ID, reason = A_REASON)
    }

    @Test
    fun `given an event with empty id when calling use case then nothing is done`() = runTest {
        val event = Event(eventId = "")

        redactLiveLocationShareEventUseCase.execute(event = event, room = fakeRoom, reason = A_REASON)

        fakeRoom.locationSharingService().verifyRedactLiveLocationShare(inverse = true, beaconInfoEventId = "", reason = A_REASON)
    }
}
