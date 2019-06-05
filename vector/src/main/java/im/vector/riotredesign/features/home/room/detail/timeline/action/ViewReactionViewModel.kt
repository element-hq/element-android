package im.vector.riotredesign.features.home.room.detail.timeline.action

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.airbnb.mvrx.*
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.room.model.EventAnnotationsSummary
import im.vector.riotredesign.core.extensions.localDateTime
import im.vector.riotredesign.core.platform.VectorViewModel
import im.vector.riotredesign.features.home.room.detail.timeline.helper.TimelineDateFormatter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get


data class DisplayReactionsViewState(
        val eventId: String = "",
        val roomId: String = "",
        val mapReactionKeyToMemberList: Async<List<ReactionInfo>> = Uninitialized)
    : MvRxState

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
class ViewReactionViewModel(private val session: Session,
                            private val timelineDateFormatter: TimelineDateFormatter,
                            lifecycleOwner: LifecycleOwner?,
                            liveSummary: LiveData<List<EventAnnotationsSummary>>?,
                            initialState: DisplayReactionsViewState) : VectorViewModel<DisplayReactionsViewState>(initialState) {

    init {
        loadReaction()
        if (lifecycleOwner != null) {
            liveSummary?.observe(lifecycleOwner, Observer {
                it?.firstOrNull()?.let {
                    loadReaction()
                }
            })
        }

    }

    private fun loadReaction() = withState { state ->

        GlobalScope.launch {
            try {
                val room = session.getRoom(state.roomId)
                val event = room?.getTimeLineEvent(state.eventId)
                if (event == null) {
                    setState { copy(mapReactionKeyToMemberList = Fail(Throwable())) }
                    return@launch
                }
                var results = ArrayList<ReactionInfo>()
                event.annotations?.reactionsSummary?.forEach { sum ->

                    sum.sourceEvents.mapNotNull { room.getTimeLineEvent(it) }.forEach {
                        val localDate = it.root.localDateTime()
                        results.add(ReactionInfo(it.root.eventId!!, sum.key, it.root.sender
                                ?: "", it.senderName, timelineDateFormatter.formatMessageHour(localDate)))
                    }
                }
                setState {
                    copy(
                            mapReactionKeyToMemberList = Success(results.sortedBy { it.timestamp })
                    )
                }
            } catch (t: Throwable) {
                setState {
                    copy(
                            mapReactionKeyToMemberList = Fail(t)
                    )
                }
            }
        }
    }


    companion object : MvRxViewModelFactory<ViewReactionViewModel, DisplayReactionsViewState> {

        override fun initialState(viewModelContext: ViewModelContext): DisplayReactionsViewState? {

            val roomId = (viewModelContext.args as? TimelineEventFragmentArgs)?.roomId
                    ?: return null
            val info = (viewModelContext.args as? TimelineEventFragmentArgs)?.informationData
                    ?: return null
            return DisplayReactionsViewState(info.eventId, roomId)
        }

        override fun create(viewModelContext: ViewModelContext, state: DisplayReactionsViewState): ViewReactionViewModel? {
            val session = viewModelContext.activity.get<Session>()
            val eventId = (viewModelContext.args as TimelineEventFragmentArgs).eventId
            return ViewReactionViewModel(session, viewModelContext.activity.get(), viewModelContext.activity, session.getRoom(state.roomId)?.getEventSummaryLive(eventId), state)
        }


    }
}