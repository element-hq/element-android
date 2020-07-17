/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.session.sync

import im.vector.matrix.android.api.session.room.sender.SenderInfo
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.session.room.membership.RoomMemberHelper
import im.vector.matrix.android.internal.session.typing.DefaultTypingUsersTracker
import io.realm.Realm
import javax.inject.Inject

internal class RoomTypingUsersHandler @Inject constructor(@UserId private val userId: String,
                                                          private val typingUsersTracker: DefaultTypingUsersTracker) {

    fun handle(realm: Realm, roomId: String, ephemeralResult: RoomSyncHandler.EphemeralResult?) {
        val roomMemberHelper = RoomMemberHelper(realm, roomId)
        val typingIds = ephemeralResult?.typingUserIds?.filter { it != userId } ?: emptyList()
        val senderInfo = typingIds.map { userId ->
            val roomMemberSummaryEntity = roomMemberHelper.getLastRoomMember(userId)
            SenderInfo(
                    userId = userId,
                    displayName = roomMemberSummaryEntity?.displayName,
                    isUniqueDisplayName = roomMemberHelper.isUniqueDisplayName(roomMemberSummaryEntity?.displayName),
                    avatarUrl = roomMemberSummaryEntity?.avatarUrl
            )
        }
        typingUsersTracker.setTypingUsersFromRoom(roomId, senderInfo)
    }
}
