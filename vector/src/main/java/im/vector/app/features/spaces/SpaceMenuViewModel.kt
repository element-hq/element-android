/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces

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
import im.vector.app.features.powerlevel.PowerLevelsFlowFactory
import im.vector.app.features.session.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.query.RoomCategoryFilter
import org.matrix.android.sdk.api.query.SpaceFilter
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.getRoomSummary
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.api.session.room.powerlevels.Role
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.flow.flow
import timber.log.Timber

class SpaceMenuViewModel @AssistedInject constructor(
        @Assisted val initialState: SpaceMenuState,
        val session: Session,
        val spaceStateHandler: SpaceStateHandler
) : VectorViewModel<SpaceMenuState, SpaceLeaveViewAction, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<SpaceMenuViewModel, SpaceMenuState> {
        override fun create(initialState: SpaceMenuState): SpaceMenuViewModel
    }

    companion object : MavericksViewModelFactory<SpaceMenuViewModel, SpaceMenuState> by hiltMavericksViewModelFactory()

    init {
        val roomSummary = session.getRoomSummary(initialState.spaceId)

        setState {
            copy(spaceSummary = roomSummary)
        }

        session.getRoom(initialState.spaceId)?.let { room ->

            room.flow().liveRoomSummary().onEach {
                it.getOrNull()?.let {
                    if (it.membership == Membership.LEAVE) {
                        setState { copy(leavingState = Success(Unit)) }
                        if (spaceStateHandler.getSafeActiveSpaceId() == initialState.spaceId) {
                            // switch to home?
                            spaceStateHandler.setCurrentSpace(null, session)
                        }
                    }
                }
            }.launchIn(viewModelScope)

            PowerLevelsFlowFactory(room)
                    .createFlow()
                    .onEach {
                        val powerLevelsHelper = PowerLevelsHelper(it)

                        val canInvite = powerLevelsHelper.isUserAbleToInvite(session.myUserId)
                        val canAddChild = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true, EventType.STATE_SPACE_CHILD)

                        val canChangeAvatar = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true, EventType.STATE_ROOM_AVATAR)
                        val canChangeName = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true, EventType.STATE_ROOM_NAME)
                        val canChangeTopic = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true, EventType.STATE_ROOM_TOPIC)

                        val isAdmin = powerLevelsHelper.getUserRole(session.myUserId) is Role.Admin
                        val otherAdminCount = roomSummary?.otherMemberIds
                                ?.map { powerLevelsHelper.getUserRole(it) }
                                ?.count { it is Role.Admin }
                                ?: 0
                        val isLastAdmin = isAdmin && otherAdminCount == 0

                        setState {
                            copy(
                                    canEditSettings = canChangeAvatar || canChangeName || canChangeTopic,
                                    canInvite = canInvite,
                                    canAddChild = canAddChild,
                                    isLastAdmin = isLastAdmin
                            )
                        }
                    }.launchIn(viewModelScope)
        }
    }

    override fun handle(action: SpaceLeaveViewAction) {
        when (action) {
            SpaceLeaveViewAction.SetAutoLeaveAll -> setState {
                copy(leaveMode = SpaceMenuState.LeaveMode.LEAVE_ALL, leavingState = Uninitialized)
            }
            SpaceLeaveViewAction.SetAutoLeaveNone -> setState {
                copy(leaveMode = SpaceMenuState.LeaveMode.LEAVE_NONE, leavingState = Uninitialized)
            }
            SpaceLeaveViewAction.SetAutoLeaveSelected -> setState {
                copy(leaveMode = SpaceMenuState.LeaveMode.LEAVE_SELECTED, leavingState = Uninitialized)
            }
            SpaceLeaveViewAction.LeaveSpace -> handleLeaveSpace()
        }
    }

    private fun handleLeaveSpace() = withState { state ->

        setState { copy(leavingState = Loading()) }

        session.coroutineScope.launch {
            try {
                if (state.leaveMode == SpaceMenuState.LeaveMode.LEAVE_NONE) {
                    session.spaceService().leaveSpace(initialState.spaceId)
                } else if (state.leaveMode == SpaceMenuState.LeaveMode.LEAVE_ALL) {
                    // need to find all child rooms that i have joined

                    session.roomService().getRoomSummaries(
                            roomSummaryQueryParams {
                                excludeType = null
                                spaceFilter = SpaceFilter.ActiveSpace(initialState.spaceId)
                                memberships = listOf(Membership.JOIN)
                                roomCategoryFilter = RoomCategoryFilter.ONLY_ROOMS
                            }
                    ).forEach {
                        try {
                            session.spaceService().leaveSpace(it.roomId)
                        } catch (failure: Throwable) {
                            // silently ignore?
                            Timber.e(failure, "Fail to leave sub rooms/spaces")
                        }
                    }
                    session.spaceService().leaveSpace(initialState.spaceId)
                }

                // We observe the membership and to dismiss when we have remote echo of leaving
            } catch (failure: Throwable) {
                setState { copy(leavingState = Fail(failure)) }
            }
        }
    }
}
