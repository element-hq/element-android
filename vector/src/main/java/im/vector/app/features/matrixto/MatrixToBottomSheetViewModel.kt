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

package im.vector.app.features.matrixto

import androidx.lifecycle.viewModelScope
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
import im.vector.app.R
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.createdirect.DirectRoomHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.permalinks.PermalinkData
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.peeking.PeekResult
import org.matrix.android.sdk.api.session.space.SpaceService
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.internal.session.room.alias.RoomAliasDescription
import org.matrix.android.sdk.internal.util.awaitCallback
import timber.log.Timber

class MatrixToBottomSheetViewModel @AssistedInject constructor(
        @Assisted initialState: MatrixToBottomSheetState,
        private val session: Session,
        private val stringProvider: StringProvider,
        private val directRoomHelper: DirectRoomHelper,
        private val errorFormatter: ErrorFormatter)
    : VectorViewModel<MatrixToBottomSheetState, MatrixToAction, MatrixToViewEvents>(initialState) {

    @AssistedFactory
    interface Factory {
        fun create(initialState: MatrixToBottomSheetState): MatrixToBottomSheetViewModel
    }

    init {
        when (initialState.linkType) {
            is PermalinkData.RoomLink -> {
                setState {
                    copy(roomPeekResult = Loading())
                }
            }
            is PermalinkData.UserLink -> {
                setState {
                    copy(matrixItem = Loading())
                }
            }
            is PermalinkData.GroupLink -> {
                // Not yet supported
            }
            is PermalinkData.FallbackLink -> {
                // Not yet supported
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            resolveLink(initialState)
        }
    }

    private suspend fun resolveLink(initialState: MatrixToBottomSheetState) {
        val permalinkData = initialState.linkType
        if (permalinkData is PermalinkData.FallbackLink) {
            setState {
                copy(
                        matrixItem = Fail(IllegalArgumentException(stringProvider.getString(R.string.permalink_malformed))),
                        startChattingState = Uninitialized
                )
            }
            return
        }

        when (permalinkData) {
            is PermalinkData.UserLink -> {
                val user = resolveUser(permalinkData.userId)
                setState {
                    copy(
                            matrixItem = Success(user.toMatrixItem()),
                            startChattingState = Success(Unit)
                    )
                }
            }
            is PermalinkData.RoomLink -> {
                // could this room be already known
                val knownRoom = if (permalinkData.isRoomAlias) {
                    tryOrNull {
                        awaitCallback<Optional<RoomAliasDescription>> {
                            session.getRoomIdByAlias(permalinkData.roomIdOrAlias, false, it)
                        }
                    }
                            ?.getOrNull()
                            ?.roomId?.let {
                                session.getRoom(permalinkData.roomIdOrAlias)
                            }
                } else {
                    session.getRoom(permalinkData.roomIdOrAlias)
                }?.roomSummary()

                if (knownRoom != null) {
                    setState {
                        copy(
                                roomPeekResult = Success(
                                        RoomInfoResult.FullInfo(
                                                roomItem = knownRoom.toMatrixItem(),
                                                name = knownRoom.name,
                                                topic = knownRoom.topic,
                                                memberCount = knownRoom.joinedMembersCount,
                                                alias = knownRoom.canonicalAlias,
                                                membership = knownRoom.membership,
                                                roomType = knownRoom.roomType,
                                                viaServers = null
                                        )
                                )
                        )
                    }
                } else {

                    val result = when (val peekResult = tryOrNull { resolveRoom(permalinkData.roomIdOrAlias) }) {
                        is PeekResult.Success           -> {
                            RoomInfoResult.FullInfo(
                                    roomItem = MatrixItem.RoomItem(peekResult.roomId, peekResult.name, peekResult.avatarUrl),
                                    name = peekResult.name ?: "",
                                    topic = peekResult.topic ?: "",
                                    memberCount = peekResult.numJoinedMembers,
                                    alias = peekResult.alias,
                                    membership = Membership.NONE,
                                    roomType = peekResult.roomType,
                                    viaServers = peekResult.viaServers.takeIf { it.isNotEmpty() } ?: permalinkData.viaParameters
                            )
                        }
                        is PeekResult.PeekingNotAllowed -> {
                            RoomInfoResult.PartialInfo(
                                    roomId = permalinkData.roomIdOrAlias,
                                    viaServers = permalinkData.viaParameters
                            )
                        }
                        PeekResult.UnknownAlias         -> {
                            RoomInfoResult.NotFound
                        }
                        null                            -> {
                            RoomInfoResult.PartialInfo(
                                    roomId = permalinkData.roomIdOrAlias,
                                    viaServers = permalinkData.viaParameters
                            ).takeIf { permalinkData.isRoomAlias.not() }
                                    ?: RoomInfoResult.NotFound
                        }
                    }
                    setState {
                        copy(
                                roomPeekResult = Success(result)
                        )
                    }
                }
            }
            is PermalinkData.GroupLink -> {
                // not yet supported
                _viewEvents.post(MatrixToViewEvents.Dismiss)
            }
            is PermalinkData.FallbackLink -> {
                _viewEvents.post(MatrixToViewEvents.Dismiss)
            }
        }
    }

    private suspend fun resolveUser(userId: String): User {
        return tryOrNull { session.resolveUser(userId) }
        // Create raw user in case the user is not searchable
                ?: User(userId, null, null)
    }

    /**
     * Let's try to get some information about that room,
     * main thing is trying to see if it's a space or a room
     */
    private suspend fun resolveRoom(roomIdOrAlias: String): PeekResult {
        return tryOrNull { // this should not throw as it returns a result, but better be safe
            awaitCallback {
                session.peekRoom(roomIdOrAlias, it)
            }
        }
                ?: PeekResult.PeekingNotAllowed(roomIdOrAlias, null, emptyList())
    }

    companion object : MvRxViewModelFactory<MatrixToBottomSheetViewModel, MatrixToBottomSheetState> {
        override fun create(viewModelContext: ViewModelContext, state: MatrixToBottomSheetState): MatrixToBottomSheetViewModel? {
            val fragment: MatrixToBottomSheet = (viewModelContext as FragmentViewModelContext).fragment()

            return fragment.matrixToBottomSheetViewModelFactory.create(state)
        }
    }

    override fun handle(action: MatrixToAction) {
        when (action) {
            is MatrixToAction.StartChattingWithUser -> handleStartChatting(action)
            MatrixToAction.FailedToResolveUser -> {
                _viewEvents.post(MatrixToViewEvents.Dismiss)
            }
            MatrixToAction.FailedToStartChatting -> {
                _viewEvents.post(MatrixToViewEvents.Dismiss)
            }
            is MatrixToAction.JoinSpace -> handleJoinSpace(action)
            is MatrixToAction.JoinRoom -> handleJoinRoom(action)
            is MatrixToAction.OpenSpace -> {
                _viewEvents.post(MatrixToViewEvents.NavigateToSpace(action.spaceID))
            }
            is MatrixToAction.OpenRoom -> {
                _viewEvents.post(MatrixToViewEvents.NavigateToRoom(action.roomId))
            }
        }.exhaustive
    }

    private fun handleJoinSpace(joinSpace: MatrixToAction.JoinSpace) {
        setState {
            copy(startChattingState = Loading())
        }
        viewModelScope.launch {
            try {
                val joinResult = session.spaceService().joinSpace(joinSpace.spaceID, null, joinSpace.viaServers?.take(3) ?: emptyList())
                if (joinResult.isSuccess()) {
                    _viewEvents.post(MatrixToViewEvents.NavigateToSpace(joinSpace.spaceID))
                } else {
                    val errMsg = errorFormatter.toHumanReadable((joinResult as? SpaceService.JoinSpaceResult.Fail)?.error)
                    _viewEvents.post(MatrixToViewEvents.ShowModalError(errMsg))
                }
            } catch (failure: Throwable) {
                _viewEvents.post(MatrixToViewEvents.ShowModalError(errorFormatter.toHumanReadable(failure)))
            } finally {
                setState {
                    // we can hide this button has we will navigate out
                    copy(startChattingState = Uninitialized)
                }
            }

        }
    }

    private fun handleJoinRoom(action: MatrixToAction.JoinRoom) {
        setState {
            copy(startChattingState = Loading())
        }
        viewModelScope.launch {
            try {
                awaitCallback<Unit> {
                    session.joinRoom(action.roomId, null, action.viaServers?.take(3) ?: emptyList(), it)
                }
                _viewEvents.post(MatrixToViewEvents.NavigateToRoom(action.roomId))
            } catch (failure: Throwable) {
                _viewEvents.post(MatrixToViewEvents.ShowModalError(errorFormatter.toHumanReadable(failure)))
            } finally {
                setState {
                    // we can hide this button has we will navigate out
                    copy(startChattingState = Uninitialized)
                }
            }

        }
    }

    private fun handleStartChatting(action: MatrixToAction.StartChattingWithUser) {
        setState {
            copy(startChattingState = Loading())
        }
        viewModelScope.launch {
            val roomId = try {
                directRoomHelper.ensureDMExists(action.matrixItem.id)
            } catch (failure: Throwable) {
                setState {
                    copy(startChattingState = Fail(Exception(stringProvider.getString(R.string.invite_users_to_room_failure))))
                }
                return@launch
            }
            setState {
                // we can hide this button has we will navigate out
                copy(startChattingState = Uninitialized)
            }
            _viewEvents.post(MatrixToViewEvents.NavigateToRoom(roomId))
        }
    }
}
