/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.sync.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.events.model.Event

/**
 * `MXRoomSyncUnreadNotifications` represents the unread counts for a room.
 */
@JsonClass(generateAdapter = true)
data class RoomSyncUnreadNotifications(
        /**
         * List of account data events (array of Event).
         */
        @Json(name = "events") val events: List<Event>? = null,

        /**
         * The number of unread messages that match the push notification rules.
         */
        @Json(name = "notification_count") val notificationCount: Int? = null,

        /**
         * The number of highlighted unread messages (subset of notifications).
         */
        @Json(name = "highlight_count") val highlightCount: Int? = null
)
