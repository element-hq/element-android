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

package im.vector.app.features.spaces.explore

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.platform.VectorViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.platform.VectorViewModelAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.RoomType
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.internal.util.awaitCallback
import org.matrix.android.sdk.rx.rx
import timber.log.Timber

data class SpaceDirectoryState(
        // The current filter
        val spaceId: String,
        val currentFilter: String = "",
        val spaceSummary: Async<RoomSummary> = Uninitialized,
        val spaceSummaryApiResult: Async<List<SpaceChildInfo>> = Uninitialized,
        val childList: List<SpaceChildInfo> = emptyList(),
        val hierarchyStack: List<String> = emptyList(),
        // True if more result are available server side
        val hasMore: Boolean = false,
        // Set of joined roomId / spaces,
        val joinedRoomsIds: Set<String> = emptySet(),
        // keys are room alias or roomId
        val changeMembershipStates: Map<String, ChangeMembershipState> = emptyMap()
) : MvRxState {
    constructor(args: SpaceDirectoryArgs) : this(
            spaceId = args.spaceId
    )
}

sealed class SpaceDirectoryViewAction : VectorViewModelAction {
    data class ExploreSubSpace(val spaceChildInfo: SpaceChildInfo) : SpaceDirectoryViewAction()
    data class JoinOrOpen(val spaceChildInfo: SpaceChildInfo) : SpaceDirectoryViewAction()
    object HandleBack : SpaceDirectoryViewAction()
}

sealed class SpaceDirectoryViewEvents : VectorViewEvents {
    object Dismiss : SpaceDirectoryViewEvents()
    data class NavigateToRoom(val roomId: String) : SpaceDirectoryViewEvents()
}

class SpaceDirectoryViewModel @AssistedInject constructor(
        @Assisted initialState: SpaceDirectoryState,
        private val session: Session
) : VectorViewModel<SpaceDirectoryState, SpaceDirectoryViewAction, SpaceDirectoryViewEvents>(initialState) {

    @AssistedFactory
    interface Factory {
        fun create(initialState: SpaceDirectoryState): SpaceDirectoryViewModel
    }

    companion object : MvRxViewModelFactory<SpaceDirectoryViewModel, SpaceDirectoryState> {
        override fun create(viewModelContext: ViewModelContext, state: SpaceDirectoryState): SpaceDirectoryViewModel? {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    init {

        val spaceSum = session.getRoomSummary(initialState.spaceId)
        setState {
            copy(
                    childList = spaceSum?.children ?: emptyList(),
                    spaceSummaryApiResult = Loading()
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val query = session.spaceService().querySpaceChildren(initialState.spaceId)
                setState {
                    copy(
                            spaceSummaryApiResult = Success(query.second)
                    )
                }
            } catch (failure: Throwable) {
                setState {
                    copy(
                            spaceSummaryApiResult = Fail(failure)
                    )
                }
            }
        }
        observeJoinedRooms()
        observeMembershipChanges()
    }

    private fun observeJoinedRooms() {
        val queryParams = roomSummaryQueryParams {
            memberships = listOf(Membership.JOIN)
        }
        session
                .rx()
                .liveRoomSummaries(queryParams)
                .subscribe { list ->
                    val joinedRoomIds = list
                            ?.map { it.roomId }
                            ?.toSet()
                            ?: emptySet()

                    setState {
                        copy(joinedRoomsIds = joinedRoomIds)
                    }
                }
                .disposeOnClear()
    }

    private fun observeMembershipChanges() {
        session.rx()
                .liveRoomChangeMembershipState()
                .subscribe {
                    setState { copy(changeMembershipStates = it) }
                }
                .disposeOnClear()
    }

    override fun handle(action: SpaceDirectoryViewAction) {
        when (action) {
            is SpaceDirectoryViewAction.ExploreSubSpace -> {
                setState {
                    copy(hierarchyStack = hierarchyStack + listOf(action.spaceChildInfo.childRoomId))
                }
            }
            SpaceDirectoryViewAction.HandleBack -> {
                withState {
                    if (it.hierarchyStack.isEmpty()) {
                        _viewEvents.post(SpaceDirectoryViewEvents.Dismiss)
                    } else {
                        setState {
                            copy(
                                    hierarchyStack = hierarchyStack.dropLast(1)
                            )
                        }
                    }
                }
            }
            is SpaceDirectoryViewAction.JoinOrOpen -> {
                handleJoinOrOpen(action.spaceChildInfo)
            }
        }
    }

    private fun handleJoinOrOpen(spaceChildInfo: SpaceChildInfo) = withState { state ->
        val isSpace = spaceChildInfo.roomType == RoomType.SPACE
        if (state.joinedRoomsIds.contains(spaceChildInfo.childRoomId)) {
            if (isSpace) {
                handle(SpaceDirectoryViewAction.ExploreSubSpace(spaceChildInfo))
            } else {
                _viewEvents.post(SpaceDirectoryViewEvents.NavigateToRoom(spaceChildInfo.childRoomId))
            }
        } else {
            // join
            viewModelScope.launch {
                try {
                    if (isSpace) {
                        session.spaceService().joinSpace(spaceChildInfo.childRoomId, null, spaceChildInfo.viaServers)
                    } else {
                        awaitCallback<Unit> {
                            session.joinRoom(spaceChildInfo.childRoomId, null, spaceChildInfo.viaServers, it)
                        }
                    }
                } catch (failure: Throwable) {
                    Timber.e(failure, "## Space: Failed to join room or subsapce")
                }
            }
        }
    }
}
