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
 *
 */

package im.vector.riotx.features.grouplist

import android.os.Bundle
import android.view.View
import com.airbnb.mvrx.Incomplete
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.fragmentViewModel
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.riotx.R
import im.vector.riotx.core.extensions.cleanup
import im.vector.riotx.core.extensions.configureWith
import im.vector.riotx.core.extensions.observeEvent
import im.vector.riotx.core.platform.StateView
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.features.home.HomeActivitySharedAction
import im.vector.riotx.features.home.HomeSharedActionViewModel
import kotlinx.android.synthetic.main.fragment_group_list.*
import javax.inject.Inject

class GroupListFragment @Inject constructor(
        val groupListViewModelFactory: GroupListViewModel.Factory,
        private val groupController: GroupSummaryController
) : VectorBaseFragment(), GroupSummaryController.Callback {

    private lateinit var sharedActionViewModel: HomeSharedActionViewModel
    private val viewModel: GroupListViewModel by fragmentViewModel()

    override fun getLayoutResId() = R.layout.fragment_group_list

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedActionViewModel = activityViewModelProvider.get(HomeSharedActionViewModel::class.java)
        groupController.callback = this
        stateView.contentView = groupListView
        groupListView.configureWith(groupController)
        viewModel.subscribe { renderState(it) }
        viewModel.openGroupLiveData.observeEvent(this) {
            sharedActionViewModel.post(HomeActivitySharedAction.OpenGroup)
        }
    }

    override fun onDestroyView() {
        groupController.callback = null
        groupListView.cleanup()
        super.onDestroyView()
    }

    private fun renderState(state: GroupListViewState) {
        when (state.asyncGroups) {
            is Incomplete -> stateView.state = StateView.State.Loading
            is Success    -> stateView.state = StateView.State.Content
        }
        groupController.update(state)
    }

    override fun onGroupSelected(groupSummary: GroupSummary) {
        viewModel.handle(GroupListAction.SelectGroup(groupSummary))
    }
}
