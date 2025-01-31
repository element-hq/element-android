/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.create

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.room.model.Membership

/**
 * Class representing the EventType.LOCAL_STATE_ROOM_THIRD_PARTY_INVITE state event content
 * This class is only used to store the third party invite data of a local room.
 */
@JsonClass(generateAdapter = true)
internal data class LocalRoomThirdPartyInviteContent(
        @Json(name = "membership") val membership: Membership,
        @Json(name = "displayname") val displayName: String? = null,
        @Json(name = "is_direct") val isDirect: Boolean = false,
        @Json(name = "third_party_invite") val thirdPartyInvite: ThreePid? = null,
)
