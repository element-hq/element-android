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

package org.matrix.android.sdk.internal.session.sync

import io.realm.Realm
import io.realm.kotlin.createObject
import org.matrix.android.sdk.R
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.tag.RoomTagContent
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.internal.crypto.DefaultCryptoService
import org.matrix.android.sdk.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.internal.crypto.algorithms.olm.OlmDecryptionResult
import org.matrix.android.sdk.internal.database.helper.addOrUpdate
import org.matrix.android.sdk.internal.database.helper.addTimelineEvent
import org.matrix.android.sdk.internal.database.helper.deleteOnCascade
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntity
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntity
import org.matrix.android.sdk.internal.database.query.copyToRealmOrIgnore
import org.matrix.android.sdk.internal.database.query.find
import org.matrix.android.sdk.internal.database.query.findLastForwardChunkOfRoom
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.database.query.getOrNull
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.DefaultInitialSyncProgressService
import org.matrix.android.sdk.internal.session.mapWithProgress
import org.matrix.android.sdk.internal.session.room.membership.RoomChangeMembershipStateDataSource
import org.matrix.android.sdk.internal.session.room.membership.RoomMemberEventHandler
import org.matrix.android.sdk.internal.session.room.read.FullyReadContent
import org.matrix.android.sdk.internal.session.room.summary.RoomSummaryUpdater
import org.matrix.android.sdk.internal.session.room.timeline.PaginationDirection
import org.matrix.android.sdk.internal.session.room.timeline.TimelineInput
import org.matrix.android.sdk.internal.session.room.typing.TypingEventContent
import org.matrix.android.sdk.internal.session.sync.model.InvitedRoomSync
import org.matrix.android.sdk.internal.session.sync.model.RoomSync
import org.matrix.android.sdk.internal.session.sync.model.RoomSyncAccountData
import org.matrix.android.sdk.internal.session.sync.model.RoomSyncEphemeral
import org.matrix.android.sdk.internal.session.sync.model.RoomsSyncResponse
import timber.log.Timber
import javax.inject.Inject

