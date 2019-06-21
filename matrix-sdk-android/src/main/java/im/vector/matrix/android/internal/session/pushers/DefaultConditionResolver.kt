/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.matrix.android.internal.session.pushers

import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.pushrules.*
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.RoomService
import im.vector.matrix.android.internal.di.MatrixKoinComponent
import org.koin.standalone.inject
import timber.log.Timber

internal class DefaultConditionResolver(val event: Event) : ConditionResolver, MatrixKoinComponent {

    private val roomService by inject<RoomService>()

    private val sessionParams by inject<SessionParams>()

    override fun resolveEventMatchCondition(eventMatchCondition: EventMatchCondition): Boolean {
        return eventMatchCondition.isSatisfied(event)
    }

    override fun resolveRoomMemberCountCondition(roomMemberCountCondition: RoomMemberCountCondition): Boolean {
        return roomMemberCountCondition.isSatisfied(event, roomService)
    }

    override fun resolveSenderNotificationPermissionCondition(senderNotificationPermissionCondition: SenderNotificationPermissionCondition): Boolean {
//        val roomId = event.roomId ?: return false
//        val room = roomService.getRoom(roomId) ?: return false
        //TODO RoomState not yet managed
        Timber.e("POWER LEVELS STATE NOT YET MANAGED BY RIOTX")
        return false //senderNotificationPermissionCondition.isSatisfied(event, )
    }

    override fun resolveContainsDisplayNameCondition(containsDisplayNameCondition: ContainsDisplayNameCondition): Boolean {
        val roomId = event.roomId ?: return false
        val room = roomService.getRoom(roomId) ?: return false
        val myDisplayName = room.getRoomMember(sessionParams.credentials.userId)?.displayName
                ?: return false
        return containsDisplayNameCondition.isSatisfied(event, myDisplayName)
    }
}