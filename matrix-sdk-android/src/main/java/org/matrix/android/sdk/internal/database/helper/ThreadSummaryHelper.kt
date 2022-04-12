/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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
import io.realm.Sort
import io.realm.kotlin.createObject
import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.crypto.model.OlmDecryptionResult
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.threads.model.ThreadSummary
import org.matrix.android.sdk.api.session.room.threads.model.ThreadSummaryUpdateType
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntity
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntity
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.model.threads.ThreadSummaryEntity
import org.matrix.android.sdk.internal.database.model.threads.ThreadSummaryEntityFields
import org.matrix.android.sdk.internal.database.query.copyToRealmOrIgnore
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.database.query.getOrNull
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.session.events.getFixedRoomMemberContent
import org.matrix.android.sdk.internal.session.room.timeline.TimelineEventDecryptor
import timber.log.Timber
import java.util.UUID

internal fun ThreadSummaryEntity.updateThreadSummary(
        rootThreadEventEntity: EventEntity,
        numberOfThreads: Int?,
        latestThreadEventEntity: EventEntity?,
        isUserParticipating: Boolean,
        roomMemberContentsByUser: HashMap<String, RoomMemberContent?>) {
    updateThreadSummaryRootEvent(rootThreadEventEntity, roomMemberContentsByUser)
    updateThreadSummaryLatestEvent(latestThreadEventEntity, roomMemberContentsByUser)
    this.isUserParticipating = isUserParticipating
    numberOfThreads?.let {
        // Update number of threads only when there is an actual value
        this.numberOfThreads = it
    }
}

/**
 * Updates the root thread event properties
 */
internal fun ThreadSummaryEntity.updateThreadSummaryRootEvent(
        rootThreadEventEntity: EventEntity,
        roomMemberContentsByUser: HashMap<String, RoomMemberContent?>
) {
    val roomId = rootThreadEventEntity.roomId
    val rootThreadRoomMemberContent = roomMemberContentsByUser[rootThreadEventEntity.sender ?: ""]
    this.rootThreadEventEntity = rootThreadEventEntity
    this.rootThreadSenderAvatar = rootThreadRoomMemberContent?.avatarUrl
    this.rootThreadSenderName = rootThreadRoomMemberContent?.displayName
    this.rootThreadIsUniqueDisplayName = if (rootThreadRoomMemberContent?.displayName != null) {
        computeIsUnique(realm, roomId, false, rootThreadRoomMemberContent, roomMemberContentsByUser)
    } else {
        true
    }
}

/**
 * Updates the latest thread event properties
 */
internal fun ThreadSummaryEntity.updateThreadSummaryLatestEvent(
        latestThreadEventEntity: EventEntity?,
        roomMemberContentsByUser: HashMap<String, RoomMemberContent?>
) {
    val roomId = latestThreadEventEntity?.roomId ?: return
    val latestThreadRoomMemberContent = roomMemberContentsByUser[latestThreadEventEntity.sender ?: ""]
    this.latestThreadEventEntity = latestThreadEventEntity
    this.latestThreadSenderAvatar = latestThreadRoomMemberContent?.avatarUrl
    this.latestThreadSenderName = latestThreadRoomMemberContent?.displayName
    this.latestThreadIsUniqueDisplayName = if (latestThreadRoomMemberContent?.displayName != null) {
        computeIsUnique(realm, roomId, false, latestThreadRoomMemberContent, roomMemberContentsByUser)
    } else {
        true
    }
}

private fun EventEntity.toTimelineEventEntity(roomMemberContentsByUser: HashMap<String, RoomMemberContent?>): TimelineEventEntity {
    val roomId = roomId
    val eventId = eventId
    val localId = TimelineEventEntity.nextId(realm)
    val senderId = sender ?: ""

    val timelineEventEntity = realm.createObject<TimelineEventEntity>().apply {
        this.localId = localId
        this.root = this@toTimelineEventEntity
        this.eventId = eventId
        this.roomId = roomId
        this.annotations = EventAnnotationsSummaryEntity.where(realm, roomId, eventId).findFirst()
                ?.also { it.cleanUp(sender) }
        this.ownedByThreadChunk = true  // To skip it from the original event flow
        val roomMemberContent = roomMemberContentsByUser[senderId]
        this.senderAvatar = roomMemberContent?.avatarUrl
        this.senderName = roomMemberContent?.displayName
        isUniqueDisplayName = if (roomMemberContent?.displayName != null) {
            computeIsUnique(realm, roomId, false, roomMemberContent, roomMemberContentsByUser)
        } else {
            true
        }
    }
    return timelineEventEntity
}

