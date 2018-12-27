package im.vector.riotredesign.features.home.room.detail

import android.arch.lifecycle.Observer
import android.arch.paging.PagedList
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.activityViewModel
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.permalinks.PermalinkParser
import im.vector.matrix.android.api.session.events.model.EnrichedEvent
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.RiotFragment
import im.vector.riotredesign.core.platform.ToolbarConfigurable
import im.vector.riotredesign.core.utils.FragmentArgumentDelegate
import im.vector.riotredesign.core.utils.UnsafeFragmentArgumentDelegate
import im.vector.riotredesign.features.home.AvatarRenderer
import im.vector.riotredesign.features.home.HomeActions
import im.vector.riotredesign.features.home.HomeViewModel
import im.vector.riotredesign.features.home.room.detail.timeline.TimelineEventController
import kotlinx.android.synthetic.main.fragment_room_detail.*
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf

class RoomDetailFragment : RiotFragment(), TimelineEventController.Callback {

    companion object {

        fun newInstance(roomId: String, eventId: String? = null): RoomDetailFragment {
            return RoomDetailFragment().apply {
                this.roomId = roomId
                this.eventId = eventId
            }
        }
    }

    private val viewModel: HomeViewModel by activityViewModel()
    private val currentSession = Matrix.getInstance().currentSession
    private var roomId: String by UnsafeFragmentArgumentDelegate()
    private var eventId: String? by FragmentArgumentDelegate()
    private val timelineEventController by inject<TimelineEventController> { parametersOf(roomId) }
    private lateinit var room: Room
    private lateinit var scrollOnNewMessageCallback: ScrollOnNewMessageCallback

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_room_detail, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        room = currentSession.getRoom(roomId)!!
        setupRecyclerView()
        setupToolbar()
        room.loadRoomMembersIfNeeded()
        room.timeline(eventId).observe(this, Observer { renderEvents(it) })
        room.roomSummary.observe(this, Observer { renderRoomSummary(it) })
        sendButton.setOnClickListener {
            val textMessage = composerEditText.text.toString()
            if (textMessage.isNotBlank()) {
                composerEditText.text = null
                room.sendTextMessage(textMessage, object : MatrixCallback<Event> {

                })
            }
        }
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

    private fun renderEvents(events: PagedList<EnrichedEvent>?) {
        scrollOnNewMessageCallback.hasBeenUpdated.set(true)
        timelineEventController.timeline = events
    }

    // TimelineEventController.Callback ************************************************************

    override fun onUrlClicked(url: String) {
        val permalinkData = PermalinkParser.parse(url)
        viewModel.accept(HomeActions.PermalinkClicked(permalinkData))
    }

}
