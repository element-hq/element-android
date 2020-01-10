/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.matrix.android.api.session.room.powerlevers

import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.model.PowerLevelsContent

/**
 * This class is an helper around PowerLevelsContent.
 */
class PowerLevelsHelper(private val powerLevelsContent: PowerLevelsContent) {

    /**
     * Returns the user power level of a dedicated user Id
     *
     * @param userId the user id
     * @return the power level
     */
    fun getUserPowerLevel(userId: String): Int {
        return powerLevelsContent.users.getOrElse(userId) {
            powerLevelsContent.usersDefault
        }
    }

    /**
     * Tell if an user can send an event of a certain type
     *
     * @param eventType the event type to check for
     * @param userId          the user id
     * @return true if the user can send this type of event
     */
    fun isAllowedToSend(eventType: String, userId: String): Boolean {
        return if (eventType.isNotEmpty() && userId.isNotEmpty()) {
            val powerLevel = getUserPowerLevel(userId)
            val minimumPowerLevel = powerLevelsContent.events[eventType]
                                    ?: if (EventType.isStateEvent(eventType)) {
                                        powerLevelsContent.stateDefault
                                    } else {
                                        powerLevelsContent.eventsDefault
                                    }
            powerLevel >= minimumPowerLevel
        } else false
    }


    /**
     * Get the notification level for a dedicated key.
     *
     * @param key the notification key
     * @return the level
     */
    fun notificationLevel(key: String): Int {
        val value = powerLevelsContent.notifications[key]
                    ?: return PowerLevelsConstants.DEFAULT_ROOM_MODERATOR_LEVEL
        return when (value) {
            // the first implementation was a string value
            is String -> value.toInt()
            is Int    -> value
            else      -> PowerLevelsConstants.DEFAULT_ROOM_MODERATOR_LEVEL
        }
    }
}
