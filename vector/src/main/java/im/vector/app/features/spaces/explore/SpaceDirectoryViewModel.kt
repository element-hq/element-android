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
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.powerlevel.PowerLevelsObservableFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.RoomType
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.rx.rx
import timber.log.Timber

class SpaceDirectoryViewModel @AssistedInject constructor(
        @Assisted val initialState: SpaceDirectoryState,
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
                    childList = spaceSum?.spaceChildren ?: emptyList(),
                    currentRootSummary = spaceSum
            )
        }

        refreshFromApi(initialState.spaceId)
        observeJoinedRooms()
        observeMembershipChanges()
        observePermissions()
    }

    private fun observePermissions() {
        val room = session.getRoom(initialState.spaceId) ?: return

        val powerLevelsContentLive = PowerLevelsObservableFactory(room).createObservable()

        powerLevelsContentLive
                .subscribe {
                    val powerLevelsHelper = PowerLevelsHelper(it)
                    setState {
                        copy(canAddRooms = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true,
                                EventType.STATE_SPACE_CHILD))
                    }
                }
                .disposeOnClear()
    }

    private fun refreshFromApi(rootId: String?) = withState { state ->
        val spaceId = rootId ?: initialState.spaceId
        setState {
            copy(
                    apiResults = state.apiResults.toMutableMap().apply {
                        this[spaceId] = Loading()
                    }.toMap()
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            val cachedResults = state.apiResults.toMutableMap()
            try {
                val query = session.spaceService().querySpaceChildren(
                        spaceId,
                        limit = 10
                )
                val knownSummaries = query.children.mapNotNull {
                    session.getRoomSummary(it.childRoomId)
                            ?.takeIf { it.membership == Membership.JOIN } // only take if joined because it will be up to date (synced)
                }.distinctBy { it.roomId }
                setState {
                    copy(
                            apiResults = cachedResults.apply {
                                this[spaceId] = Success(query)
                            },
                            currentRootSummary = query.rootSummary,
                            paginationStatus = state.paginationStatus.toMutableMap().apply {
                                this[spaceId] = Uninitialized
                            }.toMap(),
                            knownRoomSummaries = (state.knownRoomSummaries + knownSummaries).distinctBy { it.roomId }
                    )
                }
            } catch (failure: Throwable) {
                setState {
                    copy(
                            apiResults = cachedResults.apply {
                                this[spaceId] = Fail(failure)
                            }
                    )
                }
            }
        }
    }

    private fun observeJoinedRooms() {
        val queryParams = roomSummaryQueryParams {
            memberships = listOf(Membership.JOIN)
            excludeType = null
        }
        session
                .rx()
                .liveRoomSummaries(queryParams)
                .map {
                    it.map { it.roomId }.toSet()
                }
                .execute {
                    copy(joinedRoomsIds = it.invoke() ?: emptySet())
                }
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
            is SpaceDirectoryViewAction.ExploreSubSpace          -> {
                handleExploreSubSpace(action)
            }
            SpaceDirectoryViewAction.HandleBack                  -> {
                handleBack()
            }
            is SpaceDirectoryViewAction.JoinOrOpen               -> {
                handleJoinOrOpen(action.spaceChildInfo)
            }
            is SpaceDirectoryViewAction.NavigateToRoom           -> {
                _viewEvents.post(SpaceDirectoryViewEvents.NavigateToRoom(action.roomId))
            }
            is SpaceDirectoryViewAction.ShowDetails              -> {
                // This is temporary for now to at least display something for the space beta
                // It's not ideal as it's doing some peeking that is not needed.
                session.permalinkService().createRoomPermalink(action.spaceChildInfo.childRoomId)?.let {
                    _viewEvents.post(SpaceDirectoryViewEvents.NavigateToMxToBottomSheet(it))
                }
            }
            SpaceDirectoryViewAction.Retry                       -> {
                handleRetry()
            }
            SpaceDirectoryViewAction.LoadAdditionalItemsIfNeeded -> {
                loadAdditionalItemsIfNeeded()
            }
        }
    }

    private fun handleBack() = withState { state ->
        if (state.hierarchyStack.isEmpty()) {
            _viewEvents.post(SpaceDirectoryViewEvents.Dismiss)
        } else {
            val newStack = state.hierarchyStack.dropLast(1)
            val newRootId = newStack.lastOrNull() ?: initialState.spaceId
            val rootSummary = state.apiResults[newRootId]?.invoke()?.rootSummary
            setState {
                copy(
                        hierarchyStack = hierarchyStack.dropLast(1),
                        currentRootSummary = rootSummary
                )
            }
        }
    }

    private fun handleRetry() = withState { state ->
        refreshFromApi(state.hierarchyStack.lastOrNull() ?: initialState.spaceId)
    }

    private fun handleExploreSubSpace(action: SpaceDirectoryViewAction.ExploreSubSpace) = withState { state ->
        val newRootId = action.spaceChildInfo.childRoomId
        val curSum = RoomSummary(
                roomId = action.spaceChildInfo.childRoomId,
                roomType = action.spaceChildInfo.roomType,
                name = action.spaceChildInfo.name ?: "",
                canonicalAlias = action.spaceChildInfo.canonicalAlias,
                topic = action.spaceChildInfo.topic ?: "",
                joinedMembersCount = action.spaceChildInfo.activeMemberCount,
                avatarUrl = action.spaceChildInfo.avatarUrl ?: "",
                isEncrypted = false,
                joinRules = if (action.spaceChildInfo.worldReadable) RoomJoinRules.PUBLIC else RoomJoinRules.PRIVATE,
                encryptionEventTs = 0,
                typingUsers = emptyList()
        )
        setState {
            copy(
                    hierarchyStack = hierarchyStack + listOf(newRootId),
                    currentRootSummary = curSum
            )
        }
        val shouldLoad = when (state.apiResults[newRootId]) {
            Uninitialized -> true
            is Loading    -> false
            is Success    -> false
            is Fail       -> true
            null          -> true
        }

        if (shouldLoad) {
            refreshFromApi(newRootId)
        }
    }

    private fun loadAdditionalItemsIfNeeded() = withState { state ->
        val currentRootId = state.hierarchyStack.lastOrNull() ?: initialState.spaceId
        val mutablePaginationStatus = state.paginationStatus.toMutableMap().apply {
            if (this[currentRootId] == null) {
                this[currentRootId] = Uninitialized
            }
        }

        if (mutablePaginationStatus[currentRootId] is Loading) return@withState

        setState {
            copy(paginationStatus = mutablePaginationStatus.toMap())
        }

        viewModelScope.launch(Dispatchers.IO) {
            val cachedResults = state.apiResults.toMutableMap()
            try {
                val currentResponse = cachedResults[currentRootId]?.invoke()
                if (currentResponse == null) {
                    // nothing to paginate through...
                    setState {
                        copy(paginationStatus = mutablePaginationStatus.apply { this[currentRootId] = Uninitialized }.toMap())
                    }
                    return@launch
                }
                val query = session.spaceService().querySpaceChildren(
                        currentRootId,
                        limit = 10,
                        from = currentResponse.nextToken,
                        knownStateList = currentResponse.childrenState
                )
                val knownSummaries = query.children.mapNotNull {
                    session.getRoomSummary(it.childRoomId)
                            ?.takeIf { it.membership == Membership.JOIN } // only take if joined because it will be up to date (synced)
                }.distinctBy { it.roomId }

                cachedResults[currentRootId] = Success(
                        currentResponse.copy(
                                children = currentResponse.children + query.children,
                                nextToken = query.nextToken
                        )
                )
                setState {
                    copy(
                            apiResults = cachedResults.toMap(),
                            paginationStatus = mutablePaginationStatus.apply { this[currentRootId] = Success(Unit) }.toMap(),
                            knownRoomSummaries = (state.knownRoomSummaries + knownSummaries).distinctBy { it.roomId }
                    )
                }
            } catch (failure: Throwable) {
                setState {
                    copy(
                            paginationStatus = mutablePaginationStatus.apply { this[currentRootId] = Fail(failure) }.toMap()
                    )
                }
            }
        }
    }

    private fun handleJoinOrOpen(spaceChildInfo: SpaceChildInfo) = withState { state ->
        val isSpace = spaceChildInfo.roomType == RoomType.SPACE
        val childId = spaceChildInfo.childRoomId
        if (state.joinedRoomsIds.contains(childId)) {
            if (isSpace) {
                handle(SpaceDirectoryViewAction.ExploreSubSpace(spaceChildInfo))
            } else {
                _viewEvents.post(SpaceDirectoryViewEvents.NavigateToRoom(childId))
            }
        } else {
            // join
            viewModelScope.launch {
                try {
                    if (isSpace) {
                        session.spaceService().joinSpace(childId, null, spaceChildInfo.viaServers)
                    } else {
                        session.joinRoom(childId, null, spaceChildInfo.viaServers)
                    }
                } catch (failure: Throwable) {
                    Timber.e(failure, "## Space: Failed to join room or subspace")
                }
            }
        }
    }
}
