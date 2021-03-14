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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import com.zhuinden.monarchy.Monarchy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.events.model.LocalEcho
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.internal.database.mapper.TimelineEventMapper
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.query.where
import java.util.concurrent.atomic.AtomicBoolean

/**
 * This class takes care of handling case where local echo is replaced by the synced event in the db.
 */
internal class LiveTimelineEvent(private val timelineInput: TimelineInput,
                                 private val monarchy: Monarchy,
                                 private val coroutineScope: CoroutineScope,
                                 private val timelineEventMapper: TimelineEventMapper,
                                 private val roomId: String,
                                 private val eventId: String)
    : TimelineInput.Listener,
        MediatorLiveData<Optional<TimelineEvent>>() {

    private var queryLiveData: LiveData<Optional<TimelineEvent>>? = null

    // If we are listening to local echo, we want to be aware when event is synced
    private var shouldObserveSync = AtomicBoolean(LocalEcho.isLocalEchoId(eventId))

    init {
        buildAndObserveQuery(eventId)
    }

    // Makes sure it's made on the main thread
    private fun buildAndObserveQuery(eventIdToObserve: String) = coroutineScope.launch(Dispatchers.Main) {
        queryLiveData?.also {
            removeSource(it)
        }
        val liveData = monarchy.findAllMappedWithChanges(
                { TimelineEventEntity.where(it, roomId = roomId, eventId = eventIdToObserve) },
                { timelineEventMapper.map(it) }
        )
        queryLiveData = Transformations.map(liveData) { events ->
            events.firstOrNull().toOptional()
        }.also {
            addSource(it) { newValue -> value = newValue }
        }
    }

    override fun onLocalEchoSynced(roomId: String, localEchoEventId: String, syncedEventId: String) {
        if (this.roomId == roomId && localEchoEventId == this.eventId) {
            timelineInput.listeners.remove(this)
            shouldObserveSync.set(false)
            // rebuild the query with the new eventId
            buildAndObserveQuery(syncedEventId)
        }
    }

    override fun onActive() {
        super.onActive()
        if (shouldObserveSync.get()) {
            timelineInput.listeners.add(this)
        }
    }

    override fun onInactive() {
        super.onInactive()
        if (shouldObserveSync.get()) {
            timelineInput.listeners.remove(this)
        }
    }
}
