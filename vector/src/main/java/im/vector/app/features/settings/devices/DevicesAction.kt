/*
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    data class MarkAsManuallyVerified(val cryptoDeviceInfo: CryptoDeviceInfo) : DevicesAction()

    object SsoAuthDone : DevicesAction()
    data class PasswordAuthDone(val password: String) : DevicesAction()
    object ReAuthCancelled : DevicesAction()
}
