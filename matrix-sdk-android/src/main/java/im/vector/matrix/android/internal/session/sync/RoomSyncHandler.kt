package im.vector.matrix.android.internal.session.sync

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.model.MyMembership
import im.vector.matrix.android.internal.database.mapper.asEntity
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.query.findAllIncludingEvents
import im.vector.matrix.android.internal.database.query.findLastLiveChunkFromRoom
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.sync.model.InvitedRoomSync
import im.vector.matrix.android.internal.session.sync.model.RoomSync
import im.vector.matrix.android.internal.session.sync.model.RoomSyncEphemeral
import im.vector.matrix.android.internal.session.sync.model.RoomSyncSummary
import io.realm.Realm


internal class RoomSyncHandler(private val monarchy: Monarchy,
                               private val stateEventsChunkHandler: StateEventsChunkHandler,
                               private val readReceiptHandler: ReadReceiptHandler) {

    sealed class HandlingStrategy {
        data class JOINED(val data: Map<String, RoomSync>) : HandlingStrategy()
        data class INVITED(val data: Map<String, InvitedRoomSync>) : HandlingStrategy()
        data class LEFT(val data: Map<String, RoomSync>) : HandlingStrategy()
    }

    fun handleRoomSync(handlingStrategy: HandlingStrategy) {
        monarchy.runTransactionSync { realm ->
            val roomEntities = when (handlingStrategy) {
                is HandlingStrategy.JOINED -> handlingStrategy.data.map { handleJoinedRoom(realm, it.key, it.value) }
                is HandlingStrategy.INVITED -> handlingStrategy.data.map { handleInvitedRoom(realm, it.key, it.value) }
                is HandlingStrategy.LEFT -> handlingStrategy.data.map { handleLeftRoom(it.key, it.value) }
            }
            realm.insertOrUpdate(roomEntities)
        }

        if (handlingStrategy is HandlingStrategy.JOINED) {
            monarchy.runTransactionSync { realm ->
                handlingStrategy.data.forEach { (roomId, roomSync) ->
                    handleEphemeral(realm, roomId, roomSync.ephemeral)
                }
            }
        }
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleJoinedRoom(realm: Realm,
                                 roomId: String,
                                 roomSync: RoomSync): RoomEntity {

        val roomEntity = RoomEntity.where(realm, roomId).findFirst() ?: RoomEntity(roomId)

        if (roomEntity.membership == MyMembership.INVITED) {
            roomEntity.chunks.deleteAllFromRealm()
        }


        roomEntity.membership = MyMembership.JOINED

        if (roomSync.summary != null) {
            handleRoomSummary(realm, roomId, roomSync.summary)
        }
        if (roomSync.state != null && roomSync.state.events.isNotEmpty()) {
            val chunkEntity = stateEventsChunkHandler.handle(realm, roomId, roomSync.state.events)
            if (!roomEntity.chunks.contains(chunkEntity)) {
                roomEntity.chunks.add(chunkEntity)
            }
        }
        if (roomSync.timeline != null && roomSync.timeline.events.isNotEmpty()) {
            val chunkEntity = handleListOfEvent(realm, roomId, roomSync.timeline.events, roomSync.timeline.prevToken, isLimited = roomSync.timeline.limited)
            if (!roomEntity.chunks.contains(chunkEntity)) {
                roomEntity.chunks.add(chunkEntity)
            }
        }
        return roomEntity
    }

    private fun handleInvitedRoom(realm: Realm,
                                  roomId: String,
                                  roomSync:
                                  InvitedRoomSync): RoomEntity {
        val roomEntity = RoomEntity()
        roomEntity.roomId = roomId
        roomEntity.membership = MyMembership.INVITED
        if (roomSync.inviteState != null && roomSync.inviteState.events.isNotEmpty()) {
            val chunkEntity = handleListOfEvent(realm, roomId, roomSync.inviteState.events)
            if (!roomEntity.chunks.contains(chunkEntity)) {
                roomEntity.chunks.add(chunkEntity)
            }
        }
        return roomEntity
    }

    // TODO : handle it
    private fun handleLeftRoom(roomId: String,
                               roomSync: RoomSync): RoomEntity {
        return RoomEntity().apply {
            this.roomId = roomId
            this.membership = MyMembership.LEFT
        }
    }

    private fun handleRoomSummary(realm: Realm,
                                  roomId: String,
                                  roomSummary: RoomSyncSummary) {

        val roomSummaryEntity = RoomSummaryEntity.where(realm, roomId).findFirst()
                ?: RoomSummaryEntity(roomId)

        if (roomSummary.heroes.isNotEmpty()) {
            roomSummaryEntity.heroes.clear()
            roomSummaryEntity.heroes.addAll(roomSummary.heroes)
        }
        if (roomSummary.invitedMembersCount != null) {
            roomSummaryEntity.invitedMembersCount = roomSummary.invitedMembersCount
        }
        if (roomSummary.joinedMembersCount != null) {
            roomSummaryEntity.joinedMembersCount = roomSummary.joinedMembersCount
        }
        realm.insertOrUpdate(roomSummaryEntity)
    }

    private fun handleListOfEvent(realm: Realm,
                                  roomId: String,
                                  eventList: List<Event>,
                                  prevToken: String? = null,
                                  nextToken: String? = null,
                                  isLimited: Boolean = true): ChunkEntity {

        val chunkEntity = if (!isLimited) {
            ChunkEntity.findLastLiveChunkFromRoom(realm, roomId)
        } else {
            val eventIds = eventList.filter { it.eventId != null }.map { it.eventId!! }
            ChunkEntity.findAllIncludingEvents(realm, eventIds).firstOrNull()
        } ?: ChunkEntity().apply { this.prevToken = prevToken }

        chunkEntity.nextToken = nextToken

        eventList.forEach { event ->
            val eventEntity = event.asEntity().let {
                realm.copyToRealmOrUpdate(it)
            }
            if (!chunkEntity.events.contains(eventEntity)) {
                chunkEntity.events.add(eventEntity)
            }
        }
        return chunkEntity
    }

    private fun handleEphemeral(realm: Realm,
                                roomId: String,
                                ephemeral: RoomSyncEphemeral?) {
        if (ephemeral == null || ephemeral.events.isNullOrEmpty()) {
            return
        }
        ephemeral.events
                .filter { it.type == EventType.RECEIPT }
                .map { it.content<ReadReceiptContent>() }
                .flatMap { readReceiptHandler.handle(realm, roomId, it) }
    }

}