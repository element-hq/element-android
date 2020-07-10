/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.matrix.android.api.session.room.model.create

import im.vector.matrix.android.api.session.identity.ThreePid
import im.vector.matrix.android.api.session.room.model.PowerLevelsContent
import im.vector.matrix.android.api.session.room.model.RoomDirectoryVisibility
import im.vector.matrix.android.api.session.room.model.RoomHistoryVisibility
import im.vector.matrix.android.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM

class CreateRoomParamsBuilder {
    var visibility: RoomDirectoryVisibility? = null
    var roomAliasName: String? = null
    var name: String? = null
    var topic: String? = null

    /**
     * UserIds to invite
     */
    val invitedUserIds = mutableListOf<String>()

    /**
     * ThreePids to invite
     */
    val invite3pids = mutableListOf<ThreePid>()

    /**
     * If set to true, when the room will be created, if cross-signing is enabled and we can get keys for every invited users,
     * the encryption will be enabled on the created room
     */
    var enableEncryptionIfInvitedUsersSupportIt: Boolean = false

    var preset: CreateRoomPreset? = null

    var isDirect: Boolean? = null

    var creationContent: Any? = null

    var powerLevelContentOverride: PowerLevelsContent? = null

    /**
     * Mark as a direct message room.
     */
    fun setDirectMessage() {
        preset = CreateRoomPreset.PRESET_TRUSTED_PRIVATE_CHAT
        isDirect = true
    }

    /**
     * Supported value: MXCRYPTO_ALGORITHM_MEGOLM
     */
    var algorithm: String? = null
        private set

    var historyVisibility: RoomHistoryVisibility? = null

    fun enableEncryption() {
        algorithm = MXCRYPTO_ALGORITHM_MEGOLM
    }

    /**
     * Tells if the created room can be a direct chat one.
     *
     * @return true if it is a direct chat
     */
    fun isDirect(): Boolean {
        return preset == CreateRoomPreset.PRESET_TRUSTED_PRIVATE_CHAT
                && isDirect == true
    }

    /**
     * @return the first invited user id
     */
    fun getFirstInvitedUserId(): String? {
        return invitedUserIds.firstOrNull() ?: invite3pids.firstOrNull()?.value
    }
}
