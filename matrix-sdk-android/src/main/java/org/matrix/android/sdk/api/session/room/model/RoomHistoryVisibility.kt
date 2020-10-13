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

package org.matrix.android.sdk.api.session.room.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Ref: https://matrix.org/docs/spec/client_server/latest#room-history-visibility
 */
@JsonClass(generateAdapter = false)
enum class RoomHistoryVisibility {
    /**
     * All events while this is the m.room.history_visibility value may be shared by any
     * participating homeserver with anyone, regardless of whether they have ever joined the room.
     */
    @Json(name = "world_readable") WORLD_READABLE,

    /**
     * Previous events are always accessible to newly joined members. All events in the
     * room are accessible, even those sent when the member was not a part of the room.
     */
    @Json(name = "shared") SHARED,

    /**
     * Events are accessible to newly joined members from the point they were invited onwards.
     * Events stop being accessible when the member's state changes to something other than invite or join.
     */
    @Json(name = "invited") INVITED,

    /**
     * Events are accessible to newly joined members from the point they joined the room onwards.
     * Events stop being accessible when the member's state changes to something other than join.
     */
    @Json(name = "joined") JOINED
}
