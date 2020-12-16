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

package im.vector.app.features.grouplist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.Incomplete
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.StateView
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentGroupListBinding
import im.vector.app.features.home.HomeActivitySharedAction
import im.vector.app.features.home.HomeSharedActionViewModel

import org.matrix.android.sdk.api.session.group.model.GroupSummary
import javax.inject.Inject

class GroupListFragment @Inject constructor(
        val groupListViewModelFactory: GroupListViewModel.Factory,
        private val groupController: GroupSummaryController
) : VectorBaseFragment<FragmentGroupListBinding>(),
        GroupSummaryController.Callback {

    private lateinit var sharedActionViewModel: HomeSharedActionViewModel
    private val viewModel: GroupListViewModel by fragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentGroupListBinding {
        return FragmentGroupListBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedActionViewModel = activityViewModelProvider.get(HomeSharedActionViewModel::class.java)
        groupController.callback = this
        views.stateView.contentView = views.groupListView
        views.groupListView.configureWith(groupController)
        viewModel.observeViewEvents {
            when (it) {
                is GroupListViewEvents.OpenGroupSummary -> sharedActionViewModel.post(HomeActivitySharedAction.OpenGroup)
            }.exhaustive
        }
    }

    override fun onDestroyView() {
        groupController.callback = null
        views.groupListView.cleanup()
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) { state ->
        when (state.asyncGroups) {
            is Incomplete -> views.stateView.state = StateView.State.Loading
            is Success    -> views.stateView.state = StateView.State.Content
        }
        groupController.update(state)
    }

    override fun onGroupSelected(groupSummary: GroupSummary) {
        viewModel.handle(GroupListAction.SelectGroup(groupSummary))
    }
}
