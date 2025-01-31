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

// RoomSync represents the response for a room during server sync v2.
@JsonClass(generateAdapter = true)
data class RoomSync(
        /**
         * The state updates for the room.
         */
        @Json(name = "state") val state: RoomSyncState? = null,

        /**
         * The timeline of messages and state changes in the room.
         */
        @Json(name = "timeline") val timeline: RoomSyncTimeline? = null,

        /**
         * The ephemeral events in the room that aren't recorded in the timeline or state of the room (e.g. typing, receipts).
         */
        @Json(name = "ephemeral") val ephemeral: LazyRoomSyncEphemeral? = null,

        /**
         * The account data events for the room (e.g. tags).
         */
        @Json(name = "account_data") val accountData: RoomSyncAccountData? = null,

        /**
         * The notification counts for the room.
         */
        @Json(name = "unread_notifications") val unreadNotifications: RoomSyncUnreadNotifications? = null,

        /**
         * The room summary.
         */
        @Json(name = "summary") val summary: RoomSyncSummary? = null

)
