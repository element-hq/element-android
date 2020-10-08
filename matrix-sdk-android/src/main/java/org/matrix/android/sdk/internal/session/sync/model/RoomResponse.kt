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

package org.matrix.android.sdk.internal.session.sync.model

import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.events.model.Event

/**
 * Class representing a room from a JSON response from room or global initial sync.
 */
@JsonClass(generateAdapter = true)
internal data class RoomResponse(
        // The room identifier.
        val roomId: String? = null,

        // The last recent messages of the room.
        val messages: TokensChunkResponse<Event>? = null,

        // The state events.
        val state: List<Event>? = null,

        // The private data that this user has attached to this room.
        val accountData: List<Event>? = null,

        // The current user membership in this room.
        val membership: String? = null,

        // The room visibility (public/private).
        val visibility: String? = null,

        // The matrix id of the inviter in case of pending invitation.
        val inviter: String? = null,

        // The invite event if membership is invite.
        val invite: Event? = null,

        // The presence status of other users
        // (Provided in case of room initial sync @see http://matrix.org/docs/api/client-server/#!/-rooms/get_room_sync_data)).
        val presence: List<Event>? = null,

        // The read receipts (Provided in case of room initial sync).
        val receipts: List<Event>? = null
)
