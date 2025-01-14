/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices

import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo

sealed class DevicesAction : VectorViewModelAction {
    object Refresh : DevicesAction()
    data class Delete(val deviceId: String) : DevicesAction()

    //    data class Password(val password: String) : DevicesAction()
    data class Rename(val deviceId: String, val newName: String) : DevicesAction()

    data class PromptRename(val deviceId: String) : DevicesAction()
    data class VerifyMyDevice(val deviceId: String) : DevicesAction()
    data class VerifyMyDeviceManually(val deviceId: String) : DevicesAction()
    object CompleteSecurity : DevicesAction()
    object ResetSecurity : DevicesAction()
    data class MarkAsManuallyVerified(val cryptoDeviceInfo: CryptoDeviceInfo) : DevicesAction()

    object SsoAuthDone : DevicesAction()
    data class PasswordAuthDone(val password: String) : DevicesAction()
    object ReAuthCancelled : DevicesAction()
}
