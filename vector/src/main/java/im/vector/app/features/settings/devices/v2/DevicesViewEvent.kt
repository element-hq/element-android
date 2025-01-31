/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2

import im.vector.app.core.platform.VectorViewEvents
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo

sealed class DevicesViewEvent : VectorViewEvents {
    data class Loading(val message: CharSequence? = null) : DevicesViewEvent()
    data class Failure(val throwable: Throwable) : DevicesViewEvent()
    data class RequestReAuth(val registrationFlowResponse: RegistrationFlowResponse, val lastErrorCode: String?) : DevicesViewEvent()
    data class PromptRenameDevice(val deviceInfo: DeviceInfo) : DevicesViewEvent()
    data class ShowVerifyDevice(val userId: String, val transactionId: String?) : DevicesViewEvent()
    data class SelfVerification(val session: Session) : DevicesViewEvent()
    data class ShowManuallyVerify(val cryptoDeviceInfo: CryptoDeviceInfo) : DevicesViewEvent()
    object PromptResetSecrets : DevicesViewEvent()
}
