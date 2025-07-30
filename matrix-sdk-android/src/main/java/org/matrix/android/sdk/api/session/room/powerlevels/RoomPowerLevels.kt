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
import org.matrix.android.sdk.api.session.room.model.create.RoomCreateContentWithSender
import org.matrix.android.sdk.api.session.room.model.create.explicitlyPrivilegeRoomCreators
import org.matrix.android.sdk.api.session.room.model.eventsDefaultOrDefault
import org.matrix.android.sdk.api.session.room.model.inviteOrDefault
import org.matrix.android.sdk.api.session.room.model.kickOrDefault
import org.matrix.android.sdk.api.session.room.model.notificationLevelOrDefault
import org.matrix.android.sdk.api.session.room.model.redactOrDefault
import org.matrix.android.sdk.api.session.room.model.stateDefaultOrDefault
import org.matrix.android.sdk.api.session.room.model.usersDefaultOrDefault

/**
 * This class is an helper around PowerLevelsContent and RoomCreateContent.
 */
class RoomPowerLevels(
        val powerLevelsContent: PowerLevelsContent?,
        private val roomCreateContent: RoomCreateContentWithSender?,
) {

    /**
     * Returns the user power level of a dedicated user Id.
     *
     * @param userId the user id
     * @return the power level
     */
    fun getUserPowerLevel(userId: String): UserPowerLevel {
        if (shouldGiveInfinitePowerLevel(userId)) return UserPowerLevel.Infinite
        if (powerLevelsContent == null) return UserPowerLevel.User
        val value = powerLevelsContent.users
                ?.get(userId)
                ?: powerLevelsContent.usersDefaultOrDefault()

        return UserPowerLevel.Value(value)
    }

    /**
     * Returns the user power level of a dedicated user Id.
     *
     * @param userId the user id
     * @return the power level
     */
    fun getSuggestedRole(userId: String): Role {
        val value = getUserPowerLevel(userId)
        return Role.getSuggestedRole(value)
    }

    /**
     * Tell if an user can send an event of a certain type.
     *
     * @param userId the id of the user to check for.
     * @param isState true if the event is a state event (ie. state key is not null)
     * @param eventType the event type to check for
     * @return true if the user can send this type of event
     */
    fun isUserAllowedToSend(userId: String, isState: Boolean, eventType: String?): Boolean {
        return if (userId.isNotEmpty()) {
            val powerLevel = getUserPowerLevel(userId)
            val minimumPowerLevel = powerLevelsContent?.events?.get(eventType)
                    ?: if (isState) {
                        powerLevelsContent.stateDefaultOrDefault()
                    } else {
                        powerLevelsContent.eventsDefaultOrDefault()
                    }
            powerLevel >= UserPowerLevel.Value(minimumPowerLevel)
        } else false
    }

    /**
     * Check if the user have the necessary power level to invite.
     * @param userId the id of the user to check for.
     * @return true if able to invite
     */
    fun isUserAbleToInvite(userId: String): Boolean {
        val powerLevel = getUserPowerLevel(userId)
        return powerLevel >= UserPowerLevel.Value(powerLevelsContent.inviteOrDefault())
    }

    /**
     * Check if the user have the necessary power level to ban.
     * @param userId the id of the user to check for.
     * @return true if able to ban
     */
    fun isUserAbleToBan(userId: String): Boolean {
        val powerLevel = getUserPowerLevel(userId)
        return powerLevel >= UserPowerLevel.Value(powerLevelsContent.banOrDefault())
    }

    /**
     * Check if the user have the necessary power level to kick (remove).
     * @param userId the id of the user to check for.
     * @return true if able to kick
     */
    fun isUserAbleToKick(userId: String): Boolean {
        val powerLevel = getUserPowerLevel(userId)
        return powerLevel >= UserPowerLevel.Value(powerLevelsContent.kickOrDefault())
    }

    /**
     * Check if the user have the necessary power level to redact.
     * @param userId the id of the user to check for.
     * @return true if able to redact
     */
    fun isUserAbleToRedact(userId: String): Boolean {
        val powerLevel = getUserPowerLevel(userId)
        return powerLevel >= UserPowerLevel.Value(powerLevelsContent.redactOrDefault())
    }

    fun isUserAbleToTriggerNotification(userId: String, notificationKey: String): Boolean {
        val userPowerLevel = getUserPowerLevel(userId)
        val notificationPowerLevel = UserPowerLevel.Value(powerLevelsContent.notificationLevelOrDefault(key = notificationKey))
        return userPowerLevel >= notificationPowerLevel
    }

    private fun shouldGiveInfinitePowerLevel(userId: String): Boolean {
        if (roomCreateContent == null) return false
        return if (roomCreateContent.inner.explicitlyPrivilegeRoomCreators()) {
            roomCreateContent.creators.contains(userId)
        } else {
            false
        }
    }
}
