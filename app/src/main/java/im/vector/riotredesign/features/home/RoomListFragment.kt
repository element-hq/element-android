package im.vector.riotredesign.features.home

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.session.room.Room
import im.vector.riotredesign.R
import im.vector.riotredesign.core.extensions.replaceFragment
import im.vector.riotredesign.core.platform.RiotFragment
import kotlinx.android.synthetic.main.fragment_room_list.*
import org.koin.android.ext.android.inject

class RoomListFragment : RiotFragment(), RoomController.Callback {

    companion object {

        fun newInstance(): RoomListFragment {
            return RoomListFragment()
        }

    }

    private val matrix by inject<Matrix>()
    private val currentSession = matrix.currentSession!!
    private val roomController = RoomController(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_room_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        epoxyRecyclerView.setController(roomController)
        currentSession.liveRooms().observe(this, Observer<List<Room>> { renderRooms(it) })
    }

    private fun renderRooms(rooms: List<Room>?) {
        roomController.setData(rooms)
    }

    override fun onRoomSelected(room: Room) {
        val detailFragment = RoomDetailFragment.newInstance(room.roomId)
        replaceFragment(detailFragment, R.id.homeFragmentContainer)
    }


}