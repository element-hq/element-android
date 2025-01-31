/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.sync.handler.room

import io.realm.Realm
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.room.membership.RoomMemberHelper
import org.matrix.android.sdk.internal.session.typing.DefaultTypingUsersTracker
import javax.inject.Inject

internal class RoomTypingUsersHandler @Inject constructor(
        @UserId private val userId: String,
        private val typingUsersTracker: DefaultTypingUsersTracker
) {

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
