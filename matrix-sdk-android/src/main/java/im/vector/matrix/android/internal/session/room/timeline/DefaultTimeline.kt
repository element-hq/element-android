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

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.internal.database.RealmLiveEntityObserver
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.ChunkEntityFields
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.EventEntityFields
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.room.members.RoomMemberExtractor
import im.vector.matrix.android.internal.task.TaskExecutor
import io.realm.OrderedCollectionChangeSet
import io.realm.RealmResults
import io.realm.Sort


private const val INITIAL_LOAD_SIZE = 30

internal class DefaultTimeline(
        private val roomId: String,
        private val initialEventId: String? = null,
        private val monarchy: Monarchy,
        private val taskExecutor: TaskExecutor,
        private val boundaryCallback: TimelineBoundaryCallback,
        private val contextOfEventTask: GetContextOfEventTask,
        private val roomMemberExtractor: RoomMemberExtractor
) : Timeline {

    private var prevDisplayIndex: Int = 0
    private var nextDisplayIndex: Int = 0
    private val isLive = initialEventId == null

    private val listeners = mutableListOf<Timeline.Listener>()

    private val builtEvents = mutableListOf<TimelineEvent>()
    private lateinit var liveResults: RealmResults<EventEntity>

    private val entityObserver = object : RealmLiveEntityObserver<EventEntity>(monarchy) {

        override val query: Monarchy.Query<EventEntity>
            get() = buildQuery(initialEventId)

        override fun onChanged(realmResults: RealmResults<EventEntity>, changeSet: OrderedCollectionChangeSet) {
            changeSet.insertionRanges.forEach {
                val (startIndex, direction) = if (it.startIndex == 0) {
                    Pair(realmResults[it.length]!!.displayIndex, Timeline.Direction.FORWARDS)
                } else {
                    Pair(realmResults[it.startIndex]!!.displayIndex, Timeline.Direction.FORWARDS)
                }
                addFromLiveResults(startIndex, direction, it.length.toLong())
            }
        }

        override fun processInitialResults(results: RealmResults<EventEntity>) {
            // Results are ordered DESCENDING, so first items is the most recent
            liveResults = results
            val initialDisplayIndex = if (isLive) {
                results.first()?.displayIndex
            } else {
                results.where().equalTo(EventEntityFields.EVENT_ID, initialEventId).findFirst()?.displayIndex
            } ?: 0
            prevDisplayIndex = initialDisplayIndex
            nextDisplayIndex = initialDisplayIndex
            val count = Math.min(INITIAL_LOAD_SIZE, results.size).toLong()
            if (isLive) {
                addFromLiveResults(initialDisplayIndex, Timeline.Direction.BACKWARDS, count)
            } else {
                val forwardCount = count / 2L
                val backwardCount = count - forwardCount
                addFromLiveResults(initialDisplayIndex, Timeline.Direction.BACKWARDS, backwardCount)
                addFromLiveResults(initialDisplayIndex, Timeline.Direction.BACKWARDS, forwardCount)
            }
        }
    }

    override fun paginate(direction: Timeline.Direction, count: Int) {
        monarchy.postToMonarchyThread {
            val startDisplayIndex = if (direction == Timeline.Direction.BACKWARDS) prevDisplayIndex else nextDisplayIndex
            val shouldHitNetwork = addFromLiveResults(startDisplayIndex, direction, count.toLong()).not()
            if (shouldHitNetwork) {
                if (direction == Timeline.Direction.BACKWARDS) {
                    val itemAtEnd = builtEvents.last()
                    boundaryCallback.onItemAtEndLoaded(itemAtEnd)
                } else {
                    val itemAtFront = builtEvents.first()
                    boundaryCallback.onItemAtFrontLoaded(itemAtFront)
                }
            }
        }
    }

    override fun addListener(listener: Timeline.Listener) {
        if (listeners.isEmpty()) {
            entityObserver.start()
        }
        listeners.add(listener)
    }

    override fun removeListener(listener: Timeline.Listener) {
        listeners.remove(listener)
        if (listeners.isEmpty()) {
            entityObserver.dispose()
        }
    }

    override fun removeAllListeners() {
        listeners.clear()
        if (listeners.isEmpty()) {
            entityObserver.dispose()
        }
    }

    /**
     * @return true if count items has been added
     */
    private fun addFromLiveResults(startDisplayIndex: Int,
                                   direction: Timeline.Direction,
                                   count: Long): Boolean {
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
            val roomMember = roomMemberExtractor.extractFrom(eventEntity)
            val timelineEvent = TimelineEvent(eventEntity.asDomain(), eventEntity.localId, roomMember)
            val position = if (direction == Timeline.Direction.FORWARDS) 0 else builtEvents.size
            builtEvents.add(position, timelineEvent)
        }
        return offsetResults.size.toLong() == count
    }

    private fun getOffsetResults(startDisplayIndex: Int,
                                 direction: Timeline.Direction,
                                 count: Long): RealmResults<EventEntity> {
        val offsetQuery = liveResults.where()
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

    private fun buildQuery(eventId: String?): Monarchy.Query<EventEntity> {
        return Monarchy.Query<EventEntity> { realm ->
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
        }
    }

}