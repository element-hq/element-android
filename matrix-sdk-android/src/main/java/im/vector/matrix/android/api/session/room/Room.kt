package im.vector.matrix.android.api.session.room

import android.arch.lifecycle.LiveData
import im.vector.matrix.android.api.session.room.model.MyMembership
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.timeline.TimelineService
import im.vector.matrix.android.api.util.Cancelable

interface Room : TimelineService, SendService {

    val roomId: String

    val myMembership: MyMembership

    val roomSummary: LiveData<RoomSummary>

    fun loadRoomMembersIfNeeded(): Cancelable

}