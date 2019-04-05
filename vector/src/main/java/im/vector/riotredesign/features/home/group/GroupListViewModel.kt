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

import arrow.core.Option
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.rx.rx
import im.vector.riotredesign.core.platform.VectorViewModel
import org.koin.android.ext.android.get

class GroupListViewModel(initialState: GroupListViewState,
                         private val selectedGroupHolder: SelectedGroupStore,
                         private val session: Session
) : VectorViewModel<GroupListViewState>(initialState) {

    companion object : MvRxViewModelFactory<GroupListViewModel, GroupListViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: GroupListViewState): GroupListViewModel? {
            val currentSession = viewModelContext.activity.get<Session>()
            val selectedGroupHolder = viewModelContext.activity.get<SelectedGroupStore>()
            return GroupListViewModel(state, selectedGroupHolder, currentSession)
        }
    }

    init {
        observeGroupSummaries()
        observeState()
    }

    private fun observeState() {
        subscribe {
            val selectedGroup = Option.fromNullable(it.selectedGroup)
            selectedGroupHolder.post(selectedGroup)
        }
    }

    fun accept(action: GroupListActions) {
        when (action) {
            is GroupListActions.SelectGroup -> handleSelectGroup(action)
        }
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleSelectGroup(action: GroupListActions.SelectGroup) = withState { state ->
        if (state.selectedGroup?.groupId != action.groupSummary.groupId) {
            setState { copy(selectedGroup = action.groupSummary) }
        } else {
            setState { copy(selectedGroup = null) }
        }
    }


    private fun observeGroupSummaries() {
        session
                .rx().liveGroupSummaries()
                .execute { async ->
                    copy(asyncGroups = async)
                }
    }


}