/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.preview

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
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoomSummary
import org.matrix.android.sdk.api.session.room.model.RoomType
import org.matrix.android.sdk.api.session.room.peeking.PeekResult
import org.matrix.android.sdk.api.session.space.JoinSpaceResult
import org.matrix.android.sdk.api.session.space.peeking.SpacePeekResult
import org.matrix.android.sdk.api.session.space.peeking.SpaceSubChildPeekResult
import timber.log.Timber

class SpacePreviewViewModel @AssistedInject constructor(
        @Assisted private val initialState: SpacePreviewState,
        private val errorFormatter: ErrorFormatter,
        private val session: Session
) : VectorViewModel<SpacePreviewState, SpacePreviewViewAction, SpacePreviewViewEvents>(initialState) {

    private var initialized = false

    init {
        // do we have some things in cache?
        session.getRoomSummary(initialState.idOrAlias)?.let {
            setState {
                copy(name = it.name, avatarUrl = it.avatarUrl)
            }
        }
    }

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<SpacePreviewViewModel, SpacePreviewState> {
        override fun create(initialState: SpacePreviewState): SpacePreviewViewModel
    }

    companion object : MavericksViewModelFactory<SpacePreviewViewModel, SpacePreviewState> by hiltMavericksViewModelFactory()

    override fun handle(action: SpacePreviewViewAction) {
        when (action) {
            SpacePreviewViewAction.ViewReady -> handleReady()
            SpacePreviewViewAction.AcceptInvite -> handleAcceptInvite()
            SpacePreviewViewAction.DismissInvite -> handleDismissInvite()
        }
    }

    private fun handleDismissInvite() {
        // Here we need to join the space himself as well as the default rooms in that space
        setState { copy(inviteTermination = Loading()) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                session.spaceService().rejectInvite(initialState.idOrAlias, null)
            } catch (failure: Throwable) {
                Timber.e(failure, "## Space: Failed to reject invite")
                _viewEvents.post(SpacePreviewViewEvents.JoinFailure(errorFormatter.toHumanReadable(failure)))
            }
            setState { copy(inviteTermination = Uninitialized) }
        }
    }

    private fun handleAcceptInvite() = withState { state ->
        // Here we need to join the space himself as well as the default rooms in that space
        // TODO if we have no summary, we cannot find auto join rooms...
        // So maybe we should trigger a retry on summary after the join?
        val spaceInfo = state.spaceInfo.invoke()
        val spaceVia = spaceInfo?.viaServers ?: emptyList()

        // trigger modal loading
        setState { copy(inviteTermination = Loading()) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val joinResult = session.spaceService().joinSpace(initialState.idOrAlias, null, spaceVia)
                setState { copy(inviteTermination = Uninitialized) }
                when (joinResult) {
                    JoinSpaceResult.Success,
                    is JoinSpaceResult.PartialSuccess -> {
                        // For now we don't handle partial success, it's just success
                        _viewEvents.post(SpacePreviewViewEvents.JoinSuccess)
                    }
                    is JoinSpaceResult.Fail -> {
                        _viewEvents.post(SpacePreviewViewEvents.JoinFailure(errorFormatter.toHumanReadable(joinResult.error)))
                    }
                }
            } catch (failure: Throwable) {
                // should not throw
                Timber.w(failure, "## Failed to join space")
                _viewEvents.post(SpacePreviewViewEvents.JoinFailure(errorFormatter.toHumanReadable(failure)))
            }
        }
    }

    private fun handleReady() {
        if (!initialized) {
            initialized = true
            // peek for the room
            setState {
                copy(
                        spaceInfo = Loading(),
                        childInfoList = Loading()
                )
            }
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    resolveSpaceInfo()
                } catch (failure: Throwable) {
                    Timber.e(failure, "## Space: Failed to resolve space info. Fallback to picking")
                    fallBackResolve()
                }
            }
        }
    }

    private suspend fun resolveSpaceInfo() {
        val resolveResult = session.spaceService().querySpaceChildren(initialState.idOrAlias)
        setState {
            copy(
                    spaceInfo = Success(
                            resolveResult.rootSummary.let {
                                ChildInfo(
                                        roomId = it.roomId,
                                        avatarUrl = it.avatarUrl,
                                        name = it.name,
                                        topic = it.topic,
                                        memberCount = it.joinedMembersCount,
                                        isSubSpace = it.roomType == RoomType.SPACE,
                                        children = Uninitialized,
                                        viaServers = null
                                )
                            }
                    ),
                    childInfoList = Success(
                            resolveResult.children.map {
                                ChildInfo(
                                        roomId = it.childRoomId,
                                        avatarUrl = it.avatarUrl,
                                        name = it.name,
                                        topic = it.topic,
                                        memberCount = it.activeMemberCount,
                                        isSubSpace = it.roomType == RoomType.SPACE,
                                        children = Uninitialized,
                                        viaServers = null
                                )
                            }
                    )
            )
        }
    }

    private suspend fun fallBackResolve() {
        try {
            val resolveResult: SpacePeekResult = session.spaceService().peekSpace(initialState.idOrAlias)
            val spaceInfo = (resolveResult as? SpacePeekResult.Success)?.summary?.roomPeekResult
            setState {
                copy(
                        spaceInfo = Success(
                                ChildInfo(
                                        roomId = spaceInfo?.roomId ?: initialState.idOrAlias,
                                        avatarUrl = spaceInfo?.avatarUrl,
                                        name = spaceInfo?.name,
                                        topic = spaceInfo?.topic,
                                        memberCount = spaceInfo?.numJoinedMembers,
                                        isSubSpace = true,
                                        children = Uninitialized,
                                        viaServers = spaceInfo?.viaServers

                                )
                        ),
                        childInfoList = resolveResult.let {
                            when (it) {
                                is SpacePeekResult.Success -> {
                                    (resolveResult as SpacePeekResult.Success).summary.children.mapNotNull { spaceChild ->
                                        val roomPeekResult = spaceChild.roomPeekResult
                                        if (roomPeekResult is PeekResult.Success) {
                                            ChildInfo(
                                                    roomId = spaceChild.id,
                                                    avatarUrl = roomPeekResult.avatarUrl,
                                                    name = roomPeekResult.name,
                                                    topic = roomPeekResult.topic,
                                                    memberCount = roomPeekResult.numJoinedMembers,
                                                    isSubSpace = spaceChild is SpaceSubChildPeekResult,
                                                    children = Uninitialized,
                                                    viaServers = roomPeekResult.viaServers

                                            )
                                        } else {
                                            null
                                        }
                                    }
                                    Success(emptyList())
                                }
                                else -> {
                                    Fail(Exception("Failed to get info"))
                                }
                            }
                        })
            }
        } catch (failure: Throwable) {
            setState {
                copy(
                        spaceInfo = session.getRoomSummary(initialState.idOrAlias)?.let {
                            Success(
                                    ChildInfo(
                                            roomId = it.roomId,
                                            avatarUrl = it.avatarUrl,
                                            name = it.displayName,
                                            topic = it.topic,
                                            memberCount = it.joinedMembersCount,
                                            isSubSpace = false,
                                            viaServers = null,
                                            children = Uninitialized
                                    )
                            )
                        } ?: Fail(failure), childInfoList = Fail(failure))
            }
        }
    }
}
