/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

import dagger.Lazy
import io.realm.Realm
import io.realm.kotlin.createObject
import kotlinx.coroutines.runBlocking
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilitiesService
import org.matrix.android.sdk.api.session.initsync.InitSyncStep
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.threads.model.ThreadSummaryUpdateType
import org.matrix.android.sdk.api.session.sync.model.InvitedRoomSync
import org.matrix.android.sdk.api.session.sync.model.LazyRoomSyncEphemeral
import org.matrix.android.sdk.api.session.sync.model.RoomSync
import org.matrix.android.sdk.api.session.sync.model.RoomsSyncResponse
import org.matrix.android.sdk.internal.crypto.DefaultCryptoService
import org.matrix.android.sdk.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.internal.crypto.algorithms.olm.OlmDecryptionResult
import org.matrix.android.sdk.internal.database.helper.addIfNecessary
import org.matrix.android.sdk.internal.database.helper.addTimelineEvent
import org.matrix.android.sdk.internal.database.helper.createOrUpdate
import org.matrix.android.sdk.internal.database.helper.updateThreadSummaryIfNeeded
import org.matrix.android.sdk.internal.database.lightweight.LightweightSettingsStorage
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntity
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.model.deleteOnCascade
import org.matrix.android.sdk.internal.database.model.threads.ThreadSummaryEntity
import org.matrix.android.sdk.internal.database.query.copyToRealmOrIgnore
import org.matrix.android.sdk.internal.database.query.find
import org.matrix.android.sdk.internal.database.query.findLastForwardChunkOfRoom
import org.matrix.android.sdk.internal.database.query.findLastForwardChunkOfThread
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.database.query.getOrNull
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.extensions.clearWith
import org.matrix.android.sdk.internal.session.StreamEventsManager
import org.matrix.android.sdk.internal.session.events.getFixedRoomMemberContent
import org.matrix.android.sdk.internal.session.initsync.ProgressReporter
import org.matrix.android.sdk.internal.session.initsync.mapWithProgress
import org.matrix.android.sdk.internal.session.initsync.reportSubtask
import org.matrix.android.sdk.internal.session.room.membership.RoomChangeMembershipStateDataSource
import org.matrix.android.sdk.internal.session.room.membership.RoomMemberEventHandler
import org.matrix.android.sdk.internal.session.room.summary.RoomSummaryUpdater
import org.matrix.android.sdk.internal.session.room.timeline.PaginationDirection
import org.matrix.android.sdk.internal.session.room.timeline.TimelineInput
import org.matrix.android.sdk.internal.session.room.typing.TypingEventContent
import org.matrix.android.sdk.internal.session.sync.InitialSyncStrategy
import org.matrix.android.sdk.internal.session.sync.SyncResponsePostTreatmentAggregator
import org.matrix.android.sdk.internal.session.sync.initialSyncStrategy
import org.matrix.android.sdk.internal.session.sync.parsing.RoomSyncAccountDataHandler
import org.matrix.android.sdk.internal.util.computeBestChunkSize
import timber.log.Timber
import javax.inject.Inject

