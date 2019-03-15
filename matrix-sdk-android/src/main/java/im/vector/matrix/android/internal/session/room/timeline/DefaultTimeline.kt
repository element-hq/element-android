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

import androidx.annotation.UiThread
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.internal.database.model.ChunkEntityFields
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.EventEntityFields
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.PagingRequestHelper
import io.realm.OrderedCollectionChangeSet
import io.realm.OrderedRealmCollectionChangeListener
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmQuery
import io.realm.RealmResults
import io.realm.Sort
import java.util.*
import kotlin.collections.ArrayList


private const val INITIAL_LOAD_SIZE = 30

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

    private lateinit var realm: Realm
    private lateinit var liveEvents: RealmResults<EventEntity>
    private var prevDisplayIndex: Int = 0
    private var nextDisplayIndex: Int = 0
    private val isLive = initialEventId == null
    private val builtEvents = Collections.synchronizedList<TimelineEvent>(ArrayList())


    private val changeListener = OrderedRealmCollectionChangeListener<RealmResults<EventEntity>> { _, changeSet ->
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

    @UiThread
    override fun paginate(direction: Timeline.Direction, count: Int) {
        if (direction == Timeline.Direction.FORWARDS && isLive) {
            return
        }
        val startDisplayIndex = if (direction == Timeline.Direction.BACKWARDS) prevDisplayIndex else nextDisplayIndex
        val hasBuiltCountItems = insertFromLiveResults(startDisplayIndex, direction, count.toLong())
        if (hasBuiltCountItems.not()) {
            val token = getToken(direction) ?: return
            helper.runIfNotRunning(direction.toRequestType()) {
                executePaginationTask(it, token, direction.toPaginationDirection(), 30)
            }
        }
    }

    @UiThread
    override fun start() {
        realm = Realm.getInstance(realmConfiguration)
        liveEvents = buildQuery(initialEventId).findAllAsync()
        liveEvents.addChangeListener(changeListener)
    }

    @UiThread
    override fun dispose() {
        liveEvents.removeAllChangeListeners()
        realm.close()
    }

    override fun snapshot(): List<TimelineEvent> = synchronized(builtEvents) {
        return builtEvents.toList()
    }

    override fun size(): Int = synchronized(builtEvents) {
        return builtEvents.size
    }

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
                                      direction: PaginationDirection,
                                      limit: Int) {

        val params = PaginationTask.Params(roomId = roomId,
                                           from = from,
                                           direction = direction,
                                           limit = limit)

        paginationTask.configureWith(params)
                .enableRetry()
                .dispatchTo(object : MatrixCallback<Boolean> {
                    override fun onSuccess(data: Boolean) {
                        requestCallback.recordSuccess()
                    }

                    override fun onFailure(failure: Throwable) {
                        requestCallback.recordFailure(failure)
                    }
                })
                .executeBy(taskExecutor)
    }

    private fun getToken(direction: Timeline.Direction): String? {
        val chunkEntity = liveEvents.firstOrNull()?.chunk?.firstOrNull() ?: return null
        return if (direction == Timeline.Direction.BACKWARDS) chunkEntity.prevToken else chunkEntity.nextToken
    }

    /**
     * This has to be called on MonarchyThread as it access realm live results
     * @return true if count items has been added
     */
    private fun insertFromLiveResults(startDisplayIndex: Int,
                                      direction: Timeline.Direction,
                                      count: Long): Boolean = synchronized(builtEvents) {
        if (count < 1) {
            throw java.lang.IllegalStateException("You should provide a count superior to 0")
        }
        val offsetResults = getOffsetResults(startDisplayIndex, direction, count)
        if (offsetResults.isEmpty()) {
            return false
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
        return offsetResults.size.toLong() == count
    }

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

    private fun buildQuery(eventId: String?): RealmQuery<EventEntity> {
        val query = if (eventId == null) {
            EventEntity
                    .where(realm, roomId = roomId, linkFilterMode = EventEntity.LinkFilterMode.LINKED_ONLY)
                    .equalTo("${EventEntityFields.CHUNK}.${ChunkEntityFields.IS_LAST}", true)
        } else {
            EventEntity
                    .where(realm, roomId = roomId, linkFilterMode = EventEntity.LinkFilterMode.BOTH)
                    .`in`("${EventEntityFields.CHUNK}.${ChunkEntityFields.EVENTS.EVENT_ID}", arrayOf(eventId))
        }
        query.sort(EventEntityFields.DISPLAY_INDEX, Sort.DESCENDING)
        return query

    }

    private fun Timeline.Direction.toRequestType(): PagingRequestHelper.RequestType {
        return if (this == Timeline.Direction.BACKWARDS) PagingRequestHelper.RequestType.BEFORE else PagingRequestHelper.RequestType.AFTER
    }

    //Todo : remove that
    private fun Timeline.Direction.toPaginationDirection(): PaginationDirection {
        return if (this == Timeline.Direction.BACKWARDS) PaginationDirection.BACKWARDS else PaginationDirection.FORWARDS
    }
}