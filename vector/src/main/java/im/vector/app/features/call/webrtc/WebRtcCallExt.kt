/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call.webrtc

import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem

fun WebRtcCall.getOpponentAsMatrixItem(session: Session): MatrixItem? {
    return session.getRoom(nativeRoomId)?.let { room ->
        val roomSummary = room.roomSummary() ?: return@let null
        // Fallback to RoomSummary if there is no other member.
        if (roomSummary.otherMemberIds.isEmpty().orFalse()) {
            roomSummary.toMatrixItem()
        } else {
            val userId = roomSummary.otherMemberIds.first()
            return room.membershipService().getRoomMember(userId)?.toMatrixItem()
        }
    }
}
