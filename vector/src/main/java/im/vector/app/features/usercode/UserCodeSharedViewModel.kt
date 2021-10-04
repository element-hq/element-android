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
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.assisted.AssistedFactory
import im.vector.app.R
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.createdirect.DirectRoomHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.permalinks.PermalinkData
import org.matrix.android.sdk.api.session.permalinks.PermalinkParser
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.toMatrixItem

class UserCodeSharedViewModel @AssistedInject constructor(
        @Assisted val initialState: UserCodeState,
        private val session: Session,
        private val stringProvider: StringProvider,
        private val directRoomHelper: DirectRoomHelper,
        private val rawService: RawService) : VectorViewModel<UserCodeState, UserCodeActions, UserCodeShareViewEvents>(initialState) {

    companion object : MvRxViewModelFactory<UserCodeSharedViewModel, UserCodeState> {
        override fun create(viewModelContext: ViewModelContext, state: UserCodeState): UserCodeSharedViewModel? {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    init {
        val user = session.getUser(initialState.userId)
        setState {
            copy(
                    matrixItem = user?.toMatrixItem(),
                    shareLink = session.permalinkService().createPermalink(initialState.userId)
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(initialState: UserCodeState): UserCodeSharedViewModel
    }

    override fun handle(action: UserCodeActions) {
        when (action) {
            UserCodeActions.DismissAction -> _viewEvents.post(UserCodeShareViewEvents.Dismiss)
            is UserCodeActions.SwitchMode -> setState { copy(mode = action.mode) }
            is UserCodeActions.DecodedQRCode -> handleQrCodeDecoded(action)
            is UserCodeActions.StartChattingWithUser -> handleStartChatting(action)
            is UserCodeActions.CameraPermissionNotGranted -> _viewEvents.post(UserCodeShareViewEvents.CameraPermissionNotGranted(action.deniedPermanently))
            UserCodeActions.ShareByText -> handleShareByText()
        }
    }

    private fun handleShareByText() {
        session.permalinkService().createPermalink(session.myUserId)?.let { permalink ->
            val text = stringProvider.getString(R.string.invite_friends_text, permalink)
            _viewEvents.post(UserCodeShareViewEvents.SharePlainText(
                    text,
                    stringProvider.getString(R.string.invite_friends),
                    stringProvider.getString(R.string.invite_friends_rich_title)
            ))
        }
    }

    private fun handleStartChatting(withUser: UserCodeActions.StartChattingWithUser) {
        val mxId = withUser.matrixItem.id
        setState {
            copy(mode = UserCodeState.Mode.SHOW)
        }
        _viewEvents.post(UserCodeShareViewEvents.ShowWaitingScreen)
        viewModelScope.launch(Dispatchers.IO) {
            val roomId = try {
                directRoomHelper.ensureDMExists(mxId)
            } catch (failure: Throwable) {
                _viewEvents.post(UserCodeShareViewEvents.ToastMessage(stringProvider.getString(R.string.invite_users_to_room_failure)))
                return@launch
            } finally {
                _viewEvents.post(UserCodeShareViewEvents.HideWaitingScreen)
            }
            _viewEvents.post(UserCodeShareViewEvents.NavigateToRoom(roomId))
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
                is PermalinkData.RoomLink -> {
                    // not yet supported
                    _viewEvents.post(UserCodeShareViewEvents.ToastMessage(stringProvider.getString(R.string.not_implemented)))
                }
                is PermalinkData.UserLink -> {
                    val user = tryOrNull { session.resolveUser(linkedId.userId) }
                    // Create raw Uxid in case the user is not searchable
                            ?: User(linkedId.userId, null, null)

                    setState {
                        copy(
                                mode = UserCodeState.Mode.RESULT(user.toMatrixItem(), action.code)
                        )
                    }
                }
                is PermalinkData.GroupLink -> {
                    // not yet supported
                    _viewEvents.post(UserCodeShareViewEvents.ToastMessage(stringProvider.getString(R.string.not_implemented)))
                }
                is PermalinkData.FallbackLink -> {
                    // not yet supported
                    _viewEvents.post(UserCodeShareViewEvents.ToastMessage(stringProvider.getString(R.string.not_implemented)))
                }
            }
            _viewEvents.post(UserCodeShareViewEvents.HideWaitingScreen)
        }
    }
}
