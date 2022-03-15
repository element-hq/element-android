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
import io.realm.kotlin.where
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.getRelationContentForType
import org.matrix.android.sdk.api.session.events.model.getRootThreadEventId
import org.matrix.android.sdk.api.session.events.model.isSticker
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageFormat
import org.matrix.android.sdk.api.session.room.model.message.MessageRelationContent
import org.matrix.android.sdk.api.session.room.model.message.MessageTextContent
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.sync.model.SyncResponse
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.crypto.algorithms.olm.OlmDecryptionResult
import org.matrix.android.sdk.internal.database.lightweight.LightweightSettingsStorage
import org.matrix.android.sdk.internal.database.mapper.ContentMapper
import org.matrix.android.sdk.internal.database.mapper.EventMapper
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.EventEntityFields
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.database.query.copyToRealmOrIgnore
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.MoshiProvider
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
        @SessionDatabase private val monarchy: Monarchy,
        private val lightweightSettingsStorage: LightweightSettingsStorage,
        private val getEventTask: GetEventTask
) {

    // This caching is responsible to improve the performance when we receive a root event
    // to be able to know this event is a root one without checking the DB,
    // We update the list with all thread root events by checking if there is a m.thread relation on the events
    private val cacheEventRootId = hashSetOf<String>()

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
        Realm.getInstance(monarchy.realmConfiguration).use { realm ->
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
     * @return The content to inject in the roomSyncHandler live events
     */
    fun makeEventThreadAware(realm: Realm,
                             roomId: String?,
                             event: Event?,
                             eventEntity: EventEntity? = null): String? {
        event ?: return null
        roomId ?: return null
        if (lightweightSettingsStorage.areThreadMessagesEnabled() && !isReplyEvent(event)) return null
        handleRootThreadEventsIfNeeded(realm, roomId, eventEntity, event)
        if (!isThreadEvent(event)) return null
        val eventPayload = if (!event.isEncrypted()) {
            event.content?.toMutableMap() ?: return null
        } else {
            event.mxDecryptionResult?.payload?.toMutableMap() ?: return null
        }
        val eventBody = event.getDecryptedTextSummary() ?: return null
        val threadRelation = getRootThreadRelationContent(event)
        val eventIdToInject = getPreviousEventOrRoot(event) ?: run {
            return@makeEventThreadAware injectFallbackIndicator(event, eventBody, eventEntity, eventPayload, threadRelation)
        }
        val eventToInject = getEventFromDB(realm, eventIdToInject)
        val eventToInjectBody = eventToInject?.getDecryptedTextSummary()
        var contentForNonEncrypted: String?
        if (eventToInject != null && eventToInjectBody != null) {
            // If the event to inject exists and is decrypted
            // Inject it to our event
            val messageTextContent = injectEvent(
                    roomId = roomId,
                    eventBody = eventBody,
                    eventToInject = eventToInject,
                    eventToInjectBody = eventToInjectBody,
                    threadRelation = threadRelation) ?: return null

            // update the event
            contentForNonEncrypted = updateEventEntity(event, eventEntity, eventPayload, messageTextContent)
        } else {
            contentForNonEncrypted = injectFallbackIndicator(event, eventBody, eventEntity, eventPayload, threadRelation)
        }

        // Now lets try to find relations for improved results, while some events may come with reverse order
        eventEntity?.let {
            // When eventEntity is not null means that we are not from within roomSyncHandler
            handleEventsThatRelatesTo(realm, roomId, event, eventBody, false, threadRelation)
        }
        return contentForNonEncrypted
    }

    /**
     * Handle for not thread events that we have marked them as root.
     * Find relations and inject them accordingly
     * @param eventEntity the current eventEntity received
     * @param event the current event received
     * @return The content to inject in the roomSyncHandler live events
     */
    private fun handleRootThreadEventsIfNeeded(
            realm: Realm,
            roomId: String,
            eventEntity: EventEntity?,
            event: Event
    ): String? {
        if (!isThreadEvent(event) && cacheEventRootId.contains(eventEntity?.eventId)) {
            eventEntity?.let {
                val eventBody = event.getDecryptedTextSummary() ?: return null
                return handleEventsThatRelatesTo(realm, roomId, event, eventBody, true, null)
            }
        }
        return null
    }

    /**
     * This function is responsible to check if there is any event that relates to our current event
     * This is useful when we receive an event that relates to a missing parent, so when later we receive the parent
     * we can update the child as well
     * @param event the current event that we examine
     * @param eventBody the current body of the event
     * @param isFromCache determines whether or not we already know this is root thread event
     * @return The content to inject in the roomSyncHandler live events
     */
    private fun handleEventsThatRelatesTo(
            realm: Realm,
            roomId: String,
            event: Event,
            eventBody: String,
            isFromCache: Boolean,
            threadRelation: RelationDefaultContent?
    ): String? {
        event.eventId ?: return null
        val rootThreadEventId = if (isFromCache) event.eventId else event.getRootThreadEventId() ?: return null
        eventThatRelatesTo(realm, event.eventId, rootThreadEventId)?.forEach { eventEntityFound ->
            val newEventFound = eventEntityFound.asDomain()
            val newEventBody = newEventFound.getDecryptedTextSummary() ?: return null
            val newEventPayload = newEventFound.mxDecryptionResult?.payload?.toMutableMap() ?: return null

            val messageTextContent = injectEvent(
                    roomId = roomId,
                    eventBody = newEventBody,
                    eventToInject = event,
                    eventToInjectBody = eventBody,
                    threadRelation = threadRelation) ?: return null

            return updateEventEntity(newEventFound, eventEntityFound, newEventPayload, messageTextContent)
        }
        return null
    }

    /**
     * Actual update the eventEntity with the new payload
     * @return the content to inject when this is executed by RoomSyncHandler
     */
    private fun updateEventEntity(event: Event,
                                  eventEntity: EventEntity?,
                                  eventPayload: MutableMap<String, Any>,
                                  messageTextContent: Content): String? {
        eventPayload["content"] = messageTextContent

        if (event.isEncrypted()) {
            if (event.isSticker()) {
                eventPayload["type"] = EventType.MESSAGE
            }
            event.mxDecryptionResult = event.mxDecryptionResult?.copy(payload = eventPayload)
            eventEntity?.decryptionResultJson = event.mxDecryptionResult?.let {
                MoshiProvider.providesMoshi().adapter(OlmDecryptionResult::class.java).toJson(it)
            }
        } else {
            if (event.type == EventType.STICKER) {
                eventEntity?.type = EventType.MESSAGE
            }
            eventEntity?.content = ContentMapper.map(messageTextContent)
            return ContentMapper.map(messageTextContent)
        }
        return null
    }

    /**
     * Injecting $eventToInject decrypted content as a reply to $event
     * @param eventToInject the event that will inject
     * @param eventBody the actual event body
     * @return The final content with the injected event
     */
    private fun injectEvent(roomId: String,
                            eventBody: String,
                            eventToInject: Event,
                            eventToInjectBody: String,
                            threadRelation: RelationDefaultContent?
    ): Content? {
        val eventToInjectId = eventToInject.eventId ?: return null
        val eventIdToInjectSenderId = eventToInject.senderId.orEmpty()
        val permalink = permalinkFactory.createPermalink(roomId, eventToInjectId, false)
        val userLink = permalinkFactory.createPermalink(eventIdToInjectSenderId, false) ?: ""
        val replyFormatted = LocalEchoEventFactory.REPLY_PATTERN.format(
                permalink,
                userLink,
                eventIdToInjectSenderId,
                eventToInjectBody,
                eventBody)

        return MessageTextContent(
                relatesTo = threadRelation,
                msgType = MessageType.MSGTYPE_TEXT,
                format = MessageFormat.FORMAT_MATRIX_HTML,
                body = eventBody,
                formattedBody = replyFormatted
        ).toContent()
    }

    /**
     * Integrate fallback Quote reply
     */
    private fun injectFallbackIndicator(event: Event,
                                        eventBody: String,
                                        eventEntity: EventEntity?,
                                        eventPayload: MutableMap<String, Any>,
                                        threadRelation: RelationDefaultContent?): String? {
        val replyFormatted = LocalEchoEventFactory.QUOTE_PATTERN.format(
                "In reply to a thread",
                eventBody)

        val messageTextContent = MessageTextContent(
                relatesTo = threadRelation,
                msgType = MessageType.MSGTYPE_TEXT,
                format = MessageFormat.FORMAT_MATRIX_HTML,
                body = eventBody,
                formattedBody = replyFormatted
        ).toContent()

        return updateEventEntity(event, eventEntity, eventPayload, messageTextContent)
    }

    private fun eventThatRelatesTo(realm: Realm, currentEventId: String, rootThreadEventId: String): List<EventEntity>? {
        val threadList = realm.where<EventEntity>()
                .beginGroup()
                .equalTo(EventEntityFields.ROOT_THREAD_EVENT_ID, rootThreadEventId)
                .or()
                .equalTo(EventEntityFields.EVENT_ID, rootThreadEventId)
                .endGroup()
                .and()
                .findAll()
        cacheEventRootId.add(rootThreadEventId)
        return threadList.filter {
            it.asDomain().getRelationContentForType(RelationType.THREAD)?.inReplyTo?.eventId == currentEventId
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

    private fun getRootThreadRelationContent(event: Event): RelationDefaultContent? =
            event.content.toModel<MessageRelationContent>()?.relatesTo

    private fun getPreviousEventOrRoot(event: Event): String? =
            event.content.toModel<MessageRelationContent>()?.relatesTo?.inReplyTo?.eventId

    /**
     * Returns if we should html inject the current event.
     */
    private fun isReplyEvent(event: Event): Boolean {
        return isThreadEvent(event) && !isFallingBack(event) && getPreviousEventOrRoot(event) != null
    }

    private fun isFallingBack(event: Event): Boolean =
            event.content.toModel<MessageRelationContent>()?.relatesTo?.isFallingBack == true

    @Suppress("UNCHECKED_CAST")
    private fun getValueFromPayload(payload: JsonDict?, key: String): String? {
        val content = payload?.get("content") as? JsonDict
        return content?.get(key) as? String
    }
}
