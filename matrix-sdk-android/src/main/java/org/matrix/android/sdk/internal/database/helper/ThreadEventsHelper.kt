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

import com.squareup.moshi.JsonDataException
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.Sort
import org.matrix.android.sdk.api.session.events.model.UnsignedData
import org.matrix.android.sdk.api.session.events.model.isRedacted
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.threads.ThreadNotificationState
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntity
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.ReadReceiptEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntityFields
import org.matrix.android.sdk.internal.database.query.find
import org.matrix.android.sdk.internal.database.query.findIncludingEvent
import org.matrix.android.sdk.internal.database.query.findLastForwardChunkOfRoom
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.database.query.whereRoomId
import org.matrix.android.sdk.internal.di.MoshiProvider
import timber.log.Timber

private typealias Summary = Pair<Int, TimelineEventEntity>?

/**
 * Finds the root thread event and update it with the latest message summary along with the number
 * of threads included. If there is no root thread event no action is done
 */
internal fun Map<String, EventEntity>.updateThreadSummaryIfNeeded(
        roomId: String,
        realm: Realm, currentUserId: String,
        chunkEntity: ChunkEntity? = null,
        shouldUpdateNotifications: Boolean = true) {
    for ((rootThreadEventId, eventEntity) in this) {
        eventEntity.threadSummaryInThread(eventEntity.realm, rootThreadEventId, chunkEntity)?.let { threadSummary ->

            val inThreadMessages = threadSummary.first
            val latestEventInThread = threadSummary.second

            // If this is a thread message, find its root event if exists
            val rootThreadEvent = if (eventEntity.isThread()) eventEntity.findRootThreadEvent() else eventEntity

            rootThreadEvent?.markEventAsRoot(
                    inThreadMessages = inThreadMessages,
                    latestMessageTimelineEventEntity = latestEventInThread
            )
        }
    }

    if (shouldUpdateNotifications) {
        updateNotificationsNew(roomId, realm, currentUserId)
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
internal fun EventEntity.markEventAsRoot(
        inThreadMessages: Int,
        latestMessageTimelineEventEntity: TimelineEventEntity?) {
    isRootThread = true
    numberOfThreads = inThreadMessages
    threadSummaryLatestMessage = latestMessageTimelineEventEntity
}

/**
 * Count the number of threads for the provided root thread eventId, and finds the latest event message
 * note: Redactions are handled by RedactionEventProcessor
 * @param rootThreadEventId The root eventId that will find the number of threads
 * @return A ThreadSummary containing the counted threads and the latest event message
 */
internal fun EventEntity.threadSummaryInThread(realm: Realm, rootThreadEventId: String, chunkEntity: ChunkEntity?): Summary {
    val inThreadMessages = countInThreadMessages(
            realm = realm,
            roomId = roomId,
            rootThreadEventId = rootThreadEventId
    )

    if (inThreadMessages <= 0) return null

    // Find latest thread event, we know it exists
    var chunk = ChunkEntity.findLastForwardChunkOfRoom(realm, roomId) ?: chunkEntity ?: return null
    var result: TimelineEventEntity? = null

    // Iterate the chunk until we find our latest event
    while (result == null) {
        result = findLatestSortedChunkEvent(chunk, rootThreadEventId)
        chunk = ChunkEntity.find(realm, roomId, nextToken = chunk.prevToken) ?: break
    }

    if (result == null && chunkEntity != null) {
        // Find latest event from our current chunk
        result = findLatestSortedChunkEvent(chunkEntity, rootThreadEventId)
    } else if (result != null && chunkEntity != null) {
        val currentChunkLatestEvent = findLatestSortedChunkEvent(chunkEntity, rootThreadEventId)
        result = findMostRecentEvent(result, currentChunkLatestEvent)
    }

    result ?: return null

    return Summary(inThreadMessages, result)
}

/**
 * Counts the number of thread replies in the main timeline thread summary,
 * with respect to redactions.
 */
internal fun countInThreadMessages(realm: Realm, roomId: String, rootThreadEventId: String): Int =
        TimelineEventEntity
                .whereRoomId(realm, roomId = roomId)
                .equalTo(TimelineEventEntityFields.ROOT.ROOT_THREAD_EVENT_ID, rootThreadEventId)
                .distinct(TimelineEventEntityFields.ROOT.EVENT_ID)
                .findAll()
                .filterNot { timelineEvent ->
                    timelineEvent.root
                            ?.unsignedData
                            ?.takeIf { it.isNotBlank() }
                            ?.toUnsignedData()
                            .isRedacted()
                }.size

/**
 * Mapping string to UnsignedData using Moshi
 */
private fun String.toUnsignedData(): UnsignedData? =
        try {
            MoshiProvider.providesMoshi().adapter(UnsignedData::class.java).fromJson(this)
        } catch (ex: JsonDataException) {
            Timber.e(ex, "Failed to parse UnsignedData")
            null
        }

/**
 * Lets compare them in case user is moving forward in the timeline and we cannot know the
 * exact chunk sequence while currentChunk is not yet committed in the DB
 */
private fun findMostRecentEvent(result: TimelineEventEntity, currentChunkLatestEvent: TimelineEventEntity?): TimelineEventEntity {
    currentChunkLatestEvent ?: return result
    val currentChunkEventTimestamp = currentChunkLatestEvent.root?.originServerTs ?: return result
    val resultTimestamp = result.root?.originServerTs ?: return result
    if (currentChunkEventTimestamp > resultTimestamp) {
        return currentChunkLatestEvent
    }
    return result
}

/**
 * Find the latest event of the current chunk
 */
private fun findLatestSortedChunkEvent(chunk: ChunkEntity, rootThreadEventId: String): TimelineEventEntity? =
        chunk.timelineEvents.sort(TimelineEventEntityFields.DISPLAY_INDEX, Sort.DESCENDING)?.firstOrNull {
            it.root?.rootThreadEventId == rootThreadEventId
        }

/**
 * Find all TimelineEventEntity that are root threads for the specified room
 * @param roomId The room that all stored root threads will be returned
 */
internal fun TimelineEventEntity.Companion.findAllThreadsForRoomId(realm: Realm, roomId: String): RealmQuery<TimelineEventEntity> =
        TimelineEventEntity
                .whereRoomId(realm, roomId = roomId)
                .equalTo(TimelineEventEntityFields.ROOT.IS_ROOT_THREAD, true)
                .equalTo(TimelineEventEntityFields.OWNED_BY_THREAD_CHUNK, false)
                .sort("${TimelineEventEntityFields.ROOT.THREAD_SUMMARY_LATEST_MESSAGE}.${TimelineEventEntityFields.ROOT.ORIGIN_SERVER_TS}", Sort.DESCENDING)

/**
 * Map each root thread TimelineEvent with the equivalent decrypted text edition/replacement
 */
internal fun List<TimelineEvent>.mapEventsWithEdition(realm: Realm, roomId: String): List<TimelineEvent> =
        this.map {
            EventAnnotationsSummaryEntity
                    .where(realm, roomId, eventId = it.eventId)
                    .findFirst()
                    ?.editSummary
                    ?.editions
                    ?.lastOrNull()
                    ?.eventId
                    ?.let { editedEventId ->
                        TimelineEventEntity.where(realm, roomId, eventId = editedEventId).findFirst()?.let { editedEvent ->
                            it.root.threadDetails = it.root.threadDetails?.copy(lastRootThreadEdition = editedEvent.root?.asDomain()?.getDecryptedTextSummary()
                                    ?: "(edited)")
                            it
                        } ?: it
                    } ?: it
        }

/**
 * Returns a list of all the marked unread threads that exists for the specified room
 * @param roomId The roomId that the user is currently in
 */
internal fun TimelineEventEntity.Companion.findAllLocalThreadNotificationsForRoomId(realm: Realm, roomId: String): RealmQuery<TimelineEventEntity> =
        TimelineEventEntity
                .whereRoomId(realm, roomId = roomId)
                .equalTo(TimelineEventEntityFields.ROOT.IS_ROOT_THREAD, true)
                .beginGroup()
                .equalTo(TimelineEventEntityFields.ROOT.THREAD_NOTIFICATION_STATE_STR, ThreadNotificationState.NEW_MESSAGE.name)
                .or()
                .equalTo(TimelineEventEntityFields.ROOT.THREAD_NOTIFICATION_STATE_STR, ThreadNotificationState.NEW_HIGHLIGHTED_MESSAGE.name)
                .endGroup()

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
    return timelineEventEntity?.root?.asDomain()?.isUserMentioned(currentUserId) == true
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

    if (readReceiptChunkPosition == -1) return

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
