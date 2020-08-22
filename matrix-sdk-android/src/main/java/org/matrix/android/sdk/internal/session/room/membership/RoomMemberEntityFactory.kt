/*
 * Copyright 2019 New Vector Ltd
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.room.membership

import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntity

internal object RoomMemberEntityFactory {

    fun create(roomId: String, userId: String, roomMember: RoomMemberContent): RoomMemberSummaryEntity {
        val primaryKey = "${roomId}_$userId"
        return RoomMemberSummaryEntity(
                primaryKey = primaryKey,
                userId = userId,
                roomId = roomId,
                displayName = roomMember.displayName,
                avatarUrl = roomMember.avatarUrl
        ).apply {
            membership = roomMember.membership
        }
    }
}
