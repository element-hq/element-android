/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.timeline

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.internal.session.events.getFixedRoomMemberContent
import org.matrix.android.sdk.internal.session.room.state.StateEventDataSource

/**
 * Helper to observe and query the live room state.
 */
internal class LiveRoomStateListener(
        roomId: String,
        stateEventDataSource: StateEventDataSource,
        private val mainDispatcher: CoroutineDispatcher,
) {
    private val roomStateObserver = Observer<List<Event>> { stateEvents ->
        stateEvents.map { event ->
            val memberContent = event.getFixedRoomMemberContent() ?: return@map
            val stateKey = event.stateKey ?: return@map
            liveRoomState[stateKey] = memberContent
        }
    }
    private val stateEventsLiveData: LiveData<List<Event>> by lazy {
        stateEventDataSource.getStateEventsLive(
                roomId = roomId,
                eventTypes = setOf(EventType.STATE_ROOM_MEMBER),
                stateKey = QueryStringValue.IsNotNull,
        )
    }

    private val liveRoomState = mutableMapOf<String, RoomMemberContent>()

    suspend fun start() = withContext(mainDispatcher) {
        stateEventsLiveData.observeForever(roomStateObserver)
    }

    suspend fun stop() = withContext(mainDispatcher) {
        if (stateEventsLiveData.hasActiveObservers()) {
            stateEventsLiveData.removeObserver(roomStateObserver)
        }
    }

    fun getLiveState(stateKey: String): RoomMemberContent? = liveRoomState[stateKey]
}
