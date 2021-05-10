/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.spaces.manage

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.mvrx.runCatchingToAsync
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.session.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session

class SpaceManageRoomsViewModel @AssistedInject constructor(
        @Assisted val initialState: SpaceManageRoomViewState,
        private val session: Session
) : VectorViewModel<SpaceManageRoomViewState, SpaceManageRoomViewAction, SpaceManageRoomViewEvents>(initialState) {

    init {
        val spaceSummary = session.getRoomSummary(initialState.spaceId)
        setState {
            copy(
                    spaceSummary = spaceSummary?.let { Success(it) } ?: Uninitialized,
                    childrenInfo = Loading()
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val apiResult = runCatchingToAsync {
                session.spaceService().querySpaceChildren(spaceId = initialState.spaceId).second
            }
            setState {
                copy(
                        childrenInfo = apiResult
                )
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(initialState: SpaceManageRoomViewState): SpaceManageRoomsViewModel
    }

    companion object : MvRxViewModelFactory<SpaceManageRoomsViewModel, SpaceManageRoomViewState> {
        override fun create(viewModelContext: ViewModelContext, state: SpaceManageRoomViewState): SpaceManageRoomsViewModel? {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

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
        val selection = state.childrenInfo.invoke()?.filter {
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
                            suggested = suggested,
                            autoJoin = info.autoJoin
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
                session.spaceService().querySpaceChildren(spaceId = initialState.spaceId).second
            }
            setState {
                copy(
                        childrenInfo = apiResult
                )
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
