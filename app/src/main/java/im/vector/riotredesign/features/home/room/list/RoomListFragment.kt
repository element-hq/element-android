package im.vector.riotredesign.features.home.room.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Incomplete
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.activityViewModel
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.RiotFragment
import im.vector.riotredesign.core.platform.StateView
import im.vector.riotredesign.features.home.HomeActions
import im.vector.riotredesign.features.home.HomeNavigator
import im.vector.riotredesign.features.home.HomeViewModel
import im.vector.riotredesign.features.home.HomeViewState
import kotlinx.android.synthetic.main.fragment_room_list.*
import org.koin.android.ext.android.inject

class RoomListFragment : RiotFragment(), RoomSummaryController.Callback {

    companion object {
        fun newInstance(): RoomListFragment {
            return RoomListFragment()
        }
    }

    private val homeNavigator by inject<HomeNavigator>()
    private val viewModel: HomeViewModel by activityViewModel()
    private lateinit var roomController: RoomSummaryController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_room_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        roomController = RoomSummaryController(this)
        stateView.contentView = epoxyRecyclerView
        epoxyRecyclerView.setController(roomController)
        viewModel.subscribe { renderState(it) }
    }

    private fun renderState(state: HomeViewState) {
        when (state.asyncRooms) {
            is Incomplete -> renderLoading()
            is Success -> renderSuccess(state)
            is Fail -> renderFailure(state.asyncRooms.error)
        }
        if (state.shouldOpenRoomDetail && state.selectedRoom != null) {
            homeNavigator.openRoomDetail(state.selectedRoom.roomId)
            viewModel.accept(HomeActions.RoomDisplayed)
        }
    }

    private fun renderSuccess(state: HomeViewState) {
        if (state.asyncRooms().isNullOrEmpty()) {
            stateView.state = StateView.State.Empty(getString(R.string.room_list_empty))
        } else {
            stateView.state = StateView.State.Content
        }
        roomController.setData(state)
    }

    private fun renderLoading() {
        stateView.state = StateView.State.Loading
    }

    private fun renderFailure(error: Throwable) {
        val message = when (error) {
            is Failure.NetworkConnection -> getString(R.string.error_no_network)
            else -> getString(R.string.error_common)
        }
        stateView.state = StateView.State.Error(message)
    }

    override fun onRoomSelected(room: RoomSummary) {
        viewModel.accept(HomeActions.SelectRoom(room))
    }

}