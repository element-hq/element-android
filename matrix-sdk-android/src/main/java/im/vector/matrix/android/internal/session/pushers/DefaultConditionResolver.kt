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

import im.vector.matrix.android.api.pushrules.ConditionResolver
import im.vector.matrix.android.api.pushrules.ContainsDisplayNameCondition
import im.vector.matrix.android.api.pushrules.EventMatchCondition
import im.vector.matrix.android.api.pushrules.RoomMemberCountCondition
import im.vector.matrix.android.api.pushrules.SenderNotificationPermissionCondition
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.PowerLevelsContent
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.session.room.RoomGetter
import javax.inject.Inject

internal class DefaultConditionResolver @Inject constructor(
        private val roomGetter: RoomGetter,
        @UserId private val userId: String
) : ConditionResolver {

    override fun resolveEventMatchCondition(event: Event,
                                            condition: EventMatchCondition): Boolean {
        return condition.isSatisfied(event)
    }

    override fun resolveRoomMemberCountCondition(event: Event,
                                                 condition: RoomMemberCountCondition): Boolean {
        return condition.isSatisfied(event, roomGetter)
    }

    override fun resolveSenderNotificationPermissionCondition(event: Event,
                                                              condition: SenderNotificationPermissionCondition): Boolean {
        val roomId = event.roomId ?: return false
        val room = roomGetter.getRoom(roomId) ?: return false

        val powerLevelsContent = room.getStateEvent(EventType.STATE_ROOM_POWER_LEVELS)
                ?.content
                ?.toModel<PowerLevelsContent>()
                ?: PowerLevelsContent()

        return condition.isSatisfied(event, powerLevelsContent)
    }

    override fun resolveContainsDisplayNameCondition(event: Event,
                                                     condition: ContainsDisplayNameCondition): Boolean {
        val roomId = event.roomId ?: return false
        val room = roomGetter.getRoom(roomId) ?: return false
        val myDisplayName = room.getRoomMember(userId)?.displayName ?: return false
        return condition.isSatisfied(event, myDisplayName)
    }
}
