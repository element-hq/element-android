/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.usercode

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.createdirect.DirectRoomHelper
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getUserOrDefault
import org.matrix.android.sdk.api.session.permalinks.PermalinkData
import org.matrix.android.sdk.api.session.permalinks.PermalinkParser
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.toMatrixItem

class UserCodeSharedViewModel @AssistedInject constructor(
        @Assisted val initialState: UserCodeState,
        private val session: Session,
        private val stringProvider: StringProvider,
        private val directRoomHelper: DirectRoomHelper,
) : VectorViewModel<UserCodeState, UserCodeActions, UserCodeShareViewEvents>(initialState) {

    companion object : MavericksViewModelFactory<UserCodeSharedViewModel, UserCodeState> by hiltMavericksViewModelFactory()

    init {
        val user = session.getUserOrDefault(initialState.userId)
        setState {
            copy(
                    matrixItem = user.toMatrixItem(),
                    shareLink = session.permalinkService().createPermalink(initialState.userId)
            )
        }
    }

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<UserCodeSharedViewModel, UserCodeState> {
        override fun create(initialState: UserCodeState): UserCodeSharedViewModel
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
            val text = stringProvider.getString(CommonStrings.invite_friends_text, permalink)
            _viewEvents.post(
                    UserCodeShareViewEvents.SharePlainText(
                            text,
                            stringProvider.getString(CommonStrings.invite_friends),
                            stringProvider.getString(CommonStrings.invite_friends_rich_title)
                    )
            )
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
                _viewEvents.post(UserCodeShareViewEvents.ToastMessage(stringProvider.getString(CommonStrings.invite_users_to_room_failure)))
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
            _viewEvents.post(UserCodeShareViewEvents.ToastMessage(stringProvider.getString(CommonStrings.not_a_valid_qr_code)))
            return
        }
        _viewEvents.post(UserCodeShareViewEvents.ShowWaitingScreen)
        viewModelScope.launch(Dispatchers.IO) {
            when (linkedId) {
                is PermalinkData.RoomLink -> {
                    // not yet supported
                    _viewEvents.post(UserCodeShareViewEvents.ToastMessage(stringProvider.getString(CommonStrings.not_implemented)))
                }
                is PermalinkData.UserLink -> {
                    val user = tryOrNull { session.userService().resolveUser(linkedId.userId) }
                    // Create raw Uxid in case the user is not searchable
                            ?: User(linkedId.userId, null, null)

                    setState {
                        copy(
                                mode = UserCodeState.Mode.RESULT(user.toMatrixItem(), action.code)
                        )
                    }
                }
                is PermalinkData.RoomEmailInviteLink,
                is PermalinkData.FallbackLink -> {
                    // not yet supported
                    _viewEvents.post(UserCodeShareViewEvents.ToastMessage(stringProvider.getString(CommonStrings.not_implemented)))
                }
            }
            _viewEvents.post(UserCodeShareViewEvents.HideWaitingScreen)
        }
    }
}
