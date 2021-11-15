/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.database.helper

import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import org.matrix.android.sdk.BuildConfig
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntityFields
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.database.query.whereRoomId

/**
 * Finds the root thread event and update it with the latest message summary along with the number
 * of threads included. If there is no root thread event no action is done
 */
internal fun Map<String, EventEntity>.updateThreadSummaryIfNeeded() {

    if (!BuildConfig.THREADING_ENABLED) return

    for ((rootThreadEventId, eventEntity) in this) {

        eventEntity.findAllThreadsForRootEventId(eventEntity.realm, rootThreadEventId).let {

            if (it.isNullOrEmpty()) return@let

            val latestMessage = it.firstOrNull()

            // If this is a thread message, find its root event if exists
            val rootThreadEvent = if (eventEntity.isThread()) eventEntity.findRootThreadEvent() else eventEntity

            rootThreadEvent?.markEventAsRoot(
                    threadsCounted = it.size,
                    latestMessageTimelineEventEntity = latestMessage
            )
        }
    }
}

/**
 * Finds the root event of the the current thread event message.
 * Returns the EventEntity or null if the root event do not exist
 */
internal fun EventEntity.findRootThreadEvent(): EventEntity? =
        rootThreadEventId?.let {
            EventEntity
                    .where(realm, it)
                    .findFirst()
        }

/**
 * Mark or update the current event a root thread event
 */
internal fun EventEntity.markEventAsRoot(threadsCounted: Int,
                                         latestMessageTimelineEventEntity: TimelineEventEntity?) {
    isRootThread = true
    numberOfThreads = threadsCounted
    threadSummaryLatestMessage = latestMessageTimelineEventEntity
}

/**
 * Find all TimelineEventEntity that are threads bind to the Event with rootThreadEventId
 * @param rootThreadEventId The root eventId that will try to find bind threads
 */
internal fun EventEntity.findAllThreadsForRootEventId(realm: Realm, rootThreadEventId: String): RealmResults<TimelineEventEntity> =
        TimelineEventEntity
                .whereRoomId(realm, roomId = roomId)
                .equalTo(TimelineEventEntityFields.ROOT.ROOT_THREAD_EVENT_ID, rootThreadEventId)
                .sort(TimelineEventEntityFields.DISPLAY_INDEX, Sort.DESCENDING).findAll()