internal suspend fun ThreadSummaryEntity.Companion.createOrUpdate(
        threadSummaryType: ThreadSummaryUpdateType,
        realm: Realm,
        roomId: String,
        threadEventEntity: EventEntity? = null,
        rootThreadEvent: Event? = null,
        roomMemberContentsByUser: HashMap<String, RoomMemberContent?>,
        roomEntity: RoomEntity,
        userId: String,
        cryptoService: CryptoService? = null
) {
    when (threadSummaryType) {
        ThreadSummaryUpdateType.REPLACE -> {
            rootThreadEvent?.eventId ?: return
            rootThreadEvent.senderId ?: return

            val numberOfThreads = rootThreadEvent.unsignedData?.relations?.latestThread?.count ?: return

            // Something is wrong with the server return
            if (numberOfThreads <= 0) return

            val threadSummary = ThreadSummaryEntity.getOrCreate(realm, roomId, rootThreadEvent.eventId).also {
                Timber.i("###THREADS ThreadSummaryHelper REPLACE eventId:${it.rootThreadEventId} ")
            }

            val rootThreadEventEntity = createEventEntity(roomId, rootThreadEvent, realm).also {
                decryptIfNeeded(cryptoService, it, roomId)
            }
            val latestThreadEventEntity = createLatestEventEntity(roomId, rootThreadEvent, roomMemberContentsByUser, realm)?.also {
                decryptIfNeeded(cryptoService, it, roomId)
            }
            val isUserParticipating = rootThreadEvent.unsignedData.relations.latestThread.isUserParticipating == true || rootThreadEvent.senderId == userId
            roomMemberContentsByUser.addSenderState(realm, roomId, rootThreadEvent.senderId)
            threadSummary.updateThreadSummary(
                    rootThreadEventEntity = rootThreadEventEntity,
                    numberOfThreads = numberOfThreads,
                    latestThreadEventEntity = latestThreadEventEntity,
                    isUserParticipating = isUserParticipating,
                    roomMemberContentsByUser = roomMemberContentsByUser
            )

            roomEntity.addIfNecessary(threadSummary)
        }
        ThreadSummaryUpdateType.ADD     -> {
            val rootThreadEventId = threadEventEntity?.rootThreadEventId ?: return
            Timber.i("###THREADS ThreadSummaryHelper ADD for root eventId:$rootThreadEventId")

            val threadSummary = ThreadSummaryEntity.getOrNull(realm, roomId, rootThreadEventId)
            if (threadSummary != null) {
                // ThreadSummary exists so lets add the latest event
                Timber.i("###THREADS ThreadSummaryHelper ADD root eventId:$rootThreadEventId exists, lets update latest thread event.")
                threadSummary.updateThreadSummaryLatestEvent(threadEventEntity, roomMemberContentsByUser)
                threadSummary.numberOfThreads++
                if (threadEventEntity.sender == userId) {
                    threadSummary.isUserParticipating = true
                }
            } else {
                // ThreadSummary do not exists lets try to create one
                Timber.i("###THREADS ThreadSummaryHelper ADD root eventId:$rootThreadEventId do not exists, lets try to create one")
                threadEventEntity.findRootThreadEvent()?.let { rootThreadEventEntity ->
                    // Root thread event entity exists so lets create a new record
                    ThreadSummaryEntity.getOrCreate(realm, roomId, rootThreadEventEntity.eventId).let {
                        it.updateThreadSummary(
                                rootThreadEventEntity = rootThreadEventEntity,
                                numberOfThreads = 1,
                                latestThreadEventEntity = threadEventEntity,
                                isUserParticipating = threadEventEntity.sender == userId,
                                roomMemberContentsByUser = roomMemberContentsByUser
                        )
                        roomEntity.addIfNecessary(it)
                    }
                }
            }
        }
    }
}

private suspend fun decryptIfNeeded(cryptoService: CryptoService?, eventEntity: EventEntity, roomId: String) {
    cryptoService ?: return
    val event = eventEntity.asDomain()
    if (event.isEncrypted() && event.mxDecryptionResult == null && event.eventId != null) {
        try {
            Timber.i("###THREADS ThreadSummaryHelper request decryption for eventId:${event.eventId}")
            // Event from sync does not have roomId, so add it to the event first
            val result = cryptoService.decryptEvent(event.copy(roomId = roomId), "")
            event.mxDecryptionResult = OlmDecryptionResult(
                    payload = result.clearEvent,
                    senderKey = result.senderCurve25519Key,
                    keysClaimed = result.claimedEd25519Key?.let { k -> mapOf("ed25519" to k) },
                    forwardingCurve25519KeyChain = result.forwardingCurve25519KeyChain
            )
            // Save decryption result, to not decrypt every time we enter the thread list
            eventEntity.setDecryptionResult(result)
        } catch (e: MXCryptoError) {
            if (e is MXCryptoError.Base) {
                event.mCryptoError = e.errorType
                event.mCryptoErrorReason = e.technicalMessage.takeIf { it.isNotEmpty() } ?: e.detailedErrorDescription
            }
        }
    }
}

