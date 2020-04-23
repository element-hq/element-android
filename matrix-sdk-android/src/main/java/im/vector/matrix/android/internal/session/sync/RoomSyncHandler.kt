/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session.sync

import im.vector.matrix.android.R
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomMemberContent
import im.vector.matrix.android.api.session.room.model.tag.RoomTagContent
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.internal.crypto.DefaultCryptoService
import im.vector.matrix.android.internal.database.helper.addTimelineEvent
import im.vector.matrix.android.internal.database.mapper.ContentMapper
import im.vector.matrix.android.internal.database.mapper.toSQLEntity
import im.vector.matrix.android.internal.session.DefaultInitialSyncProgressService
import im.vector.matrix.android.internal.session.mapWithProgress
import im.vector.matrix.android.internal.session.room.RoomSummaryUpdater
import im.vector.matrix.android.internal.session.room.membership.RoomMemberEventHandler
import im.vector.matrix.android.internal.session.room.read.FullyReadContent
import im.vector.matrix.android.internal.session.room.timeline.PaginationDirection
import im.vector.matrix.android.internal.session.room.timeline.SQLTimeline
import im.vector.matrix.android.internal.session.room.typing.TypingEventContent
import im.vector.matrix.android.internal.session.sync.model.*
import im.vector.matrix.sqldelight.session.CurrentStateEventEntity
import im.vector.matrix.sqldelight.session.SessionDatabase
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import javax.inject.Inject

