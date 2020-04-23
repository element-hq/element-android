/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session.room.timeline

import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.db.SqlDriver
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.session.room.timeline.TimelineSettings
import im.vector.matrix.android.internal.database.awaitTransaction
import im.vector.matrix.android.internal.database.mapper.ReadReceiptMapper
import im.vector.matrix.android.internal.database.mapper.TimelineEventMapper
import im.vector.matrix.android.internal.session.room.relation.EventAnnotationsSummaryDataSource
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.util.Debouncer
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.android.internal.util.createBackgroundHandler
import im.vector.matrix.android.internal.util.createUIHandler
import im.vector.matrix.sqldelight.session.ChunkEntity
import im.vector.matrix.sqldelight.session.SessionDatabase
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import java.lang.Runnable
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal class SQLTimeline @AssistedInject constructor(
        @Assisted private val roomId: String,
        @Assisted private var initialEventId: String? = null,
        @Assisted private val settings: TimelineSettings,
        private val taskExecutor: TaskExecutor,
        private val sqlDriver: SqlDriver,
        private val sessionDatabase: SessionDatabase,
        private val contextOfEventTask: GetContextOfEventTask,
        private val paginationTask: PaginationTask,
        private val timelineEventMapper: TimelineEventMapper,
        private val eventBus: EventBus,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val eventAnnotationsSummaryDataSource: EventAnnotationsSummaryDataSource,
        private val readReceiptMapper: ReadReceiptMapper,
        private val eventDecryptor: TimelineEventDecryptor
) : Timeline {

    companion object {
        val TIMELINE_DISPATCHER = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    }

    @AssistedInject.Factory
    interface Factory {
        fun create(roomId: String, initialEventId: String?, settings: TimelineSettings): SQLTimeline
    }

    data class OnNewTimelineEvents(val roomId: String, val eventIds: List<String>)
    data class OnLocalEchoCreated(val roomId: String, val timelineEvent: TimelineEvent)


    private val timelineScope = CoroutineScope(taskExecutor.executorScope.coroutineContext + TIMELINE_DISPATCHER)

    private var currentChunkId: Long = 0
    private var localEchoChunkId: Long = 0
    private var prevDisplayIndex: Int? = null
    private var nextDisplayIndex: Int? = null
    private val isStarted = AtomicBoolean(false)
    private val isReady = AtomicBoolean(false)
    private val builtEvents = Collections.synchronizedList<TimelineEvent>(ArrayList())
    private val builtEventsIdMap = Collections.synchronizedMap(HashMap<String, Int>())
    private val backwardsState = AtomicReference(State())
    private val forwardsState = AtomicReference(State())
    private val uiHandler = createUIHandler()
    private val backgroundHandler = createBackgroundHandler("timeline_handler")
    private val uiDebouncer = Debouncer(uiHandler)
    private val bgDebouncer = Debouncer(backgroundHandler)
    private val listeners = CopyOnWriteArrayList<Timeline.Listener>()

    private var timelineInsertQuery: Query<String>? = null
    private var timelineUpdateQuery: Query<String>? = null
    private var liveChunkIdQuery: Query<Long>? = null

    private val timelineInsertListener = object : Query.Listener {
        override fun queryResultsChanged() {
            bgDebouncer.debounce("timeline_insert", Runnable {
                timelineScope.launch {
                    handleNewLiveEvents()
                }
            }, 10)
        }
    }
    private val timelineUpdateListener = object : Query.Listener {
        override fun queryResultsChanged() {
            bgDebouncer.debounce("timeline_update", Runnable {
                timelineScope.launch {
                    handleTimelineEventUpdates()
                }
            }, 10)
        }
    }

    private val liveChunkListener = object : Query.Listener {
        override fun queryResultsChanged() {
            timelineScope.launch {
                handleNewLiveChunkId()
            }
        }
    }

    private suspend fun handleNewLiveChunkId() {
        if (!isLive) {
            return
        }
        val chunkId = liveChunkIdQuery?.executeAsOne() ?: return
        if (currentChunkId != chunkId) {
            timelineInsertQuery?.removeListener(timelineInsertListener)
            timelineUpdateQuery?.removeListener(timelineUpdateListener)
            clearAllValues()
            initialLoadFromLiveChunk(chunkId)
            timelineInsertQuery = sessionDatabase.timelineEventQueries.getTimelineEventInsert(roomId).apply {
                addListener(timelineInsertListener)
            }
            timelineUpdateQuery = sessionDatabase.timelineEventQueries.getTimelineEventUpdates(roomId).apply {
                addListener(timelineUpdateListener)
            }
        }
    }

    private suspend fun handleTimelineEventUpdates() {
        val timelineUpdateIds = timelineUpdateQuery?.executeAsList()
        if (timelineUpdateIds.isNullOrEmpty()) {
            return
        }
        timelineUpdateIds.forEach { eventId ->
            rebuildEvent(eventId) { timelineEvent ->
                val updatedTimelineEvent = sessionDatabase.timelineEventQueries.get(eventId, timelineEventMapper::map).executeAsOneOrNull()
                if (updatedTimelineEvent == null) {
                    timelineEvent
                } else {
                    val readReceipts = sessionDatabase.readReceiptQueries.getAllForEvent(eventId, readReceiptMapper::map).executeAsList()
                    val annotationsSummary = eventAnnotationsSummaryDataSource.getEventAnnotationsSummary(eventId)
                    timelineEvent.copy(
                            root = updatedTimelineEvent.root,
                            isUniqueDisplayName = updatedTimelineEvent.isUniqueDisplayName,
                            senderAvatar = updatedTimelineEvent.senderAvatar,
                            senderName = updatedTimelineEvent.senderName,
                            readReceipts = readReceipts,
                            annotations = annotationsSummary
                    )
                }
            }
        }
        postSnapshot()
        withContext(Dispatchers.IO) {
            sessionDatabase.timelineEventQueries.deleteTimelineEventUpdates(timelineUpdateIds)
        }
    }

    override val timelineID: String
        get() = UUID.randomUUID().toString()

    override val isLive: Boolean
        get() = !hasMoreToLoad(Timeline.Direction.FORWARDS)

    override fun addListener(listener: Timeline.Listener): Boolean {
        if (listeners.contains(listener)) {
            return false
        }
        return listeners.add(listener).also {
            postSnapshot()
        }
    }

    override fun removeListener(listener: Timeline.Listener): Boolean {
        return listeners.remove(listener)
    }

    override fun removeAllListeners() {
        listeners.clear()
    }

    override fun start() {
        if (isStarted.compareAndSet(false, true)) {
            Timber.v("Start timeline for roomId: $roomId and eventId: $initialEventId")
            eventBus.register(this)
            eventDecryptor.start()
            timelineScope.launch {
                localEchoChunkId = sessionDatabase.chunkQueries.getLocalEchoChunkId(roomId).executeAsOne()
                handleInitialLoad()
                isReady.set(true)
                postSnapshot()

                liveChunkIdQuery = sessionDatabase.chunkQueries.getChunkIdOfLive(roomId).apply {
                    addListener(liveChunkListener)
                }
                timelineInsertQuery = sessionDatabase.timelineEventQueries.getTimelineEventInsert(roomId).apply {
                    addListener(timelineInsertListener)
                }
                timelineUpdateQuery = sessionDatabase.timelineEventQueries.getTimelineEventUpdates(roomId).apply {
                    addListener(timelineUpdateListener)
                }
            }
        }
    }

    override fun dispose() {
        if (isStarted.compareAndSet(true, false)) {
            Timber.v("Dispose timeline for roomId: $roomId and eventId: $initialEventId")
            eventBus.unregister(this)
            timelineScope.coroutineContext.cancelChildren()
            eventDecryptor.destroy()
            liveChunkIdQuery?.removeListener(liveChunkListener)
            timelineInsertQuery?.removeListener(timelineInsertListener)
            timelineUpdateQuery?.removeListener(timelineUpdateListener)
            uiDebouncer.cancelAll()
            bgDebouncer.cancelAll()
            clearAllValues()
            taskExecutor.executorScope.launch {
                sessionDatabase.awaitTransaction(coroutineDispatchers) {
                    sessionDatabase.timelineEventQueries.deleteAllTimelineEventInsert()
                    sessionDatabase.timelineEventQueries.deleteAllTimelineEventUpdates()
                }
            }
        }
    }

    override fun restartWithEventId(eventId: String?) {
        dispose()
        initialEventId = eventId
        start()
        postSnapshot()
    }

    override fun hasMoreToLoad(direction: Timeline.Direction): Boolean {
        return hasMoreInCache(direction) || !hasReachedEnd(direction)
    }

    override fun paginate(direction: Timeline.Direction, count: Int) {
        if (!canPaginate(direction)) {
            return
        }
        timelineScope.launch {
            Timber.v("Paginate $direction of $count items")
            val startDisplayIndex = if (direction == Timeline.Direction.BACKWARDS) prevDisplayIndex else nextDisplayIndex
            val shouldPostSnapshot = paginateInternal(startDisplayIndex, direction, count)
            if (shouldPostSnapshot) {
                postSnapshot()
            }
        }
    }

    override fun pendingEventCount(): Int {
        return sessionDatabase.timelineEventQueries.countEventsInChunk(
                chunkId = localEchoChunkId,
                sendStates = SendState.PENDING_STATES.map { it.name }
        ).executeAsOne().toInt()
    }

    override fun failedToDeliverEventCount(): Int {
        return sessionDatabase.timelineEventQueries.countEventsInChunk(
                chunkId = localEchoChunkId,
                sendStates = SendState.HAS_FAILED_STATES.map { it.name }
        ).executeAsOne().toInt()
    }

    override fun getTimelineEventAtIndex(index: Int): TimelineEvent? {
        return builtEvents.getOrNull(index)
    }

    override fun getIndexOfEvent(eventId: String?): Int? {
        return builtEventsIdMap[eventId]
    }

    override fun getTimelineEventWithId(eventId: String?): TimelineEvent? {
        return builtEventsIdMap[eventId]?.let {
            getTimelineEventAtIndex(it)
        }
    }

    override fun getFirstDisplayableEventId(eventId: String): String? {
        // If the item is built, the id is obviously displayable
        val builtIndex = builtEventsIdMap[eventId]
        if (builtIndex != null) {
            return eventId
        }
        // Otherwise, we should check if the event is in the db, but is hidden because of filters
        val nonFilteredEvent = GetTimelineEventFromChunkQuery(sqlDriver, currentChunkId, eventId, null, timelineEventMapper::map).executeAsOneOrNull()
                ?: return null
        val filteredEvent = GetTimelineEventFromChunkQuery(sqlDriver, currentChunkId, eventId, settings, timelineEventMapper::map).executeAsOneOrNull()
        val isHidden = filteredEvent == null
        return if (isHidden) {
            // Then we are looking for the first displayable event after the hidden one
            val displayIndex = nonFilteredEvent.displayIndex
            val firstDisplayableEvent = GetPagedTimelineEventQuery(
                    sqlDriver, currentChunkId, displayIndex, 1, settings, Timeline.Direction.BACKWARDS, timelineEventMapper::map
            ).executeAsOneOrNull()
            firstDisplayableEvent?.eventId
        } else {
            null
        }
    }

    private suspend fun handleInitialLoad() {
        val currentInitialEventId = initialEventId
        if (currentInitialEventId != null) {
            val initialEvent = sessionDatabase.timelineEventQueries.get(eventId = currentInitialEventId).executeAsOneOrNull()
            setInitialDisplayIndexes(initialEvent?.display_index)
            if (initialEvent != null) {
                Timber.v("Initial load with known event: $currentInitialEventId")
                currentChunkId = sessionDatabase.chunkQueries.getChunkIdContainingEvent(
                        roomId = roomId,
                        eventId = currentInitialEventId
                ).executeAsOne()
                val count = sessionDatabase.timelineEventQueries.countEventsInChunk(
                        chunkId = currentChunkId,
                        sendStates = SendState.values().map { it.name }
                )
                        .executeAsOne()
                        .toInt()
                        .coerceAtMost(settings.initialSize)
                paginateInternal(initialEvent.display_index, Timeline.Direction.FORWARDS, (count / 2).coerceAtLeast(1))
                paginateInternal(initialEvent.display_index.minus(1), Timeline.Direction.BACKWARDS, (count / 2).coerceAtLeast(1))
            } else {
                Timber.v("Initial load with unknown event: $currentInitialEventId")
                // We have to fetch this event. We create first a chunk
                sessionDatabase.chunkQueries.createNew(roomId)
                val chunkId = sessionDatabase.chunkQueries.lastInsertId().executeAsOne()
                currentChunkId = chunkId
                fetchEvent(currentInitialEventId, chunkId)
            }
        } else {
            Timber.v("Initial load with no event")
            val liveChunk = sessionDatabase.chunkQueries.getChunkIdOfLive(roomId).executeAsOne()
            initialLoadFromLiveChunk(liveChunk)
        }
    }

    private suspend fun initialLoadFromLiveChunk(liveChunkId: Long) {
        val firstEvent = sessionDatabase.timelineEventQueries.getLastForwardsFromChunk(liveChunkId).executeAsOneOrNull()
        currentChunkId = liveChunkId
        val count = sessionDatabase.timelineEventQueries.countEventsInChunk(
                chunkId = currentChunkId,
                sendStates = SendState.values().map { it.name }
        )
                .executeAsOne()
                .toInt()
                .coerceAtMost(settings.initialSize)
        setInitialDisplayIndexes(firstEvent?.display_index)
        paginateInternal(firstEvent?.display_index, Timeline.Direction.BACKWARDS, count)
    }

    private suspend fun fetchEvent(eventId: String, chunkId: Long) {
        val params = GetContextOfEventTask.Params(roomId, eventId, chunkId)
        try {
            contextOfEventTask.execute(params)
            executePaginationTask(Timeline.Direction.BACKWARDS, settings.initialSize / 2)
            executePaginationTask(Timeline.Direction.FORWARDS, settings.initialSize / 2)
        } catch (failure: Throwable) {
            postFailure(failure)
        }
    }

    private fun setInitialDisplayIndexes(displayIndex: Int?) {
        val safeDisplayIndex = displayIndex ?: 0
        prevDisplayIndex = safeDisplayIndex
        nextDisplayIndex = safeDisplayIndex + 1
    }

    private suspend fun paginateInternal(startDisplayIndex: Int?,
                                         direction: Timeline.Direction,
                                         count: Int): Boolean {
        Timber.v("Paginate internal called with startDisplayIndex: $startDisplayIndex, direction: $direction, count: $count")
        updateState(direction) { it.copy(isPaginating = true) }
        val builtCount = buildTimelineEvents(startDisplayIndex, direction, count.toLong())
        val shouldFetchMore = builtCount < count && !hasReachedEnd(direction)
        if (shouldFetchMore) {
            val newRequestedCount = count - builtCount
            executePaginationTask(direction, newRequestedCount)
        } else {
            updateState(direction) { it.copy(isPaginating = false) }
        }
        return !shouldFetchMore
    }

    private fun buildTimelineEvents(startDisplayIndex: Int?,
                                    direction: Timeline.Direction,
                                    count: Long): Int = synchronized(this) {
        if (count < 1 || startDisplayIndex == null) {
            return 0
        }
        var start = System.currentTimeMillis()
        val offsetResults = GetPagedTimelineEventQuery(sqlDriver, currentChunkId, startDisplayIndex, count, settings, direction, timelineEventMapper::map).executeAsList()
        var time = System.currentTimeMillis() - start
        Timber.v("Query ${offsetResults.size} items from db in $time ms")
        if (offsetResults.isEmpty()) {
            return 0
        }
        start = System.currentTimeMillis()
        val offsetIndex = offsetResults.last().displayIndex
        if (direction == Timeline.Direction.BACKWARDS) {
            prevDisplayIndex = offsetIndex - 1
        } else {
            nextDisplayIndex = offsetIndex + 1
        }
        offsetResults.forEach { timelineEvent ->
            val enrichedTimelineEvent = timelineEvent.enrichWithRelations()
            if (enrichedTimelineEvent.isEncrypted()
                    && enrichedTimelineEvent.root.mxDecryptionResult == null) {
                enrichedTimelineEvent.root.eventId?.also {
                    eventDecryptor.requestDecryption(TimelineEventDecryptor.DecryptionRequest(it, timelineID))
                }
            }
            val position = if (direction == Timeline.Direction.FORWARDS) 0 else builtEvents.size
            builtEvents.add(position, enrichedTimelineEvent)
            // Need to shift :/
            builtEventsIdMap.entries.filter { it.value >= position }.forEach { it.setValue(it.value + 1) }
            builtEventsIdMap[enrichedTimelineEvent.eventId] = position
        }
        time = System.currentTimeMillis() - start
        Timber.v("Built ${offsetResults.size} items from db in $time ms")
        return offsetResults.size
    }

    private fun postSnapshot() {
        updateLoadingStates()
        val snapshot = createSnapshot()
        val runnable = Runnable {
            Timber.v("Post snapshot with size: ${snapshot.size}")
            listeners.forEach {
                it.onTimelineUpdated(snapshot)
            }
        }
        uiDebouncer.debounce("post_snapshot", runnable, 10)
    }

    private fun postFailure(throwable: Throwable) {
        val runnable = Runnable {
            listeners.forEach {
                it.onTimelineFailure(throwable)
            }
        }
        uiHandler.post(runnable)
    }

    private fun canPaginate(direction: Timeline.Direction): Boolean {
        return !getState(direction).isPaginating && hasMoreToLoad(direction)
    }

    private fun getState(direction: Timeline.Direction): State {
        return when (direction) {
            Timeline.Direction.FORWARDS -> forwardsState.get()
            Timeline.Direction.BACKWARDS -> backwardsState.get()
        }
    }

    private fun updateState(direction: Timeline.Direction, update: (State) -> State) {
        val stateReference = when (direction) {
            Timeline.Direction.FORWARDS -> forwardsState
            Timeline.Direction.BACKWARDS -> backwardsState
        }
        val currentValue = stateReference.get()
        val newValue = update(currentValue)
        stateReference.set(newValue)
    }

    private fun updateLoadingStates() {
        if (!isReady.get()) {
            return
        }
        val lastCacheEvent = GetLastTimelineEventFromChunkQuery(sqlDriver, currentChunkId, settings, Timeline.Direction.BACKWARDS, timelineEventMapper::map).executeAsOneOrNull()
        val lastBuiltEvent = builtEvents.lastOrNull()
        val firstCacheEvent = GetLastTimelineEventFromChunkQuery(sqlDriver, currentChunkId, settings, Timeline.Direction.FORWARDS, timelineEventMapper::map).executeAsOneOrNull()
        val firstBuiltEvent = builtEvents.firstOrNull()

        updateState(Timeline.Direction.FORWARDS) {
            it.copy(
                    hasMoreInCache = firstBuiltEvent == null || firstBuiltEvent.displayIndex < firstCacheEvent?.displayIndex ?: Int.MIN_VALUE,
                    hasReachedEnd = hasReachedEndQuery(Timeline.Direction.FORWARDS)
            )
        }

        updateState(Timeline.Direction.BACKWARDS) {
            it.copy(
                    hasMoreInCache = lastBuiltEvent == null || lastBuiltEvent.displayIndex > lastCacheEvent?.displayIndex ?: Int.MAX_VALUE,
                    hasReachedEnd = hasReachedEndQuery(Timeline.Direction.BACKWARDS)
            )
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNewTimelineEvents(onNewTimelineEvents: OnNewTimelineEvents) {
        if (isLive && onNewTimelineEvents.roomId == roomId) {
            listeners.forEach {
                it.onNewTimelineEvents(onNewTimelineEvents.eventIds)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLocalEchoCreated(onLocalEchoCreated: OnLocalEchoCreated) {
        if (isLive && onLocalEchoCreated.roomId == roomId) {
            listeners.forEach {
                it.onNewTimelineEvents(listOf(onLocalEchoCreated.timelineEvent.eventId))
            }
            //inMemorySendingEvents.add(0, onLocalEchoCreated.timelineEvent)
            //postSnapshot()
        }
    }

    private fun getCurrentChunk(): ChunkEntity? {
        return sessionDatabase.chunkQueries.getWithChunkId(currentChunkId).executeAsOneOrNull()
    }

    private fun ChunkEntity.getToken(direction: Timeline.Direction): String? {
        return if (direction == Timeline.Direction.BACKWARDS) prev_token else next_token
    }

    private fun hasReachedEndQuery(direction: Timeline.Direction): Boolean {
        return if (direction == Timeline.Direction.BACKWARDS) {
            sessionDatabase.chunkQueries.isLastBackward(currentChunkId)
        } else {
            sessionDatabase.chunkQueries.isLastForward(currentChunkId)
        }.executeAsOneOrNull() ?: false
    }

    private suspend fun executePaginationTask(direction: Timeline.Direction, limit: Int) {
        val liveChunk = getCurrentChunk()
        Timber.v("Live chunk: $liveChunk")
        val token = liveChunk?.getToken(direction)
        if (token == null || hasReachedEndQuery(direction)) {
            updateState(direction) { it.copy(isPaginating = false) }
            return
        }
        val params = PaginationTask.Params(roomId = roomId,
                from = token,
                direction = direction.toPaginationDirection(),
                limit = limit)

        Timber.v("Should fetch $limit items $direction")
        try {
            val result = paginationTask.execute(params)
            when (result) {
                TokenChunkEventPersistor.Result.SUCCESS -> {
                    Timber.v("Success fetching $limit items $direction from pagination request")
                    val displayIndex = if (direction == Timeline.Direction.FORWARDS) {
                        nextDisplayIndex
                    } else {
                        prevDisplayIndex
                    }
                    buildTimelineEvents(displayIndex, direction, limit.toLong())
                    updateState(direction) {
                        it.copy(isPaginating = false)
                    }
                    postSnapshot()
                }
                TokenChunkEventPersistor.Result.REACHED_END -> {
                    postSnapshot()
                }
                TokenChunkEventPersistor.Result.SHOULD_FETCH_MORE ->
                    // Database won't be updated, so we force pagination request
                    executePaginationTask(direction, limit)
            }
        } catch (failure: Throwable) {
            updateState(direction) { it.copy(isPaginating = false) }
            postSnapshot()
            Timber.v("Failure fetching $limit items $direction from pagination request")
        }
    }

    private fun handleNewLiveEvents() {
        val numberOfNewEvents = sessionDatabase.timelineEventQueries.countForwardEvents(currentChunkId, nextDisplayIndex
                ?: 0).executeAsOne()
        val numberOfBuiltEvents = buildTimelineEvents(nextDisplayIndex, Timeline.Direction.FORWARDS, numberOfNewEvents)
        if (numberOfBuiltEvents > 0) {
            postSnapshot()
        }
    }

    private fun clearAllValues() {
        currentChunkId = 0
        prevDisplayIndex = null
        nextDisplayIndex = null
        builtEvents.clear()
        builtEventsIdMap.clear()
        backwardsState.set(State())
        forwardsState.set(State())
    }

    private fun rebuildEvent(eventId: String, builder: (TimelineEvent) -> TimelineEvent): Boolean = synchronized(this) {
        return builtEventsIdMap[eventId]?.let { builtIndex ->
            // Update the relation of existing event
            builtEvents.getOrNull(builtIndex)?.let { te ->
                try {
                    builtEvents[builtIndex] = builder(te)
                    true
                } catch (failure: Throwable) {
                    false
                }
            }
        } ?: false
    }

    private fun hasMoreInCache(direction: Timeline.Direction) = getState(direction).hasMoreInCache

    private fun hasReachedEnd(direction: Timeline.Direction) = getState(direction).hasReachedEnd

    private fun createSnapshot(): List<TimelineEvent> {
        return if (isReady.get()) {
            buildSendingEvents() + builtEvents.toList()
        } else {
            emptyList()
        }
    }

    private fun buildSendingEvents(): List<TimelineEvent> {
        return if (hasReachedEnd(Timeline.Direction.FORWARDS) && !hasMoreInCache(Timeline.Direction.FORWARDS)) {
            GetAllTimelineEventFromChunkQuery(sqlDriver, localEchoChunkId, settings, timelineEventMapper::map).executeAsList()
        } else {
            emptyList()
        }
    }

    private fun TimelineEvent.enrichWithRelations(): TimelineEvent {
        val readReceipts = sessionDatabase.readReceiptQueries.getAllForEvent(eventId, readReceiptMapper::map).executeAsList()
        val annotationsSummary = eventAnnotationsSummaryDataSource.getEventAnnotationsSummary(eventId)
        return copy(
                readReceipts = readReceipts,
                annotations = annotationsSummary
        )
    }

    private fun Timeline.Direction.toPaginationDirection(): PaginationDirection {
        return if (this == Timeline.Direction.BACKWARDS) PaginationDirection.BACKWARDS else PaginationDirection.FORWARDS
    }

    private data class State(
            val hasReachedEnd: Boolean = false,
            val hasMoreInCache: Boolean = false,
            val isPaginating: Boolean = false
    )
}
