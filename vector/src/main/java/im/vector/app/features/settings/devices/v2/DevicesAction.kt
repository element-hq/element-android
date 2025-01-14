/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2

import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo

sealed class DevicesAction : VectorViewModelAction {
    // ReAuth
    object SsoAuthDone : DevicesAction()
    data class PasswordAuthDone(val password: String) : DevicesAction()
    object ReAuthCancelled : DevicesAction()

    // Others
    object VerifyCurrentSession : DevicesAction()
    data class MarkAsManuallyVerified(val cryptoDeviceInfo: CryptoDeviceInfo) : DevicesAction()
    object MultiSignoutOtherSessions : DevicesAction()
    object ToggleIpAddressVisibility : DevicesAction()
}
