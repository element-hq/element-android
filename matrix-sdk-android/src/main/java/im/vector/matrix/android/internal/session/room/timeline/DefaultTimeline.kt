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
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.util.CancelableBag
import im.vector.matrix.android.internal.database.mapper.TimelineEventMapper
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.*
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.query.*
import im.vector.matrix.android.internal.task.TaskConstraints
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.Debouncer
import im.vector.matrix.android.internal.util.createBackgroundHandler
import im.vector.matrix.android.internal.util.createUIHandler
import io.realm.*
import timber.log.Timber
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


private const val INITIAL_LOAD_SIZE = 30
private const val MIN_FETCHING_COUNT = 30
private const val DISPLAY_INDEX_UNKNOWN = Int.MIN_VALUE

internal class DefaultTimeline(
        private val roomId: String,
        private val initialEventId: String? = null,
        private val realmConfiguration: RealmConfiguration,
        private val taskExecutor: TaskExecutor,
        private val contextOfEventTask: GetContextOfEventTask,
        private val paginationTask: PaginationTask,
        private val cryptoService: CryptoService,
        private val timelineEventMapper: TimelineEventMapper,
        private val allowedTypes: List<String>?
) : Timeline {

    private companion object {
        val BACKGROUND_HANDLER = createBackgroundHandler("TIMELINE_DB_THREAD")
    }

    override var listener: Timeline.Listener? = null
        set(value) {
            field = value
            BACKGROUND_HANDLER.post {
                postSnapshot()
            }
        }

    private val isStarted = AtomicBoolean(false)
    private val isReady = AtomicBoolean(false)
    private val mainHandler = createUIHandler()
    private val backgroundRealm = AtomicReference<Realm>()
    private val cancelableBag = CancelableBag()
    private val debouncer = Debouncer(mainHandler)

    private lateinit var liveEvents: RealmResults<TimelineEventEntity>
    private var roomEntity: RoomEntity? = null

    private var prevDisplayIndex: Int = DISPLAY_INDEX_UNKNOWN
    private var nextDisplayIndex: Int = DISPLAY_INDEX_UNKNOWN
    private val isLive = initialEventId == null
    private val builtEvents = Collections.synchronizedList<TimelineEvent>(ArrayList())
    private val builtEventsIdMap = Collections.synchronizedMap(HashMap<String, Int>())
    private val backwardsPaginationState = AtomicReference(PaginationState())
    private val forwardsPaginationState = AtomicReference(PaginationState())


    private val timelineID = UUID.randomUUID().toString()

    private lateinit var eventRelations: RealmResults<EventAnnotationsSummaryEntity>

    private val eventDecryptor = TimelineEventDecryptor(realmConfiguration, timelineID, cryptoService)

    private val eventsChangeListener = OrderedRealmCollectionChangeListener<RealmResults<TimelineEventEntity>> { results, changeSet ->
        if (changeSet.state == OrderedCollectionChangeSet.State.INITIAL) {
            handleInitialLoad()
        } else {
            // If changeSet has deletion we are having a gap, so we clear everything
            if (changeSet.deletionRanges.isNotEmpty()) {
                prevDisplayIndex = DISPLAY_INDEX_UNKNOWN
                nextDisplayIndex = DISPLAY_INDEX_UNKNOWN
                builtEvents.clear()
                builtEventsIdMap.clear()
            }
            changeSet.insertionRanges.forEach { range ->
                val (startDisplayIndex, direction) = if (range.startIndex == 0) {
                    Pair(liveEvents[range.length - 1]!!.root!!.displayIndex, Timeline.Direction.FORWARDS)
                } else {
                    Pair(liveEvents[range.startIndex]!!.root!!.displayIndex, Timeline.Direction.BACKWARDS)
                }
                val state = getPaginationState(direction)
                if (state.isPaginating) {
                    // We are getting new items from pagination
                    val shouldPostSnapshot = paginateInternal(startDisplayIndex, direction, state.requestedCount)
                    if (shouldPostSnapshot) {
                        postSnapshot()
                    }
                } else {
                    // We are getting new items from sync
                    buildTimelineEvents(startDisplayIndex, direction, range.length.toLong())
                    postSnapshot()
                }
            }

            var hasChanged = false
            changeSet.changes.forEach { index ->
                val eventEntity = results[index]
                eventEntity?.eventId?.let { eventId ->
                    builtEventsIdMap[eventId]?.let { builtIndex ->
                        //Update the relation of existing event
                        builtEvents[builtIndex]?.let { te ->
                            builtEvents[builtIndex] = timelineEventMapper.map(eventEntity)
                            hasChanged = true
                        }
                    }
                }
            }
            if (hasChanged) postSnapshot()
        }
    }

    private val relationsListener = OrderedRealmCollectionChangeListener<RealmResults<EventAnnotationsSummaryEntity>> { collection, changeSet ->

        var hasChange = false

        (changeSet.insertions + changeSet.changes).forEach {
            val eventRelations = collection[it]
            if (eventRelations != null) {
                builtEventsIdMap[eventRelations.eventId]?.let { builtIndex ->
                    //Update the relation of existing event
                    builtEvents[builtIndex]?.let { te ->
                        builtEvents[builtIndex] = te.copy(annotations = eventRelations.asDomain())
                        hasChange = true
                    }
                }
            }
        }
        if (hasChange)
            postSnapshot()
    }

//    private val newSessionListener = object : NewSessionListener {
//        override fun onNewSession(roomId: String?, senderKey: String, sessionId: String) {
//            if (roomId == this@DefaultTimeline.roomId) {
//                Timber.v("New session id detected for this room")
//                BACKGROUND_HANDLER.post {
//                    val realm = backgroundRealm.get()
//                    var hasChange = false
//                    builtEvents.forEachIndexed { index, timelineEvent ->
//                        if (timelineEvent.isEncrypted()) {
//                            val eventContent = timelineEvent.root.content.toModel<EncryptedEventContent>()
//                            if (eventContent?.sessionId == sessionId
//                                    && (timelineEvent.root.mClearEvent == null || timelineEvent.root.mCryptoError != null)) {
//                                //we need to rebuild this event
//                                EventEntity.where(realm, eventId = timelineEvent.root.eventId!!).findFirst()?.let {
//                                    //builtEvents[index] = timelineEventFactory.create(it, realm)
//                                    hasChange = true
//                                }
//                            }
//                        }
//                    }
//                    if (hasChange) postSnapshot()
//                }
//            }
//        }
//
//    }

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
            eventDecryptor.start()
            BACKGROUND_HANDLER.post {
                val realm = Realm.getInstance(realmConfiguration)
                backgroundRealm.set(realm)
                clearUnlinkedEvents(realm)


                roomEntity = RoomEntity.where(realm, roomId = roomId).findFirst()?.also {
                    it.sendingTimelineEvents.addChangeListener { _ ->
                        postSnapshot()
                    }
                }

                liveEvents = buildEventQuery(realm)
                        .sort(TimelineEventEntityFields.ROOT.DISPLAY_INDEX, Sort.DESCENDING)
                        .findAllAsync()
                        .also { it.addChangeListener(eventsChangeListener) }

                isReady.set(true)

                eventRelations = EventAnnotationsSummaryEntity.whereInRoom(realm, roomId)
                        .findAllAsync()
                        .also { it.addChangeListener(relationsListener) }
            }
        }
    }

    override fun dispose() {
        if (isStarted.compareAndSet(true, false)) {
            eventDecryptor.destroy()
            Timber.v("Dispose timeline for roomId: $roomId and eventId: $initialEventId")
            BACKGROUND_HANDLER.post {
                cancelableBag.cancel()
                roomEntity?.sendingTimelineEvents?.removeAllChangeListeners()
                eventRelations.removeAllChangeListeners()
                liveEvents.removeAllChangeListeners()
                backgroundRealm.getAndSet(null).also {
                    it.close()
                }
            }
        }
    }

    override fun hasMoreToLoad(direction: Timeline.Direction): Boolean {
        return hasMoreInCache(direction) || !hasReachedEnd(direction)
    }

