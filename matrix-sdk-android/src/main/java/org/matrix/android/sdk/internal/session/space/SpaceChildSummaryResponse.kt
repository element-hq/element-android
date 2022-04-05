/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.space

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.events.model.Event

/**
 * The fields are the same as those returned by /publicRooms (see spec), with the addition of:
 *       room_type: the value of the m.type field from the room's m.room.create event, if any.
 *       children_state: The m.space.child events of the room.
 *       For each event, only the following fields are included: type, state_key, content, room_id, sender,
 *       with the addition of: origin_server_ts: This is required for sorting of rooms as specified below.
 */
@JsonClass(generateAdapter = true)
internal data class SpaceChildSummaryResponse(

        /**
         * The room type, which is m.space for subspaces.
         * It can be omitted if there is no room type in which case it should be interpreted as a normal room.
         */
        @Json(name = "room_type") val roomType: String? = null,

        /**  The m.space.child events of the room. For each event, only the following fields are included:
         *  type, state_key, content, room_id, sender, with the addition of origin_server_ts.
         */
        @Json(name = "children_state") val childrenState: List<Event>? = null,

        /**
         * Aliases of the room. May be empty.
         */
        @Json(name = "aliases")
        val aliases: List<String>? = null,

        /**
         * The canonical alias of the room, if any.
         */
        @Json(name = "canonical_alias")
        val canonicalAlias: String? = null,

        /**
         * The name of the room, if any.
         */
        @Json(name = "name")
        val name: String? = null,

        /**
         * Required. The number of members joined to the room.
         */
        @Json(name = "num_joined_members")
        val numJoinedMembers: Int = 0,

        /**
         * Required. The ID of the room.
         */
        @Json(name = "room_id")
        val roomId: String,

        /**
         * The topic of the room, if any.
         */
        @Json(name = "topic")
        val topic: String? = null,

        /**
         * Required. Whether the room may be viewed by guest users without joining.
         */
        @Json(name = "world_readable")
        val isWorldReadable: Boolean = false,

        /**
         * Required. Whether guest users may join the room and participate in it. If they can,
         * they will be subject to ordinary power level rules like any other user.
         */
        @Json(name = "guest_can_join")
        val guestCanJoin: Boolean = false,

        /**
         * The URL for the room's avatar, if one is set.
         */
        @Json(name = "avatar_url")
        val avatarUrl: String? = null,

        /**
         * Undocumented item
         */
        @Json(name = "m.federate")
        val isFederated: Boolean = false
)
