/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.create

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.model.RoomDirectoryVisibility
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomPreset
import org.matrix.android.sdk.internal.session.room.membership.threepid.ThreePidInviteBody

/**
 * Parameter to create a room
 */
@JsonClass(generateAdapter = true)
internal data class CreateRoomBody(
        /**
         * A public visibility indicates that the room will be shown in the published room list.
         * A private visibility will hide the room from the published room list.
         * Rooms default to private visibility if this key is not included.
         * NB: This should not be confused with join_rules which also uses the word public. One of: ["public", "private"]
         */
        @Json(name = "visibility")
        val visibility: RoomDirectoryVisibility?,

        /**
         * The desired room alias local part. If this is included, a room alias will be created and mapped to the newly created room.
         * The alias will belong on the same homeserver which created the room.
         * For example, if this was set to "foo" and sent to the homeserver "example.com" the complete room alias would be #foo:example.com.
         */
        @Json(name = "room_alias_name")
        val roomAliasName: String?,

        /**
         * If this is included, an m.room.name event will be sent into the room to indicate the name of the room.
         * See Room Events for more information on m.room.name.
         */
        @Json(name = "name")
        val name: String?,

        /**
         * If this is included, an m.room.topic event will be sent into the room to indicate the topic for the room.
         * See Room Events for more information on m.room.topic.
         */
        @Json(name = "topic")
        val topic: String?,

        /**
         * A list of user IDs to invite to the room.
         * This will tell the server to invite everyone in the list to the newly created room.
         */
        @Json(name = "invite")
        val invitedUserIds: List<String>?,

        /**
         * A list of objects representing third party IDs to invite into the room.
         */
        @Json(name = "invite_3pid")
        val invite3pids: List<ThreePidInviteBody>?,

        /**
         * Extra keys, such as m.federate, to be added to the content of the m.room.create event.
         * The server will clobber the following keys: creator, room_version.
         * Future versions of the specification may allow the server to clobber other keys.
         */
        @Json(name = "creation_content")
        val creationContent: Any?,

        /**
         * A list of state events to set in the new room.
         * This allows the user to override the default state events set in the new room.
         * The expected format of the state events are an object with type, state_key and content keys set.
         * Takes precedence over events set by presets, but gets overridden by name and topic keys.
         */
        @Json(name = "initial_state")
        val initialStates: List<Event>?,

        /**
         * Convenience parameter for setting various default state events based on a preset. Must be either:
         * private_chat => join_rules is set to invite. history_visibility is set to shared.
         * trusted_private_chat => join_rules is set to invite. history_visibility is set to shared. All invitees are given the same power level as the
         * room creator.
         * public_chat: => join_rules is set to public. history_visibility is set to shared.
         */
        @Json(name = "preset")
        val preset: CreateRoomPreset?,

        /**
         * This flag makes the server set the is_direct flag on the m.room.member events sent to the users in invite and invite_3pid.
         * See Direct Messaging for more information.
         */
        @Json(name = "is_direct")
        val isDirect: Boolean?,

        /**
         * The power level content to override in the default power level event
         */
        @Json(name = "power_level_content_override")
        val powerLevelContentOverride: PowerLevelsContent?,

        /**
         * The room version to set for the room. If not provided, the homeserver is to use its configured default.
         * If provided, the homeserver will return a 400 error with the errcode M_UNSUPPORTED_ROOM_VERSION if it does not support the room version.
         */
        @Json(name = "room_version")
        val roomVersion: String?
)
