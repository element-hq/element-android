package im.vector.riotredesign.features.home.room.detail

import android.arch.lifecycle.Observer
import android.arch.paging.PagedList
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.session.events.model.EnrichedEvent
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.RiotFragment
import im.vector.riotredesign.core.platform.ToolbarConfigurable
import im.vector.riotredesign.core.utils.FragmentArgumentDelegate
import im.vector.riotredesign.core.utils.UnsafeFragmentArgumentDelegate
import im.vector.riotredesign.features.home.AvatarRenderer
import im.vector.riotredesign.features.home.room.detail.timeline.TimelineEventController
import kotlinx.android.synthetic.main.fragment_room_detail.*
import org.koin.android.ext.android.inject
import org.koin.core.parameter.ParameterList

class RoomDetailFragment : RiotFragment() {

    companion object {

        fun newInstance(roomId: String, eventId: String? = null): RoomDetailFragment {
            return RoomDetailFragment().apply {
                this.roomId = roomId
                this.eventId = eventId
            }
        }
    }

    private val matrix by inject<Matrix>()
    private val currentSession = matrix.currentSession
    private var roomId: String by UnsafeFragmentArgumentDelegate()
    private var eventId: String? by FragmentArgumentDelegate()
    private val timelineEventController by inject<TimelineEventController>(parameters = { ParameterList(roomId) })
    private lateinit var room: Room

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
                room.sendTextMessage(textMessage)
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
        val listUpdateCallback = ScrollOnNewMessageCallback(layoutManager)
        recyclerView.layoutManager = layoutManager
        timelineEventController.addModelBuildListener { it.dispatchTo(listUpdateCallback) }
        recyclerView.setController(timelineEventController)
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
        timelineEventController.timeline = events
    }

}
