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

import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmList
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.internal.database.mapper.TimelineEventMapper
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.query.where
import java.util.concurrent.atomic.AtomicReference

internal interface SendingEventsDataSource {
    fun start()
    fun stop()
    fun buildSendingEvents(): List<TimelineEvent>
}

internal class RealmSendingEventsDataSource(
        private val roomId: String,
        private val realm: AtomicReference<Realm>,
        private val uiEchoManager: UIEchoManager,
        private val timelineEventMapper: TimelineEventMapper,
        private val onEventsUpdated: (Boolean) -> Unit
) : SendingEventsDataSource {

    private var roomEntity: RoomEntity? = null
    private var sendingTimelineEvents: RealmList<TimelineEventEntity>? = null
    private var frozenSendingTimelineEvents: RealmList<TimelineEventEntity>? = null

    private val sendingTimelineEventsListener = RealmChangeListener<RealmList<TimelineEventEntity>> { events ->
        if (events.isValid) {
            uiEchoManager.onSentEventsInDatabase(events.map { it.eventId })
            updateFrozenResults(events)
            onEventsUpdated(false)
        }
    }

    override fun start() {
        val safeRealm = realm.get()
        roomEntity = RoomEntity.where(safeRealm, roomId = roomId).findFirst()
        sendingTimelineEvents = roomEntity?.sendingTimelineEvents
        sendingTimelineEvents?.addChangeListener(sendingTimelineEventsListener)
        updateFrozenResults(sendingTimelineEvents)
    }

    override fun stop() {
        sendingTimelineEvents?.removeChangeListener(sendingTimelineEventsListener)
        updateFrozenResults(null)
        sendingTimelineEvents = null
        roomEntity = null
    }

    private fun updateFrozenResults(sendingEvents: RealmList<TimelineEventEntity>?) {
        // Makes sure to close the previous frozen realm
        if (frozenSendingTimelineEvents?.isValid == true) {
            frozenSendingTimelineEvents?.realm?.close()
        }
        frozenSendingTimelineEvents = sendingEvents?.freeze()
    }

    override fun buildSendingEvents(): List<TimelineEvent> {
        val builtSendingEvents = mutableListOf<TimelineEvent>()
        uiEchoManager.getInMemorySendingEvents()
                .addWithUiEcho(builtSendingEvents)
        if (frozenSendingTimelineEvents?.isValid == true) {
            frozenSendingTimelineEvents
                    ?.filter { timelineEvent ->
                        builtSendingEvents.none { it.eventId == timelineEvent.eventId }
                    }
                    ?.map {
                        timelineEventMapper.map(it)
                    }?.addWithUiEcho(builtSendingEvents)
        }

        return builtSendingEvents
    }

    private fun List<TimelineEvent>.addWithUiEcho(target: MutableList<TimelineEvent>) {
        target.addAll(
                map { uiEchoManager.updateSentStateWithUiEcho(it) }
        )
    }
}
