/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.sync.handler.room

import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageFormat
import org.matrix.android.sdk.api.session.room.model.message.MessageRelationContent
import org.matrix.android.sdk.api.session.room.model.message.MessageTextContent
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.sync.model.SyncResponse
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.crypto.MXEventDecryptionResult
import org.matrix.android.sdk.internal.crypto.algorithms.olm.OlmDecryptionResult
import org.matrix.android.sdk.internal.database.mapper.EventMapper
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.database.query.copyToRealmOrIgnore
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.permalinks.PermalinkFactory
import org.matrix.android.sdk.internal.session.room.send.LocalEchoEventFactory
import org.matrix.android.sdk.internal.session.room.timeline.GetEventTask
import org.matrix.android.sdk.internal.util.awaitTransaction
import javax.inject.Inject

/**
 * This handler is responsible for a smooth threads migration. It will map all incoming
 * threads as replies. So a device without threads enabled/updated will be able to view
 * threads response as replies to the original message
 */
internal class ThreadsAwarenessHandler @Inject constructor(
        private val permalinkFactory: PermalinkFactory,
        private val cryptoService: CryptoService,
        @SessionDatabase private val monarchy: Monarchy,
        private val getEventTask: GetEventTask
) {

    /**
     * Fetch root thread events if they are missing from the local storage
     * @param syncResponse the sync response
     */
    suspend fun fetchRootThreadEventsIfNeeded(syncResponse: SyncResponse) {
        val handlingStrategy = syncResponse.rooms?.join?.let {
            RoomSyncHandler.HandlingStrategy.JOINED(it)
        }
        if (handlingStrategy !is RoomSyncHandler.HandlingStrategy.JOINED) return
        val eventList = handlingStrategy.data
                .mapNotNull { (roomId, roomSync) ->
                    roomSync.timeline?.events?.map {
                        it.copy(roomId = roomId)
                    }
                }.flatten()

        fetchRootThreadEventsIfNeeded(eventList)
    }

    /**
     * Fetch root thread events if they are missing from the local storage
     * @param eventList a list with the events to examine
     */
    suspend fun fetchRootThreadEventsIfNeeded(eventList: List<Event>) {
        if (eventList.isNullOrEmpty()) return

        val threadsToFetch = emptyMap<String, String>().toMutableMap()
        Realm.getInstance(monarchy.realmConfiguration).use {  realm ->
            eventList.asSequence()
                    .filter {
                        isThreadEvent(it) && it.roomId != null
                    }.mapNotNull { event ->
                        getRootThreadEventId(event)?.let {
                            Pair(it, event.roomId!!)
                        }
                    }.forEach { (rootThreadEventId, roomId) ->
                        EventEntity.where(realm, rootThreadEventId).findFirst() ?: run { threadsToFetch[rootThreadEventId] = roomId }
                    }
        }
        fetchThreadsEvents(threadsToFetch)
    }

    /**
     * Fetch multiple unique events using the fetchEvent function
     */
    private suspend fun fetchThreadsEvents(threadsToFetch: Map<String, String>) {
        val eventEntityList = threadsToFetch.mapNotNull { (eventId, roomId) ->
            fetchEvent(eventId, roomId)?.let {
                it.toEntity(roomId, SendState.SYNCED, it.ageLocalTs)
            }
        }

        if (eventEntityList.isNullOrEmpty()) return

        // Transaction should be done on its own thread, like below
        monarchy.awaitTransaction { realm ->
            eventEntityList.forEach {
                it.copyToRealmOrIgnore(realm, EventInsertType.INCREMENTAL_SYNC)
            }
        }
    }

    /**
     * This function will fetch the event from the homeserver, this is mandatory when the
     * initial thread message is too old and is not saved in the device, so in order to
     * construct the "reply to" format we need to know the event thread.
     * @return the Event or null otherwise
     */
    private suspend fun fetchEvent(eventId: String, roomId: String): Event? {
        return runCatching {
            getEventTask.execute(GetEventTask.Params(roomId = roomId, eventId = eventId))
        }.fold(
                onSuccess = {
                    it
                },
                onFailure = {
                    null
                })
    }

    /**
     * Handle events mainly coming from the RoomSyncHandler
     */
    fun handleIfNeeded(realm: Realm,
                       roomId: String,
                       event: Event) {
        val payload = transformThreadToReplyIfNeeded(
                realm = realm,
                roomId = roomId,
                event = event,
                decryptedResult = event.mxDecryptionResult?.payload) ?: return

        event.mxDecryptionResult = event.mxDecryptionResult?.copy(payload = payload)
    }

    /**
     * Handle events while they are being decrypted
     */
    fun handleIfNeededDuringDecryption(realm: Realm,
                                       roomId: String?,
                                       event: Event,
                                       result: MXEventDecryptionResult): JsonDict? {
        return transformThreadToReplyIfNeeded(
                realm = realm,
                roomId = roomId,
                event = event,
                decryptedResult = result.clearEvent)
    }

    /**
     * If the event is a thread event then transform/enhance it to a visual Reply Event,
     * If the event is not a thread event, null value will be returned
     * If there is an error (ex. the root/origin thread event is not found), null willl be returend
     */
    private fun transformThreadToReplyIfNeeded(realm: Realm, roomId: String?, event: Event, decryptedResult: JsonDict?): JsonDict? {
        roomId ?: return null
        if (!isThreadEvent(event)) return null
        val rootThreadEventId = getRootThreadEventId(event) ?: return null
        val payload = decryptedResult?.toMutableMap() ?: return null
        val body = getValueFromPayload(payload, "body") ?: return null
        val msgType = getValueFromPayload(payload, "msgtype") ?: return null
        val rootThreadEvent = getEventFromDB(realm, rootThreadEventId) ?: return null
        val rootThreadEventSenderId = rootThreadEvent.senderId ?: return null

        decryptIfNeeded(rootThreadEvent, roomId)

        val rootThreadEventBody = getValueFromPayload(rootThreadEvent.mxDecryptionResult?.payload?.toMutableMap(), "body")

        val permalink = permalinkFactory.createPermalink(roomId, rootThreadEventId, false)
        val userLink = permalinkFactory.createPermalink(rootThreadEventSenderId, false) ?: ""

        val replyFormatted = LocalEchoEventFactory.REPLY_PATTERN.format(
                permalink,
                userLink,
                rootThreadEventSenderId,
                // Remove inner mx_reply tags if any
                rootThreadEventBody,
                body)

        val messageTextContent = MessageTextContent(
                msgType = msgType,
                format = MessageFormat.FORMAT_MATRIX_HTML,
                body = body,
                formattedBody = replyFormatted
        ).toContent()

        payload["content"] = messageTextContent

        return payload
    }

    /**
     * Decrypt the event
     */

    private fun decryptIfNeeded(event: Event, roomId: String) {
        try {
            if (!event.isEncrypted() || event.mxDecryptionResult != null) return

            // Event from sync does not have roomId, so add it to the event first
            val result = cryptoService.decryptEvent(event.copy(roomId = roomId), "")
            event.mxDecryptionResult = OlmDecryptionResult(
                    payload = result.clearEvent,
                    senderKey = result.senderCurve25519Key,
                    keysClaimed = result.claimedEd25519Key?.let { k -> mapOf("ed25519" to k) },
                    forwardingCurve25519KeyChain = result.forwardingCurve25519KeyChain
            )
        } catch (e: MXCryptoError) {
            if (e is MXCryptoError.Base) {
                event.mCryptoError = e.errorType
                event.mCryptoErrorReason = e.technicalMessage.takeIf { it.isNotEmpty() } ?: e.detailedErrorDescription
            }
        }
    }

    /**
     * Try to get the event form the local DB, if the event does not exist null
     * will be returned
     */
    private fun getEventFromDB(realm: Realm, eventId: String): Event? {
        val eventEntity = EventEntity.where(realm, eventId = eventId).findFirst() ?: return null
        return EventMapper.map(eventEntity)
    }

    /**
     * Returns True if the event is a thread
     * @param event
     */
    private fun isThreadEvent(event: Event): Boolean =
            event.content.toModel<MessageRelationContent>()?.relatesTo?.type == RelationType.THREAD

    /**
     * Returns the root thread eventId or null otherwise
     * @param event
     */
    private fun getRootThreadEventId(event: Event): String? =
            event.content.toModel<MessageRelationContent>()?.relatesTo?.eventId

    @Suppress("UNCHECKED_CAST")
    private fun getValueFromPayload(payload: JsonDict?, key: String): String? {
        val content = payload?.get("content") as? JsonDict
        return content?.get(key) as? String
    }
}
