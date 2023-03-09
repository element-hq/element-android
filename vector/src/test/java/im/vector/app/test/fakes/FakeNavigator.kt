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

package im.vector.app.test.fakes

import android.content.Context
import im.vector.app.features.analytics.plan.ViewRoom
import im.vector.app.features.navigation.Navigator
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class FakeNavigator {

    val instance: Navigator = mockk()

    fun givenOpenRoomSuccess(
            context: Context,
            roomId: String,
            eventId: String?,
            buildTask: Boolean,
            isInviteAlreadyAccepted: Boolean,
            trigger: ViewRoom.Trigger?,
    ) {
        justRun { instance.openRoom(context, roomId, eventId, buildTask, isInviteAlreadyAccepted, trigger) }
    }

    fun verifyOpenRoom(
            context: Context,
            roomId: String,
            eventId: String?,
            buildTask: Boolean,
            isInviteAlreadyAccepted: Boolean,
            trigger: ViewRoom.Trigger?,
    ) {
        verify { instance.openRoom(context, roomId, eventId, buildTask, isInviteAlreadyAccepted, trigger) }
    }
}
