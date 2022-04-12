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

import io.realm.OrderedCollectionChangeSet
import io.realm.OrderedRealmCollectionChangeListener
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmResults
import io.realm.kotlin.createObject
import kotlinx.coroutines.CompletableDeferred
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.api.settings.LightweightSettingsStorage
import org.matrix.android.sdk.internal.database.helper.addIfNecessary
import org.matrix.android.sdk.internal.database.mapper.TimelineEventMapper
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.ChunkEntityFields
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.model.deleteAndClearThreadEvents
import org.matrix.android.sdk.internal.database.query.findAllIncludingEvents
import org.matrix.android.sdk.internal.database.query.findLastForwardChunkOfThread
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.session.room.relation.threads.FetchThreadTimelineTask
import org.matrix.android.sdk.internal.session.sync.handler.room.ThreadsAwarenessHandler
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference

/**
 * This class is responsible for keeping an instance of chunkEntity and timelineChunk according to the strategy.
 * There is 2 different mode: Live and Permalink.
 * In Live, we will query for the live chunk (isLastForward = true).
 * In Permalink, we will query for the chunk including the eventId we are looking for.
 * Once we got a ChunkEntity we wrap it with TimelineChunk class so we dispatch any methods for loading data.
 */

internal class LoadTimelineStrategy(
        private val roomId: String,
        private val timelineId: String,
        private val mode: Mode,
        private val dependencies: Dependencies) {

    sealed interface Mode {
        object Live : Mode
        data class Permalink(val originEventId: String) : Mode
        data class Thread(val rootThreadEventId: String) : Mode

        fun originEventId(): String? {
            return if (this is Permalink) {
                originEventId
            } else {
                null
            }
        }

//        fun getRootThreadEventId(): String? {
//            return if (this is Thread) {
//                rootThreadEventId
//            } else {
//                null
//            }
//        }
    }

    data class Dependencies(
            val timelineSettings: TimelineSettings,
            val realm: AtomicReference<Realm>,
            val eventDecryptor: TimelineEventDecryptor,
            val paginationTask: PaginationTask,
            val realmConfiguration: RealmConfiguration,
            val fetchThreadTimelineTask: FetchThreadTimelineTask,
            val fetchTokenAndPaginateTask: FetchTokenAndPaginateTask,
            val getContextOfEventTask: GetContextOfEventTask,
            val timelineInput: TimelineInput,
            val timelineEventMapper: TimelineEventMapper,
            val threadsAwarenessHandler: ThreadsAwarenessHandler,
            val lightweightSettingsStorage: LightweightSettingsStorage,
            val onEventsUpdated: (Boolean) -> Unit,
            val onLimitedTimeline: () -> Unit,
            val onNewTimelineEvents: (List<String>) -> Unit
    )

    private var getContextLatch: CompletableDeferred<Unit>? = null
    private var chunkEntity: RealmResults<ChunkEntity>? = null
    private var timelineChunk: TimelineChunk? = null
    private val chunkEntityListener = OrderedRealmCollectionChangeListener { _: RealmResults<ChunkEntity>, changeSet: OrderedCollectionChangeSet ->
        // Can be call either when you open a permalink on an unknown event
        // or when there is a gap in the timeline.
        val shouldRebuildChunk = changeSet.insertions.isNotEmpty()
        if (shouldRebuildChunk) {
            timelineChunk?.close(closeNext = true, closePrev = true)
            timelineChunk = chunkEntity?.createTimelineChunk()
            // If we are waiting for a result of get context, post completion
            getContextLatch?.complete(Unit)
            // If we have a gap, just tell the timeline about it.
            if (timelineChunk?.hasReachedLastForward().orFalse()) {
                dependencies.onLimitedTimeline()
            }
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
            if (uiEchoManager.onLocalEchoCreated(timelineEvent)) {
                dependencies.onNewTimelineEvents(listOf(timelineEvent.eventId))
                dependencies.onEventsUpdated(false)
            }
        }

        override fun onLocalEchoUpdated(roomId: String, eventId: String, sendState: SendState) {
            if (roomId != this@LoadTimelineStrategy.roomId) {
                return
            }
            if (uiEchoManager.onSendStateUpdated(eventId, sendState)) {
                dependencies.onEventsUpdated(false)
            }
        }

        override fun onNewTimelineEvents(roomId: String, eventIds: List<String>) {
            if (roomId == this@LoadTimelineStrategy.roomId && hasReachedLastForward()) {
                dependencies.onNewTimelineEvents(eventIds)
            }
        }
    }

    private val uiEchoManager = UIEchoManager(uiEchoManagerListener)
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
        getContextLatch?.cancel()
        chunkEntity = null
        timelineChunk = null
        if (mode is Mode.Thread) {
            clearThreadChunkEntity(dependencies.realm.get(), mode.rootThreadEventId)
        }
    }

    suspend fun loadMore(count: Int, direction: Timeline.Direction, fetchOnServerIfNeeded: Boolean = true): LoadMoreResult {
        if (mode is Mode.Permalink && timelineChunk == null) {
            val params = GetContextOfEventTask.Params(roomId, mode.originEventId)
            try {
                getContextLatch = CompletableDeferred()
                dependencies.getContextOfEventTask.execute(params)
                // waits for the query to be fulfilled
                getContextLatch?.await()
                getContextLatch = null
            } catch (failure: Throwable) {
                if (failure is Failure.ServerError && failure.error.code in listOf(MatrixError.M_NOT_FOUND, MatrixError.M_FORBIDDEN)) {
                    // This failure is likely permanent, so handle in DefaultTimeline to restart without eventId
                    throw failure
                }
                return LoadMoreResult.FAILURE
            }
        }
        if (mode is Mode.Thread) {
            return timelineChunk?.loadMoreThread(count) ?: LoadMoreResult.FAILURE
        }
        return timelineChunk?.loadMore(count, direction, fetchOnServerIfNeeded) ?: LoadMoreResult.FAILURE
    }

    fun getBuiltEventIndex(eventId: String): Int? {
        return timelineChunk?.getBuiltEventIndex(eventId, searchInNext = true, searchInPrev = true)
    }

    fun getBuiltEvent(eventId: String): TimelineEvent? {
        return timelineChunk?.getBuiltEvent(eventId, searchInNext = true, searchInPrev = true)
    }

    fun buildSnapshot(): List<TimelineEvent> {
        return buildSendingEvents() + timelineChunk?.builtItems(includesNext = true, includesPrev = true).orEmpty()
    }

    private fun buildSendingEvents(): List<TimelineEvent> {
        return if (hasReachedLastForward() || mode is Mode.Thread) {
            sendingEventsDataSource.buildSendingEvents()
        } else {
            emptyList()
        }
    }

    private fun getChunkEntity(realm: Realm): RealmResults<ChunkEntity> {
        return when (mode) {
            is Mode.Live      -> {
                ChunkEntity.where(realm, roomId)
                        .equalTo(ChunkEntityFields.IS_LAST_FORWARD, true)
                        .findAll()
            }
            is Mode.Permalink -> {
                ChunkEntity.findAllIncludingEvents(realm, listOf(mode.originEventId))
            }
            is Mode.Thread    -> {
                recreateThreadChunkEntity(realm, mode.rootThreadEventId)
                ChunkEntity.where(realm, roomId)
                        .equalTo(ChunkEntityFields.ROOT_THREAD_EVENT_ID, mode.rootThreadEventId)
                        .equalTo(ChunkEntityFields.IS_LAST_FORWARD_THREAD, true)
                        .findAll()
            }
        }
    }

    /**
     * Clear any existing thread chunk entity and create a new one, with the
     * rootThreadEventId included
     */
    private fun recreateThreadChunkEntity(realm: Realm, rootThreadEventId: String) {
        realm.executeTransaction {
            // Lets delete the chunk and start a new one
            ChunkEntity.findLastForwardChunkOfThread(it, roomId, rootThreadEventId)?.deleteAndClearThreadEvents()?.let {
                Timber.i("###THREADS LoadTimelineStrategy [onStart] thread chunk cleared..")
            }
            val threadChunk = it.createObject<ChunkEntity>().apply {
                Timber.i("###THREADS LoadTimelineStrategy [onStart] Created new thread chunk with rootThreadEventId: $rootThreadEventId")
                this.rootThreadEventId = rootThreadEventId
                this.isLastForwardThread = true
            }
            if (threadChunk.isValid) {
                RoomEntity.where(it, roomId).findFirst()?.addIfNecessary(threadChunk)
            }
        }
    }

    /**
     * Clear any existing thread chunk
     */
    private fun clearThreadChunkEntity(realm: Realm, rootThreadEventId: String) {
        realm.executeTransaction {
            ChunkEntity.findLastForwardChunkOfThread(it, roomId, rootThreadEventId)?.deleteAndClearThreadEvents()?.let {
                Timber.i("###THREADS LoadTimelineStrategy [onStop] thread chunk cleared..")
            }
        }
    }

    private fun hasReachedLastForward(): Boolean {
        return timelineChunk?.hasReachedLastForward().orFalse()
    }

    private fun RealmResults<ChunkEntity>.createTimelineChunk(): TimelineChunk? {
        return firstOrNull()?.let {
            return TimelineChunk(
                    chunkEntity = it,
                    timelineSettings = dependencies.timelineSettings,
                    roomId = roomId,
                    timelineId = timelineId,
                    fetchThreadTimelineTask = dependencies.fetchThreadTimelineTask,
                    eventDecryptor = dependencies.eventDecryptor,
                    paginationTask = dependencies.paginationTask,
                    realmConfiguration = dependencies.realmConfiguration,
                    fetchTokenAndPaginateTask = dependencies.fetchTokenAndPaginateTask,
                    timelineEventMapper = dependencies.timelineEventMapper,
                    uiEchoManager = uiEchoManager,
                    threadsAwarenessHandler = dependencies.threadsAwarenessHandler,
                    lightweightSettingsStorage = dependencies.lightweightSettingsStorage,
                    initialEventId = mode.originEventId(),
                    onBuiltEvents = dependencies.onEventsUpdated
            )
        }
    }
}
