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

import androidx.lifecycle.asFlow
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.AppStateHandler
import im.vector.app.RoomGroupingMethod
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.analytics.plan.Interaction
import im.vector.app.features.invite.AutoAcceptInvites
import im.vector.app.features.session.coroutineScope
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.group
import im.vector.app.space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.query.ActiveSpaceFilter
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.group.groupSummaryQueryParams
import org.matrix.android.sdk.api.session.room.RoomSortOrder
import org.matrix.android.sdk.api.session.room.accountdata.RoomAccountDataTypes
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.spaceSummaryQueryParams
import org.matrix.android.sdk.api.session.room.summary.RoomAggregateNotificationCount
import org.matrix.android.sdk.api.session.space.SpaceOrderUtils
import org.matrix.android.sdk.api.session.space.model.SpaceOrderContent
import org.matrix.android.sdk.api.session.space.model.TopLevelSpaceComparator
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.flow.flow

class SpaceListViewModel @AssistedInject constructor(@Assisted initialState: SpaceListViewState,
                                                     private val appStateHandler: AppStateHandler,
                                                     private val session: Session,
                                                     private val vectorPreferences: VectorPreferences,
                                                     private val autoAcceptInvites: AutoAcceptInvites,
                                                     private val analyticsTracker: AnalyticsTracker
) : VectorViewModel<SpaceListViewState, SpaceListAction, SpaceListViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<SpaceListViewModel, SpaceListViewState> {
        override fun create(initialState: SpaceListViewState): SpaceListViewModel
    }

    companion object : MavericksViewModelFactory<SpaceListViewModel, SpaceListViewState> by hiltMavericksViewModelFactory()

