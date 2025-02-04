/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
