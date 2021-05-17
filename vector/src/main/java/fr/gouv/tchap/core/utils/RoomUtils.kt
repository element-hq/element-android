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

import org.matrix.android.sdk.api.session.room.model.RoomSummary

enum class RoomAccessRules {
    DIRECT,
    RESTRICTED,
    UNRESTRICTED
}

enum class RoomTchapType {
    UNKNOWN,
    DIRECT,
    PRIVATE,
    EXTERNAL,
    FORUM
}

object RoomUtils {
    /**
     * FIXME: Change roomAccessRule with real implementation
     */
    fun getRoomType(roomSummary: RoomSummary): RoomTchapType {
        val roomAccessRule = RoomAccessRules.RESTRICTED

        return when {
            roomSummary.isDirect    -> RoomTchapType.DIRECT
            roomSummary.isEncrypted -> when (roomAccessRule) {
                RoomAccessRules.RESTRICTED   -> RoomTchapType.PRIVATE
                RoomAccessRules.UNRESTRICTED -> RoomTchapType.EXTERNAL
                else                         -> RoomTchapType.UNKNOWN
            }
            roomSummary.isPublic    -> RoomTchapType.FORUM
            else                    -> RoomTchapType.UNKNOWN
        }
    }
}
