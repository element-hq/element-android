/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.pin.lockscreen.ui

import androidx.fragment.app.FragmentActivity
import im.vector.app.core.platform.VectorViewModelAction

sealed class LockScreenAction : VectorViewModelAction {
    data class PinCodeEntered(val value: String) : LockScreenAction()
    data class ShowBiometricPrompt(val callingActivity: FragmentActivity) : LockScreenAction()
    object OnUIReady : LockScreenAction()
}
