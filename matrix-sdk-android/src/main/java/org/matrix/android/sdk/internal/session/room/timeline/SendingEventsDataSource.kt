/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