// Private methods *****************************************************************************

    private fun hasMoreInCache(direction: Timeline.Direction): Boolean {
        return Realm.getInstance(realmConfiguration).use { localRealm ->
            val timelineEventEntity = buildEventQuery(localRealm).findFirst(direction)
                    ?: return false
            if (direction == Timeline.Direction.FORWARDS) {
                if (findCurrentChunk(localRealm)?.isLastForward == true) {
                    return false
                }
                val firstEvent = builtEvents.firstOrNull() ?: return true
                firstEvent.displayIndex < timelineEventEntity.root!!.displayIndex
            } else {
                val lastEvent = builtEvents.lastOrNull() ?: return true
                lastEvent.displayIndex > timelineEventEntity.root!!.displayIndex
            }
        }
    }

    private fun hasReachedEnd(direction: Timeline.Direction): Boolean {
        return Realm.getInstance(realmConfiguration).use { localRealm ->
            val currentChunk = findCurrentChunk(localRealm) ?: return false
            if (direction == Timeline.Direction.FORWARDS) {
                currentChunk.isLastForward
            } else {
                val eventEntity = buildEventQuery(localRealm).findFirst(direction)
                currentChunk.isLastBackward || eventEntity?.root?.type == EventType.STATE_ROOM_CREATE
            }
        }
    }


    /**
     * This has to be called on TimelineThread as it access realm live results
     * @return true if createSnapshot should be posted
     */
    private fun paginateInternal(startDisplayIndex: Int,
                                 direction: Timeline.Direction,
                                 count: Int): Boolean {
        updatePaginationState(direction) { it.copy(requestedCount = count, isPaginating = true) }
        val builtCount = buildTimelineEvents(startDisplayIndex, direction, count.toLong())
        val shouldFetchMore = builtCount < count && !hasReachedEnd(direction)
        if (shouldFetchMore) {
            val newRequestedCount = count - builtCount
            updatePaginationState(direction) { it.copy(requestedCount = newRequestedCount) }
            val fetchingCount = Math.max(MIN_FETCHING_COUNT, newRequestedCount)
            executePaginationTask(direction, fetchingCount)
        } else {
            updatePaginationState(direction) { it.copy(isPaginating = false, requestedCount = 0) }
        }

        return !shouldFetchMore
    }

    private fun createSnapshot(): List<TimelineEvent> {
        return buildSendingEvents() + builtEvents.toList()
    }

    private fun buildSendingEvents(): List<TimelineEvent> {
        val sendingEvents = ArrayList<TimelineEvent>()
        if (hasReachedEnd(Timeline.Direction.FORWARDS)) {
            roomEntity?.sendingTimelineEvents
                    ?.filter { allowedTypes?.contains(it.root?.type) ?: false }
                    ?.forEach {
                        sendingEvents.add(timelineEventMapper.map(it))
                    }
        }
        return sendingEvents
    }

    private fun canPaginate(direction: Timeline.Direction): Boolean {
        return isReady.get() && !getPaginationState(direction).isPaginating && hasMoreToLoad(direction)
    }

    private fun getPaginationState(direction: Timeline.Direction): PaginationState {
        return when (direction) {
            Timeline.Direction.FORWARDS  -> forwardsPaginationState.get()
            Timeline.Direction.BACKWARDS -> backwardsPaginationState.get()
        }
    }

    private fun updatePaginationState(direction: Timeline.Direction, update: (PaginationState) -> PaginationState) {
        val stateReference = when (direction) {
            Timeline.Direction.FORWARDS  -> forwardsPaginationState
            Timeline.Direction.BACKWARDS -> backwardsPaginationState
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
        val initialDisplayIndex = if (isLive) {
            liveEvents.firstOrNull()?.root?.displayIndex
        } else {
            val initialEvent = liveEvents.where()
                    .equalTo(TimelineEventEntityFields.EVENT_ID, initialEventId)
                    .findFirst()
            shouldFetchInitialEvent = initialEvent == null
            initialEvent?.root?.displayIndex
        } ?: DISPLAY_INDEX_UNKNOWN

        prevDisplayIndex = initialDisplayIndex
        nextDisplayIndex = initialDisplayIndex
        if (initialEventId != null && shouldFetchInitialEvent) {
            fetchEvent(initialEventId)
        } else {
            val count = Math.min(INITIAL_LOAD_SIZE, liveEvents.size)
            if (isLive) {
                paginateInternal(initialDisplayIndex, Timeline.Direction.BACKWARDS, count)
            } else {
                paginateInternal(initialDisplayIndex, Timeline.Direction.FORWARDS, count / 2)
                paginateInternal(initialDisplayIndex, Timeline.Direction.BACKWARDS, count / 2)
            }
        }
        postSnapshot()
    }

    /**
     * This has to be called on TimelineThread as it access realm live results
     */
    private fun executePaginationTask(direction: Timeline.Direction, limit: Int) {
        val token = getTokenLive(direction) ?: return
        val params = PaginationTask.Params(roomId = roomId,
                from = token,
                direction = direction.toPaginationDirection(),
                limit = limit)

        Timber.v("Should fetch $limit items $direction")
        cancelableBag += paginationTask
                .configureWith(params) {
                    this.retryCount = Int.MAX_VALUE
                    this.constraints = TaskConstraints(connectedToNetwork = true)
                    this.callback = object : MatrixCallback<TokenChunkEventPersistor.Result> {
                        override fun onSuccess(data: TokenChunkEventPersistor.Result) {
                            if (data == TokenChunkEventPersistor.Result.SUCCESS) {
                                Timber.v("Success fetching $limit items $direction from pagination request")
                            } else {
                                // Database won't be updated, so we force pagination request
                                BACKGROUND_HANDLER.post {
                                    executePaginationTask(direction, limit)
                                }
                            }
                        }

                        override fun onFailure(failure: Throwable) {
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
        return liveEvents.firstOrNull()?.chunk?.firstOrNull()
    }

    /**
     * This has to be called on TimelineThread as it access realm live results
     * @return number of items who have been added
     */
    private fun buildTimelineEvents(startDisplayIndex: Int,
                                    direction: Timeline.Direction,
                                    count: Long): Int {
        if (count < 1) {
            return 0
        }
        val start = System.currentTimeMillis()
        val offsetResults = getOffsetResults(startDisplayIndex, direction, count)
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
            val timelineEvent = timelineEventMapper.map(eventEntity)

            if (timelineEvent.isEncrypted()
                    && timelineEvent.root.mxDecryptionResult == null) {
                timelineEvent.root.eventId?.let { eventDecryptor.requestDecryption(it) }
            }

            val position = if (direction == Timeline.Direction.FORWARDS) 0 else builtEvents.size
            builtEvents.add(position, timelineEvent)
            //Need to shift :/
            builtEventsIdMap.entries.filter { it.value >= position }.forEach { it.setValue(it.value + 1) }
            builtEventsIdMap[eventEntity.eventId] = position
        }
        val time = System.currentTimeMillis() - start
        Timber.v("Built ${offsetResults.size} items from db in $time ms")
        return offsetResults.size
    }

    /**
     * This has to be called on TimelineThread as it access realm live results
     */
    private fun getOffsetResults(startDisplayIndex: Int,
                                 direction: Timeline.Direction,
                                 count: Long): RealmResults<TimelineEventEntity> {
        val offsetQuery = liveEvents.where()
        if (direction == Timeline.Direction.BACKWARDS) {
            offsetQuery
                    .sort(TimelineEventEntityFields.ROOT.DISPLAY_INDEX, Sort.DESCENDING)
                    .lessThanOrEqualTo(TimelineEventEntityFields.ROOT.DISPLAY_INDEX, startDisplayIndex)
        } else {
            offsetQuery
                    .sort(TimelineEventEntityFields.ROOT.DISPLAY_INDEX, Sort.ASCENDING)
                    .greaterThanOrEqualTo(TimelineEventEntityFields.ROOT.DISPLAY_INDEX, startDisplayIndex)
        }
        return offsetQuery
                .filterAllowedTypes()
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

    private fun findCurrentChunk(realm: Realm): ChunkEntity? {
        return if (initialEventId == null) {
            ChunkEntity.findLastLiveChunkFromRoom(realm, roomId)
        } else {
            ChunkEntity.findIncludingEvent(realm, initialEventId)
        }
    }

    private fun clearUnlinkedEvents(realm: Realm) {
        realm.executeTransaction {
            val unlinkedChunks = ChunkEntity
                    .where(it, roomId = roomId)
                    .equalTo("${ChunkEntityFields.TIMELINE_EVENTS.ROOT}.${EventEntityFields.IS_UNLINKED}", true)
                    .findAll()
            unlinkedChunks.deleteAllFromRealm()
        }
    }

    private fun fetchEvent(eventId: String) {
        val params = GetContextOfEventTask.Params(roomId, eventId)
        contextOfEventTask.configureWith(params).executeBy(taskExecutor)
    }

    private fun postSnapshot() {
        val snapshot = createSnapshot()
        val runnable = Runnable { listener?.onUpdated(snapshot) }
        debouncer.debounce("post_snapshot", runnable, 50)
    }

// Extension methods ***************************************************************************

    private fun Timeline.Direction.toPaginationDirection(): PaginationDirection {
        return if (this == Timeline.Direction.BACKWARDS) PaginationDirection.BACKWARDS else PaginationDirection.FORWARDS
    }

    private fun RealmQuery<TimelineEventEntity>.findFirst(direction: Timeline.Direction): TimelineEventEntity? {
        return if (direction == Timeline.Direction.FORWARDS) {
            sort(TimelineEventEntityFields.ROOT.DISPLAY_INDEX, Sort.DESCENDING)
        } else {
            sort(TimelineEventEntityFields.ROOT.DISPLAY_INDEX, Sort.ASCENDING)
        }
                .filterAllowedTypes()
                .findFirst()
    }

    private fun RealmQuery<TimelineEventEntity>.filterAllowedTypes(): RealmQuery<TimelineEventEntity> {
        if (allowedTypes != null) {
            `in`(TimelineEventEntityFields.ROOT.TYPE, allowedTypes.toTypedArray())
        }
        return this
    }
}

private data class PaginationState(
        val isPaginating: Boolean = false,
        val requestedCount: Int = 0
)
