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
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.EventAnnotationsSummary
import org.matrix.android.sdk.api.session.room.model.ReactionAggregatedSummary
import org.matrix.android.sdk.api.session.room.model.ReadReceipt
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.relation.ReactionContent
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.api.util.CancelableBag
import org.matrix.android.sdk.internal.database.RealmSessionProvider
import org.matrix.android.sdk.internal.database.mapper.TimelineEventMapper
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.ChunkEntityFields
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntityFields
import org.matrix.android.sdk.internal.database.query.filterEvents
import org.matrix.android.sdk.internal.database.query.findAllInRoomWithSendStates
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.database.query.whereRoomId
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
        private val hiddenReadReceipts: TimelineHiddenReadReceipts,
        private val eventBus: EventBus,
        private val eventDecryptor: TimelineEventDecryptor,
        private val realmSessionProvider: RealmSessionProvider
) : Timeline, TimelineHiddenReadReceipts.Delegate {

    data class OnNewTimelineEvents(val roomId: String, val eventIds: List<String>)
    data class OnLocalEchoCreated(val roomId: String, val timelineEvent: TimelineEvent)
    data class OnLocalEchoUpdated(val roomId: String, val eventId: String, val sendState: SendState)

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

    private lateinit var nonFilteredEvents: RealmResults<TimelineEventEntity>
    private lateinit var filteredEvents: RealmResults<TimelineEventEntity>
    private lateinit var sendingEvents: RealmResults<TimelineEventEntity>

    private var prevDisplayIndex: Int? = null
    private var nextDisplayIndex: Int? = null

    private val uiEchoManager = UIEchoManager()

    private val builtEvents = Collections.synchronizedList<TimelineEvent>(ArrayList())
    private val builtEventsIdMap = Collections.synchronizedMap(HashMap<String, Int>())
    private val backwardsState = AtomicReference(State())
    private val forwardsState = AtomicReference(State())

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
            eventBus.register(this)
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
                    uiEchoManager.sentEventsUpdated(events)
                    postSnapshot()
                }

                nonFilteredEvents = buildEventQuery(realm).sort(TimelineEventEntityFields.DISPLAY_INDEX, Sort.DESCENDING).findAll()
                filteredEvents = nonFilteredEvents.where()
                        .filterEventsWithSettings()
                        .findAll()
                nonFilteredEvents.addChangeListener(eventsChangeListener)
                handleInitialLoad()
                if (settings.shouldHandleHiddenReadReceipts()) {
                    hiddenReadReceipts.start(realm, filteredEvents, nonFilteredEvents, this)
                }
                isReady.set(true)
            }
        }
    }

    private fun TimelineSettings.shouldHandleHiddenReadReceipts(): Boolean {
        return buildReadReceipts && (filters.filterEdits || filters.filterTypes)
    }

    override fun dispose() {
        if (isStarted.compareAndSet(true, false)) {
            isReady.set(false)
            eventBus.unregister(this)
            Timber.v("Dispose timeline for roomId: $roomId and eventId: $initialEventId")
            cancelableBag.cancel()
            BACKGROUND_HANDLER.removeCallbacksAndMessages(null)
            BACKGROUND_HANDLER.post {
                if (this::sendingEvents.isInitialized) {
                    sendingEvents.removeAllChangeListeners()
                }
                if (this::nonFilteredEvents.isInitialized) {
                    nonFilteredEvents.removeAllChangeListeners()
                }
                if (settings.shouldHandleHiddenReadReceipts()) {
                    hiddenReadReceipts.dispose()
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

    override fun getFirstDisplayableEventId(eventId: String): String? {
        // If the item is built, the id is obviously displayable
        val builtIndex = builtEventsIdMap[eventId]
        if (builtIndex != null) {
            return eventId
        }
        // Otherwise, we should check if the event is in the db, but is hidden because of filters
        return realmSessionProvider.withRealm { localRealm ->
            val nonFilteredEvents = buildEventQuery(localRealm)
                    .sort(TimelineEventEntityFields.DISPLAY_INDEX, Sort.DESCENDING)
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
                val displayIndex = nonFilteredEvent?.displayIndex
                if (displayIndex != null) {
                    // Then we are looking for the first displayable event after the hidden one
                    val firstDisplayedEvent = filteredEvents.where()
                            .lessThanOrEqualTo(TimelineEventEntityFields.DISPLAY_INDEX, displayIndex)
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
        if (uiEchoManager.onLocalEchoCreated(onLocalEchoCreated)) {
            postSnapshot()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLocalEchoUpdated(onLocalEchoUpdated: OnLocalEchoUpdated) {
        if (uiEchoManager.onLocalEchoUpdated(onLocalEchoUpdated)) {
            postSnapshot()
        }
    }

// Private methods *****************************************************************************

    private fun rebuildEvent(eventId: String, builder: (TimelineEvent) -> TimelineEvent?): Boolean {
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
        val builtSendingEvents = ArrayList<TimelineEvent>()
        if (hasReachedEnd(Timeline.Direction.FORWARDS) && !hasMoreInCache(Timeline.Direction.FORWARDS)) {
            builtSendingEvents.addAll(uiEchoManager.getInMemorySendingEvents().filterEventsWithSettings())
            sendingEvents
                    .map { timelineEventMapper.map(it) }
                    // Filter out sending event that are not displayable!
                    .filterEventsWithSettings()
                    .forEach { timelineEvent ->
                        if (builtSendingEvents.find { it.eventId == timelineEvent.eventId } == null) {
                            uiEchoManager.updateSentStateWithUiEcho(timelineEvent)
                            builtSendingEvents.add(timelineEvent)
                        }
                    }
        }
        return builtSendingEvents
    }

    private fun canPaginate(direction: Timeline.Direction): Boolean {
        return isReady.get() && !getState(direction).isPaginating && hasMoreToLoad(direction)
    }

    private fun getState(direction: Timeline.Direction): State {
        return when (direction) {
            Timeline.Direction.FORWARDS  -> forwardsState.get()
            Timeline.Direction.BACKWARDS -> backwardsState.get()
        }
    }

    private fun updateState(direction: Timeline.Direction, update: (State) -> State) {
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
            nonFilteredEvents.firstOrNull()?.displayIndex
        } else {
            val initialEvent = nonFilteredEvents.where()
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
            val count = filteredEvents.size.coerceAtMost(settings.initialSize)
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
                    val builtEvent = buildTimelineEvent(eventEntity)
                    listOf(builtEvent).filterEventsWithSettings().firstOrNull()
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
                    nonFilteredEvents.firstOrNull()?.eventId
                } else {
                    nonFilteredEvents.lastOrNull()?.eventId
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
        return nonFilteredEvents.firstOrNull()?.chunk?.firstOrNull()
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
        updateLoadingStates(filteredEvents)
        return offsetResults.size
    }

    private fun buildTimelineEvent(eventEntity: TimelineEventEntity) = timelineEventMapper.map(
            timelineEventEntity = eventEntity,
            buildReadReceipts = settings.buildReadReceipts,
            correctedReadReceipts = hiddenReadReceipts.correctedReadReceipts(eventEntity.eventId)
    ).let {
        // eventually enhance with ui echo?
        (uiEchoManager.decorateEventWithReactionUiEcho(it) ?: it)
    }

    /**
     * This has to be called on TimelineThread as it accesses realm live results
     */
    private fun getOffsetResults(startDisplayIndex: Int,
                                 direction: Timeline.Direction,
                                 count: Long): RealmResults<TimelineEventEntity> {
        val offsetQuery = filteredEvents.where()
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
                    .equalTo("${TimelineEventEntityFields.CHUNK}.${ChunkEntityFields.IS_LAST_FORWARD}", true)
        } else {
            TimelineEventEntity
                    .whereRoomId(realm, roomId = roomId)
                    .`in`("${TimelineEventEntityFields.CHUNK}.${ChunkEntityFields.TIMELINE_EVENTS.EVENT_ID}", arrayOf(initialEventId))
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
            updateLoadingStates(filteredEvents)
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
        backwardsState.set(State())
        forwardsState.set(State())
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

    private fun RealmQuery<TimelineEventEntity>.filterEventsWithSettings(): RealmQuery<TimelineEventEntity> {
        return filterEvents(settings.filters)
    }

    private fun List<TimelineEvent>.filterEventsWithSettings(): List<TimelineEvent> {
        return filter { event ->
            val filterType = !settings.filters.filterTypes
                    || settings.filters.allowedTypes.any { it.eventType == event.root.type && (it.stateKey == null || it.stateKey == event.root.senderId) }
            if (!filterType) return@filter false

            val filterEdits = if (settings.filters.filterEdits && event.root.getClearType() == EventType.MESSAGE) {
                val messageContent = event.root.getClearContent().toModel<MessageContent>()
                messageContent?.relatesTo?.type != RelationType.REPLACE && messageContent?.relatesTo?.type != RelationType.RESPONSE
            } else {
                true
            }
            if (!filterEdits) return@filter false

            val filterRedacted = settings.filters.filterRedacted && event.root.isRedacted()
            !filterRedacted
        }
    }

    private data class State(
            val hasReachedEnd: Boolean = false,
            val hasMoreInCache: Boolean = true,
            val isPaginating: Boolean = false,
            val requestedPaginationCount: Int = 0
    )

    private data class ReactionUiEchoData(
            val localEchoId: String,
            val reactedOnEventId: String,
            val reaction: String
    )

    inner class UIEchoManager {

        private val inMemorySendingEvents = Collections.synchronizedList<TimelineEvent>(ArrayList())

        fun getInMemorySendingEvents(): List<TimelineEvent> {
            return inMemorySendingEvents.toList()
        }

        /**
         * Due to lag of DB updates, we keep some UI echo of some properties to update timeline faster
         */
        private val inMemorySendingStates = Collections.synchronizedMap<String, SendState>(HashMap())

        private val inMemoryReactions = Collections.synchronizedMap<String, MutableList<ReactionUiEchoData>>(HashMap())

        fun sentEventsUpdated(events: RealmResults<TimelineEventEntity>) {
            // Remove in memory as soon as they are known by database
            events.forEach { te ->
                inMemorySendingEvents.removeAll { te.eventId == it.eventId }
            }
            inMemorySendingStates.keys.removeAll { key ->
                events.find { it.eventId == key } == null
            }

            inMemoryReactions.forEach { (_, uiEchoData) ->
                uiEchoData.removeAll { data ->
                    // I remove the uiEcho, when the related event is not anymore in the sending list
                    // (means that it is synced)!
                    events.find { it.eventId == data.localEchoId } == null
                }
            }
        }

        fun onLocalEchoUpdated(onLocalEchoUpdated: OnLocalEchoUpdated): Boolean {
            if (isLive && onLocalEchoUpdated.roomId == roomId) {
                val existingState = inMemorySendingStates[onLocalEchoUpdated.eventId]
                inMemorySendingStates[onLocalEchoUpdated.eventId] = onLocalEchoUpdated.sendState
                if (existingState != onLocalEchoUpdated.sendState) {
                    return true
                }
            }
            return false
        }

        // return true if should update
        fun onLocalEchoCreated(onLocalEchoCreated: OnLocalEchoCreated): Boolean {
            var postSnapshot = false
            if (isLive && onLocalEchoCreated.roomId == roomId) {
                // Manage some ui echos (do it before filter because actual event could be filtered out)
                when (onLocalEchoCreated.timelineEvent.root.getClearType()) {
                    EventType.REDACTION -> {
                    }
                    EventType.REACTION  -> {
                        val content = onLocalEchoCreated.timelineEvent.root.content?.toModel<ReactionContent>()
                        if (RelationType.ANNOTATION == content?.relatesTo?.type) {
                            val reaction = content.relatesTo.key
                            val relatedEventID = content.relatesTo.eventId
                            inMemoryReactions.getOrPut(relatedEventID) { mutableListOf() }
                                    .add(
                                            ReactionUiEchoData(
                                                    localEchoId = onLocalEchoCreated.timelineEvent.eventId,
                                                    reactedOnEventId = relatedEventID,
                                                    reaction = reaction
                                            )
                                    )
                            postSnapshot = rebuildEvent(relatedEventID) {
                                decorateEventWithReactionUiEcho(it)
                            } || postSnapshot
                        }
                    }
                }

                // do not add events that would have been filtered
                if (listOf(onLocalEchoCreated.timelineEvent).filterEventsWithSettings().isNotEmpty()) {
                    listeners.forEach {
                        it.onNewTimelineEvents(listOf(onLocalEchoCreated.timelineEvent.eventId))
                    }
                    Timber.v("On local echo created: ${onLocalEchoCreated.timelineEvent.eventId}")
                    inMemorySendingEvents.add(0, onLocalEchoCreated.timelineEvent)
                    postSnapshot = true
                }
            }
            return postSnapshot
        }

        fun decorateEventWithReactionUiEcho(timelineEvent: TimelineEvent): TimelineEvent? {
            val relatedEventID = timelineEvent.eventId
            val contents = inMemoryReactions[relatedEventID] ?: return null

            var existingAnnotationSummary = timelineEvent.annotations ?: EventAnnotationsSummary(
                    relatedEventID
            )
            val updateReactions = existingAnnotationSummary.reactionsSummary.toMutableList()

            contents.forEach { uiEchoReaction ->
                val existing = updateReactions.firstOrNull { it.key == uiEchoReaction.reaction }
                if (existing == null) {
                    // just add the new key
                    ReactionAggregatedSummary(
                            key = uiEchoReaction.reaction,
                            count = 1,
                            addedByMe = true,
                            firstTimestamp = System.currentTimeMillis(),
                            sourceEvents = emptyList(),
                            localEchoEvents = listOf(uiEchoReaction.localEchoId)
                    ).let { updateReactions.add(it) }
                } else {
                    // update Existing Key
                    if (!existing.localEchoEvents.contains(uiEchoReaction.localEchoId)) {
                        updateReactions.remove(existing)
                        // only update if echo is not yet there
                        ReactionAggregatedSummary(
                                key = existing.key,
                                count = existing.count + 1,
                                addedByMe = true,
                                firstTimestamp = existing.firstTimestamp,
                                sourceEvents = existing.sourceEvents,
                                localEchoEvents = existing.localEchoEvents + uiEchoReaction.localEchoId

                        ).let { updateReactions.add(it) }
                    }
                }
            }

            existingAnnotationSummary = existingAnnotationSummary.copy(
                    reactionsSummary = updateReactions
            )
            return timelineEvent.copy(
                    annotations = existingAnnotationSummary
            )
        }

        fun updateSentStateWithUiEcho(element: TimelineEvent) {
            inMemorySendingStates[element.eventId]?.let {
                // Timber.v("## ${System.currentTimeMillis()} Send event refresh echo with live state ${it} from state ${element.root.sendState}")
                element.root.sendState = element.root.sendState.takeIf { it == SendState.SENT } ?: it
            }
        }

        fun onSyncedEvent(transactionId: String?) {
            val sendingEvent = inMemorySendingEvents.find {
                it.eventId == transactionId
            }
            inMemorySendingEvents.remove(sendingEvent)
            // Is it too early to clear it? will be done when removed from sending anyway?
            inMemoryReactions.forEach { (_, u) ->
                u.filterNot { it.localEchoId == transactionId }
            }
        }
    }
}
