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

package im.vector.matrix.android.internal.database.mapper

import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomMemberSummary
import im.vector.matrix.android.internal.database.model.RoomMemberSummaryEntity
import im.vector.matrix.sqldelight.session.Memberships
import javax.inject.Inject

internal class RoomMemberSummaryMapper @Inject constructor() {

    fun map(roomMemberSummaryEntity: RoomMemberSummaryEntity): RoomMemberSummary {
        return RoomMemberSummary(
                userId = roomMemberSummaryEntity.userId,
                avatarUrl = roomMemberSummaryEntity.avatarUrl,
                displayName = roomMemberSummaryEntity.displayName,
                membership = roomMemberSummaryEntity.membership
        )
    }

    fun map(roomMemberSummaryEntity: im.vector.matrix.sqldelight.session.RoomMemberSummaryEntity): RoomMemberSummary {
        return RoomMemberSummary(
                userId = roomMemberSummaryEntity.user_id,
                avatarUrl = roomMemberSummaryEntity.avatar_url,
                displayName = roomMemberSummaryEntity.display_name,
                membership = Membership.valueOf(roomMemberSummaryEntity.membership.name)
        )
    }

    fun map(room_id: String, user_id: String, display_name: String?, avatar_url: String?, membership: Memberships): RoomMemberSummary {
        return RoomMemberSummary(
                userId = user_id,
                membership = membership.map(),
                displayName = display_name,
                avatarUrl = avatar_url
        )
    }

}

internal fun im.vector.matrix.sqldelight.session.RoomMemberSummaryEntity.asDomain(): RoomMemberSummary {
    return RoomMemberSummaryMapper().map(this)
}

internal fun RoomMemberSummaryEntity.asDomain(): RoomMemberSummary {
    return RoomMemberSummaryMapper().map(this)
}
