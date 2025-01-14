/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.manage

import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.mvrx.runCatchingToAsync
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.session.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoomSummary

class SpaceManageRoomsViewModel @AssistedInject constructor(
        @Assisted val initialState: SpaceManageRoomViewState,
        private val session: Session
) : VectorViewModel<SpaceManageRoomViewState, SpaceManageRoomViewAction, SpaceManageRoomViewEvents>(initialState) {

    private val paginationLimit = 10

    init {
        val spaceSummary = session.getRoomSummary(initialState.spaceId)
        setState {
            copy(
                    spaceSummary = spaceSummary?.let { Success(it) } ?: Uninitialized,
                    childrenInfo = Loading()
            )
        }

        refreshSummaryAPI()
    }

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<SpaceManageRoomsViewModel, SpaceManageRoomViewState> {
        override fun create(initialState: SpaceManageRoomViewState): SpaceManageRoomsViewModel
    }

    companion object : MavericksViewModelFactory<SpaceManageRoomsViewModel, SpaceManageRoomViewState> by hiltMavericksViewModelFactory()

    override fun handle(action: SpaceManageRoomViewAction) {
        when (action) {
            is SpaceManageRoomViewAction.ToggleSelection -> handleToggleSelection(action)
            is SpaceManageRoomViewAction.UpdateFilter -> {
                setState { copy(currentFilter = action.filter) }
            }
            SpaceManageRoomViewAction.ClearSelection -> {
                setState { copy(selectedRooms = emptyList()) }
            }
            SpaceManageRoomViewAction.BulkRemove -> {
                handleBulkRemove()
            }
            is SpaceManageRoomViewAction.MarkAllAsSuggested -> {
                handleBulkMarkAsSuggested(action.suggested)
            }
            SpaceManageRoomViewAction.RefreshFromServer -> {
                refreshSummaryAPI()
            }
            SpaceManageRoomViewAction.LoadAdditionalItemsIfNeeded -> {
                paginateIfNeeded()
            }
        }
    }

    private fun handleBulkRemove() = withState { state ->
        setState { copy(actionState = Loading()) }
        val selection = state.selectedRooms
        session.coroutineScope.launch(Dispatchers.IO) {
            val errorList = mutableListOf<Throwable>()
            selection.forEach {
                try {
                    session.spaceService().getSpace(state.spaceId)?.removeChildren(it)
                } catch (failure: Throwable) {
                    errorList.add(failure)
                }
                tryOrNull {
                    // also remove space parent if any? and if I can
                    session.spaceService().removeSpaceParent(it, state.spaceId)
                }
            }
            if (errorList.isEmpty()) {
                // success
            } else {
                _viewEvents.post(SpaceManageRoomViewEvents.BulkActionFailure(errorList))
            }
            refreshSummaryAPI()
            setState { copy(actionState = Uninitialized) }
        }
    }

    private fun handleBulkMarkAsSuggested(suggested: Boolean) = withState { state ->
        setState { copy(actionState = Loading()) }
        val selection = state.childrenInfo.invoke()?.children?.filter {
            state.selectedRooms.contains(it.childRoomId)
        }.orEmpty()
        session.coroutineScope.launch(Dispatchers.IO) {
            val errorList = mutableListOf<Throwable>()
            selection.forEach { info ->
                try {
                    session.spaceService().getSpace(state.spaceId)?.addChildren(
                            roomId = info.childRoomId,
                            viaServers = info.viaServers,
                            order = info.order,
                            suggested = suggested
//                            autoJoin = info.autoJoin
                    )
                } catch (failure: Throwable) {
                    errorList.add(failure)
                }
            }
            if (errorList.isEmpty()) {
                // success
            } else {
                _viewEvents.post(SpaceManageRoomViewEvents.BulkActionFailure(errorList))
            }
            refreshSummaryAPI()
            setState { copy(actionState = Uninitialized) }
        }
    }

    private fun refreshSummaryAPI() {
        setState {
            copy(
                    childrenInfo = Loading()
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            val apiResult = runCatchingToAsync {
                session.spaceService().querySpaceChildren(
                        spaceId = initialState.spaceId,
                        limit = paginationLimit
                )
            }
            setState {
                copy(
                        childrenInfo = apiResult,
                        paginationStatus = Uninitialized,
                        knownRoomSummaries = apiResult.invoke()?.children?.mapNotNull {
                            session.getRoomSummary(it.childRoomId)
                        }.orEmpty()
                )
            }
        }
    }

    private fun paginateIfNeeded() = withState { state ->
        if (state.paginationStatus is Loading) return@withState
        val knownResults = state.childrenInfo.invoke()
        val nextToken = knownResults?.nextToken
        if (knownResults == null || nextToken == null) {
            setState {
                copy(
                        paginationStatus = Uninitialized
                )
            }
            return@withState
        }
        setState {
            copy(
                    paginationStatus = Loading()
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiResult = session.spaceService().querySpaceChildren(
                        spaceId = initialState.spaceId,
                        from = nextToken,
                        knownStateList = knownResults.childrenState,
                        limit = paginationLimit
                )
                val newKnown = apiResult.children.mapNotNull { session.getRoomSummary(it.childRoomId) }
                setState {
                    copy(
                            childrenInfo = Success(
                                    knownResults.copy(
                                            children = knownResults.children + apiResult.children,
                                            nextToken = apiResult.nextToken
                                    )
                            ),
                            paginationStatus = Success(Unit),
                            knownRoomSummaries = (state.knownRoomSummaries + newKnown).distinctBy { it.roomId }
                    )
                }
            } catch (failure: Throwable) {
                setState {
                    copy(
                            paginationStatus = Fail(failure)
                    )
                }
            }
        }
    }

    private fun handleToggleSelection(action: SpaceManageRoomViewAction.ToggleSelection) = withState { state ->
        val existing = state.selectedRooms.toMutableList()
        if (existing.contains(action.roomId)) {
            existing.remove(action.roomId)
        } else {
            existing.add(action.roomId)
        }
        setState {
            copy(
                    selectedRooms = existing.toList()
            )
        }
    }
}