internal class RoomSyncHandler @Inject constructor(private val readReceiptHandler: ReadReceiptHandler,
                                                   private val roomSummaryUpdater: RoomSummaryUpdater,
                                                   private val roomTagHandler: RoomTagHandler,
                                                   private val roomFullyReadHandler: RoomFullyReadHandler,
                                                   private val cryptoService: DefaultCryptoService,
                                                   private val roomMemberEventHandler: RoomMemberEventHandler,
                                                   private val roomTypingUsersHandler: RoomTypingUsersHandler,
                                                   private val roomChangeMembershipStateDataSource: RoomChangeMembershipStateDataSource,
                                                   @UserId private val userId: String,
                                                   private val timelineInput: TimelineInput) {

    sealed class HandlingStrategy {
        data class JOINED(val data: Map<String, RoomSync>) : HandlingStrategy()
        data class INVITED(val data: Map<String, InvitedRoomSync>) : HandlingStrategy()
        data class LEFT(val data: Map<String, RoomSync>) : HandlingStrategy()
    }

    fun handle(
            realm: Realm,
            roomsSyncResponse: RoomsSyncResponse,
            isInitialSync: Boolean,
            reporter: DefaultInitialSyncProgressService? = null
    ) {
        Timber.v("Execute transaction from $this")
        handleRoomSync(realm, HandlingStrategy.JOINED(roomsSyncResponse.join), isInitialSync, reporter)
        handleRoomSync(realm, HandlingStrategy.INVITED(roomsSyncResponse.invite), isInitialSync, reporter)
        handleRoomSync(realm, HandlingStrategy.LEFT(roomsSyncResponse.leave), isInitialSync, reporter)
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleRoomSync(realm: Realm, handlingStrategy: HandlingStrategy, isInitialSync: Boolean, reporter: DefaultInitialSyncProgressService?) {
        val insertType = if (isInitialSync) {
            EventInsertType.INITIAL_SYNC
        } else {
            EventInsertType.INCREMENTAL_SYNC
        }
        val syncLocalTimeStampMillis = System.currentTimeMillis()
        val rooms = when (handlingStrategy) {
            is HandlingStrategy.JOINED  ->
                handlingStrategy.data.mapWithProgress(reporter, R.string.initial_sync_start_importing_account_joined_rooms, 0.6f) {
                    handleJoinedRoom(realm, it.key, it.value, isInitialSync, insertType, syncLocalTimeStampMillis)
                }
            is HandlingStrategy.INVITED ->
                handlingStrategy.data.mapWithProgress(reporter, R.string.initial_sync_start_importing_account_invited_rooms, 0.1f) {
                    handleInvitedRoom(realm, it.key, it.value, insertType, syncLocalTimeStampMillis)
                }

            is HandlingStrategy.LEFT    -> {
                handlingStrategy.data.mapWithProgress(reporter, R.string.initial_sync_start_importing_account_left_rooms, 0.3f) {
                    handleLeftRoom(realm, it.key, it.value, insertType, syncLocalTimeStampMillis)
                }
            }
        }
        realm.insertOrUpdate(rooms)
    }

    private fun handleJoinedRoom(realm: Realm,
                                 roomId: String,
                                 roomSync: RoomSync,
                                 isInitialSync: Boolean,
                                 insertType: EventInsertType,
                                 syncLocalTimestampMillis: Long): RoomEntity {
        Timber.v("Handle join sync for room $roomId")

        var ephemeralResult: EphemeralResult? = null
        if (roomSync.ephemeral?.events?.isNotEmpty() == true) {
            ephemeralResult = handleEphemeral(realm, roomId, roomSync.ephemeral, isInitialSync)
        }

        if (roomSync.accountData?.events?.isNotEmpty() == true) {
            handleRoomAccountDataEvents(realm, roomId, roomSync.accountData)
        }

        val roomEntity = RoomEntity.where(realm, roomId).findFirst() ?: realm.createObject(roomId)

        if (roomEntity.membership == Membership.INVITE) {
            roomEntity.chunks.deleteAllFromRealm()
        }
        roomEntity.membership = Membership.JOIN

        // State event
        if (roomSync.state?.events?.isNotEmpty() == true) {
            for (event in roomSync.state.events) {
                if (event.eventId == null || event.stateKey == null) {
                    continue
                }
                val ageLocalTs = event.unsignedData?.age?.let { syncLocalTimestampMillis - it }
                val eventEntity = event.toEntity(roomId, SendState.SYNCED, ageLocalTs).copyToRealmOrIgnore(realm, insertType)
                CurrentStateEventEntity.getOrCreate(realm, roomId, event.stateKey, event.type).apply {
                    eventId = event.eventId
                    root = eventEntity
                }
                // Give info to crypto module
                cryptoService.onStateEvent(roomId, event)
                roomMemberEventHandler.handle(realm, roomId, event)
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
                    isInitialSync
            )
            roomEntity.addOrUpdate(chunkEntity)
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
        val roomEntity = RoomEntity.where(realm, roomId).findFirst() ?: realm.createObject(roomId)
        roomEntity.membership = Membership.INVITE
        if (roomSync.inviteState != null && roomSync.inviteState.events.isNotEmpty()) {
            roomSync.inviteState.events.forEach { event ->
                if (event.stateKey == null) {
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
        val roomEntity = RoomEntity.where(realm, roomId).findFirst() ?: realm.createObject(roomId)
        for (event in roomSync.state?.events.orEmpty()) {
            if (event.eventId == null || event.stateKey == null) {
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
            if (event.eventId == null || event.senderId == null) {
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
        roomEntity.chunks.deleteAllFromRealm()
        roomTypingUsersHandler.handle(realm, roomId, null)
        roomChangeMembershipStateDataSource.setMembershipFromSync(roomId, Membership.LEAVE)
        roomSummaryUpdater.update(realm, roomId, membership, roomSync.summary, roomSync.unreadNotifications)
        return roomEntity
    }

    private fun handleTimelineEvents(realm: Realm,
                                     roomId: String,
                                     roomEntity: RoomEntity,
                                     eventList: List<Event>,
                                     prevToken: String? = null,
                                     isLimited: Boolean = true,
                                     insertType: EventInsertType,
                                     syncLocalTimestampMillis: Long,
                                     isInitialSync: Boolean): ChunkEntity {
        val lastChunk = ChunkEntity.findLastForwardChunkOfRoom(realm, roomEntity.roomId)
        val chunkEntity = if (!isLimited && lastChunk != null) {
            lastChunk
        } else {
            realm.createObject<ChunkEntity>().apply { this.prevToken = prevToken }
        }
        // Only one chunk has isLastForward set to true
        lastChunk?.isLastForward = false
        chunkEntity.isLastForward = true

        val eventIds = ArrayList<String>(eventList.size)
        val roomMemberContentsByUser = HashMap<String, RoomMemberContent?>()

        for (event in eventList) {
            if (event.eventId == null || event.senderId == null) {
                continue
            }
            eventIds.add(event.eventId)

            if (event.isEncrypted() && !isInitialSync) {
                decryptIfNeeded(event, roomId)
            }

            val ageLocalTs = event.unsignedData?.age?.let { syncLocalTimestampMillis - it }
            val eventEntity = event.toEntity(roomId, SendState.SYNCED, ageLocalTs).copyToRealmOrIgnore(realm, insertType)
            if (event.stateKey != null) {
                CurrentStateEventEntity.getOrCreate(realm, roomId, event.stateKey, event.type).apply {
                    eventId = event.eventId
                    root = eventEntity
                }
                if (event.type == EventType.STATE_ROOM_MEMBER) {
                    val fixedContent = event.getFixedRoomMemberContent()
                    roomMemberContentsByUser[event.stateKey] = fixedContent
                    roomMemberEventHandler.handle(realm, roomEntity.roomId, event.stateKey, fixedContent)
                }
            }
            roomMemberContentsByUser.getOrPut(event.senderId) {
                // If we don't have any new state on this user, get it from db
                val rootStateEvent = CurrentStateEventEntity.getOrNull(realm, roomId, event.senderId, EventType.STATE_ROOM_MEMBER)?.root
                rootStateEvent?.asDomain()?.getFixedRoomMemberContent()
            }

            chunkEntity.addTimelineEvent(roomId, eventEntity, PaginationDirection.FORWARDS, roomMemberContentsByUser)
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
                    sendingEventEntity.deleteOnCascade()
                } else {
                    Timber.v("Can't find corresponding local echo for tx:$it")
                }
            }
        }
        // posting new events to timeline if any is registered
        timelineInput.onNewTimelineEvents(roomId = roomId, eventIds = eventIds)
        return chunkEntity
    }

    private fun decryptIfNeeded(event: Event, roomId: String) {
        try {
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
                                ephemeral: RoomSyncEphemeral,
                                isInitialSync: Boolean): EphemeralResult {
        var result = EphemeralResult()
        for (event in ephemeral.events) {
            when (event.type) {
                EventType.RECEIPT -> {
                    @Suppress("UNCHECKED_CAST")
                    (event.content as? ReadReceiptContent)?.let { readReceiptContent ->
                        readReceiptHandler.handle(realm, roomId, readReceiptContent, isInitialSync)
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

    private fun handleRoomAccountDataEvents(realm: Realm, roomId: String, accountData: RoomSyncAccountData) {
        for (event in accountData.events) {
            val eventType = event.getClearType()
            if (eventType == EventType.TAG) {
                val content = event.getClearContent().toModel<RoomTagContent>()
                roomTagHandler.handle(realm, roomId, content)
            } else if (eventType == EventType.FULLY_READ) {
                val content = event.getClearContent().toModel<FullyReadContent>()
                roomFullyReadHandler.handle(realm, roomId, content)
            }
        }
    }

    private fun Event.getFixedRoomMemberContent(): RoomMemberContent? {
        val content = content.toModel<RoomMemberContent>()
        // if user is leaving, we should grab his last name and avatar from prevContent
        return if (content?.membership?.isLeft() == true) {
            val prevContent = resolvedPrevContent().toModel<RoomMemberContent>()
            content.copy(
                    displayName = prevContent?.displayName,
                    avatarUrl = prevContent?.avatarUrl
            )
        } else {
            content
        }
    }
}