/**
 * Request decryption
 */
private fun requestDecryption(eventDecryptor: TimelineEventDecryptor?, event: Event?) {
    eventDecryptor ?: return
    event ?: return
    if (event.isEncrypted() &&
            event.mxDecryptionResult == null && event.eventId != null) {
        Timber.i("###THREADS ThreadSummaryHelper request decryption for eventId:${event.eventId}")

        eventDecryptor.requestDecryption(TimelineEventDecryptor.DecryptionRequest(event, UUID.randomUUID().toString()))
    }
}

/**
 * If we don't have any new state on this user, get it from db
 */
private fun HashMap<String, RoomMemberContent?>.addSenderState(realm: Realm, roomId: String, senderId: String) {
    getOrPut(senderId) {
        CurrentStateEventEntity
                .getOrNull(realm, roomId, senderId, EventType.STATE_ROOM_MEMBER)
                ?.root?.asDomain()
                ?.getFixedRoomMemberContent()
    }
}

/**
 * Create an EventEntity for the root thread event or get an existing one
 */
private fun createEventEntity(roomId: String, event: Event, realm: Realm): EventEntity {
    val ageLocalTs = event.unsignedData?.age?.let { System.currentTimeMillis() - it }
    return event.toEntity(roomId, SendState.SYNCED, ageLocalTs).copyToRealmOrIgnore(realm, EventInsertType.PAGINATION)
}

/**
 * Create an EventEntity for the latest thread event or get an existing one. Also update the user room member
 * state
 */
private fun createLatestEventEntity(
        roomId: String,
        rootThreadEvent: Event,
        roomMemberContentsByUser: HashMap<String, RoomMemberContent?>,
        realm: Realm): EventEntity? {
    return getLatestEvent(rootThreadEvent)?.let {
        it.senderId?.let { senderId ->
            roomMemberContentsByUser.addSenderState(realm, roomId, senderId)
        }
        createEventEntity(roomId, it, realm)
    }
}

/**
 * Returned the latest event message, if any
 */
private fun getLatestEvent(rootThreadEvent: Event): Event? {
    return rootThreadEvent.unsignedData?.relations?.latestThread?.event
}

/**
 * Find all ThreadSummaryEntity for the specified roomId, sorted by origin server
 * note: Sorting cannot be provided by server, so we have to use that unstable property
 * @param roomId The id of the room
 */
internal fun ThreadSummaryEntity.Companion.findAllThreadsForRoomId(realm: Realm, roomId: String): RealmQuery<ThreadSummaryEntity> =
        ThreadSummaryEntity
                .where(realm, roomId = roomId)
                .sort(ThreadSummaryEntityFields.LATEST_THREAD_EVENT_ENTITY.ORIGIN_SERVER_TS, Sort.DESCENDING)

/**
 * Enhance each [ThreadSummary] root and latest event with the equivalent decrypted text edition/replacement
 */
internal fun List<ThreadSummary>.enhanceWithEditions(realm: Realm, roomId: String): List<ThreadSummary> =
        this.map {
            it.addEditionIfNeeded(realm, roomId, true)
            it.addEditionIfNeeded(realm, roomId, false)
            it
        }

private fun ThreadSummary.addEditionIfNeeded(realm: Realm, roomId: String, enhanceRoot: Boolean) {
    val eventId = if (enhanceRoot) rootEventId else latestEvent?.eventId ?: return
    EventAnnotationsSummaryEntity
            .where(realm, roomId, eventId)
            .findFirst()
            ?.editSummary
            ?.editions
            ?.lastOrNull()
            ?.eventId
            ?.let { editedEventId ->
                TimelineEventEntity.where(realm, roomId, eventId = editedEventId).findFirst()?.let { editedEvent ->
                    if (enhanceRoot) {
                        threadEditions.rootThreadEdition = editedEvent.root?.asDomain()?.getDecryptedTextSummary() ?: "(edited)"
                    } else {
                        threadEditions.latestThreadEdition = editedEvent.root?.asDomain()?.getDecryptedTextSummary() ?: "(edited)"
                    }
                }
            }
}
