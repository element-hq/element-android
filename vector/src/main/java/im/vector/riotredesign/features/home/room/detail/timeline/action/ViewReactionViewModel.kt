package im.vector.riotredesign.features.home.room.detail.timeline.action

import com.airbnb.mvrx.*
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.rx.RxRoom
import im.vector.riotredesign.core.extensions.localDateTime
import im.vector.riotredesign.core.platform.VectorViewModel
import im.vector.riotredesign.features.home.room.detail.timeline.helper.TimelineDateFormatter
import io.reactivex.Observable


data class DisplayReactionsViewState(
        val eventId: String,
        val roomId: String,
        val mapReactionKeyToMemberList: Async<List<ReactionInfo>> = Uninitialized)
    : MvRxState {

    constructor(args: TimelineEventFragmentArgs) : this(roomId = args.roomId, eventId = args.eventId)

}

data class ReactionInfo(
        val eventId: String,
        val reactionKey: String,
        val authorId: String,
        val authorName: String? = null,
        val timestamp: String? = null
)

/**
 * Used to display the list of members that reacted to a given event
 */
class ViewReactionViewModel @AssistedInject constructor(@Assisted
                                                        initialState: DisplayReactionsViewState,
                                                        private val session: Session,
                                                        private val timelineDateFormatter: TimelineDateFormatter
) : VectorViewModel<DisplayReactionsViewState>(initialState) {

    private val roomId = initialState.roomId
    private val eventId = initialState.eventId
    private val room = session.getRoom(roomId)
            ?: throw IllegalStateException("Shouldn't use this ViewModel without a room")

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: DisplayReactionsViewState): ViewReactionViewModel
    }

    companion object : MvRxViewModelFactory<ViewReactionViewModel, DisplayReactionsViewState> {

        override fun create(viewModelContext: ViewModelContext, state: DisplayReactionsViewState): ViewReactionViewModel? {
            val fragment: ViewReactionBottomSheet = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.viewReactionViewModelFactory.create(state)
        }

    }


    init {
        observeEventAnnotationSummaries()
    }

    private fun observeEventAnnotationSummaries() {
        RxRoom(room)
                .liveAnnotationSummary(eventId)
                .flatMapSingle { summaries ->
                    Observable
                            .fromIterable(summaries)
                            .flatMapIterable { it.reactionsSummary }
                            .map {
                                val event = room.getTimeLineEvent(eventId)
                                        ?: throw RuntimeException("Your eventId is not valid")
                                val localDate = event.root.localDateTime()
                                ReactionInfo(
                                        event.root.eventId!!,
                                        it.key,
                                        event.root.sender ?: "",
                                        event.senderName,
                                        timelineDateFormatter.formatMessageHour(localDate)
                                )
                            }
                            .toList()
                }
                .execute {
                    copy(mapReactionKeyToMemberList = it)
                }
    }
}