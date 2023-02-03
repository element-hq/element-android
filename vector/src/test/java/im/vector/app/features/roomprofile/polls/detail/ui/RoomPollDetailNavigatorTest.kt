/*
 * Copyright (c) 2023 New Vector Ltd
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
