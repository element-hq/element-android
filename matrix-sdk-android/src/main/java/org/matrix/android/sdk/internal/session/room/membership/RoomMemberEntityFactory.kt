/*
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
import org.matrix.android.sdk.internal.database.model.presence.UserPresenceEntity

internal object RoomMemberEntityFactory {

    fun create(roomId: String, userId: String, roomMember: RoomMemberContent, presence: UserPresenceEntity?): RoomMemberSummaryEntity {
        val primaryKey = "${roomId}_$userId"
        return RoomMemberSummaryEntity().apply {
            this.primaryKey = primaryKey
            this.userId = userId
            this.roomId = roomId
            this.displayName = roomMember.displayName
            this.avatarUrl = roomMember.avatarUrl
            this.membership = roomMember.membership
            this.userPresenceEntity = presence
        }
    }
}
