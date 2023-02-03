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
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.crypto.model.OlmDecryptionResult
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.getRelationContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilitiesService
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.threads.model.ThreadSummaryUpdateType
import org.matrix.android.sdk.api.session.sync.InitialSyncStep
import org.matrix.android.sdk.api.session.sync.InitialSyncStrategy
import org.matrix.android.sdk.api.session.sync.initialSyncStrategy
import org.matrix.android.sdk.api.session.sync.model.InvitedRoomSync
import org.matrix.android.sdk.api.session.sync.model.LazyRoomSyncEphemeral
import org.matrix.android.sdk.api.session.sync.model.RoomSync
import org.matrix.android.sdk.api.session.sync.model.RoomsSyncResponse
import org.matrix.android.sdk.api.settings.LightweightSettingsStorage
import org.matrix.android.sdk.internal.crypto.DefaultCryptoService
import org.matrix.android.sdk.internal.crypto.algorithms.megolm.UnRequestedForwardManager
import org.matrix.android.sdk.internal.database.helper.addIfNecessary
import org.matrix.android.sdk.internal.database.helper.addTimelineEvent
import org.matrix.android.sdk.internal.database.helper.createOrUpdate
import org.matrix.android.sdk.internal.database.helper.updateThreadSummaryIfNeeded
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntity
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntity
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.model.deleteOnCascade
import org.matrix.android.sdk.internal.database.model.threads.ThreadSummaryEntity
import org.matrix.android.sdk.internal.database.query.copyToRealmOrIgnore
import org.matrix.android.sdk.internal.database.query.find
import org.matrix.android.sdk.internal.database.query.findAll
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
import org.matrix.android.sdk.internal.session.room.membership.RoomChangeMembershipStateDataSource
import org.matrix.android.sdk.internal.session.room.membership.RoomMemberEventHandler
import org.matrix.android.sdk.internal.session.room.summary.RoomSummaryUpdater
import org.matrix.android.sdk.internal.session.room.timeline.PaginationDirection
import org.matrix.android.sdk.internal.session.room.timeline.TimelineInput
import org.matrix.android.sdk.internal.session.room.typing.TypingEventContent
import org.matrix.android.sdk.internal.session.sync.ProgressReporter
import org.matrix.android.sdk.internal.session.sync.SyncResponsePostTreatmentAggregator
import org.matrix.android.sdk.internal.session.sync.mapWithProgress
import org.matrix.android.sdk.internal.session.sync.parsing.RoomSyncAccountDataHandler
import org.matrix.android.sdk.internal.session.sync.reportSubtask
import org.matrix.android.sdk.internal.util.computeBestChunkSize
import org.matrix.android.sdk.internal.util.time.Clock
import timber.log.Timber
import javax.inject.Inject