internal class RoomSyncHandler @Inject constructor(private val readReceiptHandler: ReadReceiptHandler,
                                                   private val roomSummaryUpdater: RoomSummaryUpdater,
                                                   private val roomAccountDataHandler: RoomSyncAccountDataHandler,
                                                   private val cryptoService: DefaultCryptoService,
                                                   private val roomMemberEventHandler: RoomMemberEventHandler,
                                                   private val roomTypingUsersHandler: RoomTypingUsersHandler,
                                                   private val threadsAwarenessHandler: ThreadsAwarenessHandler,
                                                   private val roomChangeMembershipStateDataSource: RoomChangeMembershipStateDataSource,
                                                   @UserId private val userId: String,
                                                   private val homeServerCapabilitiesService: HomeServerCapabilitiesService,
                                                   private val lightweightSettingsStorage: LightweightSettingsStorage,
                                                   private val timelineInput: TimelineInput,
                                                   private val liveEventService: Lazy<StreamEventsManager>) {

    sealed class HandlingStrategy {
        data class JOINED(val data: Map<String, RoomSync>) : HandlingStrategy()
        data class INVITED(val data: Map<String, InvitedRoomSync>) : HandlingStrategy()
        data class LEFT(val data: Map<String, RoomSync>) : HandlingStrategy()
    }

    suspend fun handle(realm: Realm,
                       roomsSyncResponse: RoomsSyncResponse,
                       isInitialSync: Boolean,
                       aggregator: SyncResponsePostTreatmentAggregator,
                       reporter: ProgressReporter? = null) {
        Timber.v("Execute transaction from $this")
        handleRoomSync(realm, HandlingStrategy.JOINED(roomsSyncResponse.join), isInitialSync, aggregator, reporter)
        handleRoomSync(realm, HandlingStrategy.INVITED(roomsSyncResponse.invite), isInitialSync, aggregator, reporter)
        handleRoomSync(realm, HandlingStrategy.LEFT(roomsSyncResponse.leave), isInitialSync, aggregator, reporter)

        // post room sync validation
//        roomSummaryUpdater.validateSpaceRelationship(realm)
    }

    fun postSyncSpaceHierarchyHandle(realm: Realm) {
        roomSummaryUpdater.validateSpaceRelationship(realm)
    }
    // PRIVATE METHODS *****************************************************************************

    private suspend fun handleRoomSync(realm: Realm,
                                       handlingStrategy: HandlingStrategy,
                                       isInitialSync: Boolean,
                                       aggregator: SyncResponsePostTreatmentAggregator,
                                       reporter: ProgressReporter?) {
        val insertType = if (isInitialSync) {
            EventInsertType.INITIAL_SYNC
        } else {
            EventInsertType.INCREMENTAL_SYNC
        }
        val syncLocalTimeStampMillis = System.currentTimeMillis()
        val rooms = when (handlingStrategy) {
            is HandlingStrategy.JOINED  -> {
                if (isInitialSync && initialSyncStrategy is InitialSyncStrategy.Optimized) {
                    insertJoinRoomsFromInitSync(realm, handlingStrategy, syncLocalTimeStampMillis, aggregator, reporter)
                    // Rooms are already inserted, return an empty list
                    emptyList()
                } else {
                    handlingStrategy.data.mapWithProgress(reporter, InitSyncStep.ImportingAccountJoinedRooms, 0.6f) {
                        handleJoinedRoom(realm, it.key, it.value, insertType, syncLocalTimeStampMillis, aggregator)
                    }
                }
            }
            is HandlingStrategy.INVITED ->
                handlingStrategy.data.mapWithProgress(reporter, InitSyncStep.ImportingAccountInvitedRooms, 0.1f) {
                    handleInvitedRoom(realm, it.key, it.value, insertType, syncLocalTimeStampMillis)
                }

            is HandlingStrategy.LEFT    -> {
                handlingStrategy.data.mapWithProgress(reporter, InitSyncStep.ImportingAccountLeftRooms, 0.3f) {
                    handleLeftRoom(realm, it.key, it.value, insertType, syncLocalTimeStampMillis)
                }
            }
        }
        realm.insertOrUpdate(rooms)
    }

    private suspend fun insertJoinRoomsFromInitSync(realm: Realm,
                                                    handlingStrategy: HandlingStrategy.JOINED,
                                                    syncLocalTimeStampMillis: Long,
                                                    aggregator: SyncResponsePostTreatmentAggregator,
                                                    reporter: ProgressReporter?) {
        val bestChunkSize = computeBestChunkSize(
                listSize = handlingStrategy.data.keys.size,
                limit = (initialSyncStrategy as? InitialSyncStrategy.Optimized)?.maxRoomsToInsert ?: Int.MAX_VALUE
        )

        if (bestChunkSize.shouldChunk()) {
            reportSubtask(reporter, InitSyncStep.ImportingAccountJoinedRooms, bestChunkSize.numberOfChunks, 0.6f) {
                Timber.d("INIT_SYNC ${handlingStrategy.data.keys.size} rooms to insert, split with $bestChunkSize")
                // I cannot find a better way to chunk a map, so chunk the keys and then create new maps
                handlingStrategy.data.keys
                        .chunked(bestChunkSize.chunkSize)
                        .forEachIndexed { index, roomIds ->
                            val roomEntities = roomIds
                                    .also { Timber.d("INIT_SYNC insert ${roomIds.size} rooms") }
                                    .map {
                                        handleJoinedRoom(
                                                realm = realm,
                                                roomId = it,
                                                roomSync = handlingStrategy.data[it] ?: error("Should not happen"),
                                                insertType = EventInsertType.INITIAL_SYNC,
                                                syncLocalTimestampMillis = syncLocalTimeStampMillis,
                                                aggregator
                                        )
                                    }
                            realm.insertOrUpdate(roomEntities)
                            reporter?.reportProgress(index + 1F)
                        }
            }
        } else {
            // No need to split
            val rooms = handlingStrategy.data.mapWithProgress(reporter, InitSyncStep.ImportingAccountJoinedRooms, 0.6f) {
                handleJoinedRoom(realm, it.key, it.value, EventInsertType.INITIAL_SYNC, syncLocalTimeStampMillis, aggregator)
            }
            realm.insertOrUpdate(rooms)
        }
    }

    private suspend fun handleJoinedRoom(realm: Realm,
                                         roomId: String,
                                         roomSync: RoomSync,
                                         insertType: EventInsertType,
                                         syncLocalTimestampMillis: Long,
                                         aggregator: SyncResponsePostTreatmentAggregator): RoomEntity {
        Timber.v("Handle join sync for room $roomId")

        val ephemeralResult = (roomSync.ephemeral as? LazyRoomSyncEphemeral.Parsed)
                ?._roomSyncEphemeral
                ?.events
                ?.takeIf { it.isNotEmpty() }
                ?.let { handleEphemeral(realm, roomId, it, insertType == EventInsertType.INITIAL_SYNC, aggregator) }

        if (roomSync.accountData != null) {
            roomAccountDataHandler.handle(realm, roomId, roomSync.accountData)
        }

        val roomEntity = RoomEntity.getOrCreate(realm, roomId)

        if (roomEntity.membership == Membership.INVITE) {
            roomEntity.chunks.deleteAllFromRealm()
        }
        roomEntity.membership = Membership.JOIN

        // State event
        if (roomSync.state?.events?.isNotEmpty() == true) {
            for (event in roomSync.state.events) {
                if (event.eventId == null || event.stateKey == null || event.type == null) {
                    continue
                }
                val ageLocalTs = event.unsignedData?.age?.let { syncLocalTimestampMillis - it }
                val eventEntity = event.toEntity(roomId, SendState.SYNCED, ageLocalTs).copyToRealmOrIgnore(realm, insertType)
                Timber.v("## received state event ${event.type} and key ${event.stateKey}")
                CurrentStateEventEntity.getOrCreate(realm, roomId, event.stateKey, event.type).apply {
                    // Timber.v("## Space state event: $eventEntity")
                    eventId = event.eventId
                    root = eventEntity
                }
                // Give info to crypto module
                cryptoService.onStateEvent(roomId, event)
                roomMemberEventHandler.handle(realm, roomId, event, aggregator)
            }
        }
        if (roomSync.timeline?.events?.isNotEmpty() == true) {
            val chunkEntity = handleTimelineEvents(
                    realm,
                    roomId,
                    roomEntity,
                    roomSync.timeline.events,
                    roomSync.timeline.prevToken,
                    roomSync.timeline.limited,
                    insertType,
                    syncLocalTimestampMillis,
                    aggregator
            )
            roomEntity.addIfNecessary(chunkEntity)
        }
        val hasRoomMember = roomSync.state?.events?.firstOrNull {
            it.type == EventType.STATE_ROOM_MEMBER
        } != null || roomSync.timeline?.events?.firstOrNull {
            it.type == EventType.STATE_ROOM_MEMBER
        } != null

        roomTypingUsersHandler.handle(realm, roomId, ephemeralResult)
        roomChangeMembershipStateDataSource.setMembershipFromSync(roomId, Membership.JOIN)
        roomSummaryUpdater.update(
                realm,
                roomId,
                Membership.JOIN,
                roomSync.summary,
                roomSync.unreadNotifications,
                updateMembers = hasRoomMember
        )
        return roomEntity
    }

    private fun handleInvitedRoom(realm: Realm,
                                  roomId: String,
                                  roomSync: InvitedRoomSync,
                                  insertType: EventInsertType,
                                  syncLocalTimestampMillis: Long): RoomEntity {
        Timber.v("Handle invited sync for room $roomId")
        val roomEntity = RoomEntity.getOrCreate(realm, roomId)
        roomEntity.membership = Membership.INVITE
        if (roomSync.inviteState != null && roomSync.inviteState.events.isNotEmpty()) {
            roomSync.inviteState.events.forEach { event ->
                if (event.stateKey == null || event.type == null) {
                    return@forEach
                }
                val ageLocalTs = event.unsignedData?.age?.let { syncLocalTimestampMillis - it }
                val eventEntity = event.toEntity(roomId, SendState.SYNCED, ageLocalTs).copyToRealmOrIgnore(realm, insertType)
                CurrentStateEventEntity.getOrCreate(realm, roomId, event.stateKey, event.type).apply {
                    eventId = eventEntity.eventId
                    root = eventEntity
                }
                roomMemberEventHandler.handle(realm, roomId, event)
            }
        }
        val inviterEvent = roomSync.inviteState?.events?.lastOrNull {
            it.type == EventType.STATE_ROOM_MEMBER
        }
        roomChangeMembershipStateDataSource.setMembershipFromSync(roomId, Membership.INVITE)
        roomSummaryUpdater.update(realm, roomId, Membership.INVITE, updateMembers = true, inviterId = inviterEvent?.senderId)
        return roomEntity
    }

    private fun handleLeftRoom(realm: Realm,
                               roomId: String,
                               roomSync: RoomSync,
                               insertType: EventInsertType,
                               syncLocalTimestampMillis: Long): RoomEntity {
        val roomEntity = RoomEntity.getOrCreate(realm, roomId)
        for (event in roomSync.state?.events.orEmpty()) {
            if (event.eventId == null || event.stateKey == null || event.type == null) {
                continue
            }
            val ageLocalTs = event.unsignedData?.age?.let { syncLocalTimestampMillis - it }
            val eventEntity = event.toEntity(roomId, SendState.SYNCED, ageLocalTs).copyToRealmOrIgnore(realm, insertType)
            CurrentStateEventEntity.getOrCreate(realm, roomId, event.stateKey, event.type).apply {
                eventId = event.eventId
                root = eventEntity
            }
            roomMemberEventHandler.handle(realm, roomId, event)
        }
        for (event in roomSync.timeline?.events.orEmpty()) {
            if (event.eventId == null || event.senderId == null || event.type == null) {
                continue
            }
            val ageLocalTs = event.unsignedData?.age?.let { syncLocalTimestampMillis - it }
            val eventEntity = event.toEntity(roomId, SendState.SYNCED, ageLocalTs).copyToRealmOrIgnore(realm, insertType)
            if (event.stateKey != null) {
                CurrentStateEventEntity.getOrCreate(realm, roomId, event.stateKey, event.type).apply {
                    eventId = event.eventId
                    root = eventEntity
                }
                if (event.type == EventType.STATE_ROOM_MEMBER) {
                    roomMemberEventHandler.handle(realm, roomEntity.roomId, event)
                }
            }
        }
        val leftMember = RoomMemberSummaryEntity.where(realm, roomId, userId).findFirst()
        val membership = leftMember?.membership ?: Membership.LEAVE
        roomEntity.membership = membership
        roomEntity.chunks.clearWith { it.deleteOnCascade(deleteStateEvents = true, canDeleteRoot = true) }
        roomTypingUsersHandler.handle(realm, roomId, null)
        roomChangeMembershipStateDataSource.setMembershipFromSync(roomId, Membership.LEAVE)
        roomSummaryUpdater.update(realm, roomId, membership, roomSync.summary, roomSync.unreadNotifications)
        return roomEntity
    }

    private suspend fun handleTimelineEvents(realm: Realm,
                                             roomId: String,
                                             roomEntity: RoomEntity,
                                             eventList: List<Event>,
                                             prevToken: String? = null,
                                             isLimited: Boolean = true,
                                             insertType: EventInsertType,
                                             syncLocalTimestampMillis: Long,
                                             aggregator: SyncResponsePostTreatmentAggregator): ChunkEntity {
        val lastChunk = ChunkEntity.findLastForwardChunkOfRoom(realm, roomEntity.roomId)
        if (isLimited && lastChunk != null) {
            lastChunk.deleteOnCascade(deleteStateEvents = false, canDeleteRoot = true)
        }
        val chunkEntity = if (!isLimited && lastChunk != null) {
            lastChunk
        } else {
            realm.createObject<ChunkEntity>().apply {
                this.prevToken = prevToken
                this.isLastForward = true
            }
        }
        val eventIds = ArrayList<String>(eventList.size)
        val roomMemberContentsByUser = HashMap<String, RoomMemberContent?>()

        val optimizedThreadSummaryMap = hashMapOf<String, EventEntity>()
        for (event in eventList) {
            if (event.eventId == null || event.senderId == null || event.type == null) {
                continue
            }

            eventIds.add(event.eventId)
            liveEventService.get().dispatchLiveEventReceived(event, roomId, insertType == EventInsertType.INITIAL_SYNC)

            val isInitialSync = insertType == EventInsertType.INITIAL_SYNC

            if (event.isEncrypted() && !isInitialSync) {
                runBlocking {
                    decryptIfNeeded(event, roomId)
                }
            }
            var contentToInject: String? = null
            if (!isInitialSync) {
                contentToInject = threadsAwarenessHandler.makeEventThreadAware(realm, roomId, event)
            }

            val ageLocalTs = event.unsignedData?.age?.let { syncLocalTimestampMillis - it }
            val eventEntity = event.toEntity(roomId, SendState.SYNCED, ageLocalTs, contentToInject).copyToRealmOrIgnore(realm, insertType)
            if (event.stateKey != null) {
                CurrentStateEventEntity.getOrCreate(realm, roomId, event.stateKey, event.type).apply {
                    eventId = event.eventId
                    root = eventEntity
                }
                if (event.type == EventType.STATE_ROOM_MEMBER) {
                    val fixedContent = event.getFixedRoomMemberContent()
                    roomMemberContentsByUser[event.stateKey] = fixedContent
                    roomMemberEventHandler.handle(realm, roomEntity.roomId, event.stateKey, fixedContent, aggregator)
                }
            }

            roomMemberContentsByUser.getOrPut(event.senderId) {
                // If we don't have any new state on this user, get it from db
                val rootStateEvent = CurrentStateEventEntity.getOrNull(realm, roomId, event.senderId, EventType.STATE_ROOM_MEMBER)?.root
                rootStateEvent?.asDomain()?.getFixedRoomMemberContent()
            }

            val timelineEventAdded = chunkEntity.addTimelineEvent(
                    roomId = roomId,
                    eventEntity = eventEntity,
                    direction = PaginationDirection.FORWARDS,
                    roomMemberContentsByUser = roomMemberContentsByUser)
            if (lightweightSettingsStorage.areThreadMessagesEnabled()) {
                eventEntity.rootThreadEventId?.let {
                    // This is a thread event
                    optimizedThreadSummaryMap[it] = eventEntity
                    // Add the same thread timeline event to Thread Chunk
                    addToThreadChunkIfNeeded(realm, roomId, it, timelineEventAdded, roomEntity)
                    if (homeServerCapabilitiesService.getHomeServerCapabilities().canUseThreading) {
                        // Update thread summaries only if homeserver supports threading
                        ThreadSummaryEntity.createOrUpdate(
                                threadSummaryType = ThreadSummaryUpdateType.ADD,
                                realm = realm,
                                roomId = roomId,
                                threadEventEntity = eventEntity,
                                roomMemberContentsByUser = roomMemberContentsByUser,
                                userId = userId,
                                roomEntity = roomEntity)
                    }
                } ?: run {
                    // This is a normal event or a root thread one
                    optimizedThreadSummaryMap[eventEntity.eventId] = eventEntity
                }
            }
            // Give info to crypto module
            cryptoService.onLiveEvent(roomEntity.roomId, event)

            // Try to remove local echo
            event.unsignedData?.transactionId?.also {
                val sendingEventEntity = roomEntity.sendingTimelineEvents.find(it)
                if (sendingEventEntity != null) {
                    Timber.v("Remove local echo for tx:$it")
                    roomEntity.sendingTimelineEvents.remove(sendingEventEntity)
                    if (event.isEncrypted() && event.content?.get("algorithm") as? String == MXCRYPTO_ALGORITHM_MEGOLM) {
                        // updated with echo decryption, to avoid seeing it decrypt again
                        val adapter = MoshiProvider.providesMoshi().adapter<OlmDecryptionResult>(OlmDecryptionResult::class.java)
                        sendingEventEntity.root?.decryptionResultJson?.let { json ->
                            eventEntity.decryptionResultJson = json
                            event.mxDecryptionResult = adapter.fromJson(json)
                        }
                    }
                    // Finally delete the local echo
                    sendingEventEntity.deleteOnCascade(true)
                } else {
                    Timber.v("Can't find corresponding local echo for tx:$it")
                }
            }
        }
        // Handle deletion of [stuck] local echos if needed
        deleteLocalEchosIfNeeded(insertType, roomEntity, eventList)
        if (lightweightSettingsStorage.areThreadMessagesEnabled()) {
            optimizedThreadSummaryMap.updateThreadSummaryIfNeeded(
                    roomId = roomId,
                    realm = realm,
                    chunkEntity = chunkEntity,
                    currentUserId = userId)
        }

        // posting new events to timeline if any is registered
        timelineInput.onNewTimelineEvents(roomId = roomId, eventIds = eventIds)
        return chunkEntity
    }

    /**
     * Adds new event to the appropriate thread chunk. If the event is already in
     * the thread timeline and /relations api, we should not added it
     */
    private fun addToThreadChunkIfNeeded(realm: Realm,
                                         roomId: String,
                                         threadId: String,
                                         timelineEventEntity: TimelineEventEntity?,
                                         roomEntity: RoomEntity) {
        val eventId = timelineEventEntity?.eventId ?: return

        ChunkEntity.findLastForwardChunkOfThread(realm, roomId, threadId)?.let { threadChunk ->
            val existingEvent = threadChunk.timelineEvents.find(eventId)
            if (existingEvent?.ownedByThreadChunk == true) {
                Timber.i("###THREADS RoomSyncHandler event:${timelineEventEntity.eventId} already exists, do not add")
                return@addToThreadChunkIfNeeded
            }
            threadChunk.timelineEvents.add(0, timelineEventEntity)
            roomEntity.addIfNecessary(threadChunk)
        }
    }

    private suspend fun decryptIfNeeded(event: Event, roomId: String) {
        try {
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

    data class EphemeralResult(
            val typingUserIds: List<String> = emptyList()
    )

    private fun handleEphemeral(realm: Realm,
                                roomId: String,
                                ephemeralEvents: List<Event>,
                                isInitialSync: Boolean,
                                aggregator: SyncResponsePostTreatmentAggregator): EphemeralResult {
        var result = EphemeralResult()
        for (event in ephemeralEvents) {
            when (event.type) {
                EventType.RECEIPT -> {
                    @Suppress("UNCHECKED_CAST")
                    (event.content as? ReadReceiptContent)?.let { readReceiptContent ->
                        readReceiptHandler.handle(realm, roomId, readReceiptContent, isInitialSync, aggregator)
                    }
                }
                EventType.TYPING  -> {
                    event.content.toModel<TypingEventContent>()?.let { typingEventContent ->
                        result = result.copy(typingUserIds = typingEventContent.typingUserIds)
                    }
                }
                else              -> Timber.w("Ephemeral event type '${event.type}' not yet supported")
            }
        }

        return result
    }

    /**
     * There are multiple issues like #516 that report stuck local echo events
     * at the bottom of each room timeline.
     *
     * That can happen when a message is SENT but not received back from the /sync.
     * Until now we use unsignedData.transactionId to determine whether or not the local
     * event should be deleted on every /sync. However, this is partially correct, lets have a look
     * at the following scenario:
     *
     * [There is no Internet connection] --> [10 Messages are sent] --> [The 10 messages are in the queue] -->
     * [Internet comes back for 1 second] --> [3 messages are sent] --> [Internet drops again] -->
     * [No /sync response is triggered | home server can even replied with /sync but never arrived while we are offline]
     *
     * So the state until now is that we have 7 pending events to send and 3 sent but not received them back from /sync
     * Subsequently, those 3 local messages will not be deleted while there is no transactionId from the /sync
     *
     * lets continue:
     * [Now lets assume that in the same room another user sent 15 events] -->
     * [We are finally back online!] -->
     * [We will receive the 10 latest events for the room and of course sent the pending 7 messages] -->
     * Now /sync response will NOT contain the 3 local messages so our events will stuck in the device.
     *
     * Someone can say, yes but it will come with the rooms/{roomId}/messages while paginating,
     * so the problem will be solved. No that is not the case for two reasons:
     *   1. rooms/{roomId}/messages response do not contain the unsignedData.transactionId so we cannot know which event
     *   to delete
     *   2. even if transactionId was there, currently we are not deleting it from the pagination
     *
     * ---------------------------------------------------------------------------------------------
     * While we cannot know when a specific event arrived from the pagination (no transactionId included), after each room /sync
     * we clear all SENT events, and we are sure that we will receive it from /sync or pagination
     */
    private fun deleteLocalEchosIfNeeded(insertType: EventInsertType, roomEntity: RoomEntity, eventList: List<Event>) {
        // Skip deletion if we are on initial sync
        if (insertType == EventInsertType.INITIAL_SYNC) return
        // Skip deletion if there are no timeline events or there is no event received from the current user
        if (eventList.firstOrNull { it.senderId == userId } == null) return
        roomEntity.sendingTimelineEvents.filter { timelineEvent ->
            timelineEvent.root?.sendState == SendState.SENT
        }.forEach {
            roomEntity.sendingTimelineEvents.remove(it)
            it.deleteOnCascade(true)
        }
    }
}
