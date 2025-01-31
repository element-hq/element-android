/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.membership

import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntity
import org.matrix.android.sdk.internal.database.model.presence.UserPresenceEntity

internal object RoomMemberEntityFactory {

    fun create(roomId: String, userId: String, roomMember: RoomMemberContent, presence: UserPresenceEntity?): RoomMemberSummaryEntity {
        val primaryKey = "${roomId}_$userId"
        return RoomMemberSummaryEntity(
                primaryKey = primaryKey,
                userId = userId,
                roomId = roomId,
                displayName = roomMember.displayName,
                avatarUrl = roomMember.avatarUrl
        ).apply {
            membership = roomMember.membership
            userPresenceEntity = presence
        }
    }
}
