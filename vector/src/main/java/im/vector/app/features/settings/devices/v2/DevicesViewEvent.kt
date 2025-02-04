/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2

import im.vector.app.core.platform.VectorViewEvents
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo

sealed class DevicesViewEvent : VectorViewEvents {
    data class RequestReAuth(
            val registrationFlowResponse: RegistrationFlowResponse,
            val lastErrorCode: String?
    ) : DevicesViewEvent()

    data class ShowVerifyDevice(val userId: String, val transactionId: String?) : DevicesViewEvent()
    object SelfVerification : DevicesViewEvent()
    data class ShowManuallyVerify(val cryptoDeviceInfo: CryptoDeviceInfo) : DevicesViewEvent()
    object PromptResetSecrets : DevicesViewEvent()
    object SignoutSuccess : DevicesViewEvent()
    data class SignoutError(val error: Throwable) : DevicesViewEvent()
}
