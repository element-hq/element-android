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

package org.matrix.android.sdk.internal.session.user

import io.realm.Realm
import io.realm.kotlin.createObject
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.internal.database.model.StringCountedEntity
import org.matrix.android.sdk.internal.database.model.UserEntity
import org.matrix.android.sdk.internal.database.query.where

internal object UserEntityFactory {

    /**
     * The strategy here is to store all the used display name of a user for all rooms.
     * So we maintain a counter for each display name and then we select the display name which is the most used
     * as room member name for the user display name.
     * We make the assumption that the user will not update display name per room (using /myroomnick) for a big number of rooms.
     *
     * The same algorithm is used for avatarUrl.
     */
    fun insertOrUpdate(realm: Realm,
                       userId: String,
                       roomMember: RoomMemberContent,
                       previousRoomMember: RoomMemberContent?) {
        val entity = UserEntity.where(realm, userId).findFirst() ?: realm.createObject(userId)

        // Display Name
        val newDisplayName = roomMember.displayName ?: ""
        val previousDisplayName = previousRoomMember?.displayName

        if (newDisplayName != previousDisplayName) {
            if (previousDisplayName != null) {
                entity.displayNameInRoom.indexOfFirst { it.value == previousDisplayName }
                        .takeIf { it != -1 }
                        ?.let { entity.displayNameInRoom[it] }
                        ?.takeIf { it.counter > 0 }
                        ?.counter
                        ?.dec()
            }
            entity.displayNameInRoom.indexOfFirst { it.value == newDisplayName }
                    .let { index ->
                        if (index == -1) {
                            entity.displayNameInRoom.add(StringCountedEntity(newDisplayName, 1))
                        } else {
                            entity.displayNameInRoom[index]?.counter?.inc()
                        }
                    }

            // Update displayName
            entity.displayNameInRoom.maxByOrNull { it.counter }
                    ?.let { entity.displayName = it.value }
        }

        // Avatar
        val newAvatar = roomMember.avatarUrl ?: ""
        val previousAvatar = previousRoomMember?.avatarUrl

        if (newAvatar != previousAvatar) {
            if (previousAvatar != null) {
                entity.avatarUrlInRoom.indexOfFirst { it.value == previousAvatar }
                        .takeIf { it != -1 }
                        ?.let { entity.avatarUrlInRoom[it] }
                        ?.takeIf { it.counter > 0 }
                        ?.counter
                        ?.dec()
            }
            entity.avatarUrlInRoom.indexOfFirst { it.value == newAvatar }
                    .let { index ->
                        if (index == -1) {
                            entity.avatarUrlInRoom.add(StringCountedEntity(newAvatar, 1))
                        } else {
                            entity.avatarUrlInRoom[index]?.counter?.inc()
                        }
                    }

            // Update avatar
            entity.avatarUrlInRoom.maxByOrNull { it.counter }
                    ?.let { entity.avatarUrl = it.value }
        }
    }
}
