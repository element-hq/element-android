package im.vector.riotredesign.features.home.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.Incomplete
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.fragmentViewModel
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.RiotFragment
import im.vector.riotredesign.core.platform.StateView
import kotlinx.android.synthetic.main.fragment_group_list.*

class GroupListFragment : RiotFragment(), GroupSummaryController.Callback {

    companion object {
        fun newInstance(): GroupListFragment {
            return GroupListFragment()
        }
    }

    private val viewModel: GroupListViewModel by fragmentViewModel()

    private lateinit var groupController: GroupSummaryController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_group_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        groupController = GroupSummaryController(this)
        stateView.contentView = epoxyRecyclerView
        epoxyRecyclerView.setController(groupController)
        viewModel.subscribe { renderState(it) }
    }

    private fun renderState(state: GroupListViewState) {
        when (state.asyncGroups) {
            is Incomplete -> renderLoading()
            is Success -> renderSuccess(state)
        }
    }

    private fun renderSuccess(state: GroupListViewState) {
        stateView.state = StateView.State.Content
        groupController.setData(state)
    }

    private fun renderLoading() {
        stateView.state = StateView.State.Loading
    }

    override fun onGroupSelected(groupSummary: GroupSummary) {
        viewModel.accept(GroupListActions.SelectGroup(groupSummary))
    }

}