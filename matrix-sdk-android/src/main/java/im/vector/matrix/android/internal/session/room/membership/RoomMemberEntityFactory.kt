/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session.room.membership

import im.vector.matrix.android.api.session.room.model.RoomMemberContent
import im.vector.matrix.android.internal.database.model.RoomMemberEntity

internal object RoomMemberEntityFactory {

    fun create(roomId: String, userId: String, roomMember: RoomMemberContent): RoomMemberEntity {
        val primaryKey = "${roomId}_$userId"
        return RoomMemberEntity(
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
