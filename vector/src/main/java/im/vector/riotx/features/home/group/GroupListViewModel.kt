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

package im.vector.riotx.features.home.group

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import arrow.core.Option
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.rx.rx
import im.vector.riotx.R
import im.vector.riotx.core.extensions.postLiveEvent
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.core.utils.LiveEvent
import io.reactivex.Observable
import io.reactivex.functions.BiFunction

const val ALL_COMMUNITIES_GROUP_ID = "ALL_COMMUNITIES_GROUP_ID"

class GroupListViewModel @AssistedInject constructor(@Assisted initialState: GroupListViewState,
                                                     private val selectedGroupStore: SelectedGroupDataSource,
                                                     private val session: Session,
                                                     private val stringProvider: StringProvider
) : VectorViewModel<GroupListViewState>(initialState) {

    @AssistedInject.Factory
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

    private val _openGroupLiveData = MutableLiveData<LiveEvent<GroupSummary>>()
    val openGroupLiveData: LiveData<LiveEvent<GroupSummary>>
        get() = _openGroupLiveData

    init {
        observeGroupSummaries()
        observeSelectionState()
    }

    private fun observeSelectionState() {
        selectSubscribe(GroupListViewState::selectedGroup) {
            if (it != null) {
                val selectedGroup = _openGroupLiveData.value?.peekContent()
                // We only wan to open group if the updated selectedGroup is a different one.
                if (selectedGroup?.groupId != it.groupId) {
                    _openGroupLiveData.postLiveEvent(it)
                }
                val optionGroup = Option.fromNullable(it)
                selectedGroupStore.post(optionGroup)
            }
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
        }
    }

    private fun observeGroupSummaries() {
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
                        .liveGroupSummaries()
                        // Keep only joined groups. Group invitations will be managed later
                        .map { it.filter { groupSummary -> groupSummary.membership == Membership.JOIN } },
                BiFunction { allCommunityGroup, communityGroups ->
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
