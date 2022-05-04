/*
 * Copyright (c) 2021 New Vector Ltd
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
