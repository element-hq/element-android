/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.presence.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.presence.model.PresenceEnum

/**
 * Class representing the EventType.PRESENCE event content.
 */
@JsonClass(generateAdapter = true)
internal data class PresenceContent(
        /**
         * Required. The presence state for this user. One of: ["online", "offline", "unavailable"]
         */
        @Json(name = "presence") val presence: PresenceEnum,
        /**
         * The last time since this used performed some action, in milliseconds.
         */
        @Json(name = "last_active_ago") val lastActiveAgo: Long? = null,
        /**
         * An optional description to accompany the presence.
         */
        @Json(name = "status_msg") val statusMessage: String? = null,
        /**
         * Whether the user is currently active.
         */
        @Json(name = "currently_active") val isCurrentlyActive: Boolean = false,
        /**
         * The current avatar URL for this user, if any.
         */
        @Json(name = "avatar_url") val avatarUrl: String? = null,
        /**
         * The current display name for this user, if any.
         */
        @Json(name = "displayname") val displayName: String? = null
)
