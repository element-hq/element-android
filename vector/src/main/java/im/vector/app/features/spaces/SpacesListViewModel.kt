/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.spaces

import arrow.core.Option
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.R
import im.vector.app.core.platform.VectorViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.grouplist.SelectedSpaceDataSource
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.session.space.SpaceSummary
import org.matrix.android.sdk.rx.rx

const val ALL_COMMUNITIES_GROUP_ID = "+ALL_COMMUNITIES_GROUP_ID"

sealed class SpaceListAction : VectorViewModelAction {
    data class SelectSpace(val spaceSummary: SpaceSummary) : SpaceListAction()
    object AddSpace : SpaceListAction()
}

/**
 * Transient events for group list screen
 */
sealed class SpaceListViewEvents : VectorViewEvents {
    object OpenSpace : SpaceListViewEvents()
    data class OpenSpaceSummary(val id: String) : SpaceListViewEvents()
    object AddSpace : SpaceListViewEvents()
}

data class SpaceListViewState(
        val asyncSpaces: Async<List<SpaceSummary>> = Uninitialized,
        val selectedSpace: SpaceSummary? = null
) : MvRxState

class SpacesListViewModel @AssistedInject constructor(@Assisted initialState: SpaceListViewState,
                                                      private val selectedSpaceDataSource: SelectedSpaceDataSource,
                                                      private val session: Session,
                                                      private val stringProvider: StringProvider
) : VectorViewModel<SpaceListViewState, SpaceListAction, SpaceListViewEvents>(initialState) {

    @AssistedFactory
    interface Factory {
        fun create(initialState: SpaceListViewState): SpacesListViewModel
    }

    companion object : MvRxViewModelFactory<SpacesListViewModel, SpaceListViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: SpaceListViewState): SpacesListViewModel {
            val groupListFragment: SpaceListFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return groupListFragment.spaceListViewModelFactory.create(state)
        }
    }

    private var currentGroupId = ""

    init {
        observeSpaceSummaries()
        observeSelectionState()
        selectedSpaceDataSource.observe().execute {
            if (this.selectedSpace != it.invoke()?.orNull()) {
                copy(
                        selectedSpace = it.invoke()?.orNull()
                )
            } else this
        }
    }

    private fun observeSelectionState() {
        selectSubscribe(SpaceListViewState::selectedSpace) { spaceSummary ->
            if (spaceSummary != null) {
                // We only want to open group if the updated selectedGroup is a different one.
                if (currentGroupId != spaceSummary.spaceId) {
                    currentGroupId = spaceSummary.spaceId
                    _viewEvents.post(SpaceListViewEvents.OpenSpace)
                }
                val optionGroup = Option.just(spaceSummary)
                selectedSpaceDataSource.post(optionGroup)
            } else {
                // If selected group is null we force to default. It can happens when leaving the selected group.
                setState {
                    copy(selectedSpace = this.asyncSpaces()?.find { it.spaceId == ALL_COMMUNITIES_GROUP_ID })
                }
            }
        }
    }

    override fun handle(action: SpaceListAction) {
        when (action) {
            is SpaceListAction.SelectSpace -> handleSelectSpace(action)
            else                           -> handleAddSpace()
        }
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleSelectSpace(action: SpaceListAction.SelectSpace) = withState { state ->
        // get uptodate version of the space
        val summary = session.spaceService().getSpaceSummaries(roomSummaryQueryParams { roomId = QueryStringValue.Equals(action.spaceSummary.spaceId) })
                .firstOrNull()
        if (summary?.roomSummary?.membership == Membership.INVITE) {
            _viewEvents.post(SpaceListViewEvents.OpenSpaceSummary(summary.roomSummary.roomId))
//            viewModelScope.launch(Dispatchers.IO) {
//                tryOrNull { session.spaceService().peekSpace(action.spaceSummary.spaceId) }.let {
//                    Timber.d("PEEK RESULT/ $it")
//                }
//            }
        } else {
            if (state.selectedSpace?.spaceId != action.spaceSummary.spaceId) {
//                state.selectedSpace?.let {
//                    selectedSpaceDataSource.post(Option.just(state.selectedSpace))
//                }
                setState { copy(selectedSpace = action.spaceSummary) }
            }
        }
    }

    private fun handleAddSpace() {
        _viewEvents.post(SpaceListViewEvents.AddSpace)
    }

    private fun observeSpaceSummaries() {
        val spaceSummaryQueryParams = roomSummaryQueryParams() {
            memberships = listOf(Membership.JOIN, Membership.INVITE)
            displayName = QueryStringValue.IsNotEmpty
            excludeType = listOf(/**RoomType.MESSAGING,$*/
                    null)
        }
        Observable.combineLatest<SpaceSummary, List<SpaceSummary>, List<SpaceSummary>>(
                session
                        .rx()
                        .liveUser(session.myUserId)
                        .map { optionalUser ->
                            SpaceSummary(
                                    spaceId = ALL_COMMUNITIES_GROUP_ID,
                                    roomSummary = RoomSummary(
                                            roomId = ALL_COMMUNITIES_GROUP_ID,
                                            membership = Membership.JOIN,
                                            displayName = stringProvider.getString(R.string.group_all_communities),
                                            avatarUrl = optionalUser.getOrNull()?.avatarUrl ?: "",
                                            encryptionEventTs = 0,
                                            isEncrypted = false,
                                            typingUsers = emptyList()
                                    ),
                                    children = emptyList()
                            )
                        },
                session
                        .rx()
                        .liveSpaceSummaries(spaceSummaryQueryParams),
                BiFunction { allCommunityGroup, communityGroups ->
                    listOf(allCommunityGroup) + communityGroups
                }
        )
                .execute { async ->
                    val currentSelectedGroupId = selectedSpace?.spaceId
                    val newSelectedGroup = if (currentSelectedGroupId != null) {
                        async()?.find { it.spaceId == currentSelectedGroupId }
                    } else {
                        async()?.firstOrNull()
                    }
                    copy(asyncSpaces = async, selectedSpace = newSelectedGroup)
                }
    }
}
