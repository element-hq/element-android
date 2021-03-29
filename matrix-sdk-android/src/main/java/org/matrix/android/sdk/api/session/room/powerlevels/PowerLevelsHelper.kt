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
 *
 */

package org.matrix.android.sdk.api.session.room.powerlevels

import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.model.banOrDefault
import org.matrix.android.sdk.api.session.room.model.eventsDefaultOrDefault
import org.matrix.android.sdk.api.session.room.model.inviteOrDefault
import org.matrix.android.sdk.api.session.room.model.kickOrDefault
import org.matrix.android.sdk.api.session.room.model.redactOrDefault
import org.matrix.android.sdk.api.session.room.model.stateDefaultOrDefault
import org.matrix.android.sdk.api.session.room.model.usersDefaultOrDefault

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
    fun getUserPowerLevelValue(userId: String): Int {
        return powerLevelsContent.users
                ?.get(userId)
                ?: powerLevelsContent.usersDefaultOrDefault()
    }

    /**
     * Returns the user power level of a dedicated user Id
     *
     * @param userId the user id
     * @return the power level
     */
    fun getUserRole(userId: String): Role {
        val value = getUserPowerLevelValue(userId)
        // I think we should use powerLevelsContent.usersDefault, but Ganfra told me that it was like that on riot-Web
        return Role.fromValue(value, powerLevelsContent.eventsDefaultOrDefault())
    }

    /**
     * Tell if an user can send an event of a certain type
     *
     * @param userId  the id of the user to check for.
     * @param isState true if the event is a state event (ie. state key is not null)
     * @param eventType the event type to check for
     * @return true if the user can send this type of event
     */
    fun isUserAllowedToSend(userId: String, isState: Boolean, eventType: String?): Boolean {
        return if (userId.isNotEmpty()) {
            val powerLevel = getUserPowerLevelValue(userId)
            val minimumPowerLevel = powerLevelsContent.events?.get(eventType)
                    ?: if (isState) {
                        powerLevelsContent.stateDefaultOrDefault()
                    } else {
                        powerLevelsContent.eventsDefaultOrDefault()
                    }
            powerLevel >= minimumPowerLevel
        } else false
    }

    /**
     * Check if the user have the necessary power level to invite
     * @param userId the id of the user to check for.
     * @return true if able to invite
     */
    fun isUserAbleToInvite(userId: String): Boolean {
        val powerLevel = getUserPowerLevelValue(userId)
        return powerLevel >= powerLevelsContent.inviteOrDefault()
    }

    /**
     * Check if the user have the necessary power level to ban
     * @param userId the id of the user to check for.
     * @return true if able to ban
     */
    fun isUserAbleToBan(userId: String): Boolean {
        val powerLevel = getUserPowerLevelValue(userId)
        return powerLevel >= powerLevelsContent.banOrDefault()
    }

    /**
     * Check if the user have the necessary power level to kick
     * @param userId the id of the user to check for.
     * @return true if able to kick
     */
    fun isUserAbleToKick(userId: String): Boolean {
        val powerLevel = getUserPowerLevelValue(userId)
        return powerLevel >= powerLevelsContent.kickOrDefault()
    }

    /**
     * Check if the user have the necessary power level to redact
     * @param userId the id of the user to check for.
     * @return true if able to redact
     */
    fun isUserAbleToRedact(userId: String): Boolean {
        val powerLevel = getUserPowerLevelValue(userId)
        return powerLevel >= powerLevelsContent.redactOrDefault()
    }
}
