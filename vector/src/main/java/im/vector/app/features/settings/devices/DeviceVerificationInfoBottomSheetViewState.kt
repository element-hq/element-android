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

package im.vector.app.features.settings.devices

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo

data class DeviceVerificationInfoBottomSheetViewState(
        val deviceId: String,
        val cryptoDeviceInfo: Async<CryptoDeviceInfo?> = Uninitialized,
        val deviceInfo: Async<DeviceInfo> = Uninitialized,
        val hasAccountCrossSigning: Boolean = false,
        val accountCrossSigningIsTrusted: Boolean = false,
        val isMine: Boolean = false,
        val hasOtherSessions: Boolean = false,
        val isRecoverySetup: Boolean = false
) : MavericksState {

    constructor(args: DeviceVerificationInfoArgs) : this(deviceId = args.deviceId)

    val canVerifySession = hasOtherSessions || isRecoverySetup
}
