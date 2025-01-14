/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.polls.detail.ui

import android.content.Context
import im.vector.app.test.fakes.FakeNavigator
import io.mockk.mockk
import org.junit.Test

internal class RoomPollDetailNavigatorTest {

    private val fakeNavigator = FakeNavigator()

    private val roomPollDetailNavigator = RoomPollDetailNavigator(
            navigator = fakeNavigator.instance,
    )

    @Test
    fun `given main navigator when goToTimelineEvent then correct method main navigator is called`() {
        // Given
        val aContext = mockk<Context>()
        val aRoomId = "roomId"
        val anEventId = "eventId"
        fakeNavigator.givenOpenRoomSuccess(
                context = aContext,
                roomId = aRoomId,
                eventId = anEventId,
                buildTask = true,
                isInviteAlreadyAccepted = false,
                trigger = null,
        )

        // When
        roomPollDetailNavigator.goToTimelineEvent(aContext, aRoomId, anEventId)

        // Then
        fakeNavigator.verifyOpenRoom(
                context = aContext,
                roomId = aRoomId,
                eventId = anEventId,
                buildTask = true,
                isInviteAlreadyAccepted = false,
                trigger = null,
        )
    }
}
