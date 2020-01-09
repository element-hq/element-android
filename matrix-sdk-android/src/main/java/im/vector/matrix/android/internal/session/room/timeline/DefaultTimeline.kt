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

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.model.ReadReceipt
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.session.room.timeline.TimelineSettings
import im.vector.matrix.android.api.util.CancelableBag
import im.vector.matrix.android.internal.database.mapper.TimelineEventMapper
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.*
import im.vector.matrix.android.internal.database.query.FilterContent
import im.vector.matrix.android.internal.database.query.findAllInRoomWithSendStates
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.database.query.whereInRoom
import im.vector.matrix.android.internal.task.TaskConstraints
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.Debouncer
import im.vector.matrix.android.internal.util.createUIHandler
import io.realm.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.lang.Runnable
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.max
import kotlin.math.min

private const val MIN_FETCHING_COUNT = 30

internal class DefaultTimeline(
        private val roomId: String,
        private var initialEventId: String? = null,
        private val realmConfiguration: RealmConfiguration,
        private val taskExecutor: TaskExecutor,
        private val contextOfEventTask: GetContextOfEventTask,
        private val clearUnlinkedEventsTask: ClearUnlinkedEventsTask,
        private val paginationTask: PaginationTask,
        private val cryptoService: CryptoService,
        private val timelineEventMapper: TimelineEventMapper,
        private val settings: TimelineSettings,
        private val hiddenReadReceipts: TimelineHiddenReadReceipts
) : Timeline, TimelineHiddenReadReceipts.Delegate {

    companion object {
        val BACKGROUND_CONTEXT = CoroutineName("TIMELINE_DB_THREAD")
    }

    private val listeners = CopyOnWriteArrayList<Timeline.Listener>()
    private val isStarted = AtomicBoolean(false)
    private val isReady = AtomicBoolean(false)
    private val mainHandler = createUIHandler()
    private val scope = CoroutineScope(SupervisorJob() + BACKGROUND_CONTEXT)
    private val backgroundRealm = AtomicReference<Realm>()
    private val cancelableBag = CancelableBag()
    private val debouncer = Debouncer(mainHandler)

    private lateinit var nonFilteredEvents: RealmResults<TimelineEventEntity>
    private lateinit var filteredEvents: RealmResults<TimelineEventEntity>
    private lateinit var eventRelations: RealmResults<EventAnnotationsSummaryEntity>

    private var roomEntity: RoomEntity? = null

    private var prevDisplayIndex: Int? = null
    private var nextDisplayIndex: Int? = null
    private val builtEvents = Collections.synchronizedList<TimelineEvent>(ArrayList())
    private val builtEventsIdMap = Collections.synchronizedMap(HashMap<String, Int>())
    private val backwardsState = AtomicReference(State())
    private val forwardsState = AtomicReference(State())

    override val timelineID = UUID.randomUUID().toString()

    override val isLive
        get() = !hasMoreToLoad(Timeline.Direction.FORWARDS)

    private val eventDecryptor = TimelineEventDecryptor(realmConfiguration, timelineID, cryptoService)

    private val eventsChangeListener = OrderedRealmCollectionChangeListener<RealmResults<TimelineEventEntity>> { results, changeSet ->
        if (!results.isLoaded || !results.isValid) {
            return@OrderedRealmCollectionChangeListener
        }
        handleUpdates(changeSet)
    }

    private val relationsListener = OrderedRealmCollectionChangeListener<RealmResults<EventAnnotationsSummaryEntity>> { collection, changeSet ->
        var hasChange = false

        (changeSet.insertions + changeSet.changes).forEach {
            val eventRelations = collection[it]
            if (eventRelations != null) {
                hasChange = rebuildEvent(eventRelations.eventId) { te ->
                    te.copy(annotations = eventRelations.asDomain())
                } || hasChange
            }
        }
        if (hasChange) postSnapshot()
    }

// Public methods ******************************************************************************

    override fun paginate(direction: Timeline.Direction, count: Int) {
        scope.launch {
            if (!canPaginate(direction)) {
                return@launch
            }
            Timber.v("Paginate $direction of $count items")
            val startDisplayIndex = if (direction == Timeline.Direction.BACKWARDS) prevDisplayIndex else nextDisplayIndex
            val shouldPostSnapshot = paginateInternal(startDisplayIndex, direction, count)
            if (shouldPostSnapshot) {
                postSnapshot()
            }
        }
    }

    override fun pendingEventCount(): Int {
        return Realm.getInstance(realmConfiguration).use {
            RoomEntity.where(it, roomId).findFirst()?.sendingTimelineEvents?.count() ?: 0
        }
    }

    override fun failedToDeliverEventCount(): Int {
        return Realm.getInstance(realmConfiguration).use {
            TimelineEventEntity.findAllInRoomWithSendStates(it, roomId, SendState.HAS_FAILED_STATES).count()
        }
    }

    override fun start() {
        if (isStarted.compareAndSet(false, true)) {
            Timber.v("Start timeline for roomId: $roomId and eventId: $initialEventId")
            scope.launch {
                eventDecryptor.start()
                val realm = Realm.getInstance(realmConfiguration)
                backgroundRealm.set(realm)

                roomEntity = RoomEntity.where(realm, roomId = roomId).findFirst()?.also {
                    it.sendingTimelineEvents.addChangeListener { _ ->
                        postSnapshot()
                    }
                }

                nonFilteredEvents = buildEventQuery(realm).sort(TimelineEventEntityFields.ROOT.DISPLAY_INDEX, Sort.DESCENDING).findAll()
                filteredEvents = nonFilteredEvents.where()
                        .filterEventsWithSettings()
                        .findAll()
                handleInitialLoad()
                filteredEvents.addChangeListener(eventsChangeListener)

                eventRelations = EventAnnotationsSummaryEntity.whereInRoom(realm, roomId)
                        .findAllAsync()
                        .also { it.addChangeListener(relationsListener) }

                if (settings.buildReadReceipts) {
                    hiddenReadReceipts.start(realm, filteredEvents, nonFilteredEvents, this@DefaultTimeline)
                }
                isReady.set(true)
            }
        }
    }

    override fun dispose() {
        if (isStarted.compareAndSet(true, false)) {
            isReady.set(false)
            Timber.v("Dispose timeline for roomId: $roomId and eventId: $initialEventId")
            cancelableBag.cancel()
            scope.coroutineContext.cancelChildren()
            scope.launch {
                roomEntity?.sendingTimelineEvents?.removeAllChangeListeners()
                if (this::eventRelations.isInitialized) {
                    eventRelations.removeAllChangeListeners()
                }
                if (this::filteredEvents.isInitialized) {
                    filteredEvents.removeAllChangeListeners()
                }
                if (settings.buildReadReceipts) {
                    hiddenReadReceipts.dispose()
                }
                clearAllValues()
                backgroundRealm.getAndSet(null).also {
                    it?.close()
                }
                eventDecryptor.destroy()
            }
            clearUnlinkedEventsTask
                    .configureWith(ClearUnlinkedEventsTask.Params(roomId))
                    .executeBy(taskExecutor)
        }
    }

    override fun restartWithEventId(eventId: String?) {
        dispose()
        initialEventId = eventId
        start()
        postSnapshot()
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
        return Realm.getInstance(realmConfiguration).use { localRealm ->
            val nonFilteredEvents = buildEventQuery(localRealm)
                    .sort(TimelineEventEntityFields.ROOT.DISPLAY_INDEX, Sort.DESCENDING)
                    .findAll()

            val nonFilteredEvent = nonFilteredEvents.where()
                    .equalTo(TimelineEventEntityFields.EVENT_ID, eventId)
                    .findFirst()

            val filteredEvents = nonFilteredEvents.where().filterEventsWithSettings().findAll()
            val isEventInDb = nonFilteredEvent != null

            val isHidden = isEventInDb && filteredEvents.where()
                    .equalTo(TimelineEventEntityFields.EVENT_ID, eventId)
                    .findFirst() == null

            if (isHidden) {
                val displayIndex = nonFilteredEvent?.root?.displayIndex
                if (displayIndex != null) {
                    // Then we are looking for the first displayable event after the hidden one
                    val firstDisplayedEvent = filteredEvents.where()
                            .lessThanOrEqualTo(TimelineEventEntityFields.ROOT.DISPLAY_INDEX, displayIndex)
                            .findFirst()
                    firstDisplayedEvent?.eventId
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    override fun hasMoreToLoad(direction: Timeline.Direction): Boolean {
        return hasMoreInCache(direction) || !hasReachedEnd(direction)
    }

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

    // TimelineHiddenReadReceipts.Delegate

    override fun rebuildEvent(eventId: String, readReceipts: List<ReadReceipt>): Boolean {
        return rebuildEvent(eventId) { te ->
            te.copy(readReceipts = readReceipts)
        }
    }

    override fun onReadReceiptsUpdated() {
        postSnapshot()
    }

    // Private methods *****************************************************************************

    private fun rebuildEvent(eventId: String, builder: (TimelineEvent) -> TimelineEvent): Boolean {
        return builtEventsIdMap[eventId]?.let { builtIndex ->
            // Update the relation of existing event
            builtEvents[builtIndex]?.let { te ->
                builtEvents[builtIndex] = builder(te)
                true
            }
        } ?: false
    }

    private fun hasMoreInCache(direction: Timeline.Direction) = getState(direction).hasMoreInCache

    private fun hasReachedEnd(direction: Timeline.Direction) = getState(direction).hasReachedEnd

    private fun updateLoadingStates(results: RealmResults<TimelineEventEntity>) {
        val lastCacheEvent = results.lastOrNull()
        val lastBuiltEvent = builtEvents.lastOrNull()
        val firstCacheEvent = results.firstOrNull()
        val firstBuiltEvent = builtEvents.firstOrNull()
        val chunkEntity = getLiveChunk()

        updateState(Timeline.Direction.FORWARDS) {
            it.copy(
                    hasMoreInCache = firstBuiltEvent == null || firstBuiltEvent.displayIndex < firstCacheEvent?.root?.displayIndex ?: Int.MIN_VALUE,
                    hasReachedEnd = chunkEntity?.isLastForward ?: false
            )
        }

        updateState(Timeline.Direction.BACKWARDS) {
            it.copy(
                    hasMoreInCache = lastBuiltEvent == null || lastBuiltEvent.displayIndex > lastCacheEvent?.root?.displayIndex ?: Int.MAX_VALUE,
                    hasReachedEnd = chunkEntity?.isLastBackward ?: false || lastCacheEvent?.root?.type == EventType.STATE_ROOM_CREATE
            )
        }
    }

    /**
     * This has to be called on TimelineThread as it access realm live results
     * @return true if createSnapshot should be posted
     */
    private fun paginateInternal(startDisplayIndex: Int?,
                                 direction: Timeline.Direction,
                                 count: Int,
                                 strict: Boolean = false): Boolean {
        updateState(direction) { it.copy(requestedPaginationCount = count, isPaginating = true) }
        val builtCount = buildTimelineEvents(startDisplayIndex, direction, count.toLong(), strict)
        val shouldFetchMore = builtCount < count && !hasReachedEnd(direction)
        if (shouldFetchMore) {
            val newRequestedCount = count - builtCount
            updateState(direction) { it.copy(requestedPaginationCount = newRequestedCount) }
            val fetchingCount = max(MIN_FETCHING_COUNT, newRequestedCount)
            executePaginationTask(direction, fetchingCount)
        } else {
            updateState(direction) { it.copy(isPaginating = false, requestedPaginationCount = 0) }
        }

        return !shouldFetchMore
    }

    private fun createSnapshot(): List<TimelineEvent> {
        return buildSendingEvents() + builtEvents.toList()
    }

    private fun buildSendingEvents(): List<TimelineEvent> {
        val sendingEvents = ArrayList<TimelineEvent>()
        if (hasReachedEnd(Timeline.Direction.FORWARDS) && !hasMoreInCache(Timeline.Direction.FORWARDS)) {
            roomEntity?.sendingTimelineEvents
                    ?.where()
                    ?.filterEventsWithSettings()
                    ?.findAll()
                    ?.forEach {
                        sendingEvents.add(timelineEventMapper.map(it))
                    }
        }
        return sendingEvents
    }

    private fun canPaginate(direction: Timeline.Direction): Boolean {
        return isReady.get() && !getState(direction).isPaginating && hasMoreToLoad(direction)
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

    /**
     * This has to be called on TimelineThread as it access realm live results
     */
    private fun handleInitialLoad() {
        var shouldFetchInitialEvent = false
        val currentInitialEventId = initialEventId
        val initialDisplayIndex = if (currentInitialEventId == null) {
            filteredEvents.firstOrNull()?.root?.displayIndex
        } else {
            val initialEvent = nonFilteredEvents.where()
                    .equalTo(TimelineEventEntityFields.EVENT_ID, initialEventId)
                    .findFirst()

            shouldFetchInitialEvent = initialEvent == null
            initialEvent?.root?.displayIndex
        }
        prevDisplayIndex = initialDisplayIndex
        nextDisplayIndex = initialDisplayIndex
        if (currentInitialEventId != null && shouldFetchInitialEvent) {
            fetchEvent(currentInitialEventId)
        } else {
            val count = min(settings.initialSize, filteredEvents.size)
            if (initialEventId == null) {
                paginateInternal(initialDisplayIndex, Timeline.Direction.BACKWARDS, count, strict = false)
            } else {
                paginateInternal(initialDisplayIndex, Timeline.Direction.FORWARDS, count / 2, strict = false)
                paginateInternal(initialDisplayIndex, Timeline.Direction.BACKWARDS, count / 2, strict = true)
            }
        }
        postSnapshot()
    }

    /**
     * This has to be called on TimelineThread as it access realm live results
     */
    private fun handleUpdates(changeSet: OrderedCollectionChangeSet) {
        // If changeSet has deletion we are having a gap, so we clear everything
        if (changeSet.deletionRanges.isNotEmpty()) {
            clearAllValues()
        }
        var postSnapshot = false
        changeSet.insertionRanges.forEach { range ->
            val (startDisplayIndex, direction) = if (range.startIndex == 0) {
                Pair(filteredEvents[range.length - 1]!!.root!!.displayIndex, Timeline.Direction.FORWARDS)
            } else {
                Pair(filteredEvents[range.startIndex]!!.root!!.displayIndex, Timeline.Direction.BACKWARDS)
            }
            val state = getState(direction)
            if (state.isPaginating) {
                // We are getting new items from pagination
                postSnapshot = paginateInternal(startDisplayIndex, direction, state.requestedPaginationCount)
            } else {
                // We are getting new items from sync
                buildTimelineEvents(startDisplayIndex, direction, range.length.toLong())
                postSnapshot = true
            }
        }
        changeSet.changes.forEach { index ->
            val eventEntity = filteredEvents[index]
            eventEntity?.eventId?.let { eventId ->
                postSnapshot = rebuildEvent(eventId) {
                    buildTimelineEvent(eventEntity)
                } || postSnapshot
            }
        }
        if (postSnapshot) {
            postSnapshot()
        }
    }

    /**
     * This has to be called on TimelineThread as it access realm live results
     */
    private fun executePaginationTask(direction: Timeline.Direction, limit: Int) {
        val token = getTokenLive(direction)
        if (token == null) {
            updateState(direction) { it.copy(isPaginating = false, requestedPaginationCount = 0) }
            return
        }
        val params = PaginationTask.Params(roomId = roomId,
                from = token,
                direction = direction.toPaginationDirection(),
                limit = limit)

        Timber.v("Should fetch $limit items $direction")
        cancelableBag += paginationTask
                .configureWith(params) {
                    this.constraints = TaskConstraints(connectedToNetwork = true)
                    this.callback = object : MatrixCallback<TokenChunkEventPersistor.Result> {
                        override fun onSuccess(data: TokenChunkEventPersistor.Result) {
                            when (data) {
                                TokenChunkEventPersistor.Result.SUCCESS -> {
                                    Timber.v("Success fetching $limit items $direction from pagination request")
                                }
                                TokenChunkEventPersistor.Result.REACHED_END -> {
                                    postSnapshot()
                                }
                                TokenChunkEventPersistor.Result.SHOULD_FETCH_MORE ->
                                    // Database won't be updated, so we force pagination request
                                    scope.launch {
                                        executePaginationTask(direction, limit)
                                    }
                            }
                        }

                        override fun onFailure(failure: Throwable) {
                            updateState(direction) { it.copy(isPaginating = false, requestedPaginationCount = 0) }
                            postSnapshot()
                            Timber.v("Failure fetching $limit items $direction from pagination request")
                        }
                    }
                }
                .executeBy(taskExecutor)
    }

    /**
     * This has to be called on TimelineThread as it access realm live results
     */

    private fun getTokenLive(direction: Timeline.Direction): String? {
        val chunkEntity = getLiveChunk() ?: return null
        return if (direction == Timeline.Direction.BACKWARDS) chunkEntity.prevToken else chunkEntity.nextToken
    }

    /**
     * This has to be called on TimelineThread as it access realm live results
     */
    private fun getLiveChunk(): ChunkEntity? {
        return filteredEvents.firstOrNull()?.chunk?.firstOrNull()
    }

    /**
     * This has to be called on TimelineThread as it access realm live results
     * @return number of items who have been added
     */
    private fun buildTimelineEvents(startDisplayIndex: Int?,
                                    direction: Timeline.Direction,
                                    count: Long,
                                    strict: Boolean = false): Int {
        if (count < 1 || startDisplayIndex == null) {
            return 0
        }
        val start = System.currentTimeMillis()
        val offsetResults = getOffsetResults(startDisplayIndex, direction, count, strict)
        if (offsetResults.isEmpty()) {
            return 0
        }
        val offsetIndex = offsetResults.last()!!.root!!.displayIndex
        if (direction == Timeline.Direction.BACKWARDS) {
            prevDisplayIndex = offsetIndex - 1
        } else {
            nextDisplayIndex = offsetIndex + 1
        }
        offsetResults.forEach { eventEntity ->

            val timelineEvent = buildTimelineEvent(eventEntity)

            if (timelineEvent.isEncrypted()
                    && timelineEvent.root.mxDecryptionResult == null) {
                timelineEvent.root.eventId?.let { eventDecryptor.requestDecryption(it) }
            }

            val position = if (direction == Timeline.Direction.FORWARDS) 0 else builtEvents.size
            builtEvents.add(position, timelineEvent)
            // Need to shift :/
            builtEventsIdMap.entries.filter { it.value >= position }.forEach { it.setValue(it.value + 1) }
            builtEventsIdMap[eventEntity.eventId] = position
        }
        val time = System.currentTimeMillis() - start
        Timber.v("Built ${offsetResults.size} items from db in $time ms")
        return offsetResults.size
    }

    private fun buildTimelineEvent(eventEntity: TimelineEventEntity) = timelineEventMapper.map(
            timelineEventEntity = eventEntity,
            buildReadReceipts = settings.buildReadReceipts,
            correctedReadReceipts = hiddenReadReceipts.correctedReadReceipts(eventEntity.eventId)
    )

    /**
     * This has to be called on TimelineThread as it access realm live results
     */
    private fun getOffsetResults(startDisplayIndex: Int,
                                 direction: Timeline.Direction,
                                 count: Long,
                                 strict: Boolean): RealmResults<TimelineEventEntity> {
        val offsetQuery = filteredEvents.where()
        if (direction == Timeline.Direction.BACKWARDS) {
            offsetQuery.sort(TimelineEventEntityFields.ROOT.DISPLAY_INDEX, Sort.DESCENDING)
            if (strict) {
                offsetQuery.lessThan(TimelineEventEntityFields.ROOT.DISPLAY_INDEX, startDisplayIndex)
            } else {
                offsetQuery.lessThanOrEqualTo(TimelineEventEntityFields.ROOT.DISPLAY_INDEX, startDisplayIndex)
            }
        } else {
            offsetQuery.sort(TimelineEventEntityFields.ROOT.DISPLAY_INDEX, Sort.ASCENDING)
            if (strict) {
                offsetQuery.greaterThan(TimelineEventEntityFields.ROOT.DISPLAY_INDEX, startDisplayIndex)
            } else {
                offsetQuery.greaterThanOrEqualTo(TimelineEventEntityFields.ROOT.DISPLAY_INDEX, startDisplayIndex)
            }
        }
        return offsetQuery
                .limit(count)
                .findAll()
    }

    private fun buildEventQuery(realm: Realm): RealmQuery<TimelineEventEntity> {
        return if (initialEventId == null) {
            TimelineEventEntity
                    .where(realm, roomId = roomId, linkFilterMode = EventEntity.LinkFilterMode.LINKED_ONLY)
                    .equalTo("${TimelineEventEntityFields.CHUNK}.${ChunkEntityFields.IS_LAST_FORWARD}", true)
        } else {
            TimelineEventEntity
                    .where(realm, roomId = roomId, linkFilterMode = EventEntity.LinkFilterMode.BOTH)
                    .`in`("${TimelineEventEntityFields.CHUNK}.${ChunkEntityFields.TIMELINE_EVENTS.EVENT_ID}", arrayOf(initialEventId))
        }
    }

    private fun fetchEvent(eventId: String) {
        val params = GetContextOfEventTask.Params(roomId, eventId, settings.initialSize)
        cancelableBag += contextOfEventTask.configureWith(params) {
            callback = object : MatrixCallback<TokenChunkEventPersistor.Result> {
                override fun onFailure(failure: Throwable) {
                    postFailure(failure)
                }
            }
        }
                .executeBy(taskExecutor)
    }

    private fun postSnapshot() {
        scope.launch {
            if (isReady.get().not()) {
                return@launch
            }
            updateLoadingStates(filteredEvents)
            val snapshot = createSnapshot()
            val runnable = Runnable {
                listeners.forEach {
                    it.onTimelineUpdated(snapshot)
                }
            }
            debouncer.debounce("post_snapshot", runnable, 50)
        }
    }

    private fun postFailure(throwable: Throwable) {
        if (isReady.get().not()) {
            return
        }
        val runnable = Runnable {
            listeners.forEach {
                it.onTimelineFailure(throwable)
            }
        }
        mainHandler.post(runnable)
    }

    private fun clearAllValues() {
        prevDisplayIndex = null
        nextDisplayIndex = null
        builtEvents.clear()
        builtEventsIdMap.clear()
        backwardsState.set(State())
        forwardsState.set(State())
    }

    // Extension methods ***************************************************************************

    private fun Timeline.Direction.toPaginationDirection(): PaginationDirection {
        return if (this == Timeline.Direction.BACKWARDS) PaginationDirection.BACKWARDS else PaginationDirection.FORWARDS
    }

    private fun RealmQuery<TimelineEventEntity>.filterEventsWithSettings(): RealmQuery<TimelineEventEntity> {
        if (settings.filterTypes) {
            `in`(TimelineEventEntityFields.ROOT.TYPE, settings.allowedTypes.toTypedArray())
        }
        if (settings.filterEdits) {
            not().like(TimelineEventEntityFields.ROOT.CONTENT, FilterContent.EDIT_TYPE)
        }
        return this
    }

    private data class State(
            val hasReachedEnd: Boolean = false,
            val hasMoreInCache: Boolean = true,
            val isPaginating: Boolean = false,
            val requestedPaginationCount: Int = 0
    )
}
