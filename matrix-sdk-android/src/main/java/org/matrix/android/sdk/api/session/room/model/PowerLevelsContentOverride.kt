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
 * Class representing the EventType.EVENT_TYPE_STATE_ROOM_POWER_LEVELS state event content.
 */
@JsonClass(generateAdapter = true)
data class PowerLevelsContentOverride(
        /**
         * The level required to ban a user. Defaults to 50 if unspecified.
         */
        @Json(name = "ban") val ban: Int? = null,
        /**
         * The level required to kick a user. Defaults to 50 if unspecified.
         */
        @Json(name = "kick") val kick: Int? = null,
        /**
         * The level required to invite a user. Defaults to 50 if unspecified.
         */
        @Json(name = "invite") val invite: Int? = null,
        /**
         * The level required to redact an event. Defaults to 50 if unspecified.
         */
        @Json(name = "redact") val redact: Int? = null,
        /**
         * The default level required to send message events. Can be overridden by the events key. Defaults to 0 if unspecified.
         */
        @Json(name = "events_default") val eventsDefault: Int? = null,
        /**
         * The level required to send specific event types. This is a mapping from event type to power level required.
         */
        @Json(name = "events") val events: Map<String, Int>? = null,
        /**
         * The default power level for every user in the room, unless their user_id is mentioned in the users key. Defaults to 0 if unspecified.
         */
        @Json(name = "users_default") val usersDefault: Int? = null,
        /**
         * The power levels for specific users. This is a mapping from user_id to power level for that user.
         */
        @Json(name = "users") val users: Map<String, Int>? = null,
        /**
         * The default level required to send state events. Can be overridden by the events key. Defaults to 50 if unspecified.
         */
        @Json(name = "state_default") val stateDefault: Int? = null,
        /**
         * The power level requirements for specific notification types. This is a mapping from key to power level for that notifications key.
         */
        @Json(name = "notifications") val notifications: Map<String, Any>? = null
)
