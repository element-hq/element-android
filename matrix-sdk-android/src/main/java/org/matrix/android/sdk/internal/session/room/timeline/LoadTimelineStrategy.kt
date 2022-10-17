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

import io.realm.kotlin.TypedRealm
import io.realm.kotlin.ext.isValid
import io.realm.kotlin.notifications.InitialResults
import io.realm.kotlin.notifications.ResultsChange
import io.realm.kotlin.notifications.UpdatedResults
import io.realm.kotlin.query.RealmResults
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.api.settings.LightweightSettingsStorage
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.helper.addIfNecessary
import org.matrix.android.sdk.internal.database.mapper.TimelineEventMapper
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.model.deleteAndClearThreadEvents
import org.matrix.android.sdk.internal.database.query.findAllIncludingEvents
import org.matrix.android.sdk.internal.database.query.findLastForwardChunkOfThread
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.session.room.relation.threads.FetchThreadTimelineTask
import org.matrix.android.sdk.internal.session.room.state.StateEventDataSource
import org.matrix.android.sdk.internal.session.sync.handler.room.ThreadsAwarenessHandler
import org.matrix.android.sdk.internal.util.time.Clock
import timber.log.Timber

/**
 * This class is responsible for keeping an instance of chunkEntity and timelineChunk according to the strategy.
 * There is 2 different mode: Live and Permalink.
 * In Live, we will query for the live chunk (isLastForward = true).
 * In Permalink, we will query for the chunk including the eventId we are looking for.
 * Once we got a ChunkEntity we wrap it with TimelineChunk class so we dispatch any methods for loading data.
 */

