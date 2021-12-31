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

@JsonClass(generateAdapter = true)
data class RoomSyncSummary(

        /**
         * Present only if the room has no m.room.name or m.room.canonical_alias.
         *
         *
         * Lists the mxids of the first 5 members in the room who are currently joined or invited (ordered by stream ordering as seen on the server,
         * to avoid it jumping around if/when topological order changes). As the heroes’ membership status changes, the list changes appropriately
         * (sending the whole new list in the next /sync response). This list always excludes the current logged in user. If there are no joined or
         * invited users, it lists the parted and banned ones instead.  Servers can choose to send more or less than 5 members if they must, but 5
         * seems like a good enough number for most naming purposes.  Clients should use all the provided members to name the room, but may truncate
         * the list if helpful for UX
         */
        @Json(name = "m.heroes") val heroes: List<String> = emptyList(),

        /**
         * The number of m.room.members in state 'joined' (including the syncing user) (can be null)
         */
        @Json(name = "m.joined_member_count") val joinedMembersCount: Int? = null,

        /**
         * The number of m.room.members in state 'invited' (can be null)
         */
        @Json(name = "m.invited_member_count") val invitedMembersCount: Int? = null
)
