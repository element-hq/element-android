/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.api.session.room.model.create

import android.util.Patterns
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.MatrixPatterns.isUserId
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toContent
import im.vector.matrix.android.api.session.room.model.PowerLevels
import im.vector.matrix.android.api.session.room.model.RoomDirectoryVisibility
import im.vector.matrix.android.api.session.room.model.RoomHistoryVisibility
import im.vector.matrix.android.internal.auth.data.ThreePidMedium
import java.util.*

/**
 * Parameter to create a room, with facilities functions to configure it
 */
@JsonClass(generateAdapter = true)
class CreateRoomParams {

    /**
     * A public visibility indicates that the room will be shown in the published room list.
     * A private visibility will hide the room from the published room list.
     * Rooms default to private visibility if this key is not included.
     * NB: This should not be confused with join_rules which also uses the word public. One of: ["public", "private"]
     */
    var visibility: RoomDirectoryVisibility? = null

    /**
     * The desired room alias local part. If this is included, a room alias will be created and mapped to the newly created room.
     * The alias will belong on the same homeserver which created the room.
     * For example, if this was set to "foo" and sent to the homeserver "example.com" the complete room alias would be #foo:example.com.
     */
    @Json(name = "room_alias_name")
    var roomAliasName: String? = null

    /**
     * If this is included, an m.room.name event will be sent into the room to indicate the name of the room.
     * See Room Events for more information on m.room.name.
     */
    var name: String? = null

    /**
     * If this is included, an m.room.topic event will be sent into the room to indicate the topic for the room.
     * See Room Events for more information on m.room.topic.
     */
    var topic: String? = null

    /**
     * A list of user IDs to invite to the room.
     * This will tell the server to invite everyone in the list to the newly created room.
     */
    @Json(name = "invite")
    var invitedUserIds: MutableList<String>? = null

    /**
     * A list of objects representing third party IDs to invite into the room.
     */
    @Json(name = "invite_3pid")
    var invite3pids: MutableList<Invite3Pid>? = null

    /**
     * Extra keys to be added to the content of the m.room.create.
     * The server will clobber the following keys: creator.
     * Future versions of the specification may allow the server to clobber other keys.
     */
    @Json(name = "creation_content")
    var creationContent: Any? = null

    /**
     * A list of state events to set in the new room.
     * This allows the user to override the default state events set in the new room.
     * The expected format of the state events are an object with type, state_key and content keys set.
     * Takes precedence over events set by presets, but gets overriden by name and topic keys.
     */
    @Json(name = "initial_state")
    var initialStates: MutableList<Event>? = null

    /**
     * Convenience parameter for setting various default state events based on a preset. Must be either:
     * private_chat => join_rules is set to invite. history_visibility is set to shared.
     * trusted_private_chat => join_rules is set to invite. history_visibility is set to shared. All invitees are given the same power level as the
     * room creator.
     * public_chat: => join_rules is set to public. history_visibility is set to shared.
     */
    var preset: CreateRoomPreset? = null

    /**
     * This flag makes the server set the is_direct flag on the m.room.member events sent to the users in invite and invite_3pid.
     * See Direct Messaging for more information.
     */
    @Json(name = "is_direct")
    var isDirect: Boolean? = null

    /**
     * The power level content to override in the default power level event
     */
    @Json(name = "power_level_content_override")
    var powerLevelContentOverride: PowerLevels? = null

    /**
     * Add the crypto algorithm to the room creation parameters.
     *
     * @param algorithm the algorithm
     */
    fun addCryptoAlgorithm(algorithm: String) {
        if (algorithm.isNotBlank()) {
            val contentMap = HashMap<String, String>()
            contentMap["algorithm"] = algorithm

            val algoEvent = Event(type = EventType.ENCRYPTION,
                    stateKey = "",
                    content = contentMap.toContent()
            )

            if (null == initialStates) {
                initialStates = Arrays.asList<Event>(algoEvent)
            } else {
                initialStates!!.add(algoEvent)
            }
        }
    }

    /**
     * Force the history visibility in the room creation parameters.
     *
     * @param historyVisibility the expected history visibility, set null to remove any existing value.
     */
    fun setHistoryVisibility(historyVisibility: RoomHistoryVisibility?) {
        // Remove the existing value if any.
        if (initialStates != null && !initialStates!!.isEmpty()) {
            val newInitialStates = ArrayList<Event>()
            for (event in initialStates!!) {
                if (event.getClearType() != EventType.STATE_HISTORY_VISIBILITY) {
                    newInitialStates.add(event)
                }
            }
            initialStates = newInitialStates
        }

        if (historyVisibility != null) {
            val contentMap = HashMap<String, RoomHistoryVisibility>()
            contentMap["history_visibility"] = historyVisibility

            val historyVisibilityEvent = Event(type = EventType.STATE_HISTORY_VISIBILITY,
                    stateKey = "",
                    content = contentMap.toContent())

            if (null == initialStates) {
                initialStates = Arrays.asList<Event>(historyVisibilityEvent)
            } else {
                initialStates!!.add(historyVisibilityEvent)
            }
        }
    }

    /**
     * Mark as a direct message room.
     */
    fun setDirectMessage() {
        preset = CreateRoomPreset.PRESET_TRUSTED_PRIVATE_CHAT
        isDirect = true
    }

    /**
     * @return the invite count
     */
    private fun getInviteCount(): Int {
        return invitedUserIds?.size ?: 0
    }

    /**
     * @return the pid invite count
     */
    private fun getInvite3PidCount(): Int {
        return invite3pids?.size ?: 0
    }

    /**
     * Tells if the created room can be a direct chat one.
     *
     * @return true if it is a direct chat
     */
    fun isDirect(): Boolean {
        return preset == CreateRoomPreset.PRESET_TRUSTED_PRIVATE_CHAT
                && isDirect == true
                && (1 == getInviteCount() || 1 == getInvite3PidCount())
    }

    /**
     * @return the first invited user id
     */
    fun getFirstInvitedUserId(): String? {
        if (0 != getInviteCount()) {
            return invitedUserIds!![0]
        }

        return if (0 != getInvite3PidCount()) {
            invite3pids!![0].address
        } else null
    }

    /**
     * Add some ids to the room creation
     * ids might be a matrix id or an email address.
     *
     * @param ids the participant ids to add.
     */
    fun addParticipantIds(hsConfig: HomeServerConnectionConfig,
                          credentials: Credentials,
                          ids: List<String>) {
        for (id in ids) {
            if (Patterns.EMAIL_ADDRESS.matcher(id).matches()) {
                if (null == invite3pids) {
                    invite3pids = ArrayList()
                }

                val pid = Invite3Pid(idServer = hsConfig.identityServerUri.host!!,
                        medium = ThreePidMedium.EMAIL,
                        address = id)

                invite3pids!!.add(pid)
            } else if (isUserId(id)) {
                // do not invite oneself
                if (credentials.userId != id) {
                    if (null == invitedUserIds) {
                        invitedUserIds = ArrayList()
                    }

                    invitedUserIds!!.add(id)
                }
            }
            // TODO add phonenumbers when it will be available
        }
    }
}