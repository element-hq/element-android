/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotredesign.features.home.group

import android.os.Bundle
import com.airbnb.mvrx.Incomplete
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.fragmentViewModel
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.riotredesign.R
import im.vector.riotredesign.core.extensions.observeEvent
import im.vector.riotredesign.core.platform.StateView
import im.vector.riotredesign.core.platform.VectorBaseFragment
import im.vector.riotredesign.features.home.HomeNavigator
import kotlinx.android.synthetic.main.fragment_group_list.*
import org.koin.android.ext.android.inject

class GroupListFragment : VectorBaseFragment(), GroupSummaryController.Callback {

    companion object {
        fun newInstance(): GroupListFragment {
            return GroupListFragment()
        }
    }

    private val viewModel: GroupListViewModel by fragmentViewModel()
    private val homeNavigator by inject<HomeNavigator>()
    private val groupController by inject<GroupSummaryController>()

    override fun getLayoutResId() = R.layout.fragment_group_list

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        groupController.callback = this
        stateView.contentView = groupListEpoxyRecyclerView
        groupListEpoxyRecyclerView.setController(groupController)
        viewModel.subscribe { renderState(it) }
        viewModel.openGroupLiveData.observeEvent(this) {
            homeNavigator.openSelectedGroup(it)
        }
    }

    private fun renderState(state: GroupListViewState) {
        when (state.asyncGroups) {
            is Incomplete -> renderLoading()
            is Success    -> renderSuccess(state)
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