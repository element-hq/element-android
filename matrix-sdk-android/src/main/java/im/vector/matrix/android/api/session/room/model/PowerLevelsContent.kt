/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.api.session.room.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.powerlevers.PowerLevelsConstants

/**
 * Class representing the EventType.EVENT_TYPE_STATE_ROOM_POWER_LEVELS state event content.
 */
@JsonClass(generateAdapter = true)
data class PowerLevelsContent(
        @Json(name = "ban") val ban: Int = PowerLevelsConstants.DEFAULT_ROOM_MODERATOR_LEVEL,
        @Json(name = "kick") val kick: Int = PowerLevelsConstants.DEFAULT_ROOM_MODERATOR_LEVEL,
        @Json(name = "invite") val invite: Int = PowerLevelsConstants.DEFAULT_ROOM_MODERATOR_LEVEL,
        @Json(name = "redact") val redact: Int = PowerLevelsConstants.DEFAULT_ROOM_MODERATOR_LEVEL,
        @Json(name = "events_default") val eventsDefault: Int = PowerLevelsConstants.DEFAULT_ROOM_USER_LEVEL,
        @Json(name = "events") val events: MutableMap<String, Int> = HashMap(),
        @Json(name = "users_default") val usersDefault: Int = PowerLevelsConstants.DEFAULT_ROOM_USER_LEVEL,
        @Json(name = "users") val users: MutableMap<String, Int> = HashMap(),
        @Json(name = "state_default") val stateDefault: Int = PowerLevelsConstants.DEFAULT_ROOM_MODERATOR_LEVEL,
        @Json(name = "notifications") val notifications: Map<String, Any> = HashMap()
)