internal class RoomSyncHandler @Inject constructor(
        private val readReceiptHandler: ReadReceiptHandler,
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
        private val liveEventService: Lazy<StreamEventsManager>,
        private val clock: Clock,
        private val unRequestedForwardManager: UnRequestedForwardManager,
) {

    sealed class HandlingStrategy {
        data class JOINED(val data: Map<String, RoomSync>) : HandlingStrategy()
        data class INVITED(val data: Map<String, InvitedRoomSync>) : HandlingStrategy()
        data class LEFT(val data: Map<String, RoomSync>) : HandlingStrategy()
    }

    fun handle(
            realm: Realm,
            roomsSyncResponse: RoomsSyncResponse,
            isInitialSync: Boolean,
            aggregator: SyncResponsePostTreatmentAggregator,
            reporter: ProgressReporter? = null
    ) {
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

    private fun handleRoomSync(
            realm: Realm,
            handlingStrategy: HandlingStrategy,
            isInitialSync: Boolean,
            aggregator: SyncResponsePostTreatmentAggregator,
            reporter: ProgressReporter?
    ) {
        val insertType = if (isInitialSync) {
            EventInsertType.INITIAL_SYNC
        } else {
            EventInsertType.INCREMENTAL_SYNC
        }
        val syncLocalTimeStampMillis = clock.epochMillis()
        val rooms = when (handlingStrategy) {
            is HandlingStrategy.JOINED -> {
                if (isInitialSync && initialSyncStrategy is InitialSyncStrategy.Optimized) {
                    insertJoinRoomsFromInitSync(realm, handlingStrategy, syncLocalTimeStampMillis, aggregator, reporter)
                    // Rooms are already inserted, return an empty list
                    emptyList()
                } else {
                    handlingStrategy.data.mapWithProgress(reporter, InitialSyncStep.ImportingAccountJoinedRooms, 0.6f) {
                        handleJoinedRoom(realm, it.key, it.value, insertType, syncLocalTimeStampMillis, aggregator)
                    }
                }
            }
            is HandlingStrategy.INVITED ->
                handlingStrategy.data.mapWithProgress(reporter, InitialSyncStep.ImportingAccountInvitedRooms, 0.1f) {
                    handleInvitedRoom(realm, it.key, it.value, insertType, syncLocalTimeStampMillis, aggregator)
                }

            is HandlingStrategy.LEFT -> {
                handlingStrategy.data.mapWithProgress(reporter, InitialSyncStep.ImportingAccountLeftRooms, 0.3f) {
                    handleLeftRoom(realm, it.key, it.value, insertType, syncLocalTimeStampMillis, aggregator)
                }
            }
        }
        realm.insertOrUpdate(rooms)
    }

    private fun insertJoinRoomsFromInitSync(
            realm: Realm,
            handlingStrategy: HandlingStrategy.JOINED,
            syncLocalTimeStampMillis: Long,
            aggregator: SyncResponsePostTreatmentAggregator,
            reporter: ProgressReporter?
    ) {
        val bestChunkSize = computeBestChunkSize(
                listSize = handlingStrategy.data.keys.size,
                limit = (initialSyncStrategy as? InitialSyncStrategy.Optimized)?.maxRoomsToInsert ?: Int.MAX_VALUE
        )

        if (bestChunkSize.shouldChunk()) {
            reportSubtask(reporter, InitialSyncStep.ImportingAccountJoinedRooms, bestChunkSize.numberOfChunks, 0.6f) {
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
            val rooms = handlingStrategy.data.mapWithProgress(reporter, InitialSyncStep.ImportingAccountJoinedRooms, 0.6f) {
                handleJoinedRoom(realm, it.key, it.value, EventInsertType.INITIAL_SYNC, syncLocalTimeStampMillis, aggregator)
            }
            realm.insertOrUpdate(rooms)
        }
    }

    private fun handleJoinedRoom(
            realm: Realm,
            roomId: String,
            roomSync: RoomSync,
            insertType: EventInsertType,
            syncLocalTimestampMillis: Long,
            aggregator: SyncResponsePostTreatmentAggregator
    ): RoomEntity {
        Timber.v("Handle join sync for room $roomId")
        val isInitialSync = insertType == EventInsertType.INITIAL_SYNC

        val ephemeralResult = (roomSync.ephemeral as? LazyRoomSyncEphemeral.Parsed)
                ?.roomSyncEphemeral
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
                val ageLocalTs = syncLocalTimestampMillis - (event.unsignedData?.age ?: 0)
                val eventEntity = event.toEntity(roomId, SendState.SYNCED, ageLocalTs).copyToRealmOrIgnore(realm, insertType)
                Timber.v("## received state event ${event.type} and key ${event.stateKey}")
                CurrentStateEventEntity.getOrCreate(realm, roomId, event.stateKey, event.type).apply {
                    // Timber.v("## Space state event: $eventEntity")
                    eventId = event.eventId
                    root = eventEntity
                }
                // Give info to crypto module
                cryptoService.onStateEvent(roomId, event, aggregator.cryptoStoreAggregator)
                roomMemberEventHandler.handle(realm, roomId, event, isInitialSync, aggregator)
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
                roomSync.unreadThreadNotifications,
                updateMembers = hasRoomMember,
                aggregator = aggregator
        )
        return roomEntity
    }

    private fun handleInvitedRoom(
            realm: Realm,
            roomId: String,
            roomSync: InvitedRoomSync,
            insertType: EventInsertType,
            syncLocalTimestampMillis: Long,
            aggregator: SyncResponsePostTreatmentAggregator
    ): RoomEntity {
        Timber.v("Handle invited sync for room $roomId")
        val isInitialSync = insertType == EventInsertType.INITIAL_SYNC
        val roomEntity = RoomEntity.getOrCreate(realm, roomId)
        roomEntity.membership = Membership.INVITE
        if (roomSync.inviteState != null && roomSync.inviteState.events.isNotEmpty()) {
            roomSync.inviteState.events.forEach { event ->
                if (event.stateKey == null || event.type == null) {
                    return@forEach
                }
                val ageLocalTs = syncLocalTimestampMillis - (event.unsignedData?.age ?: 0)
                val eventEntity = event.toEntity(roomId, SendState.SYNCED, ageLocalTs).copyToRealmOrIgnore(realm, insertType)
                CurrentStateEventEntity.getOrCreate(realm, roomId, event.stateKey, event.type).apply {
                    eventId = eventEntity.eventId
                    root = eventEntity
                }
                roomMemberEventHandler.handle(realm, roomId, event, isInitialSync)
            }
        }
        val inviterEvent = roomSync.inviteState?.events?.lastOrNull {
            it.type == EventType.STATE_ROOM_MEMBER
        }
        roomChangeMembershipStateDataSource.setMembershipFromSync(roomId, Membership.INVITE)
        roomSummaryUpdater.update(realm, roomId, Membership.INVITE, updateMembers = true, inviterId = inviterEvent?.senderId, aggregator = aggregator)
        unRequestedForwardManager.onInviteReceived(roomId, inviterEvent?.senderId.orEmpty(), clock.epochMillis())
        return roomEntity
    }

    private fun handleLeftRoom(
            realm: Realm,
            roomId: String,
            roomSync: RoomSync,
            insertType: EventInsertType,
            syncLocalTimestampMillis: Long,
            aggregator: SyncResponsePostTreatmentAggregator
    ): RoomEntity {
        val isInitialSync = insertType == EventInsertType.INITIAL_SYNC
        val roomEntity = RoomEntity.getOrCreate(realm, roomId)
        for (event in roomSync.state?.events.orEmpty()) {
            if (event.eventId == null || event.stateKey == null || event.type == null) {
                continue
            }
            val ageLocalTs = syncLocalTimestampMillis - (event.unsignedData?.age ?: 0)
            val eventEntity = event.toEntity(roomId, SendState.SYNCED, ageLocalTs).copyToRealmOrIgnore(realm, insertType)
            CurrentStateEventEntity.getOrCreate(realm, roomId, event.stateKey, event.type).apply {
                eventId = event.eventId
                root = eventEntity
            }
            roomMemberEventHandler.handle(realm, roomId, event, isInitialSync)
        }
        for (event in roomSync.timeline?.events.orEmpty()) {
            if (event.eventId == null || event.senderId == null || event.type == null) {
                continue
            }
            val ageLocalTs = syncLocalTimestampMillis - (event.unsignedData?.age ?: 0)
            val eventEntity = event.toEntity(roomId, SendState.SYNCED, ageLocalTs).copyToRealmOrIgnore(realm, insertType)
            if (event.stateKey != null) {
                CurrentStateEventEntity.getOrCreate(realm, roomId, event.stateKey, event.type).apply {
                    eventId = event.eventId
                    root = eventEntity
                }
                if (event.type == EventType.STATE_ROOM_MEMBER) {
                    roomMemberEventHandler.handle(realm, roomEntity.roomId, event, isInitialSync)
                }
            }
        }
        val leftMember = RoomMemberSummaryEntity.where(realm, roomId, userId).findFirst()
        val membership = leftMember?.membership ?: Membership.LEAVE
        roomEntity.membership = membership
        roomEntity.chunks.clearWith { it.deleteOnCascade(deleteStateEvents = true, canDeleteRoot = true) }
        roomTypingUsersHandler.handle(realm, roomId, null)
        roomChangeMembershipStateDataSource.setMembershipFromSync(roomId, Membership.LEAVE)
        roomSummaryUpdater.update(
                realm,
                roomId,
                membership,
                roomSync.summary,
                roomSync.unreadNotifications,
                roomSync.unreadThreadNotifications,
                aggregator = aggregator,
        )
        return roomEntity
    }

    private fun handleTimelineEvents(
            realm: Realm,
            roomId: String,
            roomEntity: RoomEntity,
            eventList: List<Event>,
            prevToken: String? = null,
            isLimited: Boolean = true,
            insertType: EventInsertType,
            syncLocalTimestampMillis: Long,
            aggregator: SyncResponsePostTreatmentAggregator
    ): ChunkEntity {
        val lastChunk = ChunkEntity.findLastForwardChunkOfRoom(realm, roomEntity.roomId)
        val chunkEntity = if (!isLimited && lastChunk != null) {
            lastChunk
        } else {
            // Delete all chunks of the room in case of gap.
            ChunkEntity.findAll(realm, roomId).forEach {
                it.deleteOnCascade(deleteStateEvents = false, canDeleteRoot = true)
            }
            realm.createObject<ChunkEntity>().apply {
                this.prevToken = prevToken
                this.isLastForward = true
            }
        }
        val eventIds = ArrayList<String>(eventList.size)
        val roomMemberContentsByUser = HashMap<String, RoomMemberContent?>()

        val optimizedThreadSummaryMap = hashMapOf<String, EventEntity>()
        for (rawEvent in eventList) {
            // It's annoying roomId is not there, but lot of code rely on it.
            // And had to do it now as copy would delete all decryption results..
            val ageLocalTs = syncLocalTimestampMillis - (rawEvent.unsignedData?.age ?: 0)
            val event = rawEvent.copy(roomId = roomId).also {
                it.ageLocalTs = ageLocalTs
            }
            if (event.eventId == null || event.senderId == null || event.type == null) {
                continue
            }

            val isInitialSync = insertType == EventInsertType.INITIAL_SYNC

            eventIds.add(event.eventId)
            if (!isInitialSync) {
                liveEventService.get().dispatchLiveEventReceived(event, roomId)
            }

            if (event.isEncrypted() && !isInitialSync) {
                try {
                    decryptIfNeeded(event, roomId)
                } catch (e: InterruptedException) {
                    Timber.i("Decryption got interrupted")
                }
            }
            var contentToInject: String? = null
            if (!isInitialSync) {
                contentToInject = threadsAwarenessHandler.makeEventThreadAware(realm, roomId, event)
            }

            val eventEntity = event.toEntity(roomId, SendState.SYNCED, ageLocalTs, contentToInject).copyToRealmOrIgnore(realm, insertType)
            if (event.stateKey != null) {
                CurrentStateEventEntity.getOrCreate(realm, roomId, event.stateKey, event.type).apply {
                    eventId = event.eventId
                    root = eventEntity
                }
                if (event.type == EventType.STATE_ROOM_MEMBER) {
                    roomMemberContentsByUser[event.stateKey] = event.getFixedRoomMemberContent()
                    roomMemberEventHandler.handle(realm, roomEntity.roomId, event, isInitialSync, aggregator)
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
                    roomMemberContentsByUser = roomMemberContentsByUser
            )
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
                                roomEntity = roomEntity,
                                currentTimeMillis = clock.epochMillis(),
                        )
                    }
                } ?: run {
                    // This is a normal event or a root thread one
                    optimizedThreadSummaryMap[eventEntity.eventId] = eventEntity
                }
            }
            // Give info to crypto module
            cryptoService.onLiveEvent(roomEntity.roomId, event, isInitialSync, aggregator.cryptoStoreAggregator)

            // Try to remove local echo
            event.unsignedData?.transactionId?.also { txId ->
                val sendingEventEntity = roomEntity.sendingTimelineEvents.find(txId)
                if (sendingEventEntity != null) {
                    Timber.v("Remove local echo for tx:$txId")
                    roomEntity.sendingTimelineEvents.remove(sendingEventEntity)
                    if (event.isEncrypted() && event.content?.get("algorithm") as? String == MXCRYPTO_ALGORITHM_MEGOLM) {
                        // updated with echo decryption, to avoid seeing txId decrypt again
                        val adapter = MoshiProvider.providesMoshi().adapter<OlmDecryptionResult>(OlmDecryptionResult::class.java)
                        sendingEventEntity.root?.decryptionResultJson?.let { json ->
                            eventEntity.decryptionResultJson = json
                            event.mxDecryptionResult = adapter.fromJson(json)
                        }
                    }
                    // also update potential edit that could refer to that event?
                    // If not display will flicker :/
                    val relationContent = event.getRelationContent()
                    if (relationContent?.type == RelationType.REPLACE) {
                        relationContent.eventId?.let { targetId ->
                            EventAnnotationsSummaryEntity.where(realm, roomId, targetId)
                                    .findFirst()
                                    ?.editSummary
                                    ?.editions
                                    ?.forEach {
                                        if (it.eventId == txId) {
                                            // just do that, the aggregation processor will to the rest
                                            it.event = eventEntity
                                        }
                                    }
                        }
                    }

                    // Finally delete the local echo
                    sendingEventEntity.deleteOnCascade(true)
                } else {
                    Timber.v("Can't find corresponding local echo for tx:$txId")
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
                    currentUserId = userId
            )
        }

        // posting new events to timeline if any is registered
        timelineInput.onNewTimelineEvents(roomId = roomId, eventIds = eventIds)
        return chunkEntity
    }

    /**
     * Adds new event to the appropriate thread chunk. If the event is already in
     * the thread timeline and /relations api, we should not added it
     */
    private fun addToThreadChunkIfNeeded(
            realm: Realm,
            roomId: String,
            threadId: String,
            timelineEventEntity: TimelineEventEntity?,
            roomEntity: RoomEntity
    ) {
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

    private fun decryptIfNeeded(event: Event, roomId: String) {
        try {
            val timelineId = generateTimelineId(roomId)
            // Event from sync does not have roomId, so add it to the event first
            // note: runBlocking should be used here while we are in realm single thread executor, to avoid thread switching
            val result = runBlocking { cryptoService.decryptEvent(event.copy(roomId = roomId), timelineId) }
            event.mxDecryptionResult = OlmDecryptionResult(
                    payload = result.clearEvent,
                    senderKey = result.senderCurve25519Key,
                    keysClaimed = result.claimedEd25519Key?.let { k -> mapOf("ed25519" to k) },
                    forwardingCurve25519KeyChain = result.forwardingCurve25519KeyChain,
                    isSafe = result.isSafe
            )
        } catch (e: MXCryptoError) {
            if (e is MXCryptoError.Base) {
                event.mCryptoError = e.errorType
                event.mCryptoErrorReason = e.technicalMessage.takeIf { it.isNotEmpty() } ?: e.detailedErrorDescription
            }
        }
    }

    private fun generateTimelineId(roomId: String): String {
        return "RoomSyncHandler$roomId"
    }

    data class EphemeralResult(
            val typingUserIds: List<String> = emptyList()
    )

    private fun handleEphemeral(
            realm: Realm,
            roomId: String,
            ephemeralEvents: List<Event>,
            isInitialSync: Boolean,
            aggregator: SyncResponsePostTreatmentAggregator
    ): EphemeralResult {
        var result = EphemeralResult()
        for (event in ephemeralEvents) {
            when (event.type) {
                EventType.RECEIPT -> {
                    @Suppress("UNCHECKED_CAST")
                    (event.content as? ReadReceiptContent)?.let { readReceiptContent ->
                        readReceiptHandler.handle(realm, roomId, readReceiptContent, isInitialSync, aggregator)
                    }
                }
                EventType.TYPING -> {
                    event.content.toModel<TypingEventContent>()?.let { typingEventContent ->
                        result = result.copy(typingUserIds = typingEventContent.typingUserIds)
                    }
                }
                else -> Timber.w("Ephemeral event type '${event.type}' not yet supported")
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
