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
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.api.util.Optional

interface StateService {

    /**
     * Update the topic of the room
     */
    fun updateTopic(topic: String, callback: MatrixCallback<Unit>): Cancelable

    /**
     * Update the name of the room
     */
    fun updateName(name: String, callback: MatrixCallback<Unit>): Cancelable

    /**
     * Add new alias to the room.
     */
    fun addRoomAlias(roomAlias: String, callback: MatrixCallback<Unit>): Cancelable

    /**
     * Update the canonical alias of the room
     */
    fun updateCanonicalAlias(alias: String, callback: MatrixCallback<Unit>): Cancelable

    /**
     * Update the history readability of the room
     */
    fun updateHistoryReadability(readability: RoomHistoryVisibility, callback: MatrixCallback<Unit>): Cancelable

    /**
     * Update the avatar of the room
     */
    fun updateAvatar(avatarUri: Uri, fileName: String, callback: MatrixCallback<Unit>): Cancelable

    /**
     * Delete the avatar of the room
     */
    fun deleteAvatar(callback: MatrixCallback<Unit>): Cancelable

    fun sendStateEvent(eventType: String, stateKey: String?, body: JsonDict, callback: MatrixCallback<Unit>): Cancelable

    fun getStateEvent(eventType: String, stateKey: QueryStringValue = QueryStringValue.NoCondition): Event?

    fun getStateEventLive(eventType: String, stateKey: QueryStringValue = QueryStringValue.NoCondition): LiveData<Optional<Event>>

    fun getStateEvents(eventTypes: Set<String>, stateKey: QueryStringValue = QueryStringValue.NoCondition): List<Event>

    fun getStateEventsLive(eventTypes: Set<String>, stateKey: QueryStringValue = QueryStringValue.NoCondition): LiveData<List<Event>>
}
