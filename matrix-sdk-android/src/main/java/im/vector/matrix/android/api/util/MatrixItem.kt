/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.api.util

import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.model.roomdirectory.PublicRoom
import im.vector.matrix.android.api.session.user.model.User

sealed class MatrixItem(
        open val id: String,
        open val displayName: String?,
        open val avatarUrl: String?
) {
    data class UserItem(override val id: String,
                        override val displayName: String? = null,
                        override val avatarUrl: String? = null
    ) : MatrixItem(id, displayName, avatarUrl)

    data class EventItem(override val id: String,
                         override val displayName: String? = null,
                         override val avatarUrl: String? = null)
        : MatrixItem(id, displayName, avatarUrl)

    data class RoomItem(override val id: String,
                        override val displayName: String? = null,
                        override val avatarUrl: String? = null)
        : MatrixItem(id, displayName, avatarUrl)

    data class RoomAliasItem(override val id: String,
                             override val displayName: String? = null,
                             override val avatarUrl: String? = null)
        : MatrixItem(id, displayName, avatarUrl)

    data class GroupItem(override val id: String,
                         override val displayName: String? = null,
                         override val avatarUrl: String? = null)
        : MatrixItem(id, displayName, avatarUrl)

    fun getBestName(): String {
        return displayName?.takeIf { it.isNotBlank() } ?: id
    }

    /**
     * Return the prefix as defined in the matrix spec (and not extracted from the id)
     */
    fun getPrefix() = when (this) {
        is UserItem      -> '@'
        is EventItem     -> '$'
        is RoomItem      -> '!'
        is RoomAliasItem -> '#'
        is GroupItem     -> '+'
    }

    companion object {
        fun from(user: User) = UserItem(user.userId, user.displayName, user.avatarUrl)
        fun from(groupSummary: GroupSummary) = GroupItem(groupSummary.groupId, groupSummary.displayName, groupSummary.avatarUrl)
        fun from(roomSummary: RoomSummary) = RoomItem(roomSummary.roomId, roomSummary.displayName, roomSummary.avatarUrl)
        fun from(publicRoom: PublicRoom) = RoomItem(publicRoom.roomId, publicRoom.name, publicRoom.avatarUrl)
    }
}
