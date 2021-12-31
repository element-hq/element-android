/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.sync.handler.room

import io.realm.Realm
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.room.membership.RoomMemberHelper
import org.matrix.android.sdk.internal.session.typing.DefaultTypingUsersTracker
import javax.inject.Inject

internal class RoomTypingUsersHandler @Inject constructor(@UserId private val userId: String,
                                                          private val typingUsersTracker: DefaultTypingUsersTracker) {

    // TODO This could be handled outside of the Realm transaction. Use the new aggregator?
    fun handle(realm: Realm, roomId: String, ephemeralResult: RoomSyncHandler.EphemeralResult?) {
        val roomMemberHelper = RoomMemberHelper(realm, roomId)
        val typingIds = ephemeralResult?.typingUserIds?.filter { it != userId }.orEmpty()
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
