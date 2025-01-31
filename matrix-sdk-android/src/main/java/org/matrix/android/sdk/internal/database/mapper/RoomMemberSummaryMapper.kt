/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.mapper

import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntity
import org.matrix.android.sdk.internal.database.model.presence.toUserPresence

internal object RoomMemberSummaryMapper {

    fun map(roomMemberSummaryEntity: RoomMemberSummaryEntity): RoomMemberSummary {
        return RoomMemberSummary(
                userId = roomMemberSummaryEntity.userId,
                userPresence = roomMemberSummaryEntity.userPresenceEntity?.toUserPresence(),
                avatarUrl = roomMemberSummaryEntity.avatarUrl,
                displayName = roomMemberSummaryEntity.displayName,
                membership = roomMemberSummaryEntity.membership
        )
    }
}

internal fun RoomMemberSummaryEntity.asDomain(): RoomMemberSummary {
    return RoomMemberSummaryMapper.map(this)
}
