package im.vector.matrix.android.internal.session.room

import android.arch.lifecycle.LiveData
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.api.session.room.RoomService
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.query.where

class DefaultRoomService(private val monarchy: Monarchy) : RoomService {

    override fun getAllRooms(): List<Room> {
        var rooms: List<Room> = emptyList()
        monarchy.doWithRealm { realm ->
            rooms = RoomEntity.where(realm).findAll().map { it.asDomain() }
        }
        return rooms
    }

    override fun getRoom(roomId: String): Room? {
        var room: Room? = null
        monarchy.doWithRealm { realm ->
            room = RoomEntity.where(realm, roomId).findFirst()?.let { it.asDomain() }
        }
        return room
    }

    override fun liveRooms(): LiveData<List<Room>> {
        return monarchy.findAllMappedWithChanges(
                { realm -> RoomEntity.where(realm) },
                { it.asDomain() }
        )
    }

    override fun liveRoomSummaries(): LiveData<List<RoomSummary>> {
        return monarchy.findAllMappedWithChanges(
                { realm -> RoomSummaryEntity.where(realm) },
                { it.asDomain() }
        )
    }


}