/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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
import io.realm.RealmQuery
import io.realm.RealmResults
import io.realm.Sort
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.api.util.CancelableBag
import org.matrix.android.sdk.internal.database.RealmSessionProvider
import org.matrix.android.sdk.internal.database.mapper.TimelineEventMapper
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntityFields
import org.matrix.android.sdk.internal.database.query.findAllInRoomWithSendStates
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.database.query.whereRoomId
import org.matrix.android.sdk.internal.session.room.membership.LoadRoomMembersTask
import org.matrix.android.sdk.internal.session.sync.ReadReceiptHandler
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.configureWith
import org.matrix.android.sdk.internal.util.Debouncer
import org.matrix.android.sdk.internal.util.createBackgroundHandler
import org.matrix.android.sdk.internal.util.createUIHandler
import timber.log.Timber
import java.util.Collections
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max

private const val MIN_FETCHING_COUNT = 30

internal class DefaultTimeline(
        private val roomId: String,
        private var initialEventId: String? = null,
        private val realmConfiguration: RealmConfiguration,
        private val taskExecutor: TaskExecutor,
        private val contextOfEventTask: GetContextOfEventTask,
        private val fetchTokenAndPaginateTask: FetchTokenAndPaginateTask,
        private val paginationTask: PaginationTask,
        private val timelineEventMapper: TimelineEventMapper,
        private val settings: TimelineSettings,
        private val timelineInput: TimelineInput,
        private val eventDecryptor: TimelineEventDecryptor,
        private val realmSessionProvider: RealmSessionProvider,
        private val loadRoomMembersTask: LoadRoomMembersTask,
        private val readReceiptHandler: ReadReceiptHandler
) : Timeline,
        TimelineInput.Listener,
        UIEchoManager.Listener {

    companion object {
        val BACKGROUND_HANDLER = createBackgroundHandler("TIMELINE_DB_THREAD")
    }

    private val listeners = CopyOnWriteArrayList<Timeline.Listener>()
    private val isStarted = AtomicBoolean(false)
    private val isReady = AtomicBoolean(false)
    private val mainHandler = createUIHandler()
    private val backgroundRealm = AtomicReference<Realm>()
    private val cancelableBag = CancelableBag()
    private val debouncer = Debouncer(mainHandler)

    private lateinit var timelineEvents: RealmResults<TimelineEventEntity>
    private lateinit var sendingEvents: RealmResults<TimelineEventEntity>

    private var prevDisplayIndex: Int? = null
    private var nextDisplayIndex: Int? = null

    private val uiEchoManager = UIEchoManager(settings, this)

    private val builtEvents = Collections.synchronizedList<TimelineEvent>(ArrayList())
    private val builtEventsIdMap = Collections.synchronizedMap(HashMap<String, Int>())
    private val backwardsState = AtomicReference(TimelineState())
    private val forwardsState = AtomicReference(TimelineState())

    override val timelineID = UUID.randomUUID().toString()

    override val isLive
        get() = !hasMoreToLoad(Timeline.Direction.FORWARDS)

    private val eventsChangeListener = OrderedRealmCollectionChangeListener<RealmResults<TimelineEventEntity>> { results, changeSet ->
        if (!results.isLoaded || !results.isValid) {
            return@OrderedRealmCollectionChangeListener
        }
        Timber.v("## SendEvent: [${System.currentTimeMillis()}] DB update for room $roomId")
        handleUpdates(results, changeSet)
    }

    // Public methods ******************************************************************************

    override fun paginate(direction: Timeline.Direction, count: Int) {
        BACKGROUND_HANDLER.post {
            if (!canPaginate(direction)) {
                return@post
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
        return realmSessionProvider.withRealm {
            RoomEntity.where(it, roomId).findFirst()?.sendingTimelineEvents?.count() ?: 0
        }
    }

    override fun failedToDeliverEventCount(): Int {
        return realmSessionProvider.withRealm {
            TimelineEventEntity.findAllInRoomWithSendStates(it, roomId, SendState.HAS_FAILED_STATES).count()
        }
    }

    override fun start() {
        if (isStarted.compareAndSet(false, true)) {
            Timber.v("Start timeline for roomId: $roomId and eventId: $initialEventId")
            timelineInput.listeners.add(this)
            BACKGROUND_HANDLER.post {
                eventDecryptor.start()
                val realm = Realm.getInstance(realmConfiguration)
                backgroundRealm.set(realm)

                val roomEntity = RoomEntity.where(realm, roomId = roomId).findFirst()
                        ?: throw IllegalStateException("Can't open a timeline without a room")

                // We don't want to filter here because some sending events that are not displayed
                // are still used for ui echo (relation like reaction)
                sendingEvents = roomEntity.sendingTimelineEvents.where()/*.filterEventsWithSettings()*/.findAll()
                sendingEvents.addChangeListener { events ->
                    uiEchoManager.onSentEventsInDatabase(events.map { it.eventId })
                    postSnapshot()
                }

                timelineEvents = buildEventQuery(realm).sort(TimelineEventEntityFields.DISPLAY_INDEX, Sort.DESCENDING).findAll()
                timelineEvents.addChangeListener(eventsChangeListener)
                handleInitialLoad()
                loadRoomMembersTask
                        .configureWith(LoadRoomMembersTask.Params(roomId))
                        .executeBy(taskExecutor)

                // Ensure ReadReceipt from init sync are loaded
                ensureReadReceiptAreLoaded(realm)

                isReady.set(true)
            }
        }
    }

    private fun ensureReadReceiptAreLoaded(realm: Realm) {
        readReceiptHandler.getContentFromInitSync(roomId)
                ?.also {
                    Timber.w("INIT_SYNC Insert when opening timeline RR for room $roomId")
                }
                ?.let { readReceiptContent ->
                    realm.executeTransactionAsync {
                        readReceiptHandler.handle(it, roomId, readReceiptContent, false, null)
                        readReceiptHandler.onContentFromInitSyncHandled(roomId)
                    }
                }
    }

    override fun dispose() {
        if (isStarted.compareAndSet(true, false)) {
            isReady.set(false)
            timelineInput.listeners.remove(this)
            Timber.v("Dispose timeline for roomId: $roomId and eventId: $initialEventId")
            cancelableBag.cancel()
            BACKGROUND_HANDLER.removeCallbacksAndMessages(null)
            BACKGROUND_HANDLER.post {
                if (this::sendingEvents.isInitialized) {
                    sendingEvents.removeAllChangeListeners()
                }
                if (this::timelineEvents.isInitialized) {
                    timelineEvents.removeAllChangeListeners()
                }
                clearAllValues()
                backgroundRealm.getAndSet(null).also {
                    it?.close()
                }
                eventDecryptor.destroy()
            }
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

    override fun onNewTimelineEvents(roomId: String, eventIds: List<String>) {
        if (isLive && this.roomId == roomId) {
            listeners.forEach {
                it.onNewTimelineEvents(eventIds)
            }
        }
    }

    override fun onLocalEchoCreated(roomId: String, timelineEvent: TimelineEvent) {
        if (roomId != this.roomId || !isLive) return
        uiEchoManager.onLocalEchoCreated(timelineEvent)
        listeners.forEach {
            tryOrNull {
                it.onNewTimelineEvents(listOf(timelineEvent.eventId))
            }
        }
        postSnapshot()
    }

    override fun onLocalEchoUpdated(roomId: String, eventId: String, sendState: SendState) {
        if (roomId != this.roomId || !isLive) return
        if (uiEchoManager.onSendStateUpdated(eventId, sendState)) {
            postSnapshot()
        }
    }

    override fun rebuildEvent(eventId: String, builder: (TimelineEvent) -> TimelineEvent?): Boolean {
        return tryOrNull {
            builtEventsIdMap[eventId]?.let { builtIndex ->
                // Update the relation of existing event
                builtEvents[builtIndex]?.let { te ->
                    val rebuiltEvent = builder(te)
                    // If rebuilt event is filtered its returned as null and should be removed.
                    if (rebuiltEvent == null) {
                        builtEventsIdMap.remove(eventId)
                        builtEventsIdMap.entries.filter { it.value > builtIndex }.forEach { it.setValue(it.value - 1) }
                        builtEvents.removeAt(builtIndex)
                    } else {
                        builtEvents[builtIndex] = rebuiltEvent
                    }
                    true
                }
            }
        } ?: false
    }

// Private methods *****************************************************************************

    private fun hasMoreInCache(direction: Timeline.Direction) = getState(direction).hasMoreInCache

    private fun hasReachedEnd(direction: Timeline.Direction) = getState(direction).hasReachedEnd

    private fun updateLoadingStates(results: RealmResults<TimelineEventEntity>) {
        val lastCacheEvent = results.lastOrNull()
        val firstCacheEvent = results.firstOrNull()
        val chunkEntity = getLiveChunk()

        updateState(Timeline.Direction.FORWARDS) {
            it.copy(
                    hasMoreInCache = !builtEventsIdMap.containsKey(firstCacheEvent?.eventId),
                    hasReachedEnd = chunkEntity?.isLastForward ?: false
            )
        }
        updateState(Timeline.Direction.BACKWARDS) {
            it.copy(
                    hasMoreInCache = !builtEventsIdMap.containsKey(lastCacheEvent?.eventId),
                    hasReachedEnd = chunkEntity?.isLastBackward ?: false || lastCacheEvent?.root?.type == EventType.STATE_ROOM_CREATE
            )
        }
    }

    /**
     * This has to be called on TimelineThread as it accesses realm live results
     * @return true if createSnapshot should be posted
     */
    private fun paginateInternal(startDisplayIndex: Int?,
                                 direction: Timeline.Direction,
                                 count: Int): Boolean {
        if (count == 0) {
            return false
        }
        updateState(direction) { it.copy(requestedPaginationCount = count, isPaginating = true) }
        val builtCount = buildTimelineEvents(startDisplayIndex, direction, count.toLong())
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
        val builtSendingEvents = mutableListOf<TimelineEvent>()
        if (hasReachedEnd(Timeline.Direction.FORWARDS) && !hasMoreInCache(Timeline.Direction.FORWARDS)) {
            uiEchoManager.getInMemorySendingEvents()
                    .updateWithUiEchoInto(builtSendingEvents)
            sendingEvents
                    .filter { timelineEvent ->
                        builtSendingEvents.none { it.eventId == timelineEvent.eventId }
                    }
                    .map { timelineEventMapper.map(it) }
                    .updateWithUiEchoInto(builtSendingEvents)
        }
        return builtSendingEvents
    }

    private fun List<TimelineEvent>.updateWithUiEchoInto(target: MutableList<TimelineEvent>) {
        target.addAll(
                // Get most up to date send state (in memory)
                map { uiEchoManager.updateSentStateWithUiEcho(it) }
        )
    }

    private fun canPaginate(direction: Timeline.Direction): Boolean {
        return isReady.get() && !getState(direction).isPaginating && hasMoreToLoad(direction)
    }

    private fun getState(direction: Timeline.Direction): TimelineState {
        return when (direction) {
            Timeline.Direction.FORWARDS  -> forwardsState.get()
            Timeline.Direction.BACKWARDS -> backwardsState.get()
        }
    }

    private fun updateState(direction: Timeline.Direction, update: (TimelineState) -> TimelineState) {
        val stateReference = when (direction) {
            Timeline.Direction.FORWARDS  -> forwardsState
            Timeline.Direction.BACKWARDS -> backwardsState
        }
        val currentValue = stateReference.get()
        val newValue = update(currentValue)
        stateReference.set(newValue)
    }

    /**
     * This has to be called on TimelineThread as it accesses realm live results
     */
    private fun handleInitialLoad() {
        var shouldFetchInitialEvent = false
        val currentInitialEventId = initialEventId
        val initialDisplayIndex = if (currentInitialEventId == null) {
            timelineEvents.firstOrNull()?.displayIndex
        } else {
            val initialEvent = timelineEvents.where()
                    .equalTo(TimelineEventEntityFields.EVENT_ID, initialEventId)
                    .findFirst()

            shouldFetchInitialEvent = initialEvent == null
            initialEvent?.displayIndex
        }
        prevDisplayIndex = initialDisplayIndex
        nextDisplayIndex = initialDisplayIndex
        if (currentInitialEventId != null && shouldFetchInitialEvent) {
            fetchEvent(currentInitialEventId)
        } else {
            val count = timelineEvents.size.coerceAtMost(settings.initialSize)
            if (initialEventId == null) {
                paginateInternal(initialDisplayIndex, Timeline.Direction.BACKWARDS, count)
            } else {
                paginateInternal(initialDisplayIndex, Timeline.Direction.FORWARDS, (count / 2).coerceAtLeast(1))
                paginateInternal(initialDisplayIndex?.minus(1), Timeline.Direction.BACKWARDS, (count / 2).coerceAtLeast(1))
            }
        }
        postSnapshot()
    }

    /**
     * This has to be called on TimelineThread as it accesses realm live results
     */
    private fun handleUpdates(results: RealmResults<TimelineEventEntity>, changeSet: OrderedCollectionChangeSet) {
        // If changeSet has deletion we are having a gap, so we clear everything
        if (changeSet.deletionRanges.isNotEmpty()) {
            clearAllValues()
        }
        var postSnapshot = false
        changeSet.insertionRanges.forEach { range ->
            val (startDisplayIndex, direction) = if (range.startIndex == 0) {
                Pair(results[range.length - 1]!!.displayIndex, Timeline.Direction.FORWARDS)
            } else {
                Pair(results[range.startIndex]!!.displayIndex, Timeline.Direction.BACKWARDS)
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
            val eventEntity = results[index]
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
     * This has to be called on TimelineThread as it accesses realm live results
     */
    private fun executePaginationTask(direction: Timeline.Direction, limit: Int) {
        val currentChunk = getLiveChunk()
        val token = if (direction == Timeline.Direction.BACKWARDS) currentChunk?.prevToken else currentChunk?.nextToken
        if (token == null) {
            if (direction == Timeline.Direction.BACKWARDS
                    || (direction == Timeline.Direction.FORWARDS && currentChunk?.hasBeenALastForwardChunk().orFalse())) {
                // We are in the case where event exists, but we do not know the token.
                // Fetch (again) the last event to get a token
                val lastKnownEventId = if (direction == Timeline.Direction.FORWARDS) {
                    timelineEvents.firstOrNull()?.eventId
                } else {
                    timelineEvents.lastOrNull()?.eventId
                }
                if (lastKnownEventId == null) {
                    updateState(direction) { it.copy(isPaginating = false, requestedPaginationCount = 0) }
                } else {
                    val params = FetchTokenAndPaginateTask.Params(
                            roomId = roomId,
                            limit = limit,
                            direction = direction.toPaginationDirection(),
                            lastKnownEventId = lastKnownEventId
                    )
                    cancelableBag += fetchTokenAndPaginateTask
                            .configureWith(params) {
                                this.callback = createPaginationCallback(limit, direction)
                            }
                            .executeBy(taskExecutor)
                }
            } else {
                updateState(direction) { it.copy(isPaginating = false, requestedPaginationCount = 0) }
            }
        } else {
            val params = PaginationTask.Params(
                    roomId = roomId,
                    from = token,
                    direction = direction.toPaginationDirection(),
                    limit = limit
            )
            Timber.v("Should fetch $limit items $direction")
            cancelableBag += paginationTask
                    .configureWith(params) {
                        this.callback = createPaginationCallback(limit, direction)
                    }
                    .executeBy(taskExecutor)
        }
    }

    // For debug purpose only
    private fun dumpAndLogChunks() {
        val liveChunk = getLiveChunk()
        Timber.w("Live chunk: $liveChunk")

        Realm.getInstance(realmConfiguration).use { realm ->
            ChunkEntity.where(realm, roomId).findAll()
                    .also { Timber.w("Found ${it.size} chunks") }
                    .forEach {
                        Timber.w("")
                        Timber.w("ChunkEntity: $it")
                        Timber.w("prevToken: ${it.prevToken}")
                        Timber.w("nextToken: ${it.nextToken}")
                        Timber.w("isLastBackward: ${it.isLastBackward}")
                        Timber.w("isLastForward: ${it.isLastForward}")
                        it.timelineEvents.forEach { tle ->
                            Timber.w("   TLE: ${tle.root?.content}")
                        }
                    }
        }
    }

    /**
     * This has to be called on TimelineThread as it accesses realm live results
     */
    private fun getTokenLive(direction: Timeline.Direction): String? {
        val chunkEntity = getLiveChunk() ?: return null
        return if (direction == Timeline.Direction.BACKWARDS) chunkEntity.prevToken else chunkEntity.nextToken
    }

    /**
     * This has to be called on TimelineThread as it accesses realm live results
     * Return the current Chunk
     */
    private fun getLiveChunk(): ChunkEntity? {
        return timelineEvents.firstOrNull()?.chunk?.firstOrNull()
    }

    /**
     * This has to be called on TimelineThread as it accesses realm live results
     * @return the number of items who have been added
     */
    private fun buildTimelineEvents(startDisplayIndex: Int?,
                                    direction: Timeline.Direction,
                                    count: Long): Int {
        if (count < 1 || startDisplayIndex == null) {
            return 0
        }
        val start = System.currentTimeMillis()
        val offsetResults = getOffsetResults(startDisplayIndex, direction, count)
        if (offsetResults.isEmpty()) {
            return 0
        }
        val offsetIndex = offsetResults.last()!!.displayIndex
        if (direction == Timeline.Direction.BACKWARDS) {
            prevDisplayIndex = offsetIndex - 1
        } else {
            nextDisplayIndex = offsetIndex + 1
        }
        offsetResults.forEach { eventEntity ->

            val timelineEvent = buildTimelineEvent(eventEntity)
            val transactionId = timelineEvent.root.unsignedData?.transactionId
            uiEchoManager.onSyncedEvent(transactionId)

            if (timelineEvent.isEncrypted()
                    && timelineEvent.root.mxDecryptionResult == null) {
                timelineEvent.root.eventId?.also { eventDecryptor.requestDecryption(TimelineEventDecryptor.DecryptionRequest(timelineEvent.root, timelineID)) }
            }

            val position = if (direction == Timeline.Direction.FORWARDS) 0 else builtEvents.size
            builtEvents.add(position, timelineEvent)
            // Need to shift :/
            builtEventsIdMap.entries.filter { it.value >= position }.forEach { it.setValue(it.value + 1) }
            builtEventsIdMap[eventEntity.eventId] = position
        }
        val time = System.currentTimeMillis() - start
        Timber.v("Built ${offsetResults.size} items from db in $time ms")
        // For the case where wo reach the lastForward chunk
        updateLoadingStates(timelineEvents)
        return offsetResults.size
    }

    private fun buildTimelineEvent(eventEntity: TimelineEventEntity): TimelineEvent {
        return timelineEventMapper.map(
                timelineEventEntity = eventEntity,
                buildReadReceipts = settings.buildReadReceipts
        ).let { timelineEvent ->
            // eventually enhance with ui echo?
            uiEchoManager.decorateEventWithReactionUiEcho(timelineEvent) ?: timelineEvent
        }
    }

    /**
     * This has to be called on TimelineThread as it accesses realm live results
     */
    private fun getOffsetResults(startDisplayIndex: Int,
                                 direction: Timeline.Direction,
                                 count: Long): RealmResults<TimelineEventEntity> {
        val offsetQuery = timelineEvents.where()
        if (direction == Timeline.Direction.BACKWARDS) {
            offsetQuery
                    .sort(TimelineEventEntityFields.DISPLAY_INDEX, Sort.DESCENDING)
                    .lessThanOrEqualTo(TimelineEventEntityFields.DISPLAY_INDEX, startDisplayIndex)
        } else {
            offsetQuery
                    .sort(TimelineEventEntityFields.DISPLAY_INDEX, Sort.ASCENDING)
                    .greaterThanOrEqualTo(TimelineEventEntityFields.DISPLAY_INDEX, startDisplayIndex)
        }
        return offsetQuery
                .limit(count)
                .findAll()
    }

    private fun buildEventQuery(realm: Realm): RealmQuery<TimelineEventEntity> {
        return if (initialEventId == null) {
            TimelineEventEntity
                    .whereRoomId(realm, roomId = roomId)
                    .equalTo(TimelineEventEntityFields.CHUNK.IS_LAST_FORWARD, true)
        } else {
            TimelineEventEntity
                    .whereRoomId(realm, roomId = roomId)
                    .`in`("${TimelineEventEntityFields.CHUNK.TIMELINE_EVENTS}.${TimelineEventEntityFields.EVENT_ID}", arrayOf(initialEventId))
        }
    }

    private fun fetchEvent(eventId: String) {
        val params = GetContextOfEventTask.Params(roomId, eventId)
        cancelableBag += contextOfEventTask.configureWith(params) {
            callback = object : MatrixCallback<TokenChunkEventPersistor.Result> {
                override fun onSuccess(data: TokenChunkEventPersistor.Result) {
                    postSnapshot()
                }

                override fun onFailure(failure: Throwable) {
                    postFailure(failure)
                }
            }
        }
                .executeBy(taskExecutor)
    }

    private fun postSnapshot() {
        BACKGROUND_HANDLER.post {
            if (isReady.get().not()) {
                return@post
            }
            updateLoadingStates(timelineEvents)
            val snapshot = createSnapshot()
            val runnable = Runnable {
                listeners.forEach {
                    it.onTimelineUpdated(snapshot)
                }
            }
            debouncer.debounce("post_snapshot", runnable, 1)
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
        backwardsState.set(TimelineState())
        forwardsState.set(TimelineState())
    }

    private fun createPaginationCallback(limit: Int, direction: Timeline.Direction): MatrixCallback<TokenChunkEventPersistor.Result> {
        return object : MatrixCallback<TokenChunkEventPersistor.Result> {
            override fun onSuccess(data: TokenChunkEventPersistor.Result) {
                when (data) {
                    TokenChunkEventPersistor.Result.SUCCESS           -> {
                        Timber.v("Success fetching $limit items $direction from pagination request")
                    }
                    TokenChunkEventPersistor.Result.REACHED_END       -> {
                        postSnapshot()
                    }
                    TokenChunkEventPersistor.Result.SHOULD_FETCH_MORE ->
                        // Database won't be updated, so we force pagination request
                        BACKGROUND_HANDLER.post {
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

    // Extension methods ***************************************************************************

    private fun Timeline.Direction.toPaginationDirection(): PaginationDirection {
        return if (this == Timeline.Direction.BACKWARDS) PaginationDirection.BACKWARDS else PaginationDirection.FORWARDS
    }
}
