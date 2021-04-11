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

import androidx.lifecycle.viewModelScope
import arrow.core.Option
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.AppStateHandler
import im.vector.app.R
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.ui.UiStateRepository
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.rx.rx

const val ALL_COMMUNITIES_GROUP_ID = "+ALL_COMMUNITIES_GROUP_ID"

class SpacesListViewModel @AssistedInject constructor(@Assisted initialState: SpaceListViewState,
                                                      private val appStateHandler: AppStateHandler,
                                                      private val session: Session,
                                                      private val stringProvider: StringProvider,
                                                      private val uiStateRepository: UiStateRepository
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
        appStateHandler.selectedSpaceDataSource
                .observe()
                .subscribe {
                    if (currentGroupId != it.orNull()?.roomId) {
                        setState {
                            copy(
                                    selectedSpace = it.orNull()
                            )
                        }
                    }
                }
                .disposeOnClear()
    }

    private fun observeSelectionState() {
        selectSubscribe(SpaceListViewState::selectedSpace) { spaceSummary ->
            if (spaceSummary != null) {
                // We only want to open group if the updated selectedGroup is a different one.
                if (currentGroupId != spaceSummary.roomId) {
                    currentGroupId = spaceSummary.roomId
                    _viewEvents.post(SpaceListViewEvents.OpenSpace)
                }
                val optionGroup = Option.just(spaceSummary)
                appStateHandler.selectedSpaceDataSource.post(optionGroup)
            } else {
                // If selected group is null we force to default. It can happens when leaving the selected group.
                setState {
                    copy(selectedSpace = this.asyncSpaces()?.find { it.roomId == ALL_COMMUNITIES_GROUP_ID })
                }
            }
        }
    }

    override fun handle(action: SpaceListAction) {
        when (action) {
            is SpaceListAction.SelectSpace -> handleSelectSpace(action)
            is SpaceListAction.LeaveSpace -> handleLeaveSpace(action)
            SpaceListAction.AddSpace -> handleAddSpace()
            is SpaceListAction.ToggleExpand -> handleToggleExpand(action)
        }
    }

// PRIVATE METHODS *****************************************************************************

    private fun handleSelectSpace(action: SpaceListAction.SelectSpace) = withState { state ->
        // get uptodate version of the space
        val summary = session.spaceService().getSpaceSummaries(roomSummaryQueryParams { roomId = QueryStringValue.Equals(action.spaceSummary.roomId) })
                .firstOrNull()
        if (summary?.membership == Membership.INVITE) {
            _viewEvents.post(SpaceListViewEvents.OpenSpaceSummary(summary.roomId))
//            viewModelScope.launch(Dispatchers.IO) {
//                tryOrNull { session.spaceService().peekSpace(action.spaceSummary.spaceId) }.let {
//                    Timber.d("PEEK RESULT/ $it")
//                }
//            }
        } else {
            if (state.selectedSpace?.roomId != action.spaceSummary.roomId) {
//                state.selectedSpace?.let {
//                    selectedSpaceDataSource.post(Option.just(state.selectedSpace))
//                }
                setState { copy(selectedSpace = action.spaceSummary) }
                uiStateRepository.storeSelectedSpace(action.spaceSummary.roomId, session.sessionId)
            }
        }
    }

    private fun handleToggleExpand(action: SpaceListAction.ToggleExpand) = withState { state ->
        val updatedToggleStates = state.expandedStates.toMutableMap().apply {
            this[action.spaceSummary.roomId] = !(this[action.spaceSummary.roomId] ?: false)
        }
        setState {
            copy(expandedStates = updatedToggleStates)
        }
    }

    private fun handleLeaveSpace(action: SpaceListAction.LeaveSpace) {
        viewModelScope.launch {
            tryOrNull("Failed to leave space ${action.spaceSummary.roomId}") {
                session.spaceService().getSpace(action.spaceSummary.roomId)?.leave(null)
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
        Observable.combineLatest<RoomSummary, List<RoomSummary>, List<RoomSummary>>(
                session
                        .rx()
                        .liveUser(session.myUserId)
                        .map { optionalUser ->
                            RoomSummary(
                                    roomId = ALL_COMMUNITIES_GROUP_ID,
                                    membership = Membership.JOIN,
                                    displayName = stringProvider.getString(R.string.group_all_communities),
                                    avatarUrl = optionalUser.getOrNull()?.avatarUrl ?: "",
                                    encryptionEventTs = 0,
                                    isEncrypted = false,
                                    typingUsers = emptyList()
                            )
                        },
                session
                        .rx()
                        .liveSpaceSummaries(spaceSummaryQueryParams),
                BiFunction { allCommunityGroup, communityGroups ->
                    (listOf(allCommunityGroup) + communityGroups)
                }
        )
                .execute { async ->
                    val currentSelectedGroupId = selectedSpace?.roomId
                    val newSelectedGroup = if (currentSelectedGroupId != null) {
                        async()?.find { it.roomId == currentSelectedGroupId }
                    } else {
                        async()?.firstOrNull()
                    }
                    copy(
                            asyncSpaces = async,
                            selectedSpace = newSelectedGroup,
                            rootSpaces = session.spaceService().getRootSpaceSummaries()
                    )
                }
    }
}
