package im.vector.matrix.android.api.session.room

import android.arch.lifecycle.LiveData

interface RoomService {

    fun getRoom(roomId: String): Room?

    fun getAllRooms(): List<Room>

    fun liveRooms(): LiveData<List<Room>>

}