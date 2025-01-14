/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.quads

import im.vector.app.core.platform.VectorViewEvents
import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.core.platform.WaitingViewData

sealed class SharedSecureStorageAction : VectorViewModelAction {
    object UseKey : SharedSecureStorageAction()
    object Back : SharedSecureStorageAction()
    object Cancel : SharedSecureStorageAction()
    data class SubmitPassphrase(val passphrase: String) : SharedSecureStorageAction()
    data class SubmitKey(val recoveryKey: String) : SharedSecureStorageAction()
    object ForgotResetAll : SharedSecureStorageAction()
    object DoResetAll : SharedSecureStorageAction()
}

sealed class SharedSecureStorageViewEvent : VectorViewEvents {

    object Dismiss : SharedSecureStorageViewEvent()
    data class FinishSuccess(val cypherResult: String) : SharedSecureStorageViewEvent()
    data class Error(val message: String, val dismiss: Boolean = false) : SharedSecureStorageViewEvent()
    data class InlineError(val message: String) : SharedSecureStorageViewEvent()
    data class KeyInlineError(val message: String) : SharedSecureStorageViewEvent()
    object ShowModalLoading : SharedSecureStorageViewEvent()
    object HideModalLoading : SharedSecureStorageViewEvent()
    data class UpdateLoadingState(val waitingData: WaitingViewData) : SharedSecureStorageViewEvent()
    object ShowResetBottomSheet : SharedSecureStorageViewEvent()
}
