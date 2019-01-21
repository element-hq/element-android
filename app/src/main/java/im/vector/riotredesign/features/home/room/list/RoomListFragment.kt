package im.vector.riotredesign.features.home.room.list

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Incomplete
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.activityViewModel
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.riotredesign.R
import im.vector.riotredesign.core.extensions.hideKeyboard
import im.vector.riotredesign.core.extensions.setupAsSearch
import im.vector.riotredesign.core.platform.RiotFragment
import im.vector.riotredesign.core.platform.StateView
import im.vector.riotredesign.features.home.HomeNavigator
import kotlinx.android.synthetic.main.fragment_room_list.*
import org.koin.android.ext.android.inject

class RoomListFragment : RiotFragment(), RoomSummaryController.Callback {

    companion object {
        fun newInstance(): RoomListFragment {
            return RoomListFragment()
        }
    }

    private val homeNavigator by inject<HomeNavigator>()
    private val homeViewModel: RoomListViewModel by activityViewModel()
    private lateinit var roomController: RoomSummaryController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_room_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        roomController = RoomSummaryController(this)
        stateView.contentView = epoxyRecyclerView
        epoxyRecyclerView.setController(roomController)
        setupFilterView()
        homeViewModel.subscribe { renderState(it) }
    }

    private fun renderState(state: RoomListViewState) {
        when (state.asyncRooms) {
            is Incomplete -> renderLoading()
            is Success    -> renderSuccess(state)
            is Fail       -> renderFailure(state.asyncRooms.error)
        }
    }

    private fun renderSuccess(state: RoomListViewState) {
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
            else                         -> getString(R.string.error_common)
        }
        stateView.state = StateView.State.Error(message)
    }

    private fun setupFilterView() {
        filterRoomView.setupAsSearch()
        filterRoomView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = Unit

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                homeViewModel.accept(RoomListActions.FilterRooms(s))
            }
        })
    }

    // RoomSummaryController.Callback **************************************************************

    override fun onRoomSelected(room: RoomSummary) {
        homeViewModel.accept(RoomListActions.SelectRoom(room))
        homeNavigator.openRoomDetail(room.roomId, null)
    }

}