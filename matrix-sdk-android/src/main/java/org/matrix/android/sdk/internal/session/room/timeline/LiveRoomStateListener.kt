/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
