package im.vector.matrix.android.api.session.room

import android.arch.lifecycle.LiveData
import android.arch.paging.PagedList
import im.vector.matrix.android.api.session.events.model.EnrichedEvent
import im.vector.matrix.android.api.session.room.model.MyMembership
import im.vector.matrix.android.api.util.Cancelable

interface Room {

    val roomId: String

    val myMembership: MyMembership

    fun liveTimeline(): LiveData<PagedList<EnrichedEvent>>

    fun getNumberOfJoinedMembers(): Int

    fun loadRoomMembersIfNeeded(): Cancelable
}