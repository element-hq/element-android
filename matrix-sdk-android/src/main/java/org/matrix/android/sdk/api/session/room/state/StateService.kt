/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.state

import android.net.Uri
import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.query.QueryStateEventValue
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.model.GuestAccess
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import org.matrix.android.sdk.api.session.room.model.RoomJoinRulesAllowEntry
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.api.util.Optional

interface StateService {

    /**
     * Update the topic of the room.
     */
    suspend fun updateTopic(topic: String)

    /**
     * Update the name of the room.
     */
    suspend fun updateName(name: String)

    /**
     * Update the canonical alias of the room.
     * @param alias the canonical alias, or null to reset the canonical alias of this room
     * @param altAliases the alternative aliases for this room. It should include the canonical alias if any.
     */
    suspend fun updateCanonicalAlias(alias: String?, altAliases: List<String>)

    /**
     * Update the history readability of the room.
     */
    suspend fun updateHistoryReadability(readability: RoomHistoryVisibility)

    /**
     * Update the join rule and/or the guest access.
     */
    suspend fun updateJoinRule(joinRules: RoomJoinRules?, guestAccess: GuestAccess?, allowList: List<RoomJoinRulesAllowEntry>? = null)

    /**
     * Update the avatar of the room.
     */
    suspend fun updateAvatar(avatarUri: Uri, fileName: String)

    /**
     * Delete the avatar of the room.
     */
    suspend fun deleteAvatar()

    /**
     * Send a state event to the room.
     * @param eventType The type of event to send.
     * @param stateKey The state_key for the state to send. Can be an empty string.
     * @param body The content object of the event; the fields in this object will vary depending on the type of event
     * @return the id of the created state event
     */
    suspend fun sendStateEvent(eventType: String, stateKey: String, body: JsonDict): String

    /**
     * Get a state event of the room.
     * @param eventType An eventType.
     * @param stateKey the query which will be done on the stateKey
     */
    fun getStateEvent(eventType: String, stateKey: QueryStateEventValue): Event?

    /**
     * Get a live state event of the room.
     * @param eventType An eventType.
     * @param stateKey the query which will be done on the stateKey
     */
    fun getStateEventLive(eventType: String, stateKey: QueryStateEventValue): LiveData<Optional<Event>>

    /**
     * Get state events of the room.
     * @param eventTypes Set of eventType. If empty, all state events will be returned
     * @param stateKey the query which will be done on the stateKey
     */
    fun getStateEvents(eventTypes: Set<String>, stateKey: QueryStateEventValue): List<Event>

    /**
     * Get live state events of the room.
     * @param eventTypes Set of eventType to observe. If empty, all state events will be observed
     * @param stateKey the query which will be done on the stateKey
     */
    fun getStateEventsLive(eventTypes: Set<String>, stateKey: QueryStateEventValue): LiveData<List<Event>>

    suspend fun setJoinRulePublic()
    suspend fun setJoinRuleInviteOnly()
    suspend fun setJoinRuleRestricted(allowList: List<String>)
}
