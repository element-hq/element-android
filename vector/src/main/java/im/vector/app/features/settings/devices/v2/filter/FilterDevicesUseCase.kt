/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.filter

import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import im.vector.app.features.settings.devices.v2.verification.CurrentSessionCrossSigningInfo
import org.matrix.android.sdk.api.extensions.orFalse
import javax.inject.Inject

class FilterDevicesUseCase @Inject constructor() {

    fun execute(
            currentSessionCrossSigningInfo: CurrentSessionCrossSigningInfo,
            devices: List<DeviceFullInfo>,
            filterType: DeviceManagerFilterType,
            excludedDeviceIds: List<String> = emptyList(),
    ): List<DeviceFullInfo> {
        val isCurrentSessionVerified = currentSessionCrossSigningInfo.isCrossSigningVerified.orFalse()
        return devices
                .filter {
                    when (filterType) {
                        DeviceManagerFilterType.ALL_SESSIONS -> true
                        // when current session is not verified, other session status cannot be trusted
                        DeviceManagerFilterType.VERIFIED -> isCurrentSessionVerified && it.cryptoDeviceInfo?.trustLevel?.isCrossSigningVerified().orFalse()
                        // when current session is not verified, other session status cannot be trusted
                        DeviceManagerFilterType.UNVERIFIED ->
                            (isCurrentSessionVerified && !it.cryptoDeviceInfo?.trustLevel?.isCrossSigningVerified().orFalse()) ||
                                    it.cryptoDeviceInfo == null
                        DeviceManagerFilterType.INACTIVE -> it.isInactive
                    }
                }
                .filter { it.deviceInfo.deviceId !in excludedDeviceIds }
    }
}
