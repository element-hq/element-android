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

package im.vector.app.features.usercode

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.app.R
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.raw.wellknown.getElementWellknown
import im.vector.app.features.raw.wellknown.isE2EByDefault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.permalinks.PermalinkData
import org.matrix.android.sdk.api.session.permalinks.PermalinkParser
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.internal.util.awaitCallback

class UserCodeSharedViewModel @AssistedInject constructor(
        @Assisted val initialState: UserCodeState,
        @Assisted val args: UserCodeActivity.Args,
        private val session: Session,
        private val stringProvider: StringProvider,
        private val rawService: RawService) : VectorViewModel<UserCodeState, UserCodeActions, UserCodeShareViewEvents>(initialState) {

    companion object : MvRxViewModelFactory<UserCodeSharedViewModel, UserCodeState> {
        override fun create(viewModelContext: ViewModelContext, state: UserCodeState): UserCodeSharedViewModel? {
            val args = viewModelContext.args<UserCodeActivity.Args>()
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state, args) ?: error("You should let your activity/fragment implements Factory interface")
        }

        override fun initialState(viewModelContext: ViewModelContext): UserCodeState? {
            return UserCodeState(viewModelContext.args<UserCodeActivity.Args>().userId)
        }
    }

    init {
        val user = session.getUser(args.userId)
        setState {
            copy(
                    matrixItem = user?.toMatrixItem(),
                    shareLink = session.permalinkService().createPermalink(args.userId)
            )
        }
    }

    private fun handleInviteFriend() {
        session.permalinkService().createPermalink(initialState.userId)?.let { permalink ->
            _viewEvents.post(UserCodeShareViewEvents.InviteFriend(permalink))
        }
    }

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: UserCodeState, args: UserCodeActivity.Args): UserCodeSharedViewModel
    }

    override fun handle(action: UserCodeActions) {
        when (action) {
            UserCodeActions.DismissAction -> _viewEvents.post(UserCodeShareViewEvents.Dismiss)
            is UserCodeActions.SwitchMode -> setState { copy(mode = action.mode) }
            is UserCodeActions.DecodedQRCode -> handleQrCodeDecoded(action)
            is UserCodeActions.StartChattingWithUser -> handleStartChatting(action)
        }
    }

    private fun handleStartChatting(withUser: UserCodeActions.StartChattingWithUser) {
        val mxId = withUser.matrixItem.id
        val existing = session.getExistingDirectRoomWithUser(mxId)
        setState {
            copy(mode = UserCodeState.Mode.SHOW)
        }
        if (existing != null) {
            // navigate to this room
            _viewEvents.post(UserCodeShareViewEvents.NavigateToRoom(existing))
        } else {
            // we should create the room then navigate
            _viewEvents.post(UserCodeShareViewEvents.ShowWaitingScreen)
            viewModelScope.launch(Dispatchers.IO) {
                val adminE2EByDefault = rawService.getElementWellknown(session.myUserId)
                        ?.isE2EByDefault()
                        ?: true

                val roomParams = CreateRoomParams()
                        .apply {
                            invitedUserIds.add(mxId)
                            setDirectMessage()
                            enableEncryptionIfInvitedUsersSupportIt = adminE2EByDefault
                        }

                val roomId =
                        try {
                            awaitCallback<String> { session.createRoom(roomParams, it) }
                        } catch (failure: Throwable) {
                            _viewEvents.post(UserCodeShareViewEvents.ToastMessage(stringProvider.getString(R.string.invite_users_to_room_failure)))
                            return@launch
                        } finally {
                            _viewEvents.post(UserCodeShareViewEvents.HideWaitingScreen)
                        }
                _viewEvents.post(UserCodeShareViewEvents.NavigateToRoom(roomId))
            }
        }
    }

    private fun handleQrCodeDecoded(action: UserCodeActions.DecodedQRCode) {
        val linkedId = PermalinkParser.parse(action.code)
        if (linkedId is PermalinkData.FallbackLink) {
            _viewEvents.post(UserCodeShareViewEvents.ToastMessage(stringProvider.getString(R.string.not_a_valid_qr_code)))
            return
        }
        _viewEvents.post(UserCodeShareViewEvents.ShowWaitingScreen)
        viewModelScope.launch(Dispatchers.IO) {
            when (linkedId) {
                is PermalinkData.RoomLink -> TODO()
                is PermalinkData.UserLink -> {
                    var user = session.getUser(linkedId.userId) ?: awaitCallback<List<User>> {
                        session.searchUsersDirectory(linkedId.userId, 10, emptySet(), it)
                    }.firstOrNull { it.userId == linkedId.userId }
                    // Create raw Uxid in case the user is not searchable
                    ?: User(linkedId.userId, null, null)

                    setState {
                        copy(
                                mode = UserCodeState.Mode.RESULT(user.toMatrixItem())
                        )
                    }
                }
                is PermalinkData.GroupLink -> TODO()
                is PermalinkData.FallbackLink -> TODO()
            }
            _viewEvents.post(UserCodeShareViewEvents.HideWaitingScreen)
        }
    }
}
