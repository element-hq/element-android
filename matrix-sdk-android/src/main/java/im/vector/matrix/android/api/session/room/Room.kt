package im.vector.matrix.android.api.session.room

import im.vector.matrix.android.api.session.room.model.MyMembership
import im.vector.matrix.android.api.util.Cancelable

interface Room: TimelineHolder {

    val roomId: String

    val myMembership: MyMembership

    fun getNumberOfJoinedMembers(): Int

    fun loadRoomMembersIfNeeded(): Cancelable
}