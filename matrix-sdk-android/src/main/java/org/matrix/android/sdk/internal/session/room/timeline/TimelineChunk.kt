/*
 * Copyright (c) 2021 New Vector Ltd
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
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.internal.database.mapper.TimelineEventMapper
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntityFields
import timber.log.Timber

internal class TimelineChunk(private val chunkEntity: ChunkEntity,
                             private val roomId: String,
                             private val paginationTask: PaginationTask,
                             private val timelineEventMapper: TimelineEventMapper,
                             private val initialEventId: String?,
                             private val onBuiltEvents: () -> Unit) {

    private val chunkObjectListener = RealmObjectChangeListener<ChunkEntity> { _, changeSet ->
        Timber.v("on chunk (${chunkEntity.identifier()}) changed: ${changeSet?.changedFields?.joinToString(",")}")
    }

    private val timelineEventCollectionListener = OrderedRealmCollectionChangeListener { results: RealmResults<TimelineEventEntity>, changeSet: OrderedCollectionChangeSet ->
        val frozenResults = results.freeze()
        handleChangeSet(frozenResults, changeSet)
    }

    private var timelineEventEntities: RealmResults<TimelineEventEntity> = chunkEntity.sortedTimelineEvents()
    private val builtEvents: ArrayList<TimelineEvent> = ArrayList()

    private var nextChunk: TimelineChunk? = null
    private var prevChunk: TimelineChunk? = null

    init {
        timelineEventEntities.addChangeListener(timelineEventCollectionListener)
        chunkEntity.addChangeListener(chunkObjectListener)
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

    suspend fun loadMore(count: Long, direction: SimpleTimeline.Direction) {
        val loadFromDbCount = loadFromDb(count, direction)
        val offsetCount = count - loadFromDbCount
        // We have built the right amount of data
        if (offsetCount == 0L) {
            onBuiltEvents()
            return
        }
        if (direction == SimpleTimeline.Direction.FORWARDS) {
            val nextChunkEntity = chunkEntity.nextChunk
            if (nextChunkEntity == null) {
                val token = chunkEntity.nextToken ?: return //TODO handle previous live chunk
                try {
                    fetchFromServer(token, offsetCount, direction)
                } catch (failure: Throwable) {
                    Timber.v("Failed to fetch from server: $failure")
                }
            } else {
                // otherwise we delegate to the next chunk
                if (nextChunk == null) {
                    nextChunk = createTimelineChunk(nextChunkEntity)
                }
                nextChunk?.loadMore(offsetCount, direction)
            }
        } else {
            val prevChunkEntity = chunkEntity.prevChunk
            if (prevChunkEntity == null) {
                val token = chunkEntity.prevToken ?: return
                try {
                    fetchFromServer(token, offsetCount, direction)
                } catch (failure: Throwable) {
                    Timber.v("Failed to fetch from server: $failure")
                }
            } else {
                // otherwise we delegate to the prev chunk
                if (prevChunk == null) {
                    prevChunk = createTimelineChunk(prevChunkEntity)
                }
                prevChunk?.loadMore(offsetCount, direction)
            }
        }
    }

    private fun loadFromDb(count: Long, direction: SimpleTimeline.Direction): Long {
        val displayIndex = getNextDisplayIndex(direction) ?: return 0
        val baseQuery = timelineEventEntities.where()
        val timelineEvents = baseQuery.offsets(direction, count, displayIndex).findAll().orEmpty()
        timelineEvents
                .map { timelineEventEntity ->
                    timelineEventMapper.map(timelineEventEntity)
                }.also {
                    if (direction == SimpleTimeline.Direction.FORWARDS) {
                        builtEvents.addAll(0, it)
                    } else {
                        builtEvents.addAll(it)
                    }
                }
        return timelineEvents.size.toLong()
    }

    private fun createTimelineChunk(chunkEntity: ChunkEntity): TimelineChunk {
        return TimelineChunk(
                chunkEntity,
                roomId,
                paginationTask,
                timelineEventMapper,
                initialEventId,
                onBuiltEvents
        )
    }

    private suspend fun fetchFromServer(token: String, count: Long, direction: SimpleTimeline.Direction): TokenChunkEventPersistor.Result {
        val paginationParams = PaginationTask.Params(roomId, token, direction.toPaginationDirection(), count.toInt())
        return paginationTask.execute(paginationParams)
    }

    private fun handleChangeSet(frozenResults: RealmResults<TimelineEventEntity>, changeSet: OrderedCollectionChangeSet) {
        val deletions = changeSet.deletionRanges
        if (deletions.isNotEmpty()) {
            // Do not handle deletions
        }
        val insertions = changeSet.insertionRanges
        for (range in insertions) {
            val newItems = frozenResults
                    .subList(range.startIndex, range.startIndex + range.length)
                    .map {
                        timelineEventMapper.map(it)
                    }
            builtEvents.addAll(range.startIndex, newItems)
        }
        val modifications = changeSet.changeRanges
        for (range in modifications) {
            for (modificationIndex in (range.startIndex until range.startIndex + range.length)) {
                val updatedEntity = frozenResults[modificationIndex] ?: continue
                try {
                    builtEvents[modificationIndex] = timelineEventMapper.map(updatedEntity)
                } catch (failure: Throwable) {
                    Timber.v("Fail to update items at index: $modificationIndex")
                }
            }
        }
        if (deletions.isNotEmpty() || insertions.isNotEmpty() || modifications.isNotEmpty()) {
            onBuiltEvents()
        }
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

    private fun getNextDisplayIndex(direction: SimpleTimeline.Direction): Int? {
        val frozenTimelineEvents = timelineEventEntities.freeze()
        if (frozenTimelineEvents.isEmpty()) {
            return null
        }
        return if (builtEvents.isEmpty()) {
            if (initialEventId != null) {
                frozenTimelineEvents.where().equalTo(TimelineEventEntityFields.EVENT_ID, initialEventId).findFirst()?.displayIndex
            } else if (direction == SimpleTimeline.Direction.BACKWARDS) {
                frozenTimelineEvents.first()?.displayIndex
            } else {
                frozenTimelineEvents.last()?.displayIndex
            }
        } else if (direction == SimpleTimeline.Direction.FORWARDS) {
            builtEvents.first().displayIndex
        } else {
            builtEvents.last().displayIndex
        }
    }
}

private fun RealmQuery<TimelineEventEntity>.offsets(
        direction: SimpleTimeline.Direction,
        count: Long,
        startDisplayIndex: Int
): RealmQuery<TimelineEventEntity> {
    if (direction == SimpleTimeline.Direction.BACKWARDS) {
        sort(TimelineEventEntityFields.DISPLAY_INDEX, Sort.DESCENDING)
        lessThan(TimelineEventEntityFields.DISPLAY_INDEX, startDisplayIndex)
    } else {
        sort(TimelineEventEntityFields.DISPLAY_INDEX, Sort.ASCENDING)
        greaterThan(TimelineEventEntityFields.DISPLAY_INDEX, startDisplayIndex)
    }
    return limit(count)
}

private fun SimpleTimeline.Direction.toPaginationDirection(): PaginationDirection {
    return if (this == SimpleTimeline.Direction.BACKWARDS) PaginationDirection.BACKWARDS else PaginationDirection.FORWARDS
}

private fun ChunkEntity.sortedTimelineEvents(): RealmResults<TimelineEventEntity> {
    return timelineEvents.sort(TimelineEventEntityFields.DISPLAY_INDEX, Sort.DESCENDING)
}

