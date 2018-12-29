package im.vector.riotredesign.features.home.room.detail

import android.support.v4.app.FragmentActivity
import com.airbnb.mvrx.MvRxViewModelFactory
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.rx.rx
import im.vector.riotredesign.core.platform.RiotViewModel

class RoomDetailViewModel(initialState: RoomDetailViewState,
                          session: Session
) : RiotViewModel<RoomDetailViewState>(initialState) {

    private val room = session.getRoom(initialState.roomId)!!
    private val roomId = initialState.roomId
    private val eventId = initialState.eventId

    companion object : MvRxViewModelFactory<RoomDetailViewState> {

        @JvmStatic
        override fun create(activity: FragmentActivity, state: RoomDetailViewState): RoomDetailViewModel {
            val currentSession = Matrix.getInstance().currentSession
            return RoomDetailViewModel(state, currentSession)
        }
    }

    init {
        observeRoomSummary()
        observeTimeline()
        room.loadRoomMembersIfNeeded()
    }

    fun accept(action: RoomDetailActions) {
        when (action) {
            is RoomDetailActions.SendMessage -> handleSendMessage(action)
        }
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleSendMessage(action: RoomDetailActions.SendMessage) {
        room.sendTextMessage(action.text, callback = object : MatrixCallback<Event> {})
    }

    private fun observeRoomSummary() {
        room.rx().liveRoomSummary()
                .execute { async ->
                    copy(asyncRoomSummary = async)
                }
    }

    private fun observeTimeline() {
        room.rx().timeline(eventId)
                .execute { asyncTimeline ->
                    copy(asyncTimeline = asyncTimeline)
                }
    }

}