internal class LoadTimelineStrategy constructor(
        private val roomId: String,
        private val timelineId: String,
        private val mode: Mode,
        private val dependencies: Dependencies,
        clock: Clock,
) {

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
            val timelineScope: CoroutineScope,
            val timelineSettings: TimelineSettings,
            val realmInstance: RealmInstance,
            val eventDecryptor: TimelineEventDecryptor,
            val paginationTask: PaginationTask,
            val fetchThreadTimelineTask: FetchThreadTimelineTask,
            val fetchTokenAndPaginateTask: FetchTokenAndPaginateTask,
            val getContextOfEventTask: GetContextOfEventTask,
            val timelineInput: TimelineInput,
            val timelineEventMapper: TimelineEventMapper,
            val threadsAwarenessHandler: ThreadsAwarenessHandler,
            val lightweightSettingsStorage: LightweightSettingsStorage,
            val onEventsUpdated: (Boolean) -> Unit,
            val onEventsDeleted: () -> Unit,
            val onLimitedTimeline: () -> Unit,
            val onNewTimelineEvents: (List<String>) -> Unit,
            val stateEventDataSource: StateEventDataSource,
            val matrixCoroutineDispatchers: MatrixCoroutineDispatchers,
    )

    private var getContextLatch: CompletableDeferred<Unit>? = null
    private var chunkEntity: RealmResults<ChunkEntity>? = null
    private var timelineChunk: TimelineChunk? = null

    private suspend fun onChunkResultsChanged(resultsChange: ResultsChange<ChunkEntity>) {

        suspend fun onUpdates(updatedResults: UpdatedResults<ChunkEntity>) {
            val shouldRebuildChunk = updatedResults.insertions.isNotEmpty()
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

        when (resultsChange) {
            is InitialResults -> Unit
            is UpdatedResults -> onUpdates(resultsChange)
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

    private val uiEchoManager = UIEchoManager(uiEchoManagerListener, clock)
    private val sendingEventsDataSource: SendingEventsDataSource = RealmSendingEventsDataSource(
            roomId = roomId,
            timelineScope = dependencies.timelineScope,
            realmInstance = dependencies.realmInstance,
            uiEchoManager = uiEchoManager,
            timelineEventMapper = dependencies.timelineEventMapper,
            onEventsUpdated = dependencies.onEventsUpdated
    )

    private val liveRoomStateListener = LiveRoomStateListener(
            roomId,
            dependencies.stateEventDataSource,
            dependencies.matrixCoroutineDispatchers.main
    )

    suspend fun onStart() {
        dependencies.eventDecryptor.start()
        dependencies.timelineInput.listeners.add(timelineInputListener)
        val realm = dependencies.realmInstance.getRealm()
        chunkEntity = getChunkEntity(realm).also {
            it.asFlow().onEach(this::onChunkResultsChanged).launchIn(dependencies.timelineScope)
            timelineChunk = it.createTimelineChunk()
        }
        if (dependencies.timelineSettings.useLiveSenderInfo) {
            liveRoomStateListener.start()
        }
    }

    suspend fun onStop() {
        dependencies.eventDecryptor.destroy()
        dependencies.timelineInput.listeners.remove(timelineInputListener)
        timelineChunk?.close(closeNext = true, closePrev = true)
        getContextLatch?.cancel()
        chunkEntity = null
        timelineChunk = null
        if (mode is Mode.Thread) {
            clearThreadChunkEntity(mode.rootThreadEventId)
        }
        if (dependencies.timelineSettings.useLiveSenderInfo) {
            liveRoomStateListener.stop()
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
        val events = buildSendingEvents() + timelineChunk?.builtItems(includesNext = true, includesPrev = true).orEmpty()
        return if (dependencies.timelineSettings.useLiveSenderInfo) {
            events.map(this::applyLiveRoomState)
        } else {
            events
        }
    }

    private fun applyLiveRoomState(event: TimelineEvent): TimelineEvent {
        val updatedState = liveRoomStateListener.getLiveState(event.senderInfo.userId)
        return if (updatedState != null) {
            val updatedSenderInfo = event.senderInfo.copy(avatarUrl = updatedState.avatarUrl, displayName = updatedState.displayName)
            event.copy(senderInfo = updatedSenderInfo)
        } else {
            event
        }
    }

    private fun buildSendingEvents(): List<TimelineEvent> {
        return if (hasReachedLastForward() || mode is Mode.Thread) {
            sendingEventsDataSource.buildSendingEvents()
        } else {
            emptyList()
        }
    }

    private suspend fun getChunkEntity(realm: TypedRealm): RealmResults<ChunkEntity> {
        return when (mode) {
            is Mode.Live -> {
                ChunkEntity.where(realm, roomId)
                        .query("isLastForward == true")
                        .find()
            }
            is Mode.Permalink -> {
                ChunkEntity.findAllIncludingEvents(realm, listOf(mode.originEventId))
            }
            is Mode.Thread -> {
                recreateThreadChunkEntity(mode.rootThreadEventId)
                ChunkEntity.where(realm, roomId)
                        .query("rootThreadEventId == $0", mode.rootThreadEventId)
                        .query("isLastForwardThread == true")
                        .find()
            }
        }
    }

    /**
     * Clear any existing thread chunk entity and create a new one, with the
     * rootThreadEventId included.
     */
    private suspend fun recreateThreadChunkEntity(rootThreadEventId: String) {
        val roomId = this.roomId
        dependencies.realmInstance.write {
            // Lets delete the chunk and start a new one
            ChunkEntity.findLastForwardChunkOfThread(this, roomId, rootThreadEventId)?.let {
                deleteAndClearThreadEvents(it)
                Timber.i("###THREADS LoadTimelineStrategy [onStart] thread chunk cleared..")
            }
            val threadChunk = copyToRealm(ChunkEntity().apply {
                Timber.i("###THREADS LoadTimelineStrategy [onStart] Created new thread chunk with rootThreadEventId: $rootThreadEventId")
                this.roomId = roomId
                this.rootThreadEventId = rootThreadEventId
                this.isLastForwardThread = true
            }
            )
            if (threadChunk.isValid()) {
                RoomEntity.where(this, roomId).first().find()?.addIfNecessary(threadChunk)
            }
        }
    }

    /**
     * Clear any existing thread chunk.
     */
    private suspend fun clearThreadChunkEntity(rootThreadEventId: String) {
        dependencies.realmInstance.write {
            ChunkEntity.findLastForwardChunkOfThread(this, roomId, rootThreadEventId)?.let {
                deleteAndClearThreadEvents(it)
                Timber.i("###THREADS LoadTimelineStrategy [onStop] thread chunk cleared..")
            }
        }
    }

    private fun hasReachedLastForward(): Boolean {
        return timelineChunk?.hasReachedLastForward().orFalse()
    }

    private suspend fun RealmResults<ChunkEntity>.createTimelineChunk(): TimelineChunk? {
        val realm = dependencies.realmInstance.getRealm()
        return firstOrNull()?.let {
            return TimelineChunk(
                    timelineScope = dependencies.timelineScope,
                    chunkEntity = it,
                    timelineSettings = dependencies.timelineSettings,
                    roomId = roomId,
                    timelineId = timelineId,
                    fetchThreadTimelineTask = dependencies.fetchThreadTimelineTask,
                    eventDecryptor = dependencies.eventDecryptor,
                    paginationTask = dependencies.paginationTask,
                    realm = realm,
                    fetchTokenAndPaginateTask = dependencies.fetchTokenAndPaginateTask,
                    timelineEventMapper = dependencies.timelineEventMapper,
                    uiEchoManager = uiEchoManager,
                    threadsAwarenessHandler = dependencies.threadsAwarenessHandler,
                    lightweightSettingsStorage = dependencies.lightweightSettingsStorage,
                    initialEventId = mode.originEventId(),
                    onBuiltEvents = dependencies.onEventsUpdated,
                    onEventsDeleted = dependencies.onEventsDeleted,
            )
        }
    }
}
