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
import io.realm.RealmObjectChangeListener
import io.realm.RealmQuery
import io.realm.RealmResults
import io.realm.Sort
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.internal.database.mapper.TimelineEventMapper
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.ChunkEntityFields
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntityFields
import timber.log.Timber
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

/**
 * This is the value used to fetch on server. It's better to make constant as otherwise we can have weird chunks with disparate and small chunk of data.
 */
private const val PAGINATION_COUNT = 50

/**
 * This is a wrapper around a ChunkEntity in the database.
 * It does mainly listen to the db timeline events.
 * It also triggers pagination to the server when needed, or dispatch to the prev or next chunk if any.
 */
internal class TimelineChunk constructor(private val chunkEntity: ChunkEntity,
                                         private val roomId: String,
                                         private val timelineId: String,
                                         private val eventDecryptor: TimelineEventDecryptor,
                                         private val paginationTask: PaginationTask,
                                         private val fetchTokenAndPaginateTask: FetchTokenAndPaginateTask,
                                         private val timelineEventMapper: TimelineEventMapper,
                                         private val uiEchoManager: UIEchoManager? = null,
                                         private val initialEventId: String?,
                                         private val onBuiltEvents: () -> Unit) {

    private val isLastForward = AtomicBoolean(chunkEntity.isLastForward)

    private val chunkObjectListener = RealmObjectChangeListener<ChunkEntity> { _, changeSet ->
        Timber.v("on chunk (${chunkEntity.identifier()}) changed: ${changeSet?.changedFields?.joinToString(",")}")
        if(changeSet?.isFieldChanged(ChunkEntityFields.IS_LAST_FORWARD).orFalse()){
            isLastForward.set(chunkEntity.isLastForward)
        }
    }

    private val timelineEventCollectionListener = OrderedRealmCollectionChangeListener { results: RealmResults<TimelineEventEntity>, changeSet: OrderedCollectionChangeSet ->
        val frozenResults = results.freeze()
        Timber.v("on timeline event changed: $changeSet")
        handleDatabaseChangeSet(frozenResults, changeSet)
    }

    private var timelineEventEntities: RealmResults<TimelineEventEntity> = chunkEntity.sortedTimelineEvents()
    private val builtEvents: MutableList<TimelineEvent> = Collections.synchronizedList(ArrayList())
    private val builtEventsIndexes: MutableMap<String, Int> = Collections.synchronizedMap(HashMap<String, Int>())

    private var nextChunk: TimelineChunk? = null
    private var prevChunk: TimelineChunk? = null

    init {
        timelineEventEntities.addChangeListener(timelineEventCollectionListener)
        chunkEntity.addChangeListener(chunkObjectListener)
    }

    fun hasReachedLastForward(): Boolean {
        return if (isLastForward.get()) {
            true
        } else {
            nextChunk?.hasReachedLastForward().orFalse()
        }
    }

    fun builtItems(includesNext: Boolean, includesPrev: Boolean): List<TimelineEvent> {
        val deepBuiltItems = ArrayList<TimelineEvent>(builtEvents.size)
        if (includesNext) {
            val nextEvents = nextChunk?.builtItems(includesNext = true, includesPrev = false).orEmpty()
            deepBuiltItems.addAll(nextEvents)
        }
        deepBuiltItems.addAll(builtEvents)
        if (includesPrev) {
            val prevEvents = prevChunk?.builtItems(includesNext = false, includesPrev = true).orEmpty()
            deepBuiltItems.addAll(prevEvents)
        }
        return deepBuiltItems
    }

    suspend fun loadMore(count: Long, direction: Timeline.Direction): LoadMoreResult {
        val loadFromDbCount = loadFromDb(count, direction)
        val offsetCount = count - loadFromDbCount
        // We have built the right amount of data
        if (offsetCount == 0L) {
            onBuiltEvents()
            return LoadMoreResult.SUCCESS
        }
        return if (direction == Timeline.Direction.FORWARDS) {
            val nextChunkEntity = chunkEntity.nextChunk
            if (nextChunkEntity == null) {
                // Fetch next chunk from server if not in the db
                val token = chunkEntity.nextToken
                fetchFromServer(token, direction)
            } else {
                // otherwise we delegate to the next chunk
                if (nextChunk == null) {
                    nextChunk = createTimelineChunk(nextChunkEntity)
                }
                nextChunk?.loadMore(offsetCount, direction) ?: LoadMoreResult.FAILURE
            }
        } else {
            val prevChunkEntity = chunkEntity.prevChunk
            if (prevChunkEntity == null) {
                // Fetch prev chunk from server if not in the db
                val token = chunkEntity.prevToken
                fetchFromServer(token, direction)
            } else {
                // otherwise we delegate to the prev chunk
                if (prevChunk == null) {
                    prevChunk = createTimelineChunk(prevChunkEntity)
                }
                prevChunk?.loadMore(offsetCount, direction) ?: LoadMoreResult.FAILURE
            }
        }
    }

    fun getBuiltEventIndex(eventId: String, searchInNext: Boolean, searchInPrev: Boolean): Int? {
        val builtEventIndex = builtEventsIndexes[eventId]
        if (builtEventIndex != null) {
            return getOffsetIndex() + builtEventIndex
        }
        if (searchInNext) {
            val nextBuiltEventIndex = nextChunk?.getBuiltEventIndex(eventId, searchInNext = true, searchInPrev = false)
            if (nextBuiltEventIndex != null) {
                return nextBuiltEventIndex
            }
        }
        if (searchInPrev) {
            val prevBuiltEventIndex = prevChunk?.getBuiltEventIndex(eventId, searchInNext = false, searchInPrev = true)
            if (prevBuiltEventIndex != null) {
                return prevBuiltEventIndex
            }
        }
        return null
    }

    fun getBuiltEvent(eventId: String, searchInNext: Boolean, searchInPrev: Boolean): TimelineEvent? {
        val builtEventIndex = builtEventsIndexes[eventId]
        if (builtEventIndex != null) {
            return builtEvents.getOrNull(builtEventIndex)
        }
        if (searchInNext) {
            val nextBuiltEvent = nextChunk?.getBuiltEvent(eventId, searchInNext = true, searchInPrev = false)
            if (nextBuiltEvent != null) {
                return nextBuiltEvent
            }
        }
        if (searchInPrev) {
            val prevBuiltEvent = prevChunk?.getBuiltEvent(eventId, searchInNext = false, searchInPrev = true)
            if (prevBuiltEvent != null) {
                return prevBuiltEvent
            }
        }
        return null
    }

    fun rebuildEvent(eventId: String, builder: (TimelineEvent) -> TimelineEvent?, searchInNext: Boolean, searchInPrev: Boolean): Boolean {
        return tryOrNull {
            val builtIndex = getBuiltEventIndex(eventId, searchInNext = false, searchInPrev = false)
            if (builtIndex == null) {
                val foundInPrev = searchInPrev && prevChunk?.rebuildEvent(eventId, builder, searchInNext = false, searchInPrev = true).orFalse()
                if (foundInPrev) {
                    return true
                }
                if (searchInNext) {
                    return prevChunk?.rebuildEvent(eventId, builder, searchInPrev = false, searchInNext = true).orFalse()
                }
                return false
            }
            // Update the relation of existing event
            builtEvents.getOrNull(builtIndex)?.let { te ->
                val rebuiltEvent = builder(te)
                builtEvents[builtIndex] = rebuiltEvent!!
                true
            }
        }
                ?: false
    }

    fun close(closeNext: Boolean, closePrev: Boolean) {
        if (closeNext) {
            nextChunk?.close(closeNext = true, closePrev = false)
        }
        if (closePrev) {
            prevChunk?.close(closeNext = false, closePrev = true)
        }
        nextChunk = null
        prevChunk = null
        chunkEntity.removeChangeListener(chunkObjectListener)
        timelineEventEntities.removeChangeListener(timelineEventCollectionListener)
    }

    private fun loadFromDb(count: Long, direction: Timeline.Direction): Long {
        val displayIndex = getNextDisplayIndex(direction) ?: return 0
        val baseQuery = timelineEventEntities.where()
        val timelineEvents = baseQuery.offsets(direction, count, displayIndex).findAll().orEmpty()
        if (timelineEvents.isEmpty()) return 0
        if (direction == Timeline.Direction.FORWARDS) {
            builtEventsIndexes.entries.forEach { it.setValue(it.value + timelineEvents.size) }
        }
        timelineEvents
                .mapIndexed { index, timelineEventEntity ->
                    val timelineEvent = timelineEventEntity.buildAndDecryptIfNeeded()
                    if (direction == Timeline.Direction.FORWARDS) {
                        builtEventsIndexes[timelineEvent.eventId] = index
                        builtEvents.add(index, timelineEvent)
                    } else {
                        builtEventsIndexes[timelineEvent.eventId] = builtEvents.size
                        builtEvents.add(timelineEvent)
                    }
                }
        return timelineEvents.size.toLong()
    }

    private fun TimelineEventEntity.buildAndDecryptIfNeeded(): TimelineEvent {
        val timelineEvent = buildTimelineEvent(this)
        val transactionId = timelineEvent.root.unsignedData?.transactionId
        uiEchoManager?.onSyncedEvent(transactionId)
        if (timelineEvent.isEncrypted()
                && timelineEvent.root.mxDecryptionResult == null) {
            timelineEvent.root.eventId?.also { eventDecryptor.requestDecryption(TimelineEventDecryptor.DecryptionRequest(timelineEvent.root, timelineId)) }
        }
        return timelineEvent
    }

    private fun buildTimelineEvent(eventEntity: TimelineEventEntity) = timelineEventMapper.map(
            timelineEventEntity = eventEntity
    ).let {
        // eventually enhance with ui echo?
        (uiEchoManager?.decorateEventWithReactionUiEcho(it) ?: it)
    }

    private fun createTimelineChunk(chunkEntity: ChunkEntity): TimelineChunk {
        return TimelineChunk(
                chunkEntity = chunkEntity,
                timelineId = timelineId,
                eventDecryptor = eventDecryptor,
                roomId = roomId,
                paginationTask = paginationTask,
                fetchTokenAndPaginateTask = fetchTokenAndPaginateTask,
                timelineEventMapper = timelineEventMapper,
                uiEchoManager = uiEchoManager,
                initialEventId = null,
                onBuiltEvents = onBuiltEvents
        )
    }

    private suspend fun fetchFromServer(token: String?, direction: Timeline.Direction): LoadMoreResult {
        val paginationResult = try {
            if (token == null) {
                if (direction == Timeline.Direction.BACKWARDS || !chunkEntity.hasBeenALastForwardChunk()) return LoadMoreResult.REACHED_END
                val lastKnownEventId = chunkEntity.sortedTimelineEvents().firstOrNull()?.eventId ?: return LoadMoreResult.FAILURE
                val taskParams = FetchTokenAndPaginateTask.Params(roomId, lastKnownEventId, direction.toPaginationDirection(), PAGINATION_COUNT)
                fetchTokenAndPaginateTask.execute(taskParams)
            } else {
                val taskParams = PaginationTask.Params(roomId, token, direction.toPaginationDirection(), PAGINATION_COUNT)
                paginationTask.execute(taskParams)
            }
        } catch (failure: Throwable) {
            Timber.e("Failed to fetch from server: $failure", failure)
            return LoadMoreResult.FAILURE
        }
        return paginationResult.toLoadMoreResult()
    }

    private fun TokenChunkEventPersistor.Result.toLoadMoreResult(): LoadMoreResult {
        return when (this) {
            TokenChunkEventPersistor.Result.REACHED_END -> LoadMoreResult.REACHED_END
            TokenChunkEventPersistor.Result.SHOULD_FETCH_MORE,
            TokenChunkEventPersistor.Result.SUCCESS     -> LoadMoreResult.SUCCESS
        }
    }

    private fun getOffsetIndex(): Int {
        var offset = 0
        var currentNextChunk = nextChunk
        while (currentNextChunk != null) {
            offset += currentNextChunk.builtEvents.size
            currentNextChunk = currentNextChunk.nextChunk
        }
        return offset
    }

    private fun handleDatabaseChangeSet(frozenResults: RealmResults<TimelineEventEntity>, changeSet: OrderedCollectionChangeSet) {
        val insertions = changeSet.insertionRanges
        for (range in insertions) {
            val newItems = frozenResults
                    .subList(range.startIndex, range.startIndex + range.length)
                    .map { it.buildAndDecryptIfNeeded() }
            builtEventsIndexes.entries.filter { it.value >= range.startIndex }.forEach { it.setValue(it.value + range.length) }
            newItems.mapIndexed { index, timelineEvent ->
                val correctedIndex = range.startIndex + index
                builtEvents.add(correctedIndex, timelineEvent)
                builtEventsIndexes[timelineEvent.eventId] = correctedIndex
            }
        }
        val modifications = changeSet.changeRanges
        for (range in modifications) {
            for (modificationIndex in (range.startIndex until range.startIndex + range.length)) {
                val updatedEntity = frozenResults[modificationIndex] ?: continue
                try {
                    builtEvents[modificationIndex] = updatedEntity.buildAndDecryptIfNeeded()
                } catch (failure: Throwable) {
                    Timber.v("Fail to update items at index: $modificationIndex")
                }
            }
        }
        if (insertions.isNotEmpty() || modifications.isNotEmpty()) {
            onBuiltEvents()
        }
    }

    private fun getNextDisplayIndex(direction: Timeline.Direction): Int? {
        val frozenTimelineEvents = timelineEventEntities.freeze()
        if (frozenTimelineEvents.isEmpty()) {
            return null
        }
        return if (builtEvents.isEmpty()) {
            if (initialEventId != null) {
                frozenTimelineEvents.where().equalTo(TimelineEventEntityFields.EVENT_ID, initialEventId).findFirst()?.displayIndex
            } else if (direction == Timeline.Direction.BACKWARDS) {
                frozenTimelineEvents.first()?.displayIndex
            } else {
                frozenTimelineEvents.last()?.displayIndex
            }
        } else if (direction == Timeline.Direction.FORWARDS) {
            builtEvents.first().displayIndex + 1
        } else {
            builtEvents.last().displayIndex - 1
        }
    }
}

private fun RealmQuery<TimelineEventEntity>.offsets(
        direction: Timeline.Direction,
        count: Long,
        startDisplayIndex: Int
): RealmQuery<TimelineEventEntity> {
    sort(TimelineEventEntityFields.DISPLAY_INDEX, Sort.DESCENDING)
    if (direction == Timeline.Direction.BACKWARDS) {
        lessThanOrEqualTo(TimelineEventEntityFields.DISPLAY_INDEX, startDisplayIndex)
    } else {
        greaterThanOrEqualTo(TimelineEventEntityFields.DISPLAY_INDEX, startDisplayIndex)
    }
    return limit(count)
}

private fun Timeline.Direction.toPaginationDirection(): PaginationDirection {
    return if (this == Timeline.Direction.BACKWARDS) PaginationDirection.BACKWARDS else PaginationDirection.FORWARDS
}

private fun ChunkEntity.sortedTimelineEvents(): RealmResults<TimelineEventEntity> {
    return timelineEvents.sort(TimelineEventEntityFields.DISPLAY_INDEX, Sort.DESCENDING)
}
