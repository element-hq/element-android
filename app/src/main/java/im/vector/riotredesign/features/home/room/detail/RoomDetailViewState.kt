package im.vector.riotredesign.features.home.room.detail

import android.arch.paging.PagedList
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import im.vector.matrix.android.api.session.events.model.EnrichedEvent
import im.vector.matrix.android.api.session.room.model.RoomSummary

typealias Timeline = PagedList<EnrichedEvent>

data class RoomDetailViewState(
        val roomId: String,
        val eventId: String?,
        val asyncRoomSummary: Async<RoomSummary> = Uninitialized,
        val asyncTimeline: Async<Timeline> = Uninitialized
) : MvRxState {

    constructor(args: RoomDetailArgs) : this(roomId = args.roomId, eventId = args.eventId)

}