//    private var currentGroupingMethod : RoomGroupingMethod? = null

    init {

        session.getUserLive(session.myUserId)
                .asFlow()
                .setOnEach {
                    copy(
                            myMxItem = it.getOrNull()?.toMatrixItem()?.let { Success(it) } ?: Loading()
                    )
                }

        observeSpaceSummaries()
//        observeSelectionState()
        appStateHandler.selectedRoomGroupingFlow
                .distinctUntilChanged()
                .setOnEach {
                    copy(
                            selectedGroupingMethod = it.orNull() ?: RoomGroupingMethod.BySpace(null)
                    )
                }

        session.getGroupSummariesLive(groupSummaryQueryParams {})
                .asFlow()
                .setOnEach {
                    copy(legacyGroups = it)
                }

        // XXX there should be a way to refactor this and share it
        session.getPagedRoomSummariesLive(
                roomSummaryQueryParams {
                    this.memberships = listOf(Membership.JOIN)
                    this.activeSpaceFilter = ActiveSpaceFilter.ActiveSpace(null).takeIf {
                        !vectorPreferences.prefSpacesShowAllRoomInHome()
                    } ?: ActiveSpaceFilter.None
                }, sortOrder = RoomSortOrder.NONE
        ).asFlow()
                .sample(300)
                .onEach {
                    val inviteCount = if (autoAcceptInvites.hideInvites) {
                        0
                    } else {
                        session.getRoomSummaries(
                                roomSummaryQueryParams { this.memberships = listOf(Membership.INVITE) }
                        ).size
                    }
                    val totalCount = session.getNotificationCountForRooms(
                            roomSummaryQueryParams {
                                this.memberships = listOf(Membership.JOIN)
                                this.activeSpaceFilter = ActiveSpaceFilter.ActiveSpace(null).takeIf {
                                    !vectorPreferences.prefSpacesShowAllRoomInHome()
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
                }
                .flowOn(Dispatchers.Default)
                .launchIn(viewModelScope)
    }

    override fun handle(action: SpaceListAction) {
        when (action) {
            is SpaceListAction.SelectSpace       -> handleSelectSpace(action)
            is SpaceListAction.LeaveSpace        -> handleLeaveSpace(action)
            SpaceListAction.AddSpace             -> handleAddSpace()
            is SpaceListAction.ToggleExpand      -> handleToggleExpand(action)
            is SpaceListAction.OpenSpaceInvite   -> handleSelectSpaceInvite(action)
            is SpaceListAction.SelectLegacyGroup -> handleSelectGroup(action)
            is SpaceListAction.MoveSpace         -> handleMoveSpace(action)
            is SpaceListAction.OnEndDragging     -> handleEndDragging()
            is SpaceListAction.OnStartDragging   -> handleStartDragging()
        }
    }

// PRIVATE METHODS *****************************************************************************

    var preDragExpandedState: Map<String, Boolean>? = null
    private fun handleStartDragging() = withState { state ->
        preDragExpandedState = state.expandedStates.toMap()
        setState {
            copy(
                    expandedStates = expandedStates.map {
                        it.key to false
                    }.toMap()
            )
        }
    }

    private fun handleEndDragging() {
        // restore expanded state
        setState {
            copy(
                    expandedStates = preDragExpandedState.orEmpty()
            )
        }
    }

    private fun handleMoveSpace(action: SpaceListAction.MoveSpace) = withState { state ->
        state.rootSpacesOrdered ?: return@withState
        val orderCommands = SpaceOrderUtils.orderCommandsForMove(
                state.rootSpacesOrdered.map {
                    it.roomId to (state.spaceOrderLocalEchos?.get(it.roomId) ?: state.spaceOrderInfo?.get(it.roomId))
                },
                action.spaceId,
                action.delta
        )

        // local echo
        val updatedLocalEchos = state.spaceOrderLocalEchos.orEmpty().toMutableMap().apply {
            orderCommands.forEach {
                this[it.spaceId] = it.order
            }
        }.toMap()

        setState {
            copy(
                    rootSpacesOrdered = state.rootSpacesOrdered.toMutableList().apply {
                        val index = indexOfFirst { it.roomId == action.spaceId }
                        val moved = removeAt(index)
                        add(index + action.delta, moved)
                    },
                    spaceOrderLocalEchos = updatedLocalEchos
            )
        }
        session.coroutineScope.launch {
            orderCommands.forEach {
                session.getRoom(it.spaceId)?.updateAccountData(RoomAccountDataTypes.EVENT_TYPE_SPACE_ORDER,
                        SpaceOrderContent(order = it.order).toContent()
                )
            }
        }

        // restore expanded state
        setState {
            copy(
                    expandedStates = preDragExpandedState.orEmpty()
            )
        }
    }

    private fun handleSelectSpace(action: SpaceListAction.SelectSpace) = withState { state ->
        val groupingMethod = state.selectedGroupingMethod
        if (groupingMethod is RoomGroupingMethod.ByLegacyGroup || groupingMethod.space()?.roomId != action.spaceSummary?.roomId) {
            analyticsTracker.capture(Interaction(null, null, Interaction.Name.SpacePanelSwitchSpace))
            setState { copy(selectedGroupingMethod = RoomGroupingMethod.BySpace(action.spaceSummary)) }
            appStateHandler.setCurrentSpace(action.spaceSummary?.roomId)
            _viewEvents.post(SpaceListViewEvents.OpenSpace(groupingMethod is RoomGroupingMethod.ByLegacyGroup))
        } else {
            analyticsTracker.capture(Interaction(null, null, Interaction.Name.SpacePanelSelectedSpace))
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
                session.spaceService().leaveSpace(action.spaceSummary.roomId)
            }
        }
    }

    private fun handleAddSpace() {
        _viewEvents.post(SpaceListViewEvents.AddSpace)
    }

    private fun observeSpaceSummaries() {
        val params = spaceSummaryQueryParams {
            memberships = listOf(Membership.JOIN, Membership.INVITE)
            displayName = QueryStringValue.IsNotEmpty
        }

        val flowSession = session.flow()

        combine(
                flowSession
                        .liveUser(session.myUserId)
                        .map {
                            it.getOrNull()
                        },
                flowSession
                        .liveSpaceSummaries(params),
                session
                        .accountDataService()
                        .getLiveRoomAccountDataEvents(setOf(RoomAccountDataTypes.EVENT_TYPE_SPACE_ORDER))
                        .asFlow()
        ) { _, communityGroups, _ ->
            communityGroups
        }
                .execute { async ->
                    val rootSpaces = session.spaceService().getRootSpaceSummaries()
                    val orders = rootSpaces.map {
                        it.roomId to session.getRoom(it.roomId)
                                ?.getAccountDataEvent(RoomAccountDataTypes.EVENT_TYPE_SPACE_ORDER)
                                ?.content.toModel<SpaceOrderContent>()
                                ?.safeOrder()
                    }.toMap()
                    copy(
                            asyncSpaces = async,
                            rootSpacesOrdered = rootSpaces.sortedWith(TopLevelSpaceComparator(orders)),
                            spaceOrderInfo = orders
                    )
                }

        // clear local echos on update
        session.accountDataService()
                .getLiveRoomAccountDataEvents(setOf(RoomAccountDataTypes.EVENT_TYPE_SPACE_ORDER))
                .asFlow()
                .execute {
                    copy(
                            spaceOrderLocalEchos = emptyMap()
                    )
                }
    }
}
