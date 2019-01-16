package im.vector.riotredesign.features.home.room.detail

import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.rx.rx
import im.vector.riotredesign.core.platform.RiotViewModel
import im.vector.riotredesign.features.home.room.VisibleRoomHolder
import org.koin.android.ext.android.get

class RoomDetailViewModel(initialState: RoomDetailViewState,
                          private val session: Session,
                          private val visibleRoomHolder: VisibleRoomHolder
) : RiotViewModel<RoomDetailViewState>(initialState) {

    private val room = session.getRoom(initialState.roomId)!!
    private val roomId = initialState.roomId
    private val eventId = initialState.eventId

    companion object : MvRxViewModelFactory<RoomDetailViewModel, RoomDetailViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RoomDetailViewState): RoomDetailViewModel? {
            val currentSession = Matrix.getInstance().currentSession
            val visibleRoomHolder = viewModelContext.activity.get<VisibleRoomHolder>()
            return RoomDetailViewModel(state, currentSession, visibleRoomHolder)
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
            is RoomDetailActions.IsDisplayed -> visibleRoomHolder.setVisibleRoom(roomId)
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
                .execute { timelineData ->
                    copy(asyncTimelineData = timelineData)
                }
    }


}