package im.vector.matrix.rx

import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.api.session.room.model.RoomSummary
import io.reactivex.Observable

class RxSession(private val session: Session) {

    fun liveRoomSummaries(): Observable<List<RoomSummary>> {
        return session.liveRoomSummaries().asObservable()
    }

    fun liveGroupSummaries(): Observable<List<GroupSummary>> {
        return session.liveGroupSummaries().asObservable()
    }

}

fun Session.rx(): RxSession {
    return RxSession(this)
}