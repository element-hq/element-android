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

package org.matrix.android.sdk.internal.database

import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.internal.database.helper.nextDisplayIndex
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.ChunkEntityFields
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntityFields
import org.matrix.android.sdk.internal.database.model.deleteOnCascade
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.api.session.SessionLifecycleObserver
import org.matrix.android.sdk.internal.session.room.timeline.PaginationDirection
import org.matrix.android.sdk.internal.task.TaskExecutor
import timber.log.Timber
import javax.inject.Inject

private const val MAX_NUMBER_OF_EVENTS_IN_DB = 35_000L
private const val MIN_NUMBER_OF_EVENTS_BY_CHUNK = 300

/**
 * This class makes sure to stay under a maximum number of events as it makes Realm to be unusable when listening to events
 * when the database is getting too big. This will try incrementally to remove the biggest chunks until we get below the threshold.
 * We make sure to still have a minimum number of events so it's not becoming unusable.
 * So this won't work for users with a big number of very active rooms.
 */
internal class DatabaseCleaner @Inject constructor(@SessionDatabase private val realmConfiguration: RealmConfiguration,
                                                   private val taskExecutor: TaskExecutor) : SessionLifecycleObserver {

    override fun onSessionStarted(session: Session) {
        taskExecutor.executorScope.launch(Dispatchers.Default) {
            awaitTransaction(realmConfiguration) { realm ->
                val allRooms = realm.where(RoomEntity::class.java).findAll()
                Timber.v("There are ${allRooms.size} rooms in this session")
                cleanUp(realm, MAX_NUMBER_OF_EVENTS_IN_DB / 2L)
            }
        }
    }

    private fun cleanUp(realm: Realm, threshold: Long) {
        val numberOfEvents = realm.where(EventEntity::class.java).findAll().size
        val numberOfTimelineEvents = realm.where(TimelineEventEntity::class.java).findAll().size
        Timber.v("Number of events in db: $numberOfEvents | Number of timeline events in db: $numberOfTimelineEvents")
        if (threshold <= MIN_NUMBER_OF_EVENTS_BY_CHUNK || numberOfTimelineEvents < MAX_NUMBER_OF_EVENTS_IN_DB) {
            Timber.v("Db is low enough")
        } else {
            val thresholdChunks = realm.where(ChunkEntity::class.java)
                    .greaterThan(ChunkEntityFields.NUMBER_OF_TIMELINE_EVENTS, threshold)
                    .findAll()

            Timber.v("There are ${thresholdChunks.size} chunks to clean with more than $threshold events")
            for (chunk in thresholdChunks) {
                val maxDisplayIndex = chunk.nextDisplayIndex(PaginationDirection.FORWARDS)
                val thresholdDisplayIndex = maxDisplayIndex - threshold
                val eventsToRemove = chunk.timelineEvents.where().lessThan(TimelineEventEntityFields.DISPLAY_INDEX, thresholdDisplayIndex).findAll()
                Timber.v("There are ${eventsToRemove.size} events to clean in chunk: ${chunk.identifier()} from room ${chunk.room?.first()?.roomId}")
                chunk.numberOfTimelineEvents = chunk.numberOfTimelineEvents - eventsToRemove.size
                eventsToRemove.forEach {
                    val canDeleteRoot = it.root?.stateKey == null
                    it.deleteOnCascade(canDeleteRoot)
                }
                // We reset the prevToken so we will need to fetch again.
                chunk.prevToken = null
            }
            cleanUp(realm, (threshold / 1.5).toLong())
        }
    }
}