internal class RoomSyncHandler @Inject constructor(private val readReceiptHandler: ReadReceiptHandler,
                                                   private val roomSummaryUpdater: RoomSummaryUpdater,
                                                   private val roomTagHandler: RoomTagHandler,
                                                   private val roomFullyReadHandler: RoomFullyReadHandler,
                                                   private val cryptoService: DefaultCryptoService,
                                                   private val roomMemberEventHandler: RoomMemberEventHandler,
                                                   private val eventBus: EventBus,
                                                   private val sessionDatabase: SessionDatabase) {

    sealed class HandlingStrategy {
        data class JOINED(val data: Map<String, RoomSync>) : HandlingStrategy()
        data class INVITED(val data: Map<String, InvitedRoomSync>) : HandlingStrategy()
        data class LEFT(val data: Map<String, RoomSync>) : HandlingStrategy()
    }

    fun handle(roomsSyncResponse: RoomsSyncResponse,
               reporter: DefaultInitialSyncProgressService? = null,
               isInitialSync: Boolean
    ) {
        Timber.v("Execute transaction from $this")
        handleRoomSync(HandlingStrategy.JOINED(roomsSyncResponse.join), isInitialSync, reporter)
        handleRoomSync(HandlingStrategy.INVITED(roomsSyncResponse.invite), isInitialSync, reporter)
        handleRoomSync(HandlingStrategy.LEFT(roomsSyncResponse.leave), isInitialSync, reporter)
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleRoomSync(handlingStrategy: HandlingStrategy, isInitialSync: Boolean, reporter: DefaultInitialSyncProgressService?) {
        val syncLocalTimeStampMillis = System.currentTimeMillis()
        when (handlingStrategy) {
            is HandlingStrategy.JOINED ->
                handlingStrategy.data.mapWithProgress(reporter, R.string.initial_sync_start_importing_account_joined_rooms, 0.6f) {
                    handleJoinedRoom(it.key, it.value, syncLocalTimeStampMillis, isInitialSync)
                }
            is HandlingStrategy.INVITED ->
                handlingStrategy.data.mapWithProgress(reporter, R.string.initial_sync_start_importing_account_invited_rooms, 0.1f) {
                    handleInvitedRoom(it.key, it.value)
                }

            is HandlingStrategy.LEFT -> {
                handlingStrategy.data.mapWithProgress(reporter, R.string.initial_sync_start_importing_account_left_rooms, 0.3f) {
                    handleLeftRoom(it.key, it.value)
                }
            }
        }
    }

    private fun handleJoinedRoom(roomId: String,
                                 roomSync: RoomSync,
                                 syncLocalTimestampMillis: Long,
                                 isInitialSync: Boolean) {
        Timber.v("Handle join sync for room $roomId")
        sessionDatabase.roomQueries.insertOrUpdateRoom(roomId)

        var ephemeralResult: EphemeralResult? = null
        if (roomSync.ephemeral?.events?.isNotEmpty() == true) {
            ephemeralResult = handleEphemeral(roomId, roomSync.ephemeral, isInitialSync)
        }

        if (roomSync.accountData?.events?.isNotEmpty() == true) {
            handleRoomAccountDataEvents(roomId, roomSync.accountData)
        }
        // State event
        if (roomSync.state?.events?.isNotEmpty() == true) {
            for (event in roomSync.state.events) {
                if (event.eventId == null || event.stateKey == null) {
                    continue
                }
                val eventEntity = event.toSQLEntity(roomId, SendState.SYNCED)
                val stateEventEntity = CurrentStateEventEntity.Impl(
                        event_id = event.eventId,
                        state_key = event.stateKey,
                        room_id = roomId,
                        type = event.type
                )
                sessionDatabase.eventQueries.insert(eventEntity)
                sessionDatabase.stateEventQueries.insertOrUpdate(stateEventEntity)
                // Give info to crypto module
                cryptoService.onStateEvent(roomId, event)
                roomMemberEventHandler.handle(roomId, event)
            }
        }

        if (roomSync.timeline?.events?.isNotEmpty() == true) {
            handleTimelineEvents(
                    roomId,
                    roomSync.timeline.events,
                    roomSync.timeline.prevToken,
                    roomSync.timeline.limited,
                    syncLocalTimestampMillis
            )
        }
        val hasRoomMember = roomSync.state?.events?.firstOrNull {
            it.type == EventType.STATE_ROOM_MEMBER
        } != null || roomSync.timeline?.events?.firstOrNull {
            it.type == EventType.STATE_ROOM_MEMBER
        } != null

        roomSummaryUpdater.update(
                roomId,
                Membership.JOIN,
                roomSync.summary,
                roomSync.unreadNotifications,
                updateMembers = hasRoomMember,
                ephemeralResult = ephemeralResult)
    }

    private fun handleInvitedRoom(roomId: String,
                                  roomSync: InvitedRoomSync) {
        Timber.v("Handle invited sync for room $roomId")
        sessionDatabase.roomQueries.insertOrUpdateRoom(roomId)
        if (roomSync.inviteState != null && roomSync.inviteState.events.isNotEmpty()) {
            roomSync.inviteState.events.forEach {
                if (it.stateKey == null) {
                    return@forEach
                }
                val eventEntity = it.toSQLEntity(roomId, SendState.SYNCED)
                sessionDatabase.eventQueries.insert(eventEntity)
                val stateEventEntity = CurrentStateEventEntity.Impl(
                        event_id = eventEntity.event_id,
                        state_key = it.stateKey,
                        room_id = roomId,
                        type = it.type
                )
                sessionDatabase.stateEventQueries.insertOrUpdate(stateEventEntity)
                roomMemberEventHandler.handle(roomId, it)
            }
        }
        val inviterEvent = roomSync.inviteState?.events?.lastOrNull {
            it.type == EventType.STATE_ROOM_MEMBER
        }
        roomSummaryUpdater.update(roomId, Membership.INVITE, updateMembers = true, inviterId = inviterEvent?.senderId)
    }

    private fun handleLeftRoom(roomId: String,
                               roomSync: RoomSync) {
        sessionDatabase.roomQueries.insertOrUpdateRoom(roomId)
        sessionDatabase.chunkQueries.deleteAllFromRoom(roomId)
        roomSummaryUpdater.update(roomId, Membership.LEAVE, roomSync.summary, roomSync.unreadNotifications)
    }

    private fun handleTimelineEvents(roomId: String,
                                     eventList: List<Event>,
                                     prevToken: String? = null,
                                     isLimited: Boolean = true,
                                     syncLocalTimestampMillis: Long) {
        val lastChunk = sessionDatabase.chunkQueries.getLastLive(roomId).executeAsOneOrNull()
        if (isLimited || lastChunk == null) {
            sessionDatabase.chunkQueries.insert(
                    room_id = roomId,
                    is_last_backward = false,
                    is_last_forward = true,
                    next_token = null,
                    prev_token = prevToken
            )
            if (lastChunk != null) {
                sessionDatabase.chunkQueries.setIsLastForwards(false, lastChunk.chunk_id)
            }
        }

        val liveChunkId = sessionDatabase.chunkQueries.getChunkIdOfLive(roomId).executeAsOne()
        val eventIds = ArrayList<String>(eventList.size)
        val roomMemberContentsByUser = HashMap<String, RoomMemberContent?>()

        for (event in eventList) {
            if (event.eventId == null || event.senderId == null) {
                continue
            }
            eventIds.add(event.eventId)
            val ageLocalTs = event.unsignedData?.age?.let { syncLocalTimestampMillis - it }
            if (sessionDatabase.eventQueries.exist(roomId = roomId, eventId = event.eventId).executeAsOneOrNull() == null) {
                val eventEntity = event.toSQLEntity(roomId, SendState.SYNCED, ageLocalTs)
                sessionDatabase.eventQueries.insert(eventEntity)
            }
            if (event.isStateEvent() && event.stateKey != null) {
                val stateEventEntity = CurrentStateEventEntity.Impl(
                        event_id = event.eventId,
                        state_key = event.stateKey,
                        room_id = roomId,
                        type = event.type
                )
                sessionDatabase.stateEventQueries.insertOrUpdate(stateEventEntity)
                if (event.type == EventType.STATE_ROOM_MEMBER) {
                    roomMemberContentsByUser[event.stateKey] = event.content.toModel()
                    roomMemberEventHandler.handle(roomId, event)
                }
            }
            roomMemberContentsByUser.getOrPut(event.senderId) {
                // If we don't have any new state on this user, get it from db
                val rootStateEvent = sessionDatabase.stateEventQueries.getCurrentStateEvent(roomId, stateKey = event.senderId, type = EventType.STATE_ROOM_MEMBER).executeAsOneOrNull()
                ContentMapper.map(rootStateEvent?.content).toModel()
            }
            sessionDatabase.addTimelineEvent(roomId = roomId, chunkId = liveChunkId, direction = PaginationDirection.FORWARDS, event = event, roomMemberContentsByUser = roomMemberContentsByUser)
            // Give info to crypto module
            cryptoService.onLiveEvent(roomId, event)

            // Try to remove local echo
            event.unsignedData?.transactionId?.also {
                sessionDatabase.eventQueries.delete(it)
            }
        }
        // posting new events to timeline if any is registered
        eventBus.post(SQLTimeline.OnNewTimelineEvents(roomId = roomId, eventIds = eventIds))
    }

    data class EphemeralResult(
            val typingUserIds: List<String> = emptyList()
    )

    private fun handleEphemeral(roomId: String,
                                ephemeral: RoomSyncEphemeral,
                                isInitialSync: Boolean): EphemeralResult {
        var result = EphemeralResult()
        for (event in ephemeral.events) {
            when (event.type) {
                EventType.RECEIPT -> {
                    @Suppress("UNCHECKED_CAST")
                    (event.content as? ReadReceiptContent)?.let { readReceiptContent ->
                        readReceiptHandler.handle(roomId, readReceiptContent, isInitialSync)
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

    private fun handleRoomAccountDataEvents(roomId: String, accountData: RoomSyncAccountData) {
        for (event in accountData.events) {
            val eventType = event.getClearType()
            if (eventType == EventType.TAG) {
                val content = event.getClearContent().toModel<RoomTagContent>()
                roomTagHandler.handle(roomId, content)
            } else if (eventType == EventType.FULLY_READ) {
                val content = event.getClearContent().toModel<FullyReadContent>()
                roomFullyReadHandler.handle(roomId, content)
            }
        }
    }
}
