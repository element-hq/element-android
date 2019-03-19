/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.matrix.android.internal.session.room.timeline

import android.os.Handler
import android.os.HandlerThread
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.util.CancelableBag
import im.vector.matrix.android.api.util.addTo
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.ChunkEntityFields
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.EventEntityFields
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.PagingRequestHelper
import io.realm.*
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.ArrayList


private const val INITIAL_LOAD_SIZE = 20
private const val THREAD_NAME = "TIMELINE_DB_THREAD"

internal class DefaultTimeline(
        private val roomId: String,
        private val initialEventId: String? = null,
        private val realmConfiguration: RealmConfiguration,
        private val taskExecutor: TaskExecutor,
        private val contextOfEventTask: GetContextOfEventTask,
        private val timelineEventFactory: TimelineEventFactory,
        private val paginationTask: PaginationTask,
        private val helper: PagingRequestHelper
) : Timeline {

    override var listener: Timeline.Listener? = null
        set(value) {
            field = value
            listener?.onUpdated(snapshot())
        }

    private val isStarted = AtomicBoolean(false)
    private val handlerThread = AtomicReference<HandlerThread>()
    private val handler = AtomicReference<Handler>()
    private val realm = AtomicReference<Realm>()

    private val cancelableBag = CancelableBag()
    private lateinit var liveEvents: RealmResults<EventEntity>
    private var prevDisplayIndex: Int = 0
    private var nextDisplayIndex: Int = 0
    private val isLive = initialEventId == null
    private val builtEvents = Collections.synchronizedList<TimelineEvent>(ArrayList())

    private val eventsChangeListener = OrderedRealmCollectionChangeListener<RealmResults<EventEntity>> { _, changeSet ->
        if (changeSet.state == OrderedCollectionChangeSet.State.INITIAL) {
            handleInitialLoad()
        } else {
            changeSet.insertionRanges.forEach {
                val (startDisplayIndex, direction) = if (it.startIndex == 0) {
                    Pair(liveEvents[it.length - 1]!!.displayIndex, Timeline.Direction.FORWARDS)
                } else {
                    Pair(liveEvents[it.startIndex]!!.displayIndex, Timeline.Direction.BACKWARDS)
                }
                insertFromLiveResults(startDisplayIndex, direction, it.length.toLong())
            }
        }
    }

    override fun paginate(direction: Timeline.Direction, count: Int) {
        handler.get()?.post {
            if (!hasMoreToLoadLive(direction) && hasReachedEndLive(direction)) {
                return@post
            }
            val startDisplayIndex = if (direction == Timeline.Direction.BACKWARDS) prevDisplayIndex else nextDisplayIndex
            val builtCountItems = insertFromLiveResults(startDisplayIndex, direction, count.toLong())
            if (builtCountItems < count) {
                val limit = count - builtCountItems
                val token = getTokenLive(direction) ?: return@post
                helper.runIfNotRunning(direction.toRequestType()) { executePaginationTask(it, token, direction, limit) }
            }
        }
    }

    override fun start() {
        if (isStarted.compareAndSet(false, true)) {
            val handlerThread = HandlerThread(THREAD_NAME)
            handlerThread.start()
            val handler = Handler(handlerThread.looper)
            this.handlerThread.set(handlerThread)
            this.handler.set(handler)
            handler.post {
                val realm = Realm.getInstance(realmConfiguration)
                this.realm.set(realm)
                liveEvents = buildEventQuery(realm).findAllAsync()
                liveEvents.addChangeListener(eventsChangeListener)
            }
        }

    }

    override fun dispose() {
        if (isStarted.compareAndSet(true, false)) {
            handler.get()?.post {
                cancelableBag.cancel()
                liveEvents.removeAllChangeListeners()
                realm.getAndSet(null)?.close()
                handler.set(null)
                handlerThread.getAndSet(null)?.quit()
            }
        }
    }

    override fun snapshot(): List<TimelineEvent> = synchronized(builtEvents) {
        return builtEvents.toList()
    }

    override fun size(): Int = synchronized(builtEvents) {
        return builtEvents.size
    }

    override fun hasReachedEnd(direction: Timeline.Direction): Boolean {
        return handler.get()?.postAndWait {
            hasReachedEndLive(direction)
        } ?: false
    }

    override fun hasMoreToLoad(direction: Timeline.Direction): Boolean {
        return handler.get()?.postAndWait {
            hasMoreToLoadLive(direction)
        } ?: false
    }

    /**
     * This has to be called on TimelineThread as it access realm live results
     */
    private fun handleInitialLoad() = synchronized(builtEvents) {
        val initialDisplayIndex = if (isLive) {
            liveEvents.firstOrNull()?.displayIndex
        } else {
            liveEvents.where().equalTo(EventEntityFields.EVENT_ID, initialEventId).findFirst()?.displayIndex
        } ?: 0
        prevDisplayIndex = initialDisplayIndex
        nextDisplayIndex = initialDisplayIndex
        val count = Math.min(INITIAL_LOAD_SIZE, liveEvents.size).toLong()
        if (count == 0L) {
            return@synchronized
        }
        if (isLive) {
            insertFromLiveResults(initialDisplayIndex, Timeline.Direction.BACKWARDS, count)
        } else {
            val forwardCount = count / 2L
            val backwardCount = count - forwardCount
            insertFromLiveResults(initialDisplayIndex, Timeline.Direction.BACKWARDS, backwardCount)
            insertFromLiveResults(initialDisplayIndex, Timeline.Direction.BACKWARDS, forwardCount)
        }
    }

    private fun executePaginationTask(requestCallback: PagingRequestHelper.Request.Callback,
                                      from: String,
                                      direction: Timeline.Direction,
                                      limit: Int) {

        val params = PaginationTask.Params(roomId = roomId,
                from = from,
                direction = direction.toPaginationDirection(),
                limit = limit)

        paginationTask.configureWith(params)
                .enableRetry()
                .dispatchTo(object : MatrixCallback<TokenChunkEventPersistor.Result> {
                    override fun onSuccess(data: TokenChunkEventPersistor.Result) {
                        requestCallback.recordSuccess()
                        if (data == TokenChunkEventPersistor.Result.SHOULD_FETCH_MORE) {
                            paginate(direction, limit)
                        }
                    }

                    override fun onFailure(failure: Throwable) {
                        requestCallback.recordFailure(failure)
                    }
                })
                .executeBy(taskExecutor)
                .addTo(cancelableBag)
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
    private fun hasReachedEndLive(direction: Timeline.Direction): Boolean {
        val liveChunk = getLiveChunk() ?: return false
        return if (direction == Timeline.Direction.FORWARDS) {
            liveChunk.isLastForward
        } else {
            liveChunk.isLastBackward || liveEvents.lastOrNull()?.type == EventType.STATE_ROOM_CREATE
        }
    }

    /**
     * This has to be called on TimelineThread as it access realm live results
     */
    private fun hasMoreToLoadLive(direction: Timeline.Direction): Boolean {
        if (liveEvents.isEmpty()) {
            return true
        }
        return if (direction == Timeline.Direction.FORWARDS) {
            builtEvents.firstOrNull()?.displayIndex != liveEvents.firstOrNull()?.displayIndex
        } else {
            builtEvents.lastOrNull()?.displayIndex != liveEvents.lastOrNull()?.displayIndex
        }
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
    private fun insertFromLiveResults(startDisplayIndex: Int,
                                      direction: Timeline.Direction,
                                      count: Long): Int = synchronized(builtEvents) {
        if (count < 1) {
            throw java.lang.IllegalStateException("You should provide a count superior to 0")
        }
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
            val timelineEvent = timelineEventFactory.create(eventEntity)
            val position = if (direction == Timeline.Direction.FORWARDS) 0 else builtEvents.size
            builtEvents.add(position, timelineEvent)
        }
        listener?.onUpdated(snapshot())
        return offsetResults.size
    }

    /**
     * This has to be called on TimelineThread as it access realm live results
     */
    private fun getOffsetResults(startDisplayIndex: Int,
                                 direction: Timeline.Direction,
                                 count: Long): RealmResults<EventEntity> {
        val offsetQuery = liveEvents.where()
        if (direction == Timeline.Direction.BACKWARDS) {
            offsetQuery
                    .sort(EventEntityFields.DISPLAY_INDEX, Sort.DESCENDING)
                    .lessThanOrEqualTo(EventEntityFields.DISPLAY_INDEX, startDisplayIndex)
        } else {
            offsetQuery
                    .sort(EventEntityFields.DISPLAY_INDEX, Sort.ASCENDING)
                    .greaterThanOrEqualTo(EventEntityFields.DISPLAY_INDEX, startDisplayIndex)
        }
        return offsetQuery.limit(count).findAll()
    }

    private fun buildEventQuery(realm: Realm): RealmQuery<EventEntity> {
        val query = if (initialEventId == null) {
            EventEntity
                    .where(realm, roomId = roomId, linkFilterMode = EventEntity.LinkFilterMode.LINKED_ONLY)
                    .equalTo("${EventEntityFields.CHUNK}.${ChunkEntityFields.IS_LAST_FORWARD}", true)
        } else {
            EventEntity
                    .where(realm, roomId = roomId, linkFilterMode = EventEntity.LinkFilterMode.BOTH)
                    .`in`("${EventEntityFields.CHUNK}.${ChunkEntityFields.EVENTS.EVENT_ID}", arrayOf(initialEventId))
        }
        query.sort(EventEntityFields.DISPLAY_INDEX, Sort.DESCENDING)
        return query

    }

    private fun <T> Handler.postAndWait(runnable: () -> T): T {
        val lock = CountDownLatch(1)
        val atomicReference = AtomicReference<T>()
        post {
            val result = runnable()
            atomicReference.set(result)
            lock.countDown()
        }
        lock.await()
        return atomicReference.get()
    }

    private fun Timeline.Direction.toRequestType(): PagingRequestHelper.RequestType {
        return if (this == Timeline.Direction.BACKWARDS) PagingRequestHelper.RequestType.BEFORE else PagingRequestHelper.RequestType.AFTER
    }

    //Todo : remove that
    private fun Timeline.Direction.toPaginationDirection(): PaginationDirection {
        return if (this == Timeline.Direction.BACKWARDS) PaginationDirection.BACKWARDS else PaginationDirection.FORWARDS
    }

}
