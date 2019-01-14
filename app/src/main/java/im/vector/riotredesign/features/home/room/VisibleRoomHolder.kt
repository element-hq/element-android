package im.vector.riotredesign.features.home.room

import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

class VisibleRoomHolder {

    private val visibleRoomStream = BehaviorRelay.create<String>()

    fun setVisibleRoom(roomId: String) {
        visibleRoomStream.accept(roomId)
    }

    fun visibleRoom(): Observable<String> {
        return visibleRoomStream.hide()
    }


}