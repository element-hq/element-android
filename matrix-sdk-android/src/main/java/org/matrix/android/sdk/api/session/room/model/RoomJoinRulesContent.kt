/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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
 *
 */

package org.matrix.android.sdk.api.session.room.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import timber.log.Timber

/**
 * Class representing the EventType.STATE_ROOM_JOIN_RULES state event content
 */
@JsonClass(generateAdapter = true)
data class RoomJoinRulesContent(
        @Json(name = "join_rule") val _joinRules: String? = null,
        /**
         * If the allow key is an empty list (or not a list at all), then the room reverts to standard public join rules
         */
        @Json(name = "allow") val allowList: List<RoomJoinRulesAllowEntry>? = null
) {
    val joinRules: RoomJoinRules? = when (_joinRules) {
        "public" -> RoomJoinRules.PUBLIC
        "invite" -> RoomJoinRules.INVITE
        "knock" -> RoomJoinRules.KNOCK
        "private" -> RoomJoinRules.PRIVATE
        "restricted" -> RoomJoinRules.RESTRICTED
        else         -> {
            Timber.w("Invalid value for RoomJoinRules: `$_joinRules`")
            null
        }
    }
}
