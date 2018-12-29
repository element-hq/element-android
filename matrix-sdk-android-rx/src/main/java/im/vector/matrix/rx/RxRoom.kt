package im.vector.matrix.rx

import android.arch.paging.PagedList
import im.vector.matrix.android.api.session.events.model.EnrichedEvent
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.api.session.room.model.RoomSummary
import io.reactivex.Observable

class RxRoom(private val room: Room) {

    fun liveRoomSummary(): Observable<RoomSummary> {
        return room.roomSummary.asObservable()
    }

    fun timeline(eventId: String? = null): Observable<PagedList<EnrichedEvent>> {
        return room.timeline(eventId).asObservable()
    }

}

fun Room.rx(): RxRoom {
    return RxRoom(this)
}