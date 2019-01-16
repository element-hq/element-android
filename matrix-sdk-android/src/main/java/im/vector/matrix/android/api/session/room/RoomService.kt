package im.vector.matrix.android.api.session.room

import androidx.lifecycle.LiveData
import im.vector.matrix.android.api.session.room.model.RoomSummary

interface RoomService {

    fun getRoom(roomId: String): Room?

    fun liveRoomSummaries(): LiveData<List<RoomSummary>>

}