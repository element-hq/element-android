/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.leave

import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.SpaceStateHandler
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import okhttp3.internal.toImmutableList
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.query.RoomCategoryFilter
import org.matrix.android.sdk.api.query.SpaceFilter
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.getStateEvent
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.api.session.room.powerlevels.Role
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.flow.unwrap
import timber.log.Timber

class SpaceLeaveAdvancedViewModel @AssistedInject constructor(
        @Assisted val initialState: SpaceLeaveAdvanceViewState,
        private val session: Session,
        private val spaceStateHandler: SpaceStateHandler
) : VectorViewModel<SpaceLeaveAdvanceViewState, SpaceLeaveAdvanceViewAction, EmptyViewEvents>(initialState) {

    init {
        val space = session.getRoom(initialState.spaceId)
        val spaceSummary = space?.roomSummary()

        val powerLevelsEvent = space?.getStateEvent(EventType.STATE_ROOM_POWER_LEVELS, QueryStringValue.IsEmpty)
        powerLevelsEvent?.content?.toModel<PowerLevelsContent>()?.let { powerLevelsContent ->
            val powerLevelsHelper = PowerLevelsHelper(powerLevelsContent)
            val isAdmin = powerLevelsHelper.getUserRole(session.myUserId) is Role.Admin
            val otherAdminCount = spaceSummary?.otherMemberIds
                    ?.map { powerLevelsHelper.getUserRole(it) }
                    ?.count { it is Role.Admin }
                    ?: 0
            val isLastAdmin = isAdmin && otherAdminCount == 0
            setState {
                copy(isLastAdmin = isLastAdmin)
            }
        }

        setState { copy(spaceSummary = spaceSummary) }
        session.getRoom(initialState.spaceId)
                ?.flow()
                ?.liveRoomSummary()
                ?.unwrap()
                ?.onEach {
                    if (it.membership == Membership.LEAVE) {
                        setState { copy(leaveState = Success(Unit)) }
                        if (spaceStateHandler.getSafeActiveSpaceId() == initialState.spaceId) {
                            // switch to home?
                            spaceStateHandler.setCurrentSpace(null, session)
                        }
                    }
                }?.launchIn(viewModelScope)

        viewModelScope.launch {
            val children = session.roomService().getRoomSummaries(
                    roomSummaryQueryParams {
                        includeType = null
                        memberships = listOf(Membership.JOIN)
                        spaceFilter = SpaceFilter.ActiveSpace(initialState.spaceId)
                        roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
                    }
            )

            setState {
                copy(allChildren = Success(children))
            }
        }
    }

    override fun handle(action: SpaceLeaveAdvanceViewAction) {
        when (action) {
            is SpaceLeaveAdvanceViewAction.UpdateFilter -> setState { copy(currentFilter = action.filter) }
            SpaceLeaveAdvanceViewAction.ClearError -> setState { copy(leaveState = Uninitialized) }
            SpaceLeaveAdvanceViewAction.SelectNone -> setState { copy(selectedRooms = emptyList()) }
            is SpaceLeaveAdvanceViewAction.SetFilteringEnabled -> setState { copy(isFilteringEnabled = action.isEnabled) }
            is SpaceLeaveAdvanceViewAction.ToggleSelection -> handleSelectionToggle(action)
            SpaceLeaveAdvanceViewAction.DoLeave -> handleLeave()
            SpaceLeaveAdvanceViewAction.SelectAll -> handleSelectAll()
        }
    }

    private fun handleSelectAll() = withState { state ->
        val filteredRooms = (state.allChildren as? Success)?.invoke()?.filter {
            it.name.contains(state.currentFilter, true)
        }
        filteredRooms?.let {
            setState { copy(selectedRooms = it.map { it.roomId }) }
        }
    }

    private fun handleLeave() = withState { state ->
        setState { copy(leaveState = Loading()) }
        viewModelScope.launch {
            try {
                state.selectedRooms.forEach {
                    try {
                        session.roomService().leaveRoom(it)
                    } catch (failure: Throwable) {
                        // silently ignore?
                        Timber.e(failure, "Fail to leave sub rooms/spaces")
                    }
                }

                session.spaceService().leaveSpace(initialState.spaceId)
                // We observe the membership and to dismiss when we have remote echo of leaving
            } catch (failure: Throwable) {
                setState { copy(leaveState = Fail(failure)) }
            }
        }
    }

    private fun handleSelectionToggle(action: SpaceLeaveAdvanceViewAction.ToggleSelection) = withState { state ->
        val existing = state.selectedRooms.toMutableList()
        if (existing.contains(action.roomId)) {
            existing.remove(action.roomId)
        } else {
            existing.add(action.roomId)
        }
        setState {
            copy(
                    selectedRooms = existing.toImmutableList(),
            )
        }
    }

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<SpaceLeaveAdvancedViewModel, SpaceLeaveAdvanceViewState> {
        override fun create(initialState: SpaceLeaveAdvanceViewState): SpaceLeaveAdvancedViewModel
    }

    companion object : MavericksViewModelFactory<SpaceLeaveAdvancedViewModel, SpaceLeaveAdvanceViewState> by hiltMavericksViewModelFactory()
}
