/*
 * Copyright (c) 2022 New Vector Ltd
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
