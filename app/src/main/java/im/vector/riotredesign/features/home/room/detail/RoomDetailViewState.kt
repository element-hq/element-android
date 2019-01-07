package im.vector.riotredesign.features.home.room.detail

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.timeline.TimelineData

data class RoomDetailViewState(
        val roomId: String,
        val eventId: String?,
        val asyncRoomSummary: Async<RoomSummary> = Uninitialized,
        val asyncTimelineData: Async<TimelineData> = Uninitialized
) : MvRxState {

    constructor(args: RoomDetailArgs) : this(roomId = args.roomId, eventId = args.eventId)

}