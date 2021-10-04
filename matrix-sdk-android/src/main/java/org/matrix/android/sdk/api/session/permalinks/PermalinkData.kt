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

package org.matrix.android.sdk.api.session.permalinks

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * This sealed class represents all the permalink cases.
 * You don't have to instantiate yourself but should use [PermalinkParser] instead.
 */
sealed class PermalinkData {

    data class RoomLink(
            val roomIdOrAlias: String,
            val isRoomAlias: Boolean,
            val eventId: String?,
            val viaParameters: List<String>
    ) : PermalinkData()

    /**
     * &room_name=Team2
        &room_avatar_url=mxc:
         &inviter_name=bob
     */
    @Parcelize
    data class RoomEmailInviteLink(
        val roomId: String,
        val email: String,
        val signUrl: String,
        val roomName: String?,
        val roomAvatarUrl: String?,
        val inviterName: String?,
        val identityServer: String,
        val token: String,
        val privateKey: String,
        val roomType: String?
    ) : PermalinkData(), Parcelable

    data class UserLink(val userId: String) : PermalinkData()

    data class GroupLink(val groupId: String) : PermalinkData()

    data class FallbackLink(val uri: Uri) : PermalinkData()
}
