package im.vector.riotredesign.features.home.room.detail

import android.arch.lifecycle.Observer
import android.arch.paging.PagedList
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
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
import im.vector.riotredesign.features.home.RoomSummaryViewHelper
import kotlinx.android.synthetic.main.fragment_room_detail.*
import org.koin.android.ext.android.inject

class RoomDetailFragment : RiotFragment(), TimelineEventAdapter.Callback {

    companion object {

        fun newInstance(roomId: String): RoomDetailFragment {
            return RoomDetailFragment().apply {
                this.roomId = roomId
            }
        }
    }

    private val matrix by inject<Matrix>()
    private val currentSession = matrix.currentSession!!
    private var roomId by FragmentArgumentDelegate<String>()
    private val timelineAdapter = TimelineEventAdapter(this)
    private val timelineEventController = TimelineEventController()
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
        room.liveTimeline().observe(this, Observer { renderEvents(it) })
        room.roomSummary.observe(this, Observer { renderRoomSummary(it) })
    }

    private fun setupToolbar() {
        val parentActivity = riotActivity
        if (parentActivity is ToolbarConfigurable) {
            parentActivity.configure(toolbar)
        }
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, true)
        layoutManager.stackFromEnd = true
        timelineAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                /*if (layoutManager.findFirstVisibleItemPosition() == 0) {
                    layoutManager.scrollToPosition(0)
                }
                */
            }
        })
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = timelineAdapter
        //recyclerView.setController(timelineEventController)
    }

    private fun renderRoomSummary(roomSummary: RoomSummary?) {
        roomSummary?.let {
            val roomSummaryViewHelper = RoomSummaryViewHelper(it)
            toolbarTitleView.text = it.displayName
            toolbarAvatarImageView.setImageDrawable(roomSummaryViewHelper.avatarDrawable(riotActivity))
            if (it.topic.isNotEmpty()) {
                toolbarSubtitleView.visibility = View.VISIBLE
                toolbarSubtitleView.text = it.topic
            } else {
                toolbarSubtitleView.visibility = View.GONE
            }
        }
    }

    private fun renderEvents(events: PagedList<EnrichedEvent>?) {
        timelineAdapter.submitList(events)
    }


    override
    fun onEventsListChanged(oldList: List<EnrichedEvent>?, newList: List<EnrichedEvent>?) {
        if (oldList == null && newList != null) {
            recyclerView.scrollToPosition(0)
        }
    }


}