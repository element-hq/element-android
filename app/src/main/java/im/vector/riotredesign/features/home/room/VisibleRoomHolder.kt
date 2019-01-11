package im.vector.riotredesign.features.home.room

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

class VisibleRoomHolder {

    private val visibleRoomStream = BehaviorSubject.create<String>()

    fun setVisibleRoom(roomId: String) {
        visibleRoomStream.onNext(roomId)
    }

    fun visibleRoom(): Observable<String> {
        return visibleRoomStream.hide()
    }


}