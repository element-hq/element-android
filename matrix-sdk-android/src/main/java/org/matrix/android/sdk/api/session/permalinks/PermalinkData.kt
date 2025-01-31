/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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

    /*
     * &room_name=Team2
     * &room_avatar_url=mxc:
     * &inviter_name=bob
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

    data class FallbackLink(val uri: Uri, val isLegacyGroupLink: Boolean = false) : PermalinkData()
}
