/*
 * Copyright 2019 New Vector Ltd
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
import org.matrix.android.sdk.api.session.room.powerlevels.Role

/**
 * Class representing the EventType.EVENT_TYPE_STATE_ROOM_POWER_LEVELS state event content.
 */
@JsonClass(generateAdapter = true)
data class PowerLevelsContent(
        @Json(name = "ban") val ban: Int = Role.Moderator.value,
        @Json(name = "kick") val kick: Int = Role.Moderator.value,
        @Json(name = "invite") val invite: Int = Role.Moderator.value,
        @Json(name = "redact") val redact: Int = Role.Moderator.value,
        @Json(name = "events_default") val eventsDefault: Int = Role.Default.value,
        @Json(name = "events") val events: MutableMap<String, Int> = HashMap(),
        @Json(name = "users_default") val usersDefault: Int = Role.Default.value,
        @Json(name = "users") val users: MutableMap<String, Int> = HashMap(),
        @Json(name = "state_default") val stateDefault: Int = Role.Moderator.value,
        @Json(name = "notifications") val notifications: Map<String, Any> = HashMap()
) {
    /**
     * Alter this content with a new power level for the specified user
     *
     * @param userId the userId to alter the power level of
     * @param powerLevel the new power level, or null to set the default value.
     */
    fun setUserPowerLevel(userId: String, powerLevel: Int?) {
        if (powerLevel == null || powerLevel == usersDefault) {
            users.remove(userId)
        } else {
            users[userId] = powerLevel
        }
    }
}
