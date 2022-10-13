/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.mapper.TimelineEventMapper
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.query.where

internal interface SendingEventsDataSource {
    fun buildSendingEvents(): List<TimelineEvent>
}

internal class RealmSendingEventsDataSource(
        private val roomId: String,
        private val realmInstance: RealmInstance,
        private val timelineScope: CoroutineScope,
        private val uiEchoManager: UIEchoManager,
        private val timelineEventMapper: TimelineEventMapper,
        private val onEventsUpdated: (Boolean) -> Unit
) : SendingEventsDataSource {

    private var sendingTimelineEvents: List<TimelineEventEntity>? = null

    init {
        start()
    }

    private fun start() {
        realmInstance.getRealmFlow().flatMapConcat { realm ->
            RoomEntity.where(realm, roomId = roomId).first().asFlow()
        }.onEach { change ->
            val events = change.obj?.sendingTimelineEvents.orEmpty()
            uiEchoManager.onSentEventsInDatabase(events.map { it.eventId })
            sendingTimelineEvents = events
            onEventsUpdated(false)
        }.launchIn(timelineScope)
    }

    override fun buildSendingEvents(): List<TimelineEvent> {
        val builtSendingEvents = mutableListOf<TimelineEvent>()
        uiEchoManager.getInMemorySendingEvents()
                .addWithUiEcho(builtSendingEvents)

        sendingTimelineEvents
                ?.filter { timelineEvent ->
                    builtSendingEvents.none { it.eventId == timelineEvent.eventId }
                }
                ?.map {
                    timelineEventMapper.map(it)
                }?.addWithUiEcho(builtSendingEvents)

        return builtSendingEvents
    }

    private fun List<TimelineEvent>.addWithUiEcho(target: MutableList<TimelineEvent>) {
        target.addAll(
                map { uiEchoManager.updateSentStateWithUiEcho(it) }
        )
    }
}
