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

package org.matrix.android.sdk.api.session.room.state

import android.net.Uri
import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.model.GuestAccess
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.api.util.Optional

interface StateService {

    /**
     * Update the topic of the room
     */
    suspend fun updateTopic(topic: String)

    /**
     * Update the name of the room
     */
    suspend fun updateName(name: String)

    /**
     * Update the canonical alias of the room
     * @param alias the canonical alias, or null to reset the canonical alias of this room
     * @param altAliases the alternative aliases for this room. It should include the canonical alias if any.
     */
    suspend fun updateCanonicalAlias(alias: String?, altAliases: List<String>)

    /**
     * Update the history readability of the room
     */
    suspend fun updateHistoryReadability(readability: RoomHistoryVisibility)

    /**
     * Update the join rule and/or the guest access
     */
    suspend fun updateJoinRule(joinRules: RoomJoinRules?, guestAccess: GuestAccess?)

    /**
     * Update the avatar of the room
     */
    suspend fun updateAvatar(avatarUri: Uri, fileName: String)

    /**
     * Delete the avatar of the room
     */
    suspend fun deleteAvatar()

    suspend fun sendStateEvent(eventType: String, stateKey: String?, body: JsonDict)

    fun getStateEvent(eventType: String, stateKey: QueryStringValue = QueryStringValue.NoCondition): Event?

    fun getStateEventLive(eventType: String, stateKey: QueryStringValue = QueryStringValue.NoCondition): LiveData<Optional<Event>>

    fun getStateEvents(eventTypes: Set<String>, stateKey: QueryStringValue = QueryStringValue.NoCondition): List<Event>

    fun getStateEventsLive(eventTypes: Set<String>, stateKey: QueryStringValue = QueryStringValue.NoCondition): LiveData<List<Event>>
}
