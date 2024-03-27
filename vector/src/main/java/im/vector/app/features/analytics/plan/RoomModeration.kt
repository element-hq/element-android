/*
 * Copyright (c) 2021 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.analytics.plan

import im.vector.app.features.analytics.itf.VectorAnalyticsEvent

// GENERATED FILE, DO NOT EDIT. FOR MORE INFORMATION VISIT
// https://github.com/matrix-org/matrix-analytics-events/

/**
 * Triggered when a moderation action is performed within a room.
 */
data class RoomModeration(
        /**
         * The action that was performed.
         */
        val action: Action,
        /**
         * When the action sets a particular power level, this is the suggested
         * role for that the power level.
         */
        val role: Role? = null,
) : VectorAnalyticsEvent {

    enum class Action {
        /**
         * Banned a room member.
         */
        BanMember,

        /**
         * Changed a room member's power level.
         */
        ChangeMemberRole,

        /**
         * Changed the power level required to ban room members.
         */
        ChangePermissionsBanMembers,

        /**
         * Changed the power level required to invite users to the room.
         */
        ChangePermissionsInviteUsers,

        /**
         * Changed the power level required to kick room members.
         */
        ChangePermissionsKickMembers,

        /**
         * Changed the power level required to redact messages in the room.
         */
        ChangePermissionsRedactMessages,

        /**
         * Changed the power level required to set the room's avatar.
         */
        ChangePermissionsRoomAvatar,

        /**
         * Changed the power level required to set the room's name.
         */
        ChangePermissionsRoomName,

        /**
         * Changed the power level required to set the room's topic.
         */
        ChangePermissionsRoomTopic,

        /**
         * Changed the power level required to send messages in the room.
         */
        ChangePermissionsSendMessages,

        /**
         * Kicked a room member.
         */
        KickMember,

        /**
         * Reset all of the room permissions back to their default values.
         */
        ResetPermissions,

        /**
         * Unbanned a room member.
         */
        UnbanMember,
    }

    enum class Role {

        /**
         * A power level of 100.
         */
        Administrator,

        /**
         * A power level of 50.
         */
        Moderator,

        /**
         * Any other power level.
         */
        Other,

        /**
         * A power level of 0.
         */
        User,
    }

    override fun getName() = "RoomModeration"

    override fun getProperties(): Map<String, Any>? {
        return mutableMapOf<String, Any>().apply {
            put("action", action.name)
            role?.let { put("role", it.name) }
        }.takeIf { it.isNotEmpty() }
    }
}
