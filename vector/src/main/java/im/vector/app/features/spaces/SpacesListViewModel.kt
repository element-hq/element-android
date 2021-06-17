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
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.AppStateHandler
import im.vector.app.RoomGroupingMethod
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.group
import im.vector.app.space
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.query.ActiveSpaceFilter
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.group.groupSummaryQueryParams
import org.matrix.android.sdk.api.session.room.RoomSortOrder
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.summary.RoomAggregateNotificationCount
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.rx.asObservable
import org.matrix.android.sdk.rx.rx
import java.util.concurrent.TimeUnit

class SpacesListViewModel @AssistedInject constructor(@Assisted initialState: SpaceListViewState,
                                                      private val appStateHandler: AppStateHandler,
                                                      private val session: Session,
                                                      private val vectorPreferences: VectorPreferences
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

//    private var currentGroupingMethod : RoomGroupingMethod? = null

    init {

        session.getUserLive(session.myUserId).asObservable()
                .subscribe {
                    setState {
                        copy(
                                myMxItem = it?.getOrNull()?.toMatrixItem()?.let { Success(it) } ?: Loading()
                        )
                    }
                }.disposeOnClear()

        observeSpaceSummaries()
//        observeSelectionState()
        appStateHandler.selectedRoomGroupingObservable
                .distinctUntilChanged()
                .subscribe {
                    setState {
                        copy(
                                selectedGroupingMethod = it.orNull() ?: RoomGroupingMethod.BySpace(null)
                        )
                    }
                }
                .disposeOnClear()

        session.getGroupSummariesLive(groupSummaryQueryParams {})
                .asObservable()
                .subscribe {
                    setState {
                        copy(
                                legacyGroups = it
                        )
                    }
                }.disposeOnClear()

        // XXX there should be a way to refactor this and share it
        session.getPagedRoomSummariesLive(
                roomSummaryQueryParams {
                    this.memberships = listOf(Membership.JOIN)
                    this.activeSpaceFilter = ActiveSpaceFilter.ActiveSpace(null).takeIf {
                        vectorPreferences.labsSpacesOnlyOrphansInHome()
                    } ?: ActiveSpaceFilter.None
                }, sortOrder = RoomSortOrder.NONE
        ).asObservable()
                .throttleFirst(300, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.computation())
                .subscribe {
                    val inviteCount = session.getRoomSummaries(
                            roomSummaryQueryParams { this.memberships = listOf(Membership.INVITE) }
                    ).size

                    val totalCount = session.getNotificationCountForRooms(
                            roomSummaryQueryParams {
                                this.memberships = listOf(Membership.JOIN)
                                this.activeSpaceFilter = ActiveSpaceFilter.ActiveSpace(null).takeIf {
                                    vectorPreferences.labsSpacesOnlyOrphansInHome()
                                } ?: ActiveSpaceFilter.None
                            }
                    )
                    val counts = RoomAggregateNotificationCount(
                            totalCount.notificationCount + inviteCount,
                            totalCount.highlightCount + inviteCount
                    )
                    setState {
                        copy(
                                homeAggregateCount = counts
                        )
                    }
                }.disposeOnClear()
    }

//    private fun observeSelectionState() {
//        selectSubscribe(SpaceListViewState::selectedSpace) { spaceSummary ->
//            if (spaceSummary != null) {
//                // We only want to open group if the updated selectedGroup is a different one.
//                if (currentGroupId != spaceSummary.roomId) {
//                    currentGroupId = spaceSummary.roomId
//                    _viewEvents.post(SpaceListViewEvents.OpenSpace)
//                }
//                appStateHandler.setCurrentSpace(spaceSummary.roomId)
//            } else {
//                // If selected group is null we force to default. It can happens when leaving the selected group.
//                setState {
//                    copy(selectedSpace = this.asyncSpaces()?.find { it.roomId == ALL_COMMUNITIES_GROUP_ID })
//                }
//            }
//        }
//    }

    override fun handle(action: SpaceListAction) {
        when (action) {
            is SpaceListAction.SelectSpace -> handleSelectSpace(action)
            is SpaceListAction.LeaveSpace -> handleLeaveSpace(action)
            SpaceListAction.AddSpace -> handleAddSpace()
            is SpaceListAction.ToggleExpand -> handleToggleExpand(action)
            is SpaceListAction.OpenSpaceInvite -> handleSelectSpaceInvite(action)
            is SpaceListAction.SelectLegacyGroup -> handleSelectGroup(action)
        }
    }

// PRIVATE METHODS *****************************************************************************

    private fun handleSelectSpace(action: SpaceListAction.SelectSpace) = withState { state ->
        val groupingMethod = state.selectedGroupingMethod
        if (groupingMethod is RoomGroupingMethod.ByLegacyGroup || groupingMethod.space()?.roomId != action.spaceSummary?.roomId) {
            setState { copy(selectedGroupingMethod = RoomGroupingMethod.BySpace(action.spaceSummary)) }
            appStateHandler.setCurrentSpace(action.spaceSummary?.roomId)
            _viewEvents.post(SpaceListViewEvents.OpenSpace(groupingMethod is RoomGroupingMethod.ByLegacyGroup))
        }
    }

    private fun handleSelectGroup(action: SpaceListAction.SelectLegacyGroup) = withState { state ->
        val groupingMethod = state.selectedGroupingMethod
        if (groupingMethod is RoomGroupingMethod.BySpace || groupingMethod.group()?.groupId != action.groupSummary?.groupId) {
            setState { copy(selectedGroupingMethod = RoomGroupingMethod.ByLegacyGroup(action.groupSummary)) }
            appStateHandler.setCurrentGroup(action.groupSummary?.groupId)
            _viewEvents.post(SpaceListViewEvents.OpenGroup(groupingMethod is RoomGroupingMethod.BySpace))
        }
    }

    private fun handleSelectSpaceInvite(action: SpaceListAction.OpenSpaceInvite) {
        _viewEvents.post(SpaceListViewEvents.OpenSpaceInvite(action.spaceSummary.roomId))
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
        val spaceSummaryQueryParams = roomSummaryQueryParams {
            memberships = listOf(Membership.JOIN, Membership.INVITE)
            displayName = QueryStringValue.IsNotEmpty
            excludeType = listOf(/**RoomType.MESSAGING,$*/
                    null)
        }
        Observable.combineLatest<User?, List<RoomSummary>, List<RoomSummary>>(
                session
                        .rx()
                        .liveUser(session.myUserId)
                        .map {
                            it.getOrNull()
                        },
                session
                        .rx()
                        .liveSpaceSummaries(spaceSummaryQueryParams),
                BiFunction { _, communityGroups ->
                    communityGroups
                }
        )
                .execute { async ->
                    copy(
                            asyncSpaces = async,
                            rootSpaces = session.spaceService().getRootSpaceSummaries()
                    )
                }
    }
}
