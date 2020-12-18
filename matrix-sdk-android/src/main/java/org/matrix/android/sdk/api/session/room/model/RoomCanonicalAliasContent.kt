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
 * Class representing the EventType.STATE_ROOM_CANONICAL_ALIAS state event content
 */
@JsonClass(generateAdapter = true)
data class RoomCanonicalAliasContent(
        /**
         * The canonical alias for the room. If not present, null, or empty the room should be considered to have no canonical alias.
         */
        @Json(name = "alias") val canonicalAlias: String? = null,

        /**
         * Alternative aliases the room advertises.
         * This list can have aliases despite the alias field being null, empty, or otherwise not present.
         */
        @Json(name = "alt_aliases") val alternativeAliases: List<String>? = null
)
