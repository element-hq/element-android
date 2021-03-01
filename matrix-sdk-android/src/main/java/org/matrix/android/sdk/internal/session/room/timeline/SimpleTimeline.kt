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
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmQuery
import io.realm.RealmResults
import io.realm.Sort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.closeQuietly
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.internal.database.mapper.TimelineEventMapper
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.ChunkEntityFields
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntityFields
import org.matrix.android.sdk.internal.database.query.findLastForwardChunkOfRoom
import org.matrix.android.sdk.internal.database.query.whereRoomId
import org.matrix.android.sdk.internal.task.SemaphoreCoroutineSequencer
import org.matrix.android.sdk.internal.util.createBackgroundHandler
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class SimpleTimeline internal constructor(val roomId: String,
                                          private val realmConfiguration: RealmConfiguration,
                                          private val paginationTask: PaginationTask,
                                          private val getEventTask: GetContextOfEventTask,
                                          private val timelineEventMapper: TimelineEventMapper,
                                          private val innerList: ArrayList<TimelineEvent> = ArrayList()) {

    interface Listener {
        fun onStateUpdated()
        fun onEventsUpdated(snapshot: List<TimelineEvent>)
    }

    companion object {
        val BACKGROUND_HANDLER = createBackgroundHandler("SimpleTimeline_Thread")
    }

    private val originEventId = AtomicReference<String>(null)
    private val listeners = CopyOnWriteArrayList<Listener>()
    private val isStarted = AtomicBoolean(false)
    private val forwardState = AtomicReference(PaginationState())
    private val backwardState = AtomicReference(PaginationState())

    private val backgroundRealm = AtomicReference<Realm>()
    private val timelineDispatcher = BACKGROUND_HANDLER.asCoroutineDispatcher()
    private val timelineScope = CoroutineScope(SupervisorJob() + timelineDispatcher)
    private val sequencer = SemaphoreCoroutineSequencer()
    private var strategy: LoadTimelineStrategy = LiveTimelineStrategy(
            roomId = roomId,
            realm = backgroundRealm,
            paginationTask = paginationTask,
            builtItems = innerList,
            timelineEventMapper = timelineEventMapper,
            onEventsUpdated = this::postSnapshot
    )

    data class PaginationState(
            val hasMoreToLoad: Boolean = true,
            val loading: Boolean = false,
            val inError: Boolean = false
    )

    enum class Direction {
        Forward,
        Backward
    }

    fun addListener(listener: Listener) {
        listeners.add(listener)
        postSnapshot()
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun removeAllListeners() {
        listeners.clear()
    }

    fun start() = timelineScope.launch {
        sequencer.post {
            if (isStarted.compareAndSet(false, true)) {
                val realm = Realm.getInstance(realmConfiguration)
                backgroundRealm.set(realm)
                strategy.onStart()
            }
        }
    }

    fun stop() = timelineScope.launch {
        sequencer.post {
            if (isStarted.compareAndSet(true, false)) {
                backgroundRealm.get().closeQuietly()
            }
        }
    }

    private fun updateState(direction: Direction, update: (PaginationState) -> PaginationState) {
        val stateReference = when (direction) {
            Direction.Forward -> forwardState
            Direction.Backward -> backwardState
        }
        val currentValue = stateReference.get()
        val newValue = update(currentValue)
        stateReference.set(newValue)
        listeners.forEach {
            tryOrNull { it.onStateUpdated() }
        }
    }

    fun getPaginationState(direction: Direction): PaginationState {
        return if (direction == Direction.Backward) {
            backwardState
        } else {
            forwardState
        }.get()
    }

    suspend fun loadMore(count: Long, direction: Direction) = withContext(timelineDispatcher) {
        val baseLogMessage = "loadMore(count: $count, direction: $direction, roomId: $roomId)"
        sequencer.post {
            Timber.v("$baseLogMessage started")
            if (!isStarted.get()) {
                throw IllegalStateException("You should call start before using timeline")
            }
            val currentState = getPaginationState(direction)
            if (!currentState.hasMoreToLoad) {
                Timber.v("$baseLogMessage : nothing more to load")
                return@post
            }
            if (currentState.loading) {
                Timber.v("$baseLogMessage : already loading")
                return@post
            }
            updateState(direction) {
                it.copy(loading = true)
            }
            val loadedItemsCount = loadFromDb(count, direction)
            Timber.v("$baseLogMessage: loaded $loadedItemsCount items from db")
            if (loadedItemsCount < count) {
                val fetchCount = count - loadedItemsCount
                Timber.v("$baseLogMessage : trigger paginate on server")
                fetchOnServer(fetchCount, direction)
            }
            updateState(direction) {
                it.copy(loading = false)
            }
            postSnapshot()
        }
    }

    private fun postSnapshot() {
        val snapshot = innerList.toList()
        listeners.forEach {
            tryOrNull { it.onEventsUpdated(snapshot) }
        }
    }

    private suspend fun loadFromDb(count: Long, direction: Direction): Int {
        return strategy.loadFromDb(count, direction)
    }

    private suspend fun fetchOnServer(count: Long, direction: Direction) {
        try {
            val fetchResult = strategy.fetchFromServer(count, direction)
            when (fetchResult) {
                TokenChunkEventPersistor.Result.SHOULD_FETCH_MORE -> {
                    strategy.fetchFromServer(count, direction)
                }
                TokenChunkEventPersistor.Result.REACHED_END -> {
                    updateState(direction) {
                        it.copy(hasMoreToLoad = false)
                    }
                }
                TokenChunkEventPersistor.Result.SUCCESS -> {
                    strategy.loadFromDb(count, direction)
                }
            }
        } catch (failure: Throwable) {
            updateState(direction) {
                it.copy(inError = true)
            }
        }
    }
}

private interface LoadTimelineStrategy {
    fun onStart()
    fun onStop()
    suspend fun loadFromDb(count: Long, direction: SimpleTimeline.Direction): Int
    suspend fun fetchFromServer(count: Long, direction: SimpleTimeline.Direction): TokenChunkEventPersistor.Result
}

private class LiveTimelineStrategy(private val roomId: String,
                                   private val realm: AtomicReference<Realm>,
                                   private val paginationTask: PaginationTask,
                                   private val builtItems: MutableList<TimelineEvent>,
                                   private val timelineEventMapper: TimelineEventMapper,
                                   private val onEventsUpdated: () -> Unit) : LoadTimelineStrategy {

    private lateinit var timelineEvents: RealmResults<TimelineEventEntity>

    override fun onStart() {
        timelineEvents = getQuery().findAll()
        val changeListener = OrderedRealmCollectionChangeListener { _: RealmResults<TimelineEventEntity>?, changeSet: OrderedCollectionChangeSet ->
            val frozenResults = timelineEvents.freeze()
            handleChangeSet(frozenResults, changeSet)
        }
        timelineEvents.addChangeListener(changeListener)
    }

    private fun handleChangeSet(frozenResults: RealmResults<TimelineEventEntity>, changeSet: OrderedCollectionChangeSet) {
        val deletions = changeSet.deletionRanges
        if (deletions.isNotEmpty()) {
            // We have a gap clear everything
        }
        val insertions = changeSet.insertionRanges
        for (range in insertions) {
            // Add only forward events
            if (range.startIndex == 0) {
                val newItems = frozenResults
                        .subList(0, range.length)
                        .map {
                            timelineEventMapper.map(it)
                        }
                builtItems.addAll(0, newItems)
            } else {
                Timber.v("Insertions from backwards handled on demand")
            }
        }
        val modifications = changeSet.changeRanges
        /*for (range in modifications) {
            for (modificationIndex in (range.startIndex..range.startIndex + range.length)) {
                val newEntity = frozenResults[modificationIndex] ?: continue
                builtItems[modificationIndex] = timelineEventMapper.map(newEntity)
            }
        }

         */
        if (deletions.isNotEmpty() || insertions.isNotEmpty() || modifications.isNotEmpty()) {
            onEventsUpdated()
        }
    }

    override fun onStop() {
    }

    private fun getQuery(): RealmQuery<TimelineEventEntity> {
        return TimelineEventEntity
                .whereRoomId(realm.get(), roomId = roomId)
                .equalTo("${TimelineEventEntityFields.CHUNK}.${ChunkEntityFields.IS_LAST_FORWARD}", true)
                // Higher display index is most recent
                .sort(TimelineEventEntityFields.DISPLAY_INDEX, Sort.DESCENDING)
    }

    override suspend fun loadFromDb(count: Long, direction: SimpleTimeline.Direction): Int {
        if (direction == SimpleTimeline.Direction.Forward) {
            return 0
        }
        val safeRealm = realm.get()
        if (safeRealm.isClosed) return 0
        val currentIndex = builtItems.lastOrNull()?.displayIndex
        val baseQuery = getChunkEntity(safeRealm)?.timelineEvents?.where()
        val timelineEvents = if (currentIndex != null) {
            baseQuery?.offsets(direction, count, currentIndex)
        } else {
            baseQuery?.sort(TimelineEventEntityFields.DISPLAY_INDEX, Sort.DESCENDING)?.limit(count)
        }?.findAll().orEmpty()

        timelineEvents
                .map { timelineEventEntity ->
                    timelineEventMapper.map(timelineEventEntity)
                }.also {
                    if (direction == SimpleTimeline.Direction.Forward) {
                        builtItems.addAll(0, it)
                    } else {
                        builtItems.addAll(it)
                    }
                }
        return timelineEvents.size
    }

    override suspend fun fetchFromServer(count: Long, direction: SimpleTimeline.Direction): TokenChunkEventPersistor.Result {
        if (direction == SimpleTimeline.Direction.Forward) {
            return TokenChunkEventPersistor.Result.REACHED_END
        }
        val safeRealm = realm.get()
        if (safeRealm.isClosed) throw java.lang.IllegalStateException("Realm has been closed")
        val chunkEntity = getChunkEntity(safeRealm) ?: throw RuntimeException("No chunk found")
        val token = chunkEntity.prevToken ?: throw RuntimeException("No token found")
        val paginationParams = PaginationTask.Params(roomId, token, direction.toPaginationDirection(), count.toInt())
        return paginationTask.execute(paginationParams)
    }

    private fun getChunkEntity(realm: Realm): ChunkEntity? {
        return ChunkEntity.findLastForwardChunkOfRoom(realm, roomId)
    }
}

private class PastTimelineStrategy(
        private val roomId: String,
        private val originEventId: String,
        private val paginationTask: PaginationTask,
        private val builtItems: MutableList<TimelineEvent>) : LoadTimelineStrategy {

    override fun onStart() {
        TODO("Not yet implemented")
    }

    override fun onStop() {
        TODO("Not yet implemented")
    }

    private fun getQuery(): RealmQuery<TimelineEventEntity> {
        TODO("Not yet implemented")
    }

    override suspend fun loadFromDb(count: Long, direction: SimpleTimeline.Direction): Int {
        return 0
    }

    override suspend fun fetchFromServer(count: Long, direction: SimpleTimeline.Direction): TokenChunkEventPersistor.Result {
        return TokenChunkEventPersistor.Result.SUCCESS
    }
}

private fun RealmQuery<TimelineEventEntity>.offsets(
        direction: SimpleTimeline.Direction,
        count: Long,
        startDisplayIndex: Int
): RealmQuery<TimelineEventEntity> {
    if (direction == SimpleTimeline.Direction.Backward) {
        sort(TimelineEventEntityFields.DISPLAY_INDEX, Sort.DESCENDING)
        lessThan(TimelineEventEntityFields.DISPLAY_INDEX, startDisplayIndex)
    } else {
        sort(TimelineEventEntityFields.DISPLAY_INDEX, Sort.ASCENDING)
        greaterThan(TimelineEventEntityFields.DISPLAY_INDEX, startDisplayIndex)
    }
    return limit(count)
}

private fun SimpleTimeline.Direction.toPaginationDirection(): PaginationDirection {
    return if (this == SimpleTimeline.Direction.Backward) PaginationDirection.BACKWARDS else PaginationDirection.FORWARDS
}
