package im.vector.matrix.rx

import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.timeline.TimelineData
import io.reactivex.Observable

class RxRoom(private val room: Room) {

    fun liveRoomSummary(): Observable<RoomSummary> {
        return room.roomSummary.asObservable()
    }

    fun timeline(eventId: String? = null): Observable<TimelineData> {
        return room.timeline(eventId).asObservable()
    }

}

fun Room.rx(): RxRoom {
    return RxRoom(this)
}