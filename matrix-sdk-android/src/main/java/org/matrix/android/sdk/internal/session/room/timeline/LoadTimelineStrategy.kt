/*
 * Copyright (c) 2021 New Vector Ltd
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

import io.realm.OrderedCollectionChangeSet
import io.realm.OrderedRealmCollectionChangeListener
import io.realm.Realm
import io.realm.RealmResults
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.internal.database.mapper.TimelineEventMapper
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.ChunkEntityFields
import org.matrix.android.sdk.internal.database.query.findAllIncludingEvents
import org.matrix.android.sdk.internal.database.query.where
import java.util.concurrent.atomic.AtomicReference

internal class LoadTimelineStrategy(
        private val roomId: String,
        private val timelineId: String,
        private val mode: Mode,
        private val dependencies: Dependencies) {

    sealed class Mode {
        object Default : Mode()
        data class Permalink(val originEventId: String) : Mode()

        fun originEventId(): String? {
            return if (this is Permalink) {
                originEventId
            } else {
                null
            }
        }
    }

    data class Dependencies(
            val realm: AtomicReference<Realm>,
            val eventDecryptor: TimelineEventDecryptor,
            val paginationTask: PaginationTask,
            val fetchTokenAndPaginateTask: FetchTokenAndPaginateTask,
            val getContextOfEventTask: GetContextOfEventTask,
            val timelineInput: TimelineInput,
            val timelineEventMapper: TimelineEventMapper,
            val onEventsUpdated: () -> Unit,
            val onNewTimelineEvents: (List<String>) -> Unit
    )

    private var chunkEntity: RealmResults<ChunkEntity>? = null
    private var timelineChunk: TimelineChunk? = null

    private val chunkEntityListener = OrderedRealmCollectionChangeListener { _: RealmResults<ChunkEntity>, changeSet: OrderedCollectionChangeSet ->
        val shouldRebuildChunk = changeSet.insertions.isNotEmpty()
        if (shouldRebuildChunk) {
            timelineChunk?.close(closeNext = true, closePrev = true)
            timelineChunk = chunkEntity?.createTimelineChunk()
            dependencies.onEventsUpdated()
        }
    }

    private val uiEchoManagerListener = object : UIEchoManager.Listener {
        override fun rebuildEvent(eventId: String, builder: (TimelineEvent) -> TimelineEvent?): Boolean {
            return timelineChunk?.rebuildEvent(eventId, builder, searchInNext = true, searchInPrev = true).orFalse()
        }
    }

    private val timelineInputListener = object : TimelineInput.Listener {
        override fun onLocalEchoCreated(roomId: String, timelineEvent: TimelineEvent) {
            if (roomId != this@LoadTimelineStrategy.roomId) {
                return
            }
            dependencies.onNewTimelineEvents(listOf(timelineEvent.eventId))
            if (uiEchoManager.onLocalEchoCreated(timelineEvent)) {
                dependencies.onEventsUpdated()
            }
        }

        override fun onLocalEchoUpdated(roomId: String, eventId: String, sendState: SendState) {
            if (roomId != this@LoadTimelineStrategy.roomId) {
                return
            }
            if (uiEchoManager.onSendStateUpdated(eventId, sendState)) {
                dependencies.onEventsUpdated()
            }
        }

        override fun onNewTimelineEvents(roomId: String, eventIds: List<String>) {
            if (roomId == this@LoadTimelineStrategy.roomId) {
                dependencies.onNewTimelineEvents(eventIds)
            }
        }
    }

    private val uiEchoManager = UIEchoManager(TimelineSettings(10), uiEchoManagerListener)
    private val sendingEventsDataSource: SendingEventsDataSource = RealmSendingEventsDataSource(
            roomId = roomId,
            realm = dependencies.realm,
            uiEchoManager = uiEchoManager,
            timelineEventMapper = dependencies.timelineEventMapper,
            onEventsUpdated = dependencies.onEventsUpdated
    )

    fun onStart() {
        dependencies.eventDecryptor.start()
        dependencies.timelineInput.listeners.add(timelineInputListener)
        val realm = dependencies.realm.get()
        sendingEventsDataSource.start()
        chunkEntity = getChunkEntity(realm).also {
            it.addChangeListener(chunkEntityListener)
            timelineChunk = it.createTimelineChunk()
        }
    }

    fun onStop() {
        dependencies.eventDecryptor.destroy()
        dependencies.timelineInput.listeners.remove(timelineInputListener)
        chunkEntity?.removeChangeListener(chunkEntityListener)
        sendingEventsDataSource.stop()
        timelineChunk?.close(closeNext = true, closePrev = true)
        chunkEntity = null
        timelineChunk = null
    }

    suspend fun loadMore(count: Long, direction: SimpleTimeline.Direction): LoadMoreResult {
        return if (mode is Mode.Permalink && timelineChunk == null) {
            val params = GetContextOfEventTask.Params(roomId, mode.originEventId)
            try {
                dependencies.getContextOfEventTask.execute(params)
                LoadMoreResult.SUCCESS
            } catch (failure: Throwable) {
                LoadMoreResult.FAILURE
            }
        } else {
            timelineChunk?.loadMore(count, direction) ?: LoadMoreResult.FAILURE
        }
    }

    fun getBuiltEventIndex(eventId: String): Int? {
        return timelineChunk?.getBuiltEventIndex(eventId, searchInNext = true, searchInPrev = true)
    }

    fun buildSnapshot(): List<TimelineEvent> {
        return buildSendingEvents() + timelineChunk?.builtItems(includesNext = true, includesPrev = true).orEmpty()
    }

    private fun buildSendingEvents(): List<TimelineEvent> {
        return if (timelineChunk?.hasReachedLastForward().orFalse()) {
            sendingEventsDataSource.buildSendingEvents()
        } else {
            emptyList()
        }
    }

    private fun getChunkEntity(realm: Realm): RealmResults<ChunkEntity> {
        return if (mode is Mode.Permalink) {
            ChunkEntity.findAllIncludingEvents(realm, listOf(mode.originEventId))
        } else {
            ChunkEntity.where(realm, roomId)
                    .equalTo(ChunkEntityFields.IS_LAST_FORWARD, true)
                    .findAll()
        }
    }

    private fun RealmResults<ChunkEntity>.createTimelineChunk(): TimelineChunk? {
        return firstOrNull()?.let {
            return TimelineChunk(
                    chunkEntity = it,
                    roomId = roomId,
                    timelineId = timelineId,
                    eventDecryptor = dependencies.eventDecryptor,
                    paginationTask = dependencies.paginationTask,
                    fetchTokenAndPaginateTask = dependencies.fetchTokenAndPaginateTask,
                    timelineEventMapper = dependencies.timelineEventMapper,
                    uiEchoManager = uiEchoManager,
                    initialEventId = mode.originEventId(),
                    onBuiltEvents = dependencies.onEventsUpdated
            )
        }
    }
}


