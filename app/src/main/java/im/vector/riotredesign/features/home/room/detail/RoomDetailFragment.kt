package im.vector.riotredesign.features.home.room.detail

import android.os.Bundle
import android.os.Parcelable
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import im.vector.matrix.android.api.permalinks.PermalinkParser
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.RiotFragment
import im.vector.riotredesign.core.platform.ToolbarConfigurable
import im.vector.riotredesign.features.home.AvatarRenderer
import im.vector.riotredesign.features.home.room.detail.timeline.TimelineEventController
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_room_detail.*
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf

@Parcelize
data class RoomDetailArgs(
        val roomId: String,
        val eventId: String? = null
) : Parcelable

class RoomDetailFragment : RiotFragment(), TimelineEventController.Callback {

    companion object {

        fun newInstance(args: RoomDetailArgs): RoomDetailFragment {
            return RoomDetailFragment().apply {
                setArguments(args)
            }
        }
    }

    private val roomDetailViewModel: RoomDetailViewModel by fragmentViewModel()
    private val roomDetailArgs: RoomDetailArgs by args()

    private val timelineEventController by inject<TimelineEventController> { parametersOf(roomDetailArgs.roomId) }
    private lateinit var scrollOnNewMessageCallback: ScrollOnNewMessageCallback

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_room_detail, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupRecyclerView()
        setupToolbar()
        sendButton.setOnClickListener {
            val textMessage = composerEditText.text.toString()
            if (textMessage.isNotBlank()) {
                composerEditText.text = null
                roomDetailViewModel.accept(RoomDetailActions.SendMessage(textMessage))
            }
        }
        roomDetailViewModel.subscribe { renderState(it) }
    }

    private fun setupToolbar() {
        val parentActivity = riotActivity
        if (parentActivity is ToolbarConfigurable) {
            parentActivity.configure(toolbar)
        }
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, true)
        scrollOnNewMessageCallback = ScrollOnNewMessageCallback(layoutManager)
        recyclerView.layoutManager = layoutManager
        timelineEventController.addModelBuildListener { it.dispatchTo(scrollOnNewMessageCallback) }
        recyclerView.setController(timelineEventController)
        timelineEventController.callback = this
    }

    private fun renderState(state: RoomDetailViewState) {
        renderTimeline(state.asyncTimeline())
        renderRoomSummary(state.asyncRoomSummary())
    }

    private fun renderRoomSummary(roomSummary: RoomSummary?) {
        roomSummary?.let {
            toolbarTitleView.text = it.displayName
            AvatarRenderer.render(it, toolbarAvatarImageView)
            if (it.topic.isNotEmpty()) {
                toolbarSubtitleView.visibility = View.VISIBLE
                toolbarSubtitleView.text = it.topic
            } else {
                toolbarSubtitleView.visibility = View.GONE
            }
        }
    }

    private fun renderTimeline(timeline: Timeline?) {
        scrollOnNewMessageCallback.hasBeenUpdated.set(true)
        timelineEventController.timeline = timeline
    }

    // TimelineEventController.Callback ************************************************************

    override fun onUrlClicked(url: String) {
        val permalinkData = PermalinkParser.parse(url)

    }

}
