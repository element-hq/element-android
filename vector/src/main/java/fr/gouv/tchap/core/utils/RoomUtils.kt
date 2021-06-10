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

package fr.gouv.tchap.core.utils

import fr.gouv.tchap.android.sdk.session.room.model.RoomAccessRules
import org.matrix.android.sdk.api.session.room.model.RoomSummary

enum class TchapRoomType {
    UNKNOWN,
    DIRECT,
    PRIVATE,
    EXTERNAL,
    FORUM
}

object RoomUtils {
    fun getRoomType(roomSummary: RoomSummary): TchapRoomType {
        return when {
            roomSummary.isEncrypted -> when (roomSummary.accessRules) {
                RoomAccessRules.RESTRICTED -> TchapRoomType.PRIVATE
                RoomAccessRules.UNRESTRICTED -> TchapRoomType.EXTERNAL
                RoomAccessRules.DIRECT -> TchapRoomType.DIRECT
                null -> if (roomSummary.isDirect) TchapRoomType.DIRECT else TchapRoomType.PRIVATE
                else                         -> TchapRoomType.UNKNOWN
            }
            roomSummary.isPublic    -> TchapRoomType.FORUM
            else                    -> TchapRoomType.UNKNOWN
        }
    }
}
