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
import org.matrix.android.sdk.api.session.room.powerlevels.Role

/**
 * Class representing the EventType.EVENT_TYPE_STATE_ROOM_POWER_LEVELS state event content.
 */
@JsonClass(generateAdapter = true)
data class PowerLevelsContent(
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
) {
    /**
     * Return a copy of this content with a new power level for the specified user
     *
     * @param userId the userId to alter the power level of
     * @param powerLevel the new power level, or null to set the default value.
     */
    fun setUserPowerLevel(userId: String, powerLevel: Int?): PowerLevelsContent {
        return copy(
                users = users.orEmpty().toMutableMap().apply {
                    if (powerLevel == null || powerLevel == usersDefault) {
                        remove(userId)
                    } else {
                        put(userId, powerLevel)
                    }
                }
        )
    }

    /**
     * Get the notification level for a dedicated key.
     *
     * @param key the notification key
     * @return the level, default to Moderator if the key is not found
     */
    fun notificationLevel(key: String): Int {
        return when (val value = notifications.orEmpty()[key]) {
            // the first implementation was a string value
            is String -> value.toInt()
            is Double -> value.toInt()
            is Int    -> value
            else      -> Role.Moderator.value
        }
    }

    companion object {
        /**
         * Key to use for content.notifications and get the level required to trigger an @room notification. Defaults to 50 if unspecified.
         */
        const val NOTIFICATIONS_ROOM_KEY = "room"
    }
}

// Fallback to default value, defined in the Matrix specification
fun PowerLevelsContent.banOrDefault() = ban ?: Role.Moderator.value
fun PowerLevelsContent.kickOrDefault() = kick ?: Role.Moderator.value
fun PowerLevelsContent.inviteOrDefault() = invite ?: Role.Moderator.value
fun PowerLevelsContent.redactOrDefault() = redact ?: Role.Moderator.value
fun PowerLevelsContent.eventsDefaultOrDefault() = eventsDefault ?: Role.Default.value
fun PowerLevelsContent.usersDefaultOrDefault() = usersDefault ?: Role.Default.value
fun PowerLevelsContent.stateDefaultOrDefault() = stateDefault ?: Role.Moderator.value
