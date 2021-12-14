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
import io.realm.RealmQuery
import io.realm.RealmResults
import io.realm.Sort
import org.matrix.android.sdk.BuildConfig
import org.matrix.android.sdk.api.session.threads.ThreadNotificationState
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.ReadReceiptEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntityFields
import org.matrix.android.sdk.internal.database.query.findIncludingEvent
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.database.query.whereRoomId

/**
 * Finds the root thread event and update it with the latest message summary along with the number
 * of threads included. If there is no root thread event no action is done
 */
internal fun Map<String, EventEntity>.updateThreadSummaryIfNeeded(roomId: String, realm: Realm, currentUserId: String) {

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

    updateNotificationsNew(roomId, realm, currentUserId)
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
internal fun EventEntity.markEventAsRoot(
        threadsCounted: Int,
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
                .sort(TimelineEventEntityFields.DISPLAY_INDEX, Sort.DESCENDING)
                .findAll()

/**
 * Find all TimelineEventEntity that are root threads for the specified room
 * @param roomId The room that all stored root threads will be returned
 */
internal fun TimelineEventEntity.Companion.findAllThreadsForRoomId(realm: Realm, roomId: String): RealmQuery<TimelineEventEntity> =
        TimelineEventEntity
                .whereRoomId(realm, roomId = roomId)
                .equalTo(TimelineEventEntityFields.ROOT.IS_ROOT_THREAD, true)
                .sort("${TimelineEventEntityFields.ROOT.THREAD_SUMMARY_LATEST_MESSAGE}.${TimelineEventEntityFields.DISPLAY_INDEX}", Sort.DESCENDING)

/**
 * Find the number of all the local notifications for the specified room
 * @param roomId The room that the number of notifications will be returned
 */
internal fun TimelineEventEntity.Companion.findAllLocalThreadNotificationsForRoomId(realm: Realm, roomId: String): RealmQuery<TimelineEventEntity> =
        TimelineEventEntity
                .whereRoomId(realm, roomId = roomId)
                .equalTo(TimelineEventEntityFields.ROOT.IS_ROOT_THREAD, true)
                .equalTo(TimelineEventEntityFields.ROOT.THREAD_NOTIFICATION_STATE_STR, ThreadNotificationState.NEW_MESSAGE.name)
                .or()
                .equalTo(TimelineEventEntityFields.ROOT.THREAD_NOTIFICATION_STATE_STR, ThreadNotificationState.NEW_HIGHLIGHTED_MESSAGE.name)

/**
 * Returns whether or not the given user is participating in a current thread
 * @param roomId the room that the thread exists
 * @param rootThreadEventId the thread that the search will be done
 * @param senderId the user that will try to find participation
 */
internal fun TimelineEventEntity.Companion.isUserParticipatingInThread(realm: Realm, roomId: String, rootThreadEventId: String, senderId: String): Boolean =
        TimelineEventEntity
                .whereRoomId(realm, roomId = roomId)
                .equalTo(TimelineEventEntityFields.ROOT.ROOT_THREAD_EVENT_ID, rootThreadEventId)
                .equalTo(TimelineEventEntityFields.ROOT.SENDER, senderId)
                .findFirst()
                ?.let { true }
                ?: false

/**
 * Returns whether or not the given user is mentioned in a current thread
 * @param roomId the room that the thread exists
 * @param rootThreadEventId the thread that the search will be done
 * @param userId the user that will try to find if there is a mention
 */
internal fun TimelineEventEntity.Companion.isUserMentionedInThread(realm: Realm, roomId: String, rootThreadEventId: String, userId: String): Boolean =
        TimelineEventEntity
                .whereRoomId(realm, roomId = roomId)
                .equalTo(TimelineEventEntityFields.ROOT.ROOT_THREAD_EVENT_ID, rootThreadEventId)
                .equalTo(TimelineEventEntityFields.ROOT.SENDER, userId)
                .findAll()
                .firstOrNull { isUserMentioned(userId, it) }
                ?.let { true }
                ?: false

/**
 * Find the read receipt for the current user
 */
internal fun findMyReadReceipt(realm: Realm, roomId: String, userId: String): String? =
        ReadReceiptEntity.where(realm, roomId = roomId, userId = userId)
                .findFirst()
                ?.eventId

/**
 * Returns whether or not the user is mentioned in the event
 */
internal fun isUserMentioned(currentUserId: String, timelineEventEntity: TimelineEventEntity?): Boolean {
    val decryptedContent = timelineEventEntity?.root?.asDomain()?.getDecryptedTextSummary().orEmpty()
    return decryptedContent.contains(currentUserId.replace("@", "").substringBefore(":"))
}

/**
 * Update badge notifications. Count the number of new thread events after the latest
 * read receipt and aggregate. This function will find and notify new thread events
 * that the user is either mentioned, or the user had participated in.
 * Important: If the root thread event is not fetched notification will not work
 * Important: It will work only with the latest chunk, while read marker will be changed
 * immediately so we should not display wrong notifications
 */
internal fun updateNotificationsNew(roomId: String, realm: Realm, currentUserId: String) {

    val readReceipt = findMyReadReceipt(realm, roomId, currentUserId) ?: return

    val readReceiptChunk = ChunkEntity
            .findIncludingEvent(realm, readReceipt) ?: return

    val readReceiptChunkTimelineEvents = readReceiptChunk
            .timelineEvents
            .where()
            .equalTo(TimelineEventEntityFields.ROOM_ID, roomId)
            .sort(TimelineEventEntityFields.DISPLAY_INDEX, Sort.ASCENDING)
            .findAll() ?: return

    val readReceiptChunkPosition = readReceiptChunkTimelineEvents.indexOfFirst { it.eventId == readReceipt }

    if(readReceiptChunkPosition == -1) return

    if (readReceiptChunkPosition < readReceiptChunkTimelineEvents.lastIndex) {
        // If the read receipt is found inside the chunk

        val threadEventsAfterReadReceipt = readReceiptChunkTimelineEvents
                .slice(readReceiptChunkPosition..readReceiptChunkTimelineEvents.lastIndex)
                .filter { it.root?.isThread() == true }

        // In order for the below code to work for old events, we should save the previous read receipt
        // and then continue with the chunk search for that read receipt
        /*
       val newThreadEventsList = arrayListOf<TimelineEventEntity>()
        newThreadEventsList.addAll(threadEventsAfterReadReceipt)

        // got from latest chunk all new threads, lets move to the others
        var nextChunk = ChunkEntity
                .find(realm = realm, roomId = roomId, nextToken = readReceiptChunk.nextToken)
                .takeIf { readReceiptChunk.nextToken != null }
        while (nextChunk != null) {
            newThreadEventsList.addAll(nextChunk.timelineEvents
                    .filter { it.root?.isThread() == true })
            nextChunk = ChunkEntity
                    .find(realm = realm, roomId = roomId, nextToken = nextChunk.nextToken)
                    .takeIf { readReceiptChunk.nextToken != null }
        }*/

        // Find if the user is mentioned in those events
        val userMentionsList = threadEventsAfterReadReceipt
                .filter {
                    isUserMentioned(currentUserId = currentUserId, it)
                }.map {
                    it.root?.rootThreadEventId
                }

        // Find the root events in the new thread events
        val rootThreads = threadEventsAfterReadReceipt.distinctBy { it.root?.rootThreadEventId }.mapNotNull { it.root?.rootThreadEventId }

        // Update root thread events only if the user have participated in
        rootThreads.forEach { eventId ->
            val isUserParticipating = TimelineEventEntity.isUserParticipatingInThread(
                    realm = realm,
                    roomId = roomId,
                    rootThreadEventId = eventId,
                    senderId = currentUserId)
            val rootThreadEventEntity = EventEntity.where(realm, eventId).findFirst()

            if (isUserParticipating) {
                rootThreadEventEntity?.threadNotificationState = ThreadNotificationState.NEW_MESSAGE
            }

            if (userMentionsList.contains(eventId)) {
                rootThreadEventEntity?.threadNotificationState = ThreadNotificationState.NEW_HIGHLIGHTED_MESSAGE
            }
        }
    }
}
