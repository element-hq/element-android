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

import androidx.lifecycle.viewModelScope
import arrow.core.Option
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.assisted.AssistedFactory
import im.vector.app.R
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import io.reactivex.Observable
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.group.groupSummaryQueryParams
import org.matrix.android.sdk.api.session.group.model.GroupSummary
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.rx.rx

const val ALL_COMMUNITIES_GROUP_ID = "+ALL_COMMUNITIES_GROUP_ID"

class GroupListViewModel @AssistedInject constructor(@Assisted initialState: GroupListViewState,
                                                     private val selectedGroupStore: SelectedGroupDataSource,
                                                     private val session: Session,
                                                     private val stringProvider: StringProvider
) : VectorViewModel<GroupListViewState, GroupListAction, GroupListViewEvents>(initialState) {

    @AssistedFactory
    interface Factory {
        fun create(initialState: GroupListViewState): GroupListViewModel
    }

    companion object : MvRxViewModelFactory<GroupListViewModel, GroupListViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: GroupListViewState): GroupListViewModel? {
            val groupListFragment: GroupListFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return groupListFragment.groupListViewModelFactory.create(state)
        }
    }

    private var currentGroupId = ""

    init {
        observeGroupSummaries()
        observeSelectionState()
    }

    private fun observeSelectionState() {
        selectSubscribe(GroupListViewState::selectedGroup) { groupSummary ->
            if (groupSummary != null) {
                // We only want to open group if the updated selectedGroup is a different one.
                if (currentGroupId != groupSummary.groupId) {
                    currentGroupId = groupSummary.groupId
                    _viewEvents.post(GroupListViewEvents.OpenGroupSummary)
                }
                val optionGroup = Option.just(groupSummary)
                selectedGroupStore.post(optionGroup)
            } else {
                // If selected group is null we force to default. It can happens when leaving the selected group.
                setState {
                    copy(selectedGroup = this.asyncGroups()?.find { it.groupId == ALL_COMMUNITIES_GROUP_ID })
                }
            }
        }
    }

    override fun handle(action: GroupListAction) {
        when (action) {
            is GroupListAction.SelectGroup -> handleSelectGroup(action)
        }
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleSelectGroup(action: GroupListAction.SelectGroup) = withState { state ->
        if (state.selectedGroup?.groupId != action.groupSummary.groupId) {
            // We take care of refreshing group data when selecting to be sure we get all the rooms and users
            viewModelScope.launch {
                session.getGroup(action.groupSummary.groupId)?.fetchGroupData()
            }
            setState { copy(selectedGroup = action.groupSummary) }
        }
    }

    private fun observeGroupSummaries() {
        val groupSummariesQueryParams = groupSummaryQueryParams {
            memberships = listOf(Membership.JOIN)
            displayName = QueryStringValue.IsNotEmpty
        }
        Observable.combineLatest<GroupSummary, List<GroupSummary>, List<GroupSummary>>(
                session
                        .rx()
                        .liveUser(session.myUserId)
                        .map { optionalUser ->
                            GroupSummary(
                                    groupId = ALL_COMMUNITIES_GROUP_ID,
                                    membership = Membership.JOIN,
                                    displayName = stringProvider.getString(R.string.group_all_communities),
                                    avatarUrl = optionalUser.getOrNull()?.avatarUrl ?: "")
                        },
                session
                        .rx()
                        .liveGroupSummaries(groupSummariesQueryParams),
                { allCommunityGroup, communityGroups ->
                    listOf(allCommunityGroup) + communityGroups
                }
        )
                .execute { async ->
                    val currentSelectedGroupId = selectedGroup?.groupId
                    val newSelectedGroup = if (currentSelectedGroupId != null) {
                        async()?.find { it.groupId == currentSelectedGroupId }
                    } else {
                        async()?.firstOrNull()
                    }
                    copy(asyncGroups = async, selectedGroup = newSelectedGroup)
                }
    }
}
