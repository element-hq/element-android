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
         * The room summary
         */
        @Json(name = "summary") val summary: RoomSyncSummary? = null

